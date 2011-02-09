package j2arduino;

/**
 Implementers of this interface can be attached to Arduinos and ArduinoGateways to monitor their bluetooth activity. They will be notified, if these
 use any BT devices.
 */
public interface ArduinoActivityListener{

/** The caller is now disconnected (i.e. there is no RFCOMM link to the device represented by the caller) */
public static final int STATE_DISCONNECTED = 0;
/** The caller is now fully connected. */
public static final int STATE_CONNECTED = 1;
/** The caller has started a transfer or other immediate BT activity (e.g. device discovery or connecting attempt) */
public static final int STATE_ACTIVE = 2;
/** The caller has ended an immediate BT activity. */
public static final int STATE_INACTIVE = 3;

/**
 Will be called whenever the bluetooth state of the caller changes. \warning Should be called by Arduinos and ArduinoGateways only.

 @param state   the new state of the caller.
 @param arduino The caller itself, or null if the state change occurred in the gateway (e.g. when a BT device discovery is started). */
public void connectionStateChanged(int state, Arduino arduino);
}
