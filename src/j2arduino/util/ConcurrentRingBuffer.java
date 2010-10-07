package j2arduino.util;

/**
 A threadsafe ring buffer backed by an Array.

 Instances of this class can be used to exchange (non-null) objects between multiple producer and consumer threads. Methods for blocking,
 uninterruptible blocking and uninterruptible blocking random access are provided. Only thread safety in the sense of serialized access to critical
 sections is guaranteed, producers as well as consumers may starve, if there are multiples of them.

 It is possible to disable adding and removing elements globally, which will raise exception if it is tried. A custom string can be set as detail
 message for those exceptions.

 */
public class ConcurrentRingBuffer{
private final Object[] buf;
private int read;
private int write;
private String disabledMsg;

/** Creates a new ring buffer with 8 elements and a default disabled message. */
public ConcurrentRingBuffer(){
	this(8, "ConcurrentRingBuffer disabled");
}

/**
 Creates a new ring buffer.

 @param size            number of elements, that the buffer can hold at a time
 @param disabledMessage the detail message of exceptions raised when the buffer is disabled */
public ConcurrentRingBuffer(int size, String disabledMessage){
	buf = new Object[size];
	write = 0;
	read = 0;
	disabledMsg = disabledMessage;
}

/**
 Uninterruptible blocking remove.

 It is not possible to interrupt this method. If the calling thread is blocked inside this method, interrupts sent to it will silently be ignored.


 @return an object previously added to this buffer */
public Object removeUninterruptible(){
	while(true){
		try{
			return remove();
		} catch(InterruptedException ignored){
		}
	}
}

/**
 Blocking remove.

 @return an object previously added to this buffer
 @throws InterruptedException if the calling thread is interrupted, while it waits for a new element. */
public Object remove() throws InterruptedException{
	Object o;
	synchronized(buf){
		o = remove(read);
		read = (read + 1) % buf.length;
	}
	return o;
}

/**
 Blocking remove with random access.

 This method allows to access a specific element of the buffer. This can be for example be used to implement a primitive concurrent hashmap.

 @param index the index of the element to access
 @return the element at index \a index of the storing array
 @throws InterruptedException if the calling thread is interrupted, while it waits for a new element */
public Object remove(int index) throws InterruptedException{
	Object o;
	synchronized(buf){
		while(true){
			if(read < 0)
				throw new IllegalStateException(disabledMsg);
			if(buf[index] != null)
				break;
			buf.wait();
		}
		o = buf[index];
		buf[index] = null;
		buf.notifyAll();
	}
	return o;
}

/**
 Uninterruptible blocking add.

 It is not possible to interrupt this method. If the calling thread is blocked inside this method, interrupts sent to it will silently be ignored.

 @param o the object to be stored in the buffer */
public void addUninterruptible(Object o){
	while(true){
		try{
			add(o);
			break;
		} catch(InterruptedException ignored){
		}
	}
}

/**
 Blocking add.

 @param o the element to be added
 @throws InterruptedException if the calling thread is interrupted, while it waits for free space in the buffer */
public void add(Object o) throws InterruptedException{
	synchronized(buf){
		add(o, write);
		write = (write + 1) % buf.length;
	}
}

/**
 Blocking add with index.

 Adds an object at a specific location in this buffer.

 @param o     the element to be stored
 @param index the index of the buffer to be used to store \a o
 @throws InterruptedException if the calling thread is interrupted, while it waits for free space in the buffer */
public void add(Object o, int index) throws InterruptedException{
	if(o == null)
		throw new NullPointerException("Storing 'null' in a ConcurrentRingBuffer is not allowed");
	synchronized(buf){
		while(true){
			if(write < 0)
				throw new IllegalStateException(disabledMsg);
			if(!(buf[index] != null))
				break;
			buf.wait();
		}
		buf[index] = o;
		buf.notifyAll();
	}
}

/**
 Returns the array, that backs this buffer.

 Use with caution!

 @return the actual storing array of this buffer */
public Object[] getBuf(){
	return buf;
}

/** Disables addition and removal of elements to/from this buffer. */
public void disable(){
	synchronized(buf){
		read = -1;
		write = -1;
		buf.notifyAll();
	}
}
}
