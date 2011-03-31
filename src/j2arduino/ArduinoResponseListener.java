package j2arduino;

/**
 Implementers of this class can be asynchronously notified of replies to requests sent to Arduinos.
 <p/>
 To be notified, implementers have to be assigned to the {@link j2arduino.ArduinoPacket#listener listener field} of the corresponding request packet
 before calling a send method of {@link j2arduino.devices.Arduino}.

 @see j2arduino.ArduinoPacket#listener
 @see j2arduino.devices.Arduino.ArduinoWorker#notifyListeners(ArduinoPacket) */
public interface ArduinoResponseListener{

/**
 Callback function for asynchronous communication with Arduinos.
 <p/>
 Called when a reply to a request packet is received and the implementer was assigned as listener to that request packet. It may also be called in
 case of communication errors in which case {@link ArduinoPacket#ex} will be set to an appropriate exception.

 @param answer the reply to the ArduinoPacket, where this instance was added as listener */
public void handleResponse(ArduinoPacket answer);
}
