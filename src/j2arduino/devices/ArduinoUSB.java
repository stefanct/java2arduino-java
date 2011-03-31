package j2arduino.devices;

import j2arduino.util.*;

import javax.usb.*;
import java.io.*;

public class ArduinoUSB extends Arduino{

private UsbInterface usbIF;
public static final byte USB_OUT_EPNUM = (byte)2;
public static final byte USB_IN_EPNUM = (byte)(0x80|1);

/**
 Creates an Arduino instance.

 @param address the hardware address of this Arduino. */
public ArduinoUSB(String address, String name, UsbInterface usbInterface){
	super(name, address);
	if(usbInterface == null)
		throw new IllegalArgumentException("usbInterface may not be null");
	if(!usbInterface.containsUsbEndpoint(USB_IN_EPNUM) || !usbInterface.containsUsbEndpoint(USB_OUT_EPNUM))
		throw new IllegalArgumentException("usbInterface needs to contain endpoints USB_OUT_EPNUM and USB_IN_EPNUM");
	this.usbIF = usbInterface;
}

@Override
protected OutputStream openOutputStream() throws IOException{
	try{
		if(!usbIF.isClaimed())
			usbIF.claim();
		UsbEndpoint outEndpoint = usbIF.getUsbEndpoint(USB_OUT_EPNUM);
		UsbPipe outPipe = outEndpoint.getUsbPipe();
		outPipe.open();
		return new UsbOutputStream(outPipe);
	} catch(UsbException e){
		throw new IOException(e);
	}
}

@Override
protected InputStream openInputStream() throws IOException{
	try{
		if(!usbIF.isClaimed())
			usbIF.claim();
		UsbEndpoint inEndpoint = usbIF.getUsbEndpoint(USB_IN_EPNUM);
		UsbPipe inPipe = inEndpoint.getUsbPipe();
		inPipe.open();
		return new UsbInputStream(inPipe);
	} catch(UsbException e){
		throw new IOException(e);
	}
}

@Override
protected void releaseResources(){
	try{
		usbIF.release();
	} catch(UsbException ignored){
	}
}

@Override
protected boolean isAvailable(){
	try{
		if(Class.forName("javax.usb.UsbHostManager") != null && UsbHostManager.getUsbServices().getRootUsbHub() != null){
			return true;
		}
	} catch(Throwable e){
		e.printStackTrace();
	}
	return false;
}
}
