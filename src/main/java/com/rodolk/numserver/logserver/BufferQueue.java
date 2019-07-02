package com.rodolk.numserver.logserver;

import java.util.LinkedList;

import com.rodolk.numserver.loggingprotocol.ArrayProvider;

public class BufferQueue {
	private LinkedList<ArrayProvider.ArrayElement> linkedList_;
	private boolean terminated_ = false;
	
	public BufferQueue() {
		linkedList_ = new LinkedList<ArrayProvider.ArrayElement>();
	}
	
	synchronized public boolean add(ArrayProvider.ArrayElement arrayElem) {
		linkedList_.add(arrayElem);
		notify();
		return true;
	}
	
	synchronized public void setTerminated() {
		terminated_ = true;
		notifyAll();
	}
	
	synchronized public ArrayProvider.ArrayElement poll() {
		while(linkedList_.isEmpty() && !terminated_) {
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (terminated_) {
			return null;
		} else {
			return linkedList_.poll();
		}
	}
}
