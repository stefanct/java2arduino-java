package j2arduino.devices;

import javax.microedition.io.*;
import java.io.*;

public class ArduinoBT extends Arduino{

public ArduinoBT(String name, String address){
	super(name, address);
}

@Override
protected OutputStream openOutputStream() throws IOException{
	return ((OutputConnection)Connector.open("btspp://" + address + ":1", Connector.READ_WRITE)).openOutputStream();
}

@Override
protected InputStream openInputStream() throws IOException{
	return ((InputConnection)Connector.open("btspp://" + address + ":1", Connector.READ_WRITE)).openInputStream();
}

@Override
protected void releaseResources(){
}

@Override
protected boolean isAvailable(){
	try{
		if(Class.forName("javax.bluetooth.LocalDevice") != null){
			return javax.bluetooth.LocalDevice.isPowerOn();
		}
	} catch(Throwable e){
		e.printStackTrace();
	}
	return false;
}
}
