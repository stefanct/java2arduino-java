package j2arduino.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 The class implements a buffered output stream. By setting up such an output stream, an application can write bytes to the underlying output stream
 without necessarily causing a call to the underlying system for each byte written.
 */
public class BufferedOutputStream extends OutputStream{

private OutputStream out;
private byte[] buf;
private int read;
private int write;

/**
 Creates a new buffered output stream to write data to the specified underlying output stream with the specified buffer size.

 @param outStream the underlying output stream
 @param bufSize   the buffer size */
public BufferedOutputStream(OutputStream outStream, int bufSize){
	if(bufSize == 0)
		throw new IllegalArgumentException("The buffer size has to be >0");
	out = outStream;
	buf = new byte[bufSize];
	write = 0;
	read = 0;
}

/**
 Creates a new buffered output stream to write data to the specified underlying output stream.

 @param outStream the underlying output stream. */
public BufferedOutputStream(OutputStream outStream){
	this(outStream, 512);
}

/** Flushes this buffered output stream. This forces any buffered output bytes to be written out to the underlying output stream. */
@Override
public void flush() throws IOException{
	boolean overflow = read > write;
	int bufLen = buf.length;
	int max1 = overflow ? bufLen : write;
	int len1 = max1 - read;
	out.write(buf, read, len1);

	if(overflow){
		out.write(buf, 0, write);
	}
	read = write = 0;
}

/** Writes the specified byte to this buffered output stream. Flushes the buffer if there is no space available in buffer. */
@Override
public void write(int b) throws IOException{
	int length = buf.length;
	if(write == ((read - 1) % length))
		flush();
	buf[write] = (byte)b;
	write = (write + 1) % length;
}

/** Closes this output stream and releases any system resources associated with this stream. */
@Override
public void close() throws IOException{
	out.close();
}

}
