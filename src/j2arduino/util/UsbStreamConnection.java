package j2arduino.util;

import javax.microedition.io.StreamConnection;
import javax.usb.UsbException;
import javax.usb.UsbPipe;
import java.io.*;

public class UsbStreamConnection implements StreamConnection{

protected InputStream in;
protected OutputStream out;
final protected UsbPipe inPipe;
final protected UsbPipe outPipe;

public UsbStreamConnection(UsbPipe inPipe, UsbPipe outPipe){
	this.inPipe = inPipe;
	this.outPipe = outPipe;
}

@Override
public InputStream openInputStream() throws IOException{
	in = new UsbInputStream(inPipe);
	return in;
}

@Override
public DataInputStream openDataInputStream() throws IOException{
	return new DataInputStream(openInputStream());
}

@Override
public OutputStream openOutputStream() throws IOException{
	out = new UsbOutputStream(outPipe);
	return out;
}

@Override
public DataOutputStream openDataOutputStream() throws IOException{
	return new DataOutputStream(openOutputStream());
}

@Override
public void close() throws IOException{
	in.close();
	out.close();
	// FIXME: closing usb pipes is racy
	try{
//		inPipe.abortAllSubmissions(); // not implemented
		inPipe.close();
	} catch(UsbException e){
		throw new IOException("Closing USB input pipe failed", e);
	}
	try{
//		outPipe.abortAllSubmissions();
		outPipe.close();
	} catch(UsbException e){
		throw new IOException("Closing USB output pipe failed", e);
	}
}
}
