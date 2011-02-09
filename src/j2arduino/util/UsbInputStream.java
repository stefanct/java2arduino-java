package j2arduino.util;

import javax.usb.UsbException;
import javax.usb.UsbIrp;
import javax.usb.UsbPipe;
import javax.usb.util.DefaultUsbIrp;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Arrays;

import static sun.security.pkcs11.wrapper.Functions.toHexString;

public class UsbInputStream extends InputStream{

protected final UsbPipe in;
private final byte[] buffer;
private int end;
private int start;

public UsbInputStream(UsbPipe inPipe){
	in = inPipe;
	buffer = new byte[in.getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize()];
	start = end = 0;
}

@Override
public int read() throws IOException{
	try{
		while(start > end){
			UsbIrp irp = new DefaultUsbIrp(buffer, 0, buffer.length, true);
			System.err.println("calling pipe.syncSubmit "+irp);
			in.syncSubmit(irp);
			start = 0;
			int actLen = irp.getActualLength();
			end = actLen - 1;
			if(actLen > 0)
				System.err.println(toHexString(Arrays.copyOfRange(buffer,0,actLen))+" returned");
			else
				System.err.println("emtpy IRP returned");
		}
		return buffer[start++];

	} catch(UsbException e){
		if(e.getCause() instanceof InterruptedException){
			InterruptedIOException ex = new InterruptedIOException();
			ex.initCause(e);
			throw ex;
		}else
			throw new IOException("Receiving from an UsbPipe failed", e);
	}
}

@Override
public int read(byte[] b, int off, int len) throws IOException{
	if(len == 0)
		return 0;
	int ret;
	// if there is nothing buffered, read at least one byte (API req)
	if(start > end){
		b[off++] = (byte)read();
		ret = 1;
	} else
		ret = 0;

	int max = off + Math.min(len - ret, available());
	for(; off < max; off++){
		b[off] = buffer[start++];
	}
	return ret + max;
}

@Override
public int available() throws IOException{
	return end - start + 1;
}
}
