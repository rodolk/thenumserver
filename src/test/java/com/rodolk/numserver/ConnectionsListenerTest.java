package com.rodolk.numserver;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.Socket;

import com.rodolk.numserver.logserver.ConnectionsListener;
import com.rodolk.numserver.logserver.ConnectionsListener.NewConnectionSubscriber;

public class ConnectionsListenerTest {

	class SubscriberTest implements NewConnectionSubscriber {
		public int called = 0;
		@Override
		public void processConnection(Socket socket) {
			called++;
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	@Test
	public void testSubscribe() {
		final short kPort = 4000;
		SubscriberTest st = new SubscriberTest();
		ConnectionsListener connListener = new ConnectionsListener(kPort);
		connListener.subscribe(st);
		connListener.start();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			for(int i = 0; i < 100; i++) {
				Socket socket = new Socket("127.0.0.1", 4000);
				socket.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			int count = 0;
			while(st.called != 100 && count < 20) {
				Thread.sleep(1000);
				count++;
			}
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		connListener.setEnd();
		try {
			Socket socket = new Socket("127.0.0.1", 4000);
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(st.called, 100);
		int joinVal = 0;
		try {
			connListener.join(10000, 0);;
			joinVal++;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(joinVal, 1);
	}

	@Test
	public void testSetEnd() {
		//fail("Not yet implemented");
	}

}
