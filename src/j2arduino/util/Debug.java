package j2arduino.util;

import j2arduino.Arduino;
import j2arduino.ArduinoPacket;

import java.io.IOException;
import java.io.InterruptedIOException;

/** A thread that polls its counterpart #a2jDebug periodically and prints the results to stderr. */
public class Debug extends Thread{
private Arduino arduino;
private boolean run = true;
/** The offset of the a2j function to be queried. */
public final int dbgCmd;

/**
 Creates a new Debug thread and fetches the offset of a2jDebug from the ArduinoFunctionMapping of Arduino \a a.

 @param a the Arduino to debug (and to provide the ArduinoFunctionMapping) */
public Debug(Arduino a){
	this(a, -1);
}

/**
 Creates a new Debug thread and sets the offset of a2jDebug. If \a debugCommand is less than -1, the actual offset will be fetched from the
 ArduinoFunctionMapping of Arduino \a a.

 @param a            the Arduino to debug
 @param debugCommand the offset to be set */
public Debug(Arduino a, int debugCommand){
	super(a.address + "-debug-thread");
	arduino = a;

	if(debugCommand < 0)
		dbgCmd = arduino.getFuncMapping().get("a2jDebug");
	else
		dbgCmd = debugCommand;
}

/** Fetches debug output from \a arduino. */
public void run(){
	while(run){
		try{
			ArduinoPacket ans = arduino.sendSync(new ArduinoPacket(dbgCmd, null, null));
			if(ans.msg == null){
				continue;
			}
			byte[] msg = ans.msg;
			int len = msg.length;
			if(len > 0){
				System.err.println("Debug: ");
				for(int i = 0; i < len; i++){
					System.err.print((char)msg[i]);
				}
				System.err.println();
			}
			sleep(15000);
		} catch(IllegalStateException e){
			// disconnect detected, shut down debug
			System.err.println(getName() + " shutting down because: " + e.getMessage());
			run = false;
			// e.printStackTrace();
		} catch(InterruptedException e){
			// if sleep or sending get interrupted
		} catch(InterruptedIOException e){
			// if transfer gets interrupted
			System.err.println(getName() + " transfer was interrupted, loss of debug messages possible");
			e.printStackTrace();
		} catch(TimeoutException e){
			// if transfer times out
			System.err.println(getName() + " transfer timed out, loss of debug messages possible");
			e.printStackTrace();
		} catch(IOException e){
			// probably ignorable, because we will get shut down automatically anyway
			System.err.println(getName() + " caught IOException: " + e.getMessage() + " - Shutting down.");
			run = false;
			// e.printStackTrace();
		} catch(Exception e){
			System.err.println(getName() + " caught general Exception: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

/** Schedules the shutdown of this thread. */
public void shutdown(){
	if(run){
		run = false;
		this.interrupt();
		System.err.println(getName() + " shut down externally");
	}
}
}
