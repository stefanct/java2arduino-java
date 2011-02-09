package j2arduino;

import javax.bluetooth.*;
import javax.usb.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/** Serves as gateway between java and the c code on the Atmega. */
public class ArduinoGateway implements DiscoveryListener, ArduinoActivityListener{
// singleton stuff
//@{
private static ArduinoGateway gateway = null;
private static final byte BT_RETRIES = 2;
private boolean hasUSB = false;
private boolean hasBT = false;
public static final String USB_MANUF = "ims.tuwien.ac.at";
public static final String USB_PROD = "USB Board";

/** This constructor should only be called from within getInstance or subclasses. */
protected ArduinoGateway(){
	try{
		if(Class.forName("javax.bluetooth.LocalDevice") != null){
			hasBT = javax.bluetooth.LocalDevice.isPowerOn();
		}
	} catch(Throwable e){
		e.printStackTrace();
	}
	try{
		if(Class.forName("javax.usb.UsbDevice") != null){
			UsbHostManager.getUsbServices().getRootUsbHub();
			hasUSB = true;
		}
	} catch(Throwable e){
		e.printStackTrace();
	}
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
/** Indicates the discovery state. -1 is idle, -2 it in progress, >= is according to DiscoveryListener's constants. */
private byte discoveryState = -1;
private final HashMap<String, Arduino> arduinos = new HashMap<String, Arduino>(1);
private final Collection<ArduinoActivityListener> listeners = new ArrayList<ArduinoActivityListener>(1);

/**
 searches for available Arduinos in range.

 An Arduino is identified by its BT-user-friendly name starting with "ARDUINOBT" (case insensitive).

 @param updateNow forces a search for new devices and invalidates the cache of known Arduinos.
 @return A Enumeration containing all Arduinos found.
 @throws javax.bluetooth.BluetoothStateException
 if the Bluetooth system could not be initialized or if the Bluetooth device does not allow an inquiry to be started.
 @throws InterruptedException if the calling thread is interrupted before discovery has completed. */
public Collection<Arduino> getAvailableArduinos(boolean updateNow) throws BluetoothStateException, InterruptedException, UsbException{
	synchronized(discoveryLock){
		if(discoveryState == -1){ // only try to start a new inquiry, if there is non in progress
			synchronized(arduinos){
				if(!updateNow && !arduinos.isEmpty()){
					return arduinos.values();
				}
				for(Arduino a : arduinos.values())
					if(!a.isConnected()){
						arduinos.remove(a.address);
					}
			}
		}
		fireActivityListeners(ArduinoActivityListener.STATE_ACTIVE, null);
		try{
			if(hasBT){
				DiscoveryAgent agent = LocalDevice.getLocalDevice().getDiscoveryAgent();
				agent.cancelInquiry(this); // buggy with bluecove 2.0.2 and a widcomm stack under win2k (cancels an ongoing inquiry, but does work the first time only)
				agent.startInquiry(DiscoveryAgent.GIAC, this);
				discoveryState = -2;
				while(true){
					discoveryLock.wait();
					// we need to check an appropriate condition to counter spurious interrupts
					if(discoveryState == DiscoveryListener.INQUIRY_COMPLETED){
						discoveryState = -1;
						break;
					}
					if(discoveryState != -2){
						discoveryState = -1;
						throw new BluetoothStateException("Inquiry request failed");
					} // ... else spurious interrupt
				}
			}
			if(hasUSB){
				try{
					UsbHub rootUsbHub = UsbHostManager.getUsbServices().getRootUsbHub();
					addAttachedUsbDevices(rootUsbHub);
				} catch(SecurityException ignored){ // thrown if access to the USB subsystem is not allowed
				} catch(UnsupportedEncodingException ignored){
				}

			}
		} finally{
			fireActivityListeners(ArduinoActivityListener.STATE_INACTIVE, null);
		}
	}

	return arduinos.values();
}

private void addAttachedUsbDevices(UsbHub usbHub) throws UsbException, UnsupportedEncodingException{
	for(UsbDevice dev : usbHub.getAttachedUsbDevices()){
		if(dev.isUsbHub()){
			addAttachedUsbDevices((UsbHub)dev);
		} else{
			UsbDeviceDescriptor devDescriptor = dev.getUsbDeviceDescriptor();
			if(devDescriptor != null && devDescriptor.bDeviceClass() == (byte)0xff){
				String manuf = dev.getManufacturerString();
				String prod = dev.getProductString();
				if(manuf != null && prod != null && manuf.equals(USB_MANUF) && prod.equals(USB_PROD)){
					UsbConfiguration config = dev.getActiveUsbConfiguration();
					UsbInterface usbIf = config.getUsbInterface((byte)0);
					if(usbIf != null){
						UsbInterfaceDescriptor usbIfDesc = usbIf.getUsbInterfaceDescriptor();
						if(config.isActive() && usbIfDesc.bInterfaceClass() == (byte)0xFF && usbIfDesc.bInterfaceSubClass() == (byte)0xDE
						   && usbIfDesc.bInterfaceProtocol() == (byte)0xAD && usbIf.getUsbEndpoint(Arduino.USB_OUT_EPNUM) != null
						   && usbIf.getUsbEndpoint(Arduino.USB_IN_EPNUM) != null){
							String address = "USB-" + dev.getSerialNumberString();
							synchronized(arduinos){
								if(!arduinos.containsKey(address)){
									Arduino arduino = new Arduino(address, address, dev);
									arduinos.put(address, arduino);
									arduino.addActivityListener(this);
								}
							}
						}
					}
				}
			}
		}
	}
}

/**
 Returns the Arduino object that represents the Arduino with the same address as parameter \a address.

 @param address the address of the Arduino
 @return the corresponding Arduino, or null if there is no Arduino known with that address */
public Arduino get(String address){
	return arduinos.get(address.toUpperCase());
}

/**
 Attaches an ArduinoActivityListener to this gateway.

 Attached listeners will be called back, whenever there is any BT activity known to this gateway. This includes device discovery as done by {@link
#getAvailableArduinos(boolean)} as well as any BT activity done in Arduino instances managed by this gateway.

 @param l a listener to attach */
public void addActivityListener(ArduinoActivityListener l){
	listeners.add(l);
}

/**
 Removes an ArduinoActivityListener from this gateway.

 @param l the listener to be removed
 @see #addActivityListener(ArduinoActivityListener) */
public void removeActivityListener(ArduinoActivityListener l){
	listeners.remove(l);
}

/**
 Informs listeners about connection state changes.

 @param newState the new connection state
 @param arduino  the Arduino instance related to the change, or null if the change is not related to a specific Arduino
 @see #addActivityListener(ArduinoActivityListener) */
private void fireActivityListeners(int newState, Arduino arduino){
	for(ArduinoActivityListener listener : listeners){
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

@Override
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

@Override
public void connectionStateChanged(int state, Arduino arduino){
	fireActivityListeners(state, arduino);
}
}
