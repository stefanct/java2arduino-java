package j2arduino.devices;

import javax.usb.UsbException;
import java.io.IOException;
import java.util.HashSet;

public interface ArduinoKind{

/** Tries to load the subsystem necessary for this kind of underlying protocol and reports its availability. */
public abstract boolean isAvailable();

/**
 Returns all available Arduinos for this kind.

 The returned set has to be treated read-only.

 @param updateNow if true the set must be updated before returning it.
 @return a set including all currently known Arduinos usable via the underlying protocol of this kind. */
public abstract HashSet<Arduino> getAvailableArduinos(boolean updateNow) throws IOException, InterruptedException, UsbException;
}