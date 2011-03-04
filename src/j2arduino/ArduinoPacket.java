package j2arduino;

import j2arduino.devices.Arduino;

import java.io.IOException;
import java.io.PrintStream;

/**
 ArduinoPackets are used as lightweight data exchange objects in {@link j2arduino} and while communicating with its clients.

 An ArduinoPacket may represent a request (e.g. when a {@link j2arduino} client calls {@link j2arduino.devices.Arduino#sendSync(ArduinoPacket)}) or a
 reply. It can also be used to propagate communication errors inside its {@link #ex} field.
 */
public class ArduinoPacket{

/** Request: function offset, answer: return value, error: probably return value. */
public int cmd;

/** Request: message payload/parameters, answer: returned payload, error: undefined. */
public byte[] msg;

/** Used for asynchronous reply handling. Null in synchronous requests or if asynchronous reply should be ignored. */
public ArduinoResponseListener listener;

/**
 Used to propagate communication errors to j2arduino clients.

 @see j2arduino.devices.Arduino.ArduinoWorker#run() */
public IOException ex;

/** Constant used internally in j2arduino to distinguish processed from unprocessed packets. */
public static final int PROCESSING = 0x100; // == 256

/**
 Creates a new ArduinoPacket and sets field cmd to \a cmd.

 @param cmd the new cmd */
public ArduinoPacket(int cmd){
	this(cmd, null, null);
}

/**
 Creates a new ArduinoPacket and sets field buf to \a payload and field cmd to -1.

 @param payload the value assigned to buf */
public ArduinoPacket(byte[] payload){
	this(-1, payload, null);
}

/**
 Creates a new ArduinoPacket and sets fields cmd, buf and listener to the given parameters.

 \warning Changes to the elements of \a payload while the instance is used inside j2arduino can have unwanted effects.

 @param command the value assigned to cmd
 @param payload the value assigned to buf
 @param l       the value assigned to listener */
public ArduinoPacket(int command, byte[] payload, ArduinoResponseListener l){
	cmd = command;
	msg = payload;
	listener = l;
	ex = null;
}

/** Prints the content of various fields in human readable format to stderr. */
public void print(){
	PrintStream stream = System.err;
	stream.println("cmd=" + cmd + " (0x" + Integer.toHexString(cmd) + ')');
	if(msg != null){
		if(msg.length > Arduino.A2J_MAX_PAYLOAD)
			stream.println("warning: length is > " + Arduino.A2J_MAX_PAYLOAD);

		for(int i = 0; i < msg.length; i++)
			stream.println("msg[" + i + "]=0x" + Integer.toHexString(msg[i]));

	} else
		stream.println("msg == null");

	stream.println("listener = " + ((listener == null) ? "null" : listener.toString()));
	stream.println("exception = " + ((ex == null) ? "null" : ex.toString()));
	stream.println();
}

/** \defgroup packethelpers Packet creation helper methods
 The two helper methods {@link #readUnsignedInteger} and {@link #writeUnsignedInteger} use little-endian,
 because Atmel's AVRs use that (most of the time) when they have to deal with multibyte integers.

 Using little-endian allows easy read access of multibyte values like shown in this example:
 \code
 uint16_t var = *(uint16_t*)(&packetBuffer[offset]);
 \endcode

 Writing is a bit awkward, but there exist \ref lilendianmacros "helper macros" for this problem. Natively it would look like this:
 \code
 uint16_t* tmp = (uint16_t*)(&packetBuffer[offset]); // first create a pointer to write to
 tmp[0] = var;
 \endcode
 */
//@{

/**
 Helper method to extract little-endian unsigned integers from byte arrays.

 Can be used to easily convert uint16_t et al. received from Arduinos to Java's primitive integer types.

 Note that input values greater than 0x7FFFFFFF will be converted to a negative \a int and that no more than four bytes will contribute to the return
 value.

 @param source    byte array from which to read
 @param index     index where to start reading
 @param byteCount number of bytes to read from \a source (usually a power of 2)
 @return the converted value. */
public static int readUnsignedInteger(byte[] source, int index, int byteCount){
	int ret = 0;
	for(int i = 0; i < byteCount; i++){
		ret += (source[index + i]&0xFF)<<(i<<3);
	}
	return ret;
}

/**
 Helper method to copy Java's primitive integer types to little-endian byte arrays.

 @param value     the value to be converted
 @param dest      the destination array
 @param off       offset in \a dest where to start
 @param byteCount number of bytes to write (usually a power of 2)
 @return the offset after the last written byte (equal to \a off+byteCount) */
public static int writeUnsignedInteger(int value, byte[] dest, int off, int byteCount){
	for(int i = 0; i < byteCount; i++){
		dest[off++] = (byte)((value>>(8 * i))&0xFF);
	}
	return off;
}
//@}
}
