package j2arduino.util;

import javax.usb.UsbException;
import javax.usb.UsbPipe;
import javax.usb.util.DefaultUsbIrp;
import java.io.IOException;
import java.io.OutputStream;

public class UsbOutputStream extends OutputStream{

protected final UsbPipe out;

public UsbOutputStream(UsbPipe outPipe){
	out = outPipe;
}

@Override
public void write(byte[] b, int off, int len) throws IOException{
	try{
		DefaultUsbIrp irp = new DefaultUsbIrp(b, off, len, true);
		out.syncSubmit(irp);
	} catch(UsbException e){
		throw new IOException("Sending to an UsbPipe failed", e);
	}
}

@Override
public void write(int i) throws IOException{
	try{
		byte[] outB = {(byte)(i&0xFF)};
		out.syncSubmit(outB);
	} catch(UsbException e){
		throw new IOException("Sending to an UsbPipe failed", e);
	}
}
}
