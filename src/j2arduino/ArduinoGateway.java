package j2arduino;

import javax.bluetooth.*;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/** Serves as gateway between java and the c code on the Atmega. */
public class ArduinoGateway implements DiscoveryListener, ArduinoActivityListener{
// singleton stuff
//@{
private static ArduinoGateway gateway = null;
private static final byte BT_RETRIES = 2;

/** This constructor should only be called from within getInstance or subclasses. */
protected ArduinoGateway(){
}

/**
 Singleton factory method: creates the gateway, if it does not exist yet.

 @return The gateway object. Multiple calls return the same object. */
synchronized public static ArduinoGateway getInstance(){
	if(gateway == null){
		gateway = new ArduinoGateway();
	}
	return gateway;
}
//@}

/** Object to synchronize discovery thread (JSR-82) with the thread calling #getAvailableArduinos. */
private final Object discoveryLock = new Object();
/** Indicates the discovery state.
 -1 is idle, -2 it in progress, >= is according to DiscoveryListener's constants.*/
private byte discoveryState = -1;
private final Hashtable arduinos = new Hashtable(1);
private final Vector listeners = new Vector(1);

/**
 searches for available Arduinos in range.

 An Arduino is identified by its BT-user-friendly name starting with "ARDUINOBT" (case insensitive).

 @param updateNow forces a search for new devices and invalidates the cache of known Arduinos.
 @return A Enumeration containing all Arduinos found.
 @throws javax.bluetooth.BluetoothStateException
 if the Bluetooth system could not be initialized or if the Bluetooth device does not allow an inquiry to be started.
 @throws InterruptedException if the calling thread is interrupted before discovery has completed. */
public Enumeration getAvailableArduinos(boolean updateNow) throws BluetoothStateException, InterruptedException{
	synchronized(discoveryLock){
		if(discoveryState == -1){ // only try to start a new inquiry, if there is non in progress
			synchronized(arduinos){
				if(!updateNow && !arduinos.isEmpty()){
					return arduinos.elements();
				}
				Enumeration curArduino = arduinos.elements();
				while(curArduino.hasMoreElements()){
					Arduino a = (Arduino)curArduino.nextElement();
					if(!a.isConnected()){
						arduinos.remove(a.address);
					}
				}
			}
			LocalDevice dev = LocalDevice.getLocalDevice();
			DiscoveryAgent agent = dev.getDiscoveryAgent();
			agent.cancelInquiry(this); // buggy with bluecove 2.0.2 and a widcomm stack under win2k (cancels an ongoing inquiry, but does work the first time only)
			fireActivityListeners(ArduinoActivityListener.STATE_ACTIVE, null);
			try{
				agent.startInquiry(DiscoveryAgent.GIAC, this);
				discoveryState = -2;
				while(true){
					discoveryLock.wait();
					// we need to check an appropriate condition to counter spurious interrupts
					if(discoveryState == DiscoveryListener.INQUIRY_COMPLETED){
						System.out.println("completed");
						discoveryState = -1;
						break;
					}
					if(discoveryState != -2){
						discoveryState = -1;
						throw new BluetoothStateException("Inquiry request failed");
					} // ... else spurious interrupt
				}
			} finally{
				fireActivityListeners(ArduinoActivityListener.STATE_INACTIVE, null);

			}
		}
	}
	return arduinos.elements();
}

/**
 Returns the Arduino object that represents the Arduino with the same address as parameter \a address.

 @param address the address of the Arduino
 @return the corresponding Arduino, or null if there is no Arduino known with that address */
public Arduino get(String address){
	return (Arduino)arduinos.get(address.toUpperCase());
}

/**
 Attaches an ArduinoActivityListener to this gateway.

 Attached listeners will be called back, whenever there is any BT activity known to this gateway. This includes device discovery as done by {@link
#getAvailableArduinos(boolean)} as well as any BT activity done in Arduino instances managed by this gateway.

 @param l a listener to attach */
public void addActivityListener(ArduinoActivityListener l){
	listeners.addElement(l);
}

/**
 Removes an ArduinoActivityListener from this gateway.

 @param l the listener to be removed
 @see #addActivityListener(ArduinoActivityListener) */
public void removeActivityListener(ArduinoActivityListener l){
	listeners.removeElement(l);
}

/**
 Informs listeners about connection state changes.

 @param newState the new connection state
 @param arduino  the Arduino instance related to the change, or null if the change is not related to a specific Arduino
 @see #addActivityListener(ArduinoActivityListener) */
private void fireActivityListeners(int newState, Arduino arduino){
	Enumeration enumeration = listeners.elements();
	while(enumeration.hasMoreElements()){
		ArduinoActivityListener listener = (ArduinoActivityListener)enumeration.nextElement();
		listener.connectionStateChanged(newState, arduino);
	}
}

/**
 Creates a Arduino instance and adds it to the gateway's database.

 This can be used to preset Arduinos (e.g. to speed up user interaction, when remote devices are known and discovery can be skipped).

 @param address the new Arduino's address
 @return the new Arduino instance */
public Arduino addArduino(String address){
	address = address.toUpperCase();
	Arduino arduino = null;
	synchronized(arduinos){
		if(!arduinos.containsKey(address)){
			arduino = new Arduino(address, "<preset>");
			arduinos.put(address, arduino);
			arduino.addActivityListener(this);
		}
	}
	return arduino;
}

public void deviceDiscovered(RemoteDevice btDevice, DeviceClass deviceClass){
	String address = btDevice.getBluetoothAddress().toUpperCase();
	for(int i = 1; i <= BT_RETRIES; i++){
		try{
			String name = btDevice.getFriendlyName(false);
			if("arduinobt".regionMatches(true, 0, name, 0, 9)){
				synchronized(arduinos){
					if(!arduinos.containsKey(address)){
						Arduino arduino = new Arduino(address, name);
						arduinos.put(address, arduino);
						arduino.addActivityListener(this);
					}
				}
			}
			return;
		} catch(IOException ignored){
		}
	}
}

public void servicesDiscovered(int i, ServiceRecord[] serviceRecords){
}

public void serviceSearchCompleted(int i, int i1){
}

public void inquiryCompleted(int i){
	synchronized(discoveryLock){
		discoveryState = (byte)i;
		discoveryLock.notify();
	}
}

public void connectionStateChanged(int state, Arduino arduino){
	fireActivityListeners(state, arduino);
}
}
