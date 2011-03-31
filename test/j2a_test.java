import j2arduino.ArduinoGateway;
import j2arduino.devices.Arduino;

import javax.usb.UsbException;
import java.io.IOException;
import java.util.*;

public class j2a_test{

private j2a_test(){
}

static public void main(String[] args) throws IOException{
	ArduinoGateway ag = ArduinoGateway.getInstance();
	try{
		final Iterator<Arduino> iterator = ag.getAvailableArduinos(true).iterator();
		if(!iterator.hasNext()){
			System.err.println("No remote device found, bailing.");
			return;
		}
		Arduino a = iterator.next();
		try{
			a.connect(null);
			int[] ints = stresstestEcho(a);
			System.out.format("stresstest a2jEcho %5s (%dB of %dB came back correctly)%n",
			                  ints[1] == 0 ? "OK" : "FAILED",
			                  ints[0],
			                  ints[0] + ints[1]);
			final int maxPayload = Arduino.A2J_MAX_PAYLOAD * 1000;
			final int maxPayloadPlaces = (int)Math.ceil(Math.log10(maxPayload)) + 1;
			ints = stresstestEchoMany(a, maxPayload, maxPayloadPlaces);
			System.out.format("stresstest a2jEchoMany %" + maxPayloadPlaces + "s (%dB of %dB came back correctly)%n",
			                  ints[1] == 0 ? "OK" : "FAILED",
			                  ints[0],
			                  ints[0] + ints[1]);

		} catch(Exception e){
			e.printStackTrace();
		}
		a.disconnect();
	} catch(InterruptedException e){
		e.printStackTrace();
	} catch(IOException e){
		e.printStackTrace();
	}
}

private static int[] stresstestEcho(Arduino a) throws InterruptedException{
	int i = 2;
	int ok = 0;
	int failed = 0;
	Random rand = new Random(0);
	while(i-- > 0){
		int len = rand.nextInt(Arduino.A2J_MAX_PAYLOAD + 1);
		byte[] payload = new byte[len];
		rand.nextBytes(payload);

		System.err.format("sending normal payload (%3dB): %s%n", len, toHexString(payload));
		try{
			final byte[] ans = a.sendSyncByName("a2jEcho", payload).msg;
			System.err.format("received               (%3dB): %s%n", ans.length, toHexString(ans));
			if(!Arrays.equals(ans, payload)){
				System.err.println("FAILED");
				int payloadLength = payload.length;
				int j = 0;
				for(; j < payloadLength; j++){
					if(ans[j] != payload[j])
						break;
				}
				ok += j;
				failed += (payloadLength - j);
			} else{
				System.err.println("OK");
				ok += ans.length;
			}
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	return new int[] {ok, failed};
}

private static int[] stresstestEchoMany(Arduino a, final int maxPayload, final int maxPayloadPlaces) throws InterruptedException{
	int i = 50;
	int ok = 0;
	int failed = 0;
	Random rand = new Random(0);
	while(i-- > 0){
		int len = rand.nextInt(maxPayload);
		byte[] payload = new byte[len];
		rand.nextBytes(payload);
		System.err.format("sending long payload (%" + maxPayloadPlaces + "dB): %s%n", len, toHexString(payload));
		try{
			final byte[] ans = a.sendLongByName("a2jEchoMany", payload).msg;
			System.err.format("received             (%" + maxPayloadPlaces + "dB): %s%n", ans.length, toHexString(ans));
			if(!Arrays.equals(ans, payload)){
				int payloadLength = payload.length;
				int j = 0;
				for(; j < payloadLength; j++){
					if(ans[j] != payload[j])
						break;
				}
				ok += j;
				failed += (payloadLength - j);
			} else{
				System.err.println("OK");
				ok += ans.length;
			}
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	return new int[] {ok, failed};
}

private static char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

/**
 Fast convert a byte array to a hex string with possible leading zero.

 @param b array of bytes to convert to string
 @return hex representation, two chars per byte. */
public static String toHexString(byte[] b){
	StringBuilder sb = new StringBuilder(b.length<<1);
	for(byte aB : b){
		// look up high nibble char
		sb.append(hexChar[(aB&0xf0)>>>4]);

		// look up low nibble char
		sb.append(hexChar[aB&0x0f]);
	}
	return sb.toString();
}
}