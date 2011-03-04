package j2arduino.devices;

import javax.bluetooth.*;
import java.io.IOException;
import java.util.HashSet;

public class ArduinoBTKind implements DiscoveryListener, ArduinoKind{

public static final byte BT_RETRIES = 2;
public static final String BT_PREFIX = "arduinobt";
public final byte INQUIRY_IDLE = -1;
public static final byte INQUIRY_IN_PROGRESS = -2;

/** Object to synchronize discovery thread (JSR-82) with the thread calling #getAvailableArduinos. */
private final Object discoveryLock = new Object();
/**
 Indicates the discovery state. -1 is idle, -2 it in progress, >= is according to {@link javax.bluetooth.DiscoveryListener DiscoveryListener's
constants}.
 */
private byte discoveryState = INQUIRY_IDLE;
private final HashSet<Arduino> arduinos = new HashSet<Arduino>(1);

public ArduinoBTKind(){
}

@Override
public boolean isAvailable(){
	try{
		if(Class.forName("javax.bluetooth.LocalDevice") != null){
			return javax.bluetooth.LocalDevice.isPowerOn();
		}
	} catch(ClassNotFoundException e){
		e.printStackTrace();
	}
	return false;
}

/**
 Returns a set of all reachable devices which BT-user-friendly name is starting with "ARDUINOBT" (case insensitive).

 @throws javax.bluetooth.BluetoothStateException
 if the Bluetooth device does not allow an inquiry to be started or the inquiry failed. */
@Override
public HashSet<Arduino> getAvailableArduinos(boolean updateNow) throws IOException, InterruptedException{
	DiscoveryAgent agent = LocalDevice.getLocalDevice().getDiscoveryAgent();
	synchronized(discoveryLock){
		agent.cancelInquiry(this); // buggy with bluecove 2.0.2 and a widcomm stack under win2k (cancels an ongoing inquiry, but does work the first time only)
		agent.startInquiry(DiscoveryAgent.GIAC, this);
		discoveryState = INQUIRY_IN_PROGRESS;
		while(true){
			discoveryLock.wait();
			// we need to check an appropriate condition to counter spurious interrupts
			if(discoveryState == DiscoveryListener.INQUIRY_COMPLETED){
				discoveryState = INQUIRY_IDLE;
				break;
			}
			if(discoveryState != INQUIRY_IN_PROGRESS){
				discoveryState = INQUIRY_IDLE;
				throw new BluetoothStateException("Inquiry request failed");
			} // ... else spurious interrupt
		}
	}
	return arduinos;
}

@Override
public void deviceDiscovered(RemoteDevice btDevice, DeviceClass deviceClass){
	String address = btDevice.getBluetoothAddress().toUpperCase();
	for(int i = 1; i <= BT_RETRIES; i++){
		try{
			String name = btDevice.getFriendlyName(false);
			if(name != null && name.startsWith(BT_PREFIX)){
				synchronized(arduinos){
					Arduino arduino = new ArduinoBT(address, name);
					arduinos.add(arduino);
				}
			}
			return;
		} catch(IOException ignored){
		}
	}
}

@Override
public void servicesDiscovered(int i, ServiceRecord[] serviceRecords){
}

@Override
public void serviceSearchCompleted(int i, int i1){
}

@Override
public void inquiryCompleted(int i){
	synchronized(discoveryLock){
		discoveryState = (byte)i;
		discoveryLock.notify();
	}
}
}