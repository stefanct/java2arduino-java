import j2arduino.util.ConcurrentRingBuffer;

import java.io.IOException;

public class ConcurrentRingBufferTest{
private ConcurrentRingBufferTest(){
}

public static void main(String[] args) throws IOException, InterruptedException{
	ConcurrentRingBuffer rb = new ConcurrentRingBuffer();
	Thread c = new Consumer(rb);
	Thread p = new Producer(rb, 1);
	Thread c2 = new Consumer(rb);
	Thread p2 = new Producer(rb, -1 );
	c.start();
  	c2.start();
	p.start();
	p2.start();
}

private static class Consumer extends Thread{
	private ConcurrentRingBuffer rb;

	Consumer(ConcurrentRingBuffer rb){
		this.rb = rb;
	}

	public void run(){
		int i = 0;
		while(true){
			Object o;
			try{
				o = rb.remove();
			} catch(InterruptedException e){
				break;
			}
			System.out.println(this.getName() + " taking '" + o+'\'');
			i = (i + 1) % rb.getBuf().length;
			synchronized(this){
				try{
					wait(1400);
				} catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}
	}
}

private static class Producer extends Thread{
	private ConcurrentRingBuffer rb;
	private int f;

	Producer(ConcurrentRingBuffer rb, int f){
		this.rb = rb;
		this.f = f;
	}

	public void run(){
		int i = 0;
		while(true){
			try{
				rb.add(this.getName() + " " + i);
			} catch(InterruptedException e){
				e.printStackTrace();
			}
			System.out.println(this.getName() + " added " + i);
			i = (f*i + 1) % rb.getBuf().length;
			synchronized(this){
				try{
					wait(1400);
				} catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}
	}
}
}
