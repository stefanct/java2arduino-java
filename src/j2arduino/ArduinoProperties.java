package j2arduino;

import java.io.IOException;
import java.util.Hashtable;

/**
 %ArduinoProperties are pairs of strings, that represent properties of remote Arduinos.

 If enabled on the remote device, this class can provide these pairs in the form of a mapping of option keys to option values.
 */
public class ArduinoProperties{

private final Hashtable<String, String> ht;
private short funcOffset;

/**
 Creates a new property mapping with a given function offset.

 @param offset the offset used to query remote Arduinos for properties */
protected ArduinoProperties(short offset){
	funcOffset = offset;
	ht = new Hashtable<String, String>();
}

/**
 Sets the offset that is used to query remote Arduinos for properties.

 @param offset the offset */
public void setFuncOffset(short offset){
	this.funcOffset = offset;
}

/**
 Tries to fetch the properties from the remote device represented by parameter \a arduino with a default timeout.

 @param arduino the Arduino to be queried
 @throws IOException          if there is a communication problem or the timeout expires
 @throws InterruptedException if the thread is interrupted before a reply is received */
protected void fetch(Arduino arduino) throws IOException, InterruptedException{
	fetch(arduino, Arduino.PACKET_TIMEOUT);
}

/**
 Tries to fetch the properties from the remote device represented by parameter \a arduino with the specified timeout.

 @param arduino the Arduino to be queried
 @param timeout the timeout after which a {@link j2arduino.util.TimeoutException} is thrown
 @throws IOException          if there is a communication problem or the timeout expires
 @throws InterruptedException if the thread is interrupted before a reply is received */
protected void fetch(Arduino arduino, int timeout) throws IOException, InterruptedException{
	if(funcOffset < 0){
		return;
	}

	byte[] msg = arduino.sendSyncWait(new ArduinoPacket(funcOffset, null, null), timeout).msg;
	if(msg.length == 0)
		return;
	StringBuilder sb = new StringBuilder();
	String first = null;
	for(byte aMsg : msg){
		switch(aMsg){
			case 0:
				if(first == null){
					first = sb.toString();
				} else{
					ht.put(first, sb.toString());
					first = null;
				}
				sb.setLength(0);
				break;
			default:
				sb.append((char)aMsg);
				break;
		}
	}
	System.err.println("props: " + ht);
}

/**
 Returns the number of available property pairs.

 @return the number of pairs */
public int size(){
	return ht.size();
}

/**
 Returns if there exists a value for a specific property key.

 @param key the name of the function in question
 @return true, if there exists a value to that key, false otherwise. */
public boolean containsKey(String key){
	return ht.containsKey(key);
}

/**
 Returns the option value associated with parameter \a key.

 @param key the key for which an option value is looked up
 @return the corresponding option value or null if there is none */
public String get(String key){
	return ht.get(key);
}

/** Clears all mapping between option keys and value. */
public synchronized void clear(){
	ht.clear();

}
}
