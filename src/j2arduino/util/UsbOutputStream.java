package j2arduino.util;

import javax.usb.*;
import javax.usb.util.DefaultUsbIrp;
import java.io.*;

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

// TODO: introduce a boolean value to reflect this and throw exceptions in methods when used while this stream is closed
@Override
public void close() throws IOException{
	try{
		out.close();
	} catch(UsbException e){
		throw new IOException("Error closing the underlying UsbPipe", e);
	}
}
}
