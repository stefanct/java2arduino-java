package j2arduino.util;

import javax.usb.*;
import javax.usb.util.DefaultUsbIrp;
import java.io.*;

public class UsbInputStream extends InputStream{

private final UsbPipe in;
private final byte[] buffer;
/** Index of the first buffered byte. */
private int start;
/** Index of the last buffered byte. */
private int end;

public UsbInputStream(UsbPipe inPipe){
	in = inPipe;
	buffer = new byte[in.getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize()];
	start = 0;
	end = -1;
}

@Override
public int read() throws IOException{
	try{
		while(start > end){
			UsbIrp irp = new DefaultUsbIrp(buffer, 0, buffer.length, true);
			in.syncSubmit(irp);
			start = 0;
			int actLen = irp.getActualLength();
			end = actLen - 1;
		}
		return buffer[start++];

	} catch(UsbException e){
		if(e.getCause() instanceof InterruptedException){
			InterruptedIOException ex = new InterruptedIOException();
			ex.initCause(e);
			throw ex;
		} else
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

	// min(bytes requested, bytes buffered)
	final int toRead = Math.min(len - ret, available());
	final int max = off + toRead;
	for(; off < max; off++){
		b[off] = buffer[start++];
	}
	return ret + toRead;
}

@Override
public int available(){
	return end - start + 1;
}

// TODO: introduce a boolean value to reflect this and throw exceptions in methods when used while this stream is closed
@Override
public void close() throws IOException{
	try{
		in.close();
	} catch(UsbException e){
		throw new IOException("Error closing the underlying UsbPipe", e);
	}
}
}
