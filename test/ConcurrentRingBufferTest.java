import j2arduino.util.ConcurrentRingBuffer;

import java.io.IOException;

public class ConcurrentRingBufferTest{
private ConcurrentRingBufferTest(){
}

public static void main(String[] args) throws IOException, InterruptedException{
	ConcurrentRingBuffer<String> rb = new ConcurrentRingBuffer<String>();
	Thread c1 = new Consumer(rb, "c1");
	Thread p1 = new Producer(rb, "p1");
	Thread c2 = new Consumer(rb, "c2");
	Thread p2 = new Producer(rb, "p2");
	c1.start();
  	c2.start();
	p1.start();
	p2.start();
}

private static class Consumer extends Thread{
	private ConcurrentRingBuffer<String> rb;

	Consumer(ConcurrentRingBuffer<String> rb, String name){
		super(name);
		this.rb = rb;
	}

	@Override
	public void run(){
		int i = 0;
		while(true){
			String o;
			try{
				o = rb.take();
			} catch(InterruptedException e){
				break;
			}
			System.out.println(this.getName() + " taking '" + o+'\'');
			i++;
			synchronized(this){
				try{
					wait(400);
				} catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}
	}
}

private static class Producer extends Thread{
	private ConcurrentRingBuffer<String> rb;

	Producer(ConcurrentRingBuffer<String> rb, String name){
		super(name);
		this.rb = rb;
	}

	public void run(){
		int i = 0;
		while(true){
			try{
				rb.put(this.getName() + " " + i);
			} catch(InterruptedException e){
				e.printStackTrace();
			}
			System.out.println(this.getName() + " added " + i);
			i++;
			synchronized(this){
				try{
					wait(300);
				} catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}
	}
}
}
