package j2arduino;

import j2arduino.devices.Arduino;

import java.io.IOException;
import java.util.*;

/**
 Represents a mapping between function names that can be called on remote Arduinos and their offsets.
 <p/>
 If enabled at the remote device and not overridden by a constant default, a mapping between (the string representation of) the callable
 functions and their offset in a function pointer array (located at the device) will be read upon connecting to it.
 */
public class ArduinoFunctionMapping{

/** The default offset of the remote function (usually "a2jGetMapping"), that provides the information necessary for this class. */
public final static byte defaultOffset = 0;
private final Hashtable<String, Byte> ht;
private byte funcOffset;

/**
 Creates an initially empty mapping.

 @see ArduinoFunctionMapping#ArduinoFunctionMapping(java.util.Hashtable) */
protected ArduinoFunctionMapping(){
	this(null);
}

/**
 Creates a mapping.
 <p/>
 If mapping is not null, it represents a fixed mapping used in the whole live of this object. Fetching remote mappings will be disabled. If mapping
 is null, an empty mapping is created, which can be populated later by calling {@link #fetch}.

 @param mapping the fixed mapping or null */
public ArduinoFunctionMapping(Hashtable mapping){
	if(mapping != null){
		int size = mapping.size();
		if(size > 256)
			throw new IllegalArgumentException("ArduinoFunctionMapping does not allow more than 256 entries");

		// we could take 'mapping' as our ht but it should not be accessible directly from outside -> copy contents
		Iterator iterator = mapping.keySet().iterator();
		ht = new Hashtable<String, Byte>(size);
		for(int i = 0; i < size; i++){
			String key = (String)iterator.next();
			Byte val = (Byte)mapping.get(key);
			ht.put(key, val);
		}
		funcOffset = -1;
	} else{
		ht = new Hashtable<String, Byte>(0);
		funcOffset = defaultOffset;
	}
}

/**
 Returns if this mapping was specified at creation time or is/was fetched from the device.

 @return true if the mapping is fixed */
public boolean isFixedMapping(){
	return funcOffset < 0;
}

/**
 Tries to fetch the mapping from the remote device represented by parameter \a arduino with a default timeout.

 @param arduino the remote device to be contacted
 @throws IOException          if there is a communication problem or the timeout expires
 @throws InterruptedException if the thread is interrupted before a reply is received */
public synchronized void fetch(Arduino arduino) throws IOException, InterruptedException{
	fetch(arduino, Arduino.PACKET_TIMEOUT);
}

/**
 Tries to fetch the mapping from the remote device represented by parameter \a arduino with the specified timeout.

 @param arduino the remote device to be contacted
 @param timeout the timeout after which a {@link j2arduino.util.TimeoutException} is thrown
 @throws IOException          if there is a communication problem or the timeout expires
 @throws InterruptedException if the thread is interrupted before a reply is received */
public void fetch(Arduino arduino, int timeout) throws IOException, InterruptedException{
	if(funcOffset < 0)
		return;
	byte[] msg = arduino.sendSyncWait(new ArduinoPacket(funcOffset, null, null), timeout).msg;
	synchronized(ht){
		ht.clear();
		StringBuilder sb = new StringBuilder(64);
		byte idx = (byte)0;
		for(byte aMsg : msg){
			switch(aMsg){
				case 0:
					ht.put(sb.toString(), idx);
					idx++;
					sb.setLength(0);
					break;
				default:
					sb.append((char)aMsg);
					break;
			}
		}
	}
}

/**
 Returns the number of available mappings.

 @return the number of mappings */
public int size(){
	return ht.size();
}

/**
 Returns if there exists a match for a specific function name.

 @param funcName the name of the function in question
 @return true, if there exists a mapping from parameter funcName to an offset in this mapping, false otherwise. */
public boolean containsKey(String funcName){
	return ht.containsKey(funcName);
}

/**
 Returns the offset of the function associated with parameter funcName.

 @param funcName the string representation of the searched function
 @return the offset of that function according to this mapping, or -1 if there exists no mapping */
public byte get(String funcName){
	Byte funcNumber = ht.get(funcName);
	if(funcNumber == null){
		return -1;
	}
	return funcNumber;
}

/** Clears all mappings in this instance. */
public void clear(){
	ht.clear(); // Unlike the new collection implementations, Hashtable is synchronized.
}

}
