package j2arduino;

import j2arduino.devices.Arduino;
import j2arduino.devices.ArduinoKind;
import j2arduino.util.J2ArduinoSettings;

import javax.usb.UsbException;
import java.io.IOException;
import java.util.*;

/** Serves as gateway between java and the c code on the microcontroller. */
public class ArduinoGateway implements ArduinoActivityListener{

private final HashMap<String, Arduino> arduinos = new HashMap<String, Arduino>(1);
private final Set<ArduinoActivityListener> listeners = new HashSet<ArduinoActivityListener>(2);
private final Object discoveryLock = new Object();
/** Stores kinds of arduinos which are currently enabled and available (according to their {@link j2arduino.devices.ArduinoKind#isAvailable()} method. */
private final ArduinoKind[] availableKinds;

// singleton stuff
//@{
private static ArduinoGateway gateway = null;
public static final String J2ARDUINO_KINDS = "j2arduino.kinds";

/** This constructor should only be called from within getInstance or subclasses. */
private ArduinoGateway() throws IOException{
	final String s = J2ArduinoSettings.getSetting(J2ARDUINO_KINDS);
	if(s == null){
		availableKinds = new ArduinoKind[0];
		System.err.println("There were no Arduino kinds specified. This is probably wrong.");
		return;
	}

	String[] providers = s.split("[\\s,]+");
	List<ArduinoKind> kinds = new LinkedList<ArduinoKind>();
	for(String p : providers){
		if(p.isEmpty())
			continue;
		int i = 0;
	 loop:
		while(true){
			String className;
			switch(i++){
				case 0:
					className = "j2arduino.devices.Arduino" + p + "Kind";
					break;
				case 1:
					className = "j2arduino.devices.Arduino" + p.toUpperCase() + "Kind";
					break;
				case 2:
					className = "j2arduino.devices.Arduino" + p.toLowerCase() + "Kind";
					break;
				default:
					break loop;
			}
			try{
				Class c = Class.forName(className);
				if(ArduinoKind.class.isAssignableFrom(c)){
					ArduinoKind kind = (ArduinoKind)c.newInstance();
					System.err.print("Loaded " + c + " successfully ");
					if(kind.isAvailable()){
						kinds.add(kind);
						System.err.println("and it seems functional.");
					} else
						System.err.println("but it seems NOT to be functional.");
					break loop;
				}
			} catch(ClassNotFoundException e){
				System.err.println("Could not find a class named: " + className);
			} catch(InstantiationException e){
				System.err.println("Could not instantiate a class named: " + className);
				System.err.println("This means it is not an instantiable Class or it has no nullary constructor.");
				System.err.println("Please report this to the author of " + className + '!');
				e.printStackTrace();
			} catch(IllegalAccessException e){
				System.err.println("Could not instantiate a class named: " + className);
				System.err.println("This means we don't have access to its constructor.");
				System.err.println("Please report this to the author of " + className + '!');
				e.printStackTrace();
			}
		}
	}
	availableKinds = kinds.toArray(new ArduinoKind[kinds.size()]);
}

/**
 Singleton factory method: creates the gateway, if it does not exist yet.

 @return The gateway object. Multiple calls return the same object. */
synchronized public static ArduinoGateway getInstance() throws IOException{
	if(gateway == null){
		gateway = new ArduinoGateway();
	}
	return gateway;
}
//@}

/**
 Returns available Arduinos.
 <p/>
 This will start a discovery process on all available kinds of busses using {@link j2arduino.devices.ArduinoKind#getAvailableArduinos(boolean)}.
 Each
 kind decides if an evaluated device is usable by different characteristics (i.e. usually constant fields provided by the underlying protocols).

 @param updateNow forces a search for new devices and invalidates the cache of known Arduinos.
 @return A collection containing all Arduinos found (usually without duplicates).
 @throws InterruptedException if the calling thread is interrupted before discovery has completed. */
public Collection<Arduino> getAvailableArduinos(boolean updateNow) throws IOException, InterruptedException, UsbException{
	synchronized(discoveryLock){
		synchronized(arduinos){
			if(!updateNow && !arduinos.isEmpty()){
				return arduinos.values();
			}
			for(Arduino a : arduinos.values())
				if(!a.isConnected()){
					arduinos.remove(a.address);
				}
		}
		fireActivityListeners(ArduinoActivityListener.STATE_ACTIVE, null);
		try{
			for(ArduinoKind k : availableKinds){
				for(Arduino a : k.getAvailableArduinos(updateNow)){
					arduinos.put(a.address, a);
				}
			}
		} finally{
			fireActivityListeners(ArduinoActivityListener.STATE_INACTIVE, null);
		}
	}

	return arduinos.values();
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
 <p/>
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
 Adds an Arduino to the gateway's database.
 <p/>
 This can be used to preset Arduinos (e.g. to speed up user interaction, when remote devices are known and discovery can be skipped).

 @param arduino the arduino to be added */
public void addArduino(Arduino arduino){
	String address = arduino.address.toUpperCase();
	synchronized(arduinos){
		if(!arduinos.containsKey(address)){
			arduinos.put(address, arduino);
			arduino.addActivityListener(this);
		}
	}
}

@Override
public void connectionStateChanged(int state, Arduino arduino){
	fireActivityListeners(state, arduino);
}
}
