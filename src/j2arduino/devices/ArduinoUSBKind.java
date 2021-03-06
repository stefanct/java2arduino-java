package j2arduino.devices;

import j2arduino.util.J2ArduinoSettings;

import javax.usb.*;
import java.io.*;
import java.util.*;

public class ArduinoUSBKind implements ArduinoKind{

private final HashSet<Arduino> arduinos = new HashSet<Arduino>(1);
public static final String USB_MANUF = "ims.tuwien.ac.at";
public static final String USB_PROD = "USB Board";
public static final byte USB_IF_CLASS = (byte)0xFF;
public static final byte USB_IF_SUBCLASS = (byte)0x12;
public static final byte USB_IF_PROTOCOL = (byte)0xEF;
public static final String JAVAX_USB_PROPERTIES_FILE = "javax.usb.properties";
public static final String JAVAX_USB_SERVICES_NAME = "javax.usb.services";
public static final String JAVAX_USB_SERVICES_VALUE = "javalibusb1.Libusb1UsbServices";
public static final String JAVAX_USB_LIBUSB_TRACE_NAME = "javax.usb.libusb.trace";
public static final String JAVAX_USB_LIBUSB_TRACE_VALUE = "false";
public static final String JAVAX_USB_LIBUSB_DEBUG_NAME = "javax.usb.libusb.debug";
public static final String JAVAX_USB_LIBUSB_DEBUG_VALUE = "0";

public ArduinoUSBKind(){
	// set default settings for javax-usb-libusb1 as system properties if they are not set yet by any means
	Properties props = J2ArduinoSettings.loadPropertiesFromFile(JAVAX_USB_PROPERTIES_FILE);
	if(props == null || !props.containsKey(JAVAX_USB_SERVICES_NAME))
		System.setProperty(JAVAX_USB_SERVICES_NAME, JAVAX_USB_SERVICES_VALUE);
	if(props == null || !props.containsKey(JAVAX_USB_LIBUSB_TRACE_NAME))
		System.setProperty(JAVAX_USB_LIBUSB_TRACE_NAME, JAVAX_USB_LIBUSB_TRACE_VALUE);
	if(props == null || !props.containsKey(JAVAX_USB_LIBUSB_DEBUG_NAME))
		System.setProperty(JAVAX_USB_LIBUSB_DEBUG_NAME, JAVAX_USB_LIBUSB_DEBUG_VALUE);
}

/** Tries to load Class {@link javax.usb.UsbDevice} and get a valid hub device via {@link javax.usb.UsbServices#getRootUsbHub()}. */
@Override
public boolean isAvailable(){
	try{
		if(Class.forName("javax.usb.UsbHostManager") != null){
			if(UsbHostManager.getUsbServices().getRootUsbHub() != null)
				return true;
		}
	} catch(ClassNotFoundException ignored){
	} catch(UsbException ignored){
	}
	return false;
}

@Override
public HashSet<Arduino> getAvailableArduinos(boolean updateNow) throws IOException, InterruptedException{
	try{
		UsbHub rootUsbHub = UsbHostManager.getUsbServices().getRootUsbHub();
		addAttachedUsbDevices(rootUsbHub);
	} catch(UsbException e){
		throw new IOException(e);
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
							final String serialNumberString = dev.getSerialNumberString();
							final String id = ((serialNumberString.isEmpty()) ? UUID.randomUUID().toString() : serialNumberString);
							synchronized(arduinos){
								Arduino arduino = new ArduinoUSB("USB-" + id, dev.getProductString() + '-' + serialNumberString, usbIf);
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