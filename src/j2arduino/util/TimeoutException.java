package j2arduino.util;

import java.io.IOException;

/**
 A subtype of IOException thrown when a blocking operation times out. Blocking operations for which a timeout is specified need a means to indicate
 that the timeout has occurred. For many such operations it is possible to return a value that indicates timeout; when that is not possible or
 desirable then TimeoutException should be declared and thrown.
 */
public class TimeoutException extends IOException{

/** Constructs an TimeoutException with without a message or cause. */
public TimeoutException(){
	super();
}

/**
 Constructs an TimeoutException with the specified detail message, but without a cause.

 @param s the detail message */
public TimeoutException(String s){
	super(s);
}

/**
 Constructs an TimeoutException with the specified detail message and cause.

 @param s     the detail message
 @param cause the cause, that led to this */
public TimeoutException(String s, Throwable cause){
	super(s, cause);
}
}
