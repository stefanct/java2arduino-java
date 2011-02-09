package j2arduino.util;

import java.io.IOException;
import java.io.InputStream;

/**
 Adds functionality to another input stream-namely, the ability to buffer the input.

 When the %BufferedInputStream is created, an internal buffer array is created. As bytes from the stream are read or skipped, the internal buffer is
 refilled as necessary from the contained input stream, many bytes at a time.

 Additionally to usual blocking read methods this implementation provides non-blocking and temporarily blocking read methods.
 */
public class BufferedInputStream extends InputStream{

final static private int NOTHING = 0xFFFF;
private InputStream in;
private byte[] buf;
private int read;
private int write;

/**
 Creates a %BufferedInputStream with the specified buffer size.

 @param inStream the underlying InputStream
 @param size     the maximum number of bytes buffered by this instance */
public BufferedInputStream(InputStream inStream, int size){
	in = inStream;
	buf = new byte[size];
	read = 0;
	write = 0;
}

/**
 Returns the number of buffered bytes.

 @return byte count */
private int buffered(){
	if(write < read){
		return write + buf.length - read;
	} else{
		return write - read;
	}
}

/**
 Returns the last unread byte or throws a TimeoutException if no bytes can be read after \a ms.

 @param ms count of minimum ms to wait for new data
 @return the last unread byte
 @throws IOException if an I/O error occurs */
public int readWait(int ms) throws IOException{
	synchronized(this){
		do{
			int tmp = readNoWait();
			if(tmp != NOTHING)
				return tmp;
			try{
				wait(10);
				ms -= 10;
			} catch(InterruptedException ignored){
			}
		} while(ms >= 0);
	}
	throw new TimeoutException(getClass().getName() + ".readWait() timed out");
}

/**
 Returns the last unread byte \a NOTHING if there is no data available at the moment.

 @return the last unread byte or \a NOTHING
 @throws IOException if an I/O error occurs */
public int readNoWait() throws IOException{
	byte ret;
	int bufLen = buf.length;
	if(buffered() > 0){
		ret = buf[read];
		read = (read + 1) % bufLen;
	} else{
		if(in.available() > 0){
			ret = (byte)in.read();
		} else{
			return NOTHING;
		}
	}
//	int strLen = in.available();
//	if(strLen > 0){
//		boolean overflow = write >= read;
//		int max1 = overflow ? bufLen : read;
//		int maxLen1 = max1 - write;
//
//		int len1 = maxLen1 < strLen ? maxLen1 : strLen;
//		write = (write + in.read(buf, write, len1)) % bufLen;
//		strLen = in.available();
//		if(overflow && write == bufLen && strLen > 0){
//			write = (write + in.read(buf, 0, read < strLen ? read : strLen)) % bufLen;
//		}
//	}
	return ret;
}

@Override
public int available() throws IOException{
	return in.available() + buffered();
}

@Override
public int read() throws IOException{
	int tmp = readNoWait();
	if(tmp == NOTHING){
		return in.read();
	} else{
		return tmp;
	}
}

//@Override
//public int read(byte[] b) throws IOException{
//	return super.read(b);
//}
//
//@Override
//public int read(byte[] b, int off, int len) throws IOException{
//	return super.read(b, off, len);
//}
//
@Override
public void close() throws IOException{
	super.close();
	in.close();
}
}
