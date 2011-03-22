package j2arduino.util;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 A threadsafe ring buffer backed by a BlockingQueue.

 Instances of this class can be used to exchange (non-null) objects between multiple producer and consumer threads. Methods for blocking,
 uninterruptible blocking and uninterruptible blocking random access are provided. Only thread safety in the sense of serialized access to critical
 sections is guaranteed, producers as well as consumers may starve, if there are multiples of them.

 It is possible to disable adding and removing elements globally, which will raise exception if it is tried. A custom string can be set as detail
 message for those exceptions.
 * @param <E> the type of elements held in this collection
 */
public class ConcurrentRingBuffer<E>{

private final BlockingQueue<E> buf;
private boolean enabled;
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
	buf = new ArrayBlockingQueue<E>(size);
	enabled = true;
	disabledMsg = disabledMessage;
}

public boolean isEnabled(){
	return enabled;
}

public void setEnabled(boolean enabled){
	this.enabled = enabled;
}

/**
 Uninterruptible blocking take.

 It is not possible to interrupt this method. If the calling thread is blocked inside this method, interrupts sent to it will silently be ignored.

 @return an object previously added to this buffer */
public E takeUninterruptible(){
	while(true){
		try{
			return buf.take();
		} catch(InterruptedException ignored){
		}
	}
}

/**
 Blocking take.

 @return an object previously added to this buffer
 @throws InterruptedException if the calling thread is interrupted, while it waits for a new element. */
public E take() throws InterruptedException{
	return buf.take();
}

/**
 Uninterruptible blocking put.

 It is not possible to interrupt this method. If the calling thread is blocked inside this method, interrupts sent to it will silently be ignored.

 @param o the object to be stored in the buffer */
public void putUninterruptible(E o){
	while(true){
		try{
			put(o);
			break;
		} catch(InterruptedException ignored){
		}
	}
}

/**
 Blocking put.

 @param o the element to be stored
 @throws InterruptedException if the calling thread is interrupted, while it waits for free space in the buffer */
public void put(E o) throws InterruptedException{
	if(!enabled)
		throw new IllegalStateException(disabledMsg);
	buf.put(o);
}

public Iterator<E> iterator(){
	return buf.iterator();
}

public int size(){
	return buf.size();
}
}
