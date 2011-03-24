package j2arduino.devices;

import j2arduino.*;
import j2arduino.util.*;

import javax.usb.UsbException;
import java.io.*;
import java.util.*;

/**
 Represents one remote device. After {@link #connect connecting} various methods to send data or "call" methods are available. To listen for link
 activity {@link #addActivityListener(j2arduino.ArduinoActivityListener)} provides the option to put {@link j2arduino.ArduinoActivityListener}s. All
 methods are thread-safe if not noted otherwise.

 @see j2arduino */
public abstract class Arduino{

/** Default timeout in milliseconds when calling sendSync() et al. 0 will block forever. */
public static final int PACKET_TIMEOUT = 2500;
/** Default timeout in milliseconds  for a connecting attempt. 0 will block forever. */
public static final int CONNECTING_TIMEOUT = 3000;
/**
 An unique identifier for the hardware device represented by this object. Usually a string representation of the hardware address of the associated
 device.

 @see Arduino#equals(Object) */
public final String address;
/** Human readable name of the corresponding remote Arduino device. */
public final String name;
private final Collection<ArduinoActivityListener> listeners;
private ConcurrentRingBuffer<ArduinoPacket> requests;
/** Indicates connection state: 0 == disconnected, 1 == connecting, 2 == connected. */
private byte connected;
private Thread workerThread;
private ArduinoWorker worker;
private ArduinoFunctionMapping funcMapping;
private ArduinoProperties props;

/**
 @addtogroup j2asizes java2arduino sizes */
//@{
/** Maximum number of bytes to be transmitted as payload in arduino2j packets. */
public static final int A2J_MAX_PAYLOAD = 255;
/** Number of bytes of the a2jMany header. */
public static final int A2J_MANY_HEADER = 6;
/** Maximum number of bytes to be transmitted as payload in a2jMany packets. */
public static final int A2J_MANY_PAYLOAD = A2J_MAX_PAYLOAD - A2J_MANY_HEADER;
/** The number of bytes to be buffered by the input and output streams connected to the underlying stream. */
public static final int BUFFER_SIZE = 300;
//@}

/**
 Creates a new Arduino object. Usually called by {@link j2arduino.ArduinoGateway}.

 @param name    a String used in GUIs to identify this Arduino
 @param address a String representation if the Arduino's hardware address. */
public Arduino(String name, String address){
	props = null;
	connected = (byte)0;
	this.name = name;
	funcMapping = null;
	requests = new ConcurrentRingBuffer<ArduinoPacket>(8, "Connection closed");
	this.address = address;
	workerThread = null;
	worker = null;
	listeners = new ArrayList<ArduinoActivityListener>(1);
}

/**
 Arduinos are uniquely identified by their addresses. Whoever instantiates objects of this type is responsible for providing system-wide unique
 identifiers. If the underlying protocol does not provide such identifiers they must be generated.

 Two Arduinos are equal if their addresses are equal.
 */
@Override
public boolean equals(Object o){
	if(o == null)
		return false;
	if(!(o instanceof Arduino))
		return false;
	return ((Arduino)o).address.equals(address);
}

@Override
public int hashCode(){
	return address.hashCode();
}

/** \defgroup arduinoConnection Arduino methods (connection related) */
/**
 Creates a new connection.

 Creates a connection including a working thread, function mapping and properties. If a (non-null) Hashtable is provided, it will be used as constant
 function mapping, else the Arduino will be queried for it.

 \ingroup arduinoConnection

 @param functionMapping a constant function mapping (may be null).
 @return true if the connection was established due to this call, false otherwise
 @throws java.io.IOException  if no connection could be established.
 @throws InterruptedException If the calling thread is interrupted before the connecting attempt succeeded */
public boolean connect(Hashtable functionMapping) throws IOException, InterruptedException, UsbException{
	synchronized(this){
		if(connected != 0)
			return false;
		fireActivityListeners(ArduinoActivityListener.STATE_ACTIVE);
		try{
			final InputStream inputStream = openInputStream();
			final OutputStream outputStream = openOutputStream();
			worker = new ArduinoWorker(requests,
			                           new BufferedInputStream(inputStream, BUFFER_SIZE),
			                           new BufferedOutputStream(outputStream, BUFFER_SIZE));
		} finally{
			fireActivityListeners(ArduinoActivityListener.STATE_INACTIVE);
		}
		workerThread = new Thread(worker, address + "-Worker");
		workerThread.start();
		funcMapping = new ArduinoFunctionMapping(functionMapping);
		props = new ArduinoProperties(funcMapping.get("a2jGetPropsOffset"));
		connected = 1;
	}
	int tries = 1;
	while(true){
		long startTime = System.currentTimeMillis();
		IOException e = null;
		InterruptedException ie = null;
		try{
			int freeTime = CONNECTING_TIMEOUT;
			funcMapping.fetch(this, freeTime);
			freeTime -= (int)(System.currentTimeMillis() - startTime);
			props.setFuncOffset(funcMapping.get("a2jGetProperties"));
			props.fetch(this, freeTime);
			break;
		} catch(TimeoutException ex){
			e = ex;
		} catch(InterruptedIOException ex){
			e = ex;
		} catch(IOException ex){
			disconnect();
			throw ex;
		} catch(InterruptedException ex){
			ie = ex;
		}
		tries--;

		if(tries <= 0){
			disconnect();
			if(e != null){
				throw e;
			} else{
				throw ie;
			}
		}
	}
	connected = 2;

	fireActivityListeners(ArduinoActivityListener.STATE_CONNECTED);
	return true;
}

protected boolean isAvailable(){
	return false;
}

protected abstract OutputStream openOutputStream() throws UsbException, IOException;

protected abstract InputStream openInputStream() throws UsbException, IOException;

protected abstract void closeStreams();

/** Stops the working thread, tears down the connection, notifies all listeners. \ingroup arduinoConnection */
public void disconnect(){
	synchronized(this){
		if(connected <= 0)
			return;
		connected = 0;
	}
	if(workerThread != null){
		worker.shutdown();
		if(Thread.currentThread() != workerThread){
			workerThread.interrupt();
			while(workerThread.isAlive()){
				try{
					workerThread.join();
				} catch(InterruptedException e){
					System.err.println("Interrupted while waiting for ArduinoWorker to die!");
					e.printStackTrace();
				}
			}
			workerThread = null;
			worker = null;
		}
	}
	closeStreams();
	funcMapping.clear();
	props.clear();
	fireActivityListeners(ArduinoActivityListener.STATE_DISCONNECTED);
}

/**
 Returns if the Arduino represented by this instance is connected or not.

 \ingroup arduinoConnection

 @return true if the Arduino is fully connected, false if it is disconnected or a connection attempt is in progress. */
public boolean isConnected(){
	return connected == 2;
}

/**
 Returns the function mapping of this Arduino.

 @return the function mapping of this Arduino */
public ArduinoFunctionMapping getFuncMapping(){
	return funcMapping;
}

/**
 Returns the properties of this Arduino.

 @return the property collection of this Arduino */
public ArduinoProperties getProps(){
	return props;
}

/** ArduinoActivityListener related. */
//@{

/**
 Adds \a l to the list of ArduinoActivityListeners that will get notified in the event of connection changes.

 @param l the element to add. */
public void addActivityListener(ArduinoActivityListener l){
	listeners.add(l);
}

/**
 Removes one instance of \a l from the list of ArduinoActivityListeners.

 @param l the element to remove. */
public void removeActivityListener(ArduinoActivityListener l){
	listeners.remove(l);
}

private void fireActivityListeners(int newState){
	for(ArduinoActivityListener listener : listeners){
		listener.connectionStateChanged(newState, this);
	}
}
//@}

/**
 Checks for malformed requests.

 @param req the ArduinoPacket to be evaluated.
 @throws IllegalArgumentException if the fields of the ArduinoPacket do not represent a valid request.
 @throws IllegalStateException    if not connected. */
public void verifyRequest(ArduinoPacket req) throws IllegalArgumentException, IllegalStateException{
	if(connected == 0)
		throw new IllegalStateException();
	if(req.cmd < 0 || req.cmd > 255 || ((req.msg != null) && (req.msg.length < 0 || req.msg.length > A2J_MAX_PAYLOAD))){
		req.print();
		throw new IllegalArgumentException("Malformed ArduinoPacket");
	}
}

/** \defgroup arduinoAsync Arduino methods (asynchronous sending)
 All methods in this group return immediately after the call has been scheduled.*/
//@{

/**
 "Calls" method \a funcName on the device represented by this instance.

 @param funcName the name of the function to be called. */
public void sendAsyncByName(String funcName){
	sendAsyncByName(funcName, null, null);
}

/**
 "Calls" method \a funcName with parameter \a payload on the device represented by this instance.

 @param funcName the name of the function to be called.
 @param payload  the payload to be sent. */
public void sendAsyncByName(String funcName, byte[] payload){
	sendAsyncByName(funcName, payload, null);
}

/**
 "Calls" method \a funcName on the device represented by this instance.

 @param funcName the name of the function to be called.
 @param listener the listener to call back when a reply is received. */
public void sendAsyncByName(String funcName, ArduinoResponseListener listener){
	sendAsyncByName(funcName, null, listener);
}

/**
 "Calls" method \a funcName with parameter \a payload on the device represented by this instance.

 @param funcName the name of the function to be called.
 @param payload  the payload to be sent.
 @param listener the listener to call back when a reply is received. */
public void sendAsyncByName(String funcName, byte[] payload, ArduinoResponseListener listener){
	ArduinoPacket req = new ArduinoPacket(funcMapping.get(funcName), payload, listener);
	sendAsync(req);
}

/**
 Puts a request into the sendQueue and returns immediately.

 @param req is the request to be added to the sendQueue.
 @throws IllegalArgumentException if the packet is malformed.
 @throws IllegalStateException    if not connected. */
public void sendAsync(ArduinoPacket req) throws IllegalArgumentException, IllegalStateException{
	verifyRequest(req);
	requests.putUninterruptible(req);
}
//@}

/** \defgroup arduinoSync Arduino methods (synchronous sending)
 All methods in this group wait at least some time for a reply before returning.*/
//@{

/**
 Sends ArduinoPacket \a req to the device represented by this instance and waits a bit.

 @param req the request to be sent.
 @return the reply of the remote device.
 @throws java.io.IOException      if an error occurred while sending, receiving or processing on the remote device (including {@link
 j2arduino.util.TimeoutException TimeoutException}).
 @throws IllegalArgumentException if the packet is malformed.
 @throws InterruptedException     if the calling thread is interrupted while waiting for space in the sender queue or for the timeout
 @throws IllegalStateException    if not connected. */
public ArduinoPacket sendSync(ArduinoPacket req) throws IOException, IllegalArgumentException, InterruptedException, IllegalStateException{
	return sendSyncWait(req, PACKET_TIMEOUT);
}

/**
 "Calls" method \a funcName on the device represented by this instance.

 @param funcName the name of the function to be called.
 @return the reply of the remote device.
 @throws java.io.IOException      if an error occurred while sending, receiving or processing on the remote device (including {@link
 j2arduino.util.TimeoutException TimeoutException}).
 @throws IllegalArgumentException if the packet is malformed.
 @throws InterruptedException     if the calling thread is interrupted while waiting for space in the sender queue or for the timeout
 @throws IllegalStateException    if not connected. */
public ArduinoPacket sendSyncByName(String funcName) throws IOException, IllegalArgumentException, IllegalStateException, InterruptedException{
	return sendSyncWait(new ArduinoPacket(funcMapping.get(funcName), null, null), PACKET_TIMEOUT);
}

/**
 "Calls" method \a funcName with parameter \a payload on the device represented by this instance.

 @param funcName the name of the function to be called.
 @param payload  the payload to be sent.
 @return the reply of the remote device.
 @throws java.io.IOException      if an error occurred while sending, receiving or processing on the remote device (including {@link
 j2arduino.util.TimeoutException TimeoutException}).
 @throws IllegalArgumentException if the packet is malformed.
 @throws InterruptedException     if the calling thread is interrupted while waiting for space in the sender queue or for the timeout
 @throws IllegalStateException    if not connected. */
public ArduinoPacket sendSyncByName(String funcName, byte[] payload)
		throws IOException, IllegalArgumentException, IllegalStateException, InterruptedException{
	return sendSyncWait(new ArduinoPacket(funcMapping.get(funcName), payload, null), PACKET_TIMEOUT);
}

/**
 Puts a request into the sender sendQueue and waits some time.

 @param req          is the request to be added to the sendQueue. req.cmd needs to be [0, 255].
 @param milliseconds is the time the method waits for a reply before returning.
 @return the answer in form of an altered version of input parameter req.
 @throws IllegalArgumentException if the packet is malformed.
 @throws j2arduino.util.TimeoutException
 if there is no answer received in time.
 @throws java.io.IOException      if an error occurred while sending, receiving or processing on the remote device.
 @throws InterruptedException     if the calling thread is interrupted while waiting for space in the sender queue or for the timeout
 @throws IllegalStateException    if not connected. */
public ArduinoPacket sendSyncWait(ArduinoPacket req, long milliseconds)
		throws IOException, IllegalArgumentException, IllegalStateException, InterruptedException{
	if(milliseconds <= 0)
		throw new TimeoutException();
	verifyRequest(req);
	synchronized(req){
		req.cmd = req.cmd + ArduinoPacket.PROCESSING; // 0-FF -> 100-1FF to distinguish processed from not processed packets later
		requests.put(req);
		long t = System.currentTimeMillis();
		long endTime = t + milliseconds;
		while(true){
			req.wait(milliseconds);
			IOException ex = req.ex;
			long curTime = System.currentTimeMillis();
			if(ex != null){ // packet was processed, but an error occurred
				throw ex;
			} else if(req.cmd < ArduinoPacket.PROCESSING)// everything ok
				return req;
			if(endTime <= curTime && req.cmd >= ArduinoPacket.PROCESSING){ // packet should be done now but is not
				workerThread.interrupt();
				throw new TimeoutException("Processing the request took too long");
			} else{ // must be spurious interrupt
				if(milliseconds != 0 && endTime - curTime != 0) // 0 is special
					milliseconds = endTime - curTime;
			}
		}
	}
}

/**
 Receives a large block of data from the device represented by this instance. Uses the scheme described \ref j2amanydetails "here" to query
 CMD_P_MANY-compatible method \a funcName for data.

 @param funcName the CMD_P_MANY-compatible method to be "called".
 @return A byte array consisting of: all small blocks in the order received, or the return value of \a funcName if it is non-null.
 @throws java.io.IOException  if an error occurred while sending, receiving or processing on the remote device.
 @throws InterruptedException if the calling thread is interrupted while waiting for space in the sender queue or for a timeout. */
public byte[] receiveLongByName(String funcName) throws IOException, InterruptedException{
	byte funcOff = funcMapping.get(funcName);
	if(funcOff < 0)
		throw new IllegalArgumentException("Function name not in mapping");

	byte[] payload = new byte[A2J_MANY_HEADER];
	payload[0] = funcOff;
	byte[] msg;
	int offset = 0;
	ByteVector tmp = new ByteVector(1024);
	do{
		ArduinoPacket.writeUnsignedInteger(offset, payload, 2, 4);
		ArduinoPacket ans = sendSyncByName("a2jMany", payload);
		if(ans.cmd != 0) // a2jMany return value
			throw new IOException("Error in a2jMany");
		msg = ans.msg;
		if(msg[0] != 0) // user function's return value
			return new byte[] {msg[0]};
		tmp.append(msg, A2J_MANY_HEADER);
		offset = tmp.length();
	} while(msg[1] == 0); // a2jMany isLast
	return tmp.getAll();
}

/**
 Sends a large payload to the device represented by this instance. Uses the scheme described \ref j2amanydetails "here" to split up \a payload into
 smaller blocks and "call" method \a "funcName" on the Arduino.

 @param funcName the CMD_P_MANY-compatible method to be "called".
 @param payload  the data to be sent.
 @return if \a funcName returns a non-null value before the last chunk this value is negated and returned, else the last return value of \a funcName
 is returned.
 @throws java.io.IOException  if an error occurred while sending, receiving or processing on the remote device.
 @throws InterruptedException if the calling thread is interrupted while waiting for space in the sender queue or for the timeout. */
public int sendLongByName(String funcName, byte[] payload) throws IOException, InterruptedException{
	byte funcOff = funcMapping.get(funcName);
	if(funcOff < 0)
		throw new IllegalArgumentException("Function name not in mapping");

	int payOff = 0;
	byte last = 0;
	int todo = payload.length;
	while(true){
		int curLen;
		if(todo <= A2J_MANY_PAYLOAD){
			last = (byte)1;
			curLen = todo;
		} else{
			curLen = A2J_MANY_PAYLOAD;
		}

		byte[] curPayload = new byte[curLen + A2J_MANY_HEADER];
		curPayload[0] = funcOff;
		curPayload[1] = last;
		ArduinoPacket.writeUnsignedInteger(payOff, curPayload, 2, 4);
		System.arraycopy(payload, payOff, curPayload, A2J_MANY_HEADER, curLen);
		ArduinoPacket ans = sendSyncByName("a2jMany", curPayload);
		if(ans.cmd != 0)
			throw new IOException("Error in a2jMany");
		if(last == 1)
			return ans.msg[0];
		if(ans.msg[0] != 0)
			return -ans.msg[0];
		payOff += curLen;
		todo -= curLen;
	}
}
//@}

/**
 Implements the actual transceiving. An instance of this class is used to create a Thread for each connection (i.e. if the device is disconnected,
 there is no additional thread running).
 */
private class ArduinoWorker implements Runnable{
	/** @addtogroup j2aframing java2arduino framing characters */
	//@{
	/** Start of a frame. */
	static final private byte A2J_SOF = 0x12;
	/** Escape character. */
	static final private byte A2J_ESC = 0x7D;
	//@}

	/** @addtogroup j2acrc java2arduino crc constants */
	//@{
	/** Constant to be added to the command offset byte. */
	public static final byte A2J_CRC_CMD = 11;
	/** Constant to be added to the length byte. */
	public static final byte A2J_CRC_LEN = 97;
	//@}

	/** @addtogroup j2aerrors java2arduino error values */
	//@{
	/** Function offset was out of bounds. */
	static final private byte A2J_RET_OOB = (byte)0xF0;
	/** Timeout while arduino2j#a2jProcess was receiving. */
	static final private byte A2J_RET_TO = (byte)0xF2;
	/** Checksum error while arduino2j#a2jProcess was receiving. */
	static final private byte A2J_RET_CHKSUM = (byte)0xF3;
	//@}

	final private ConcurrentRingBuffer<ArduinoPacket> sendQueue;
	final private BufferedInputStream in;
	final private BufferedOutputStream out;
	private boolean run = true;
	private byte seqNum = 0;

	ArduinoWorker(ConcurrentRingBuffer<ArduinoPacket> senderQueue, BufferedInputStream inputStream, BufferedOutputStream outputStream)
			throws IOException{
		sendQueue = senderQueue;
		in = inputStream;
		out = outputStream;
	}

	/**
	 Tells this instance to exit before the next write/read iteration. If there is an immediate exit required, one has to interrupt the executing thread
	 afterwards.
	 */
	private void shutdown(){
		run = false;
	}

	@Override
	public void run(){
		Exception lastEx = null;
		while(run){
			final ArduinoPacket req;
			try{
				req = sendQueue.take();
			} catch(InterruptedException e){
				// lets reevaluate the run condition
				continue;
			}
			byte cmd = (byte)(req.cmd - ArduinoPacket.PROCESSING);
			int len = (req.msg == null) ? 0 : req.msg.length;
			byte seq = seqNum;
			byte cSum = seq;
			seqNum++;
			cSum ^= A2J_CRC_CMD + (byte)req.cmd;
			cSum ^= A2J_CRC_LEN + len;

			try{
				fireActivityListeners(ArduinoActivityListener.STATE_ACTIVE);
				out.write(A2J_SOF);
				writeByte(seq);
				writeByte(cmd);
				writeByte(len);

				if(len > 0){
					for(int i = 0; i < len; i++){
						int tmp = req.msg[i];
						writeByte(tmp);
						cSum ^= tmp;
					}
				}
				writeByte(cSum);
				out.flush();


				// receiving...
				while(run){
					while(run && in.read() != A2J_SOF){
					}
					if(seq == readByte()){
						break;
					}
				}
				cmd = readByte();
				len = readByte()&0xFF; // read length of data array
				cSum = seq;
				cSum ^= A2J_CRC_CMD + cmd;
				cSum ^= A2J_CRC_LEN + len;
				byte[] msg = new byte[len];
				if(len > 0){
					for(int i = 0; i < len; i++){
						byte tmp = readByte();
						msg[i] = tmp;
						cSum ^= tmp;
					}
				}
				byte rSum = readByte();
				if(rSum != cSum){
					throw new EOFException("Checksum of received frame mismatched");
				}
				req.msg = msg;
				switch(cmd){
					case A2J_RET_OOB:
						throw new EOFException("Function offset was out of bounds");
					case A2J_RET_TO:
						int line = ((msg[0]&0xff)<<8) + (msg[1]&0xff);
						throw new TimeoutException("Timeout while peer was receiving around line " + line);
					case A2J_RET_CHKSUM:
						throw new EOFException("Checksum of sent frame mismatched");
				}
			} catch(EOFException e){
				// thrown by malformed frames... lets reevaluate the run condition
//				e.printStackTrace();
				lastEx = req.ex = e;
			} catch(TimeoutException e){
				// thrown by read timeouts... lets reevaluate the run condition
//				e.printStackTrace();
				lastEx = req.ex = e;
			} catch(InterruptedIOException e){
				// thrown by interrupted i/o operations... lets reevaluate the run condition
//				e.printStackTrace();
				lastEx = req.ex = e;
			} catch(IOException e){
				// thrown if the connection aborts (not interrupted) while we are reading, we better shutdown...?
				lastEx = req.ex = e;
				disconnect();
			} catch(RuntimeException e){
				e.printStackTrace(); // thrown by negative array indices etc. should not happen
				lastEx = req.ex = new IOException("Internal j2Arduino error in Worker: " + e.getMessage(), e);
			} finally{
				req.cmd = cmd; // marks the packet as done
				notifyListeners(req); // listeners of req need to be informed in all cases (normal, shutdown interrupt, connection abort)
				fireActivityListeners(ArduinoActivityListener.STATE_INACTIVE);
			}
		}
		synchronized(sendQueue){
			sendQueue.setEnabled(false);
			Iterator<ArduinoPacket> it = sendQueue.iterator();
			IOException endEx = new IOException("Connection closed before the request was fully processed", lastEx);
			while(it.hasNext()){
				ArduinoPacket p = it.next();
				if(p != null){
					p.ex = endEx;
					synchronized(p){
						p.notifyAll();
					}
				}
			}
		}
		try{
			in.close();
		} catch(IOException ignored){
		}
		try{
			out.close();
		} catch(IOException ignored){
		}
	}

	/**
	 Notifies all listeners of an ArduinoPacket that the processing finished.

	 If set the explicit listener of ArduinoPacket \a req will be called back to handle the answer and all threads, that synchronize on \a req will be
	 notified.

	 @param req the request that was processed
	 */
	private void notifyListeners(ArduinoPacket req){
		if(req.listener != null){
			req.listener.handleResponse(req);
		}
		synchronized(req){
			req.notifyAll();
		}
	}

	/**
	 Writes a byte to the OutputStream.

	 If the given argument has to be escaped, it writes #A2J_ESC first and then \a data-1. @see a2jframing

	 @param data the byte to write
	 @throws java.io.IOException if an I/O error occurs
	 */
	private void writeByte(int data) throws IOException{
		if(data == A2J_SOF || data == A2J_ESC){
			out.write(A2J_ESC);
			out.write(data - 1);
		} else{
			out.write(data);
		}
	}

	/**
	 Reads one byte from the InputStream.

	 Tries to read a byte from the InputStream with a timeout of #READ_TIMEOUT ms. If that byte indicates escaping, another byte is read and returned
	 after it has been incremented.

	 @return The next byte after de-escaping
	 @throws java.io.IOException If an I/O error occurs or #A2J_SOF is read first
	 */
	private byte readByte() throws IOException{
		byte data = (byte)(in.read());
		if(data == A2J_SOF)
			throw new EOFException("Unescaped delimiter character inside frame");
		if(data == A2J_ESC)
			data = (byte)(in.read() + 1);
		return data;
	}
}
}
