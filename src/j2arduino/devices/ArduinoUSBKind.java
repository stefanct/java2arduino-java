package j2arduino.devices;

import javax.usb.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.UUID;

public class ArduinoUSBKind implements ArduinoKind{

private final HashSet<Arduino> arduinos = new HashSet<Arduino>(1);
public static final String USB_MANUF = "ims.tuwien.ac.at";
public static final String USB_PROD = "USB Board";
public static final byte USB_IF_CLASS = (byte)0xFF;
public static final byte USB_IF_SUBCLASS = (byte)0x12;
public static final byte USB_IF_PROTOCOL = (byte)0xEF;

public ArduinoUSBKind(){
}

/** Tries to load Class {@link javax.usb.UsbDevice} and get a valid hub device via {@link javax.usb.UsbServices#getRootUsbHub()}. */
@Override
public boolean isAvailable(){
	try{
		if(Class.forName("javax.usb.UsbDevice") != null){
			if(UsbHostManager.getUsbServices().getRootUsbHub() != null)
				return true;
		}
	} catch(ClassNotFoundException ignored){
	} catch(UsbException ignored){
	}
	return false;
}

@Override
public HashSet<Arduino> getAvailableArduinos(boolean updateNow) throws IOException, InterruptedException, UsbException{
	try{
		UsbHub rootUsbHub = UsbHostManager.getUsbServices().getRootUsbHub();
		addAttachedUsbDevices(rootUsbHub);
//	} catch(SecurityException ignored){ // thrown if access to the USB subsystem is not allowed
	} catch(UnsupportedEncodingException ignored){
	}
	return arduinos;
}

private void addAttachedUsbDevices(UsbHub usbHub) throws UsbException, UnsupportedEncodingException{
	for(UsbDevice dev : usbHub.getAttachedUsbDevices()){
		if(dev.isUsbHub()){
			addAttachedUsbDevices((UsbHub)dev);
		} else{
			UsbDeviceDescriptor devDescriptor = dev.getUsbDeviceDescriptor();
			// check for vendor specific class
			if(devDescriptor != null && devDescriptor.bDeviceClass() == (byte)0xff){
				UsbConfiguration config = dev.getActiveUsbConfiguration();
				if(config != null){
					UsbInterface usbIf = config.getUsbInterface((byte)0);
					if(usbIf != null){
						UsbInterfaceDescriptor usbIfDesc = usbIf.getUsbInterfaceDescriptor();
						if(config.isActive()
						   && usbIfDesc.bInterfaceClass() == USB_IF_CLASS
						   && usbIfDesc.bInterfaceSubClass() == USB_IF_SUBCLASS
						   && usbIfDesc.bInterfaceProtocol() == USB_IF_PROTOCOL
						   && usbIf.containsUsbEndpoint(ArduinoUSB.USB_IN_EPNUM)
						   && usbIf.containsUsbEndpoint(ArduinoUSB.USB_OUT_EPNUM)){
							String address = "USB-" + dev.getSerialNumberString() + UUID.randomUUID();
							synchronized(arduinos){
								Arduino arduino = new ArduinoUSB(address, dev.getProductString() + '-' + dev.getSerialNumberString(), usbIf);
								arduinos.add(arduino);
							}
						}
					}
				}
			}
		}
	}
}
}