package com.rodolk.numserver.loggingprotocol;

import java.util.LinkedList;

public class ArrayProvider {
	private LinkedList<ArrayElement> freeCharArrayList_;
	private int quantity_;
	private int arrayLen_;
	private boolean terminated_ = false;
	
	public class ArrayElement {
		public char[] array_ = null;
		public int initialPos_ = 0;
		public int totalNumbers_ = 0;
		public int numberLen_ = 0;
		
		public void setArray(char[] charArray, int initialPos, int totalNumbers, int numberLen) {
			array_ = charArray;
			initialPos_ = initialPos;
			totalNumbers_ = totalNumbers;
			numberLen_ = numberLen;
		}
		public void setArrayValues(int initialPos, int totalNumbers, int numberLen) {
			initialPos_ = initialPos;
			totalNumbers_ = totalNumbers;
			numberLen_ = numberLen;
		}
	}
	
	public ArrayProvider(int len, int initialValue) {
		freeCharArrayList_ =  new LinkedList<ArrayElement>();
		quantity_ = initialValue;
		arrayLen_ = len;
		while(0 < initialValue--) {
			ArrayElement elem = new ArrayElement();
			elem.array_ = new char[len];
			freeCharArrayList_.add(elem);
		}
	}
	
	synchronized public void setTerminated() {
		terminated_ = true;
		notifyAll();
	}

	synchronized public ArrayElement getArray() {
		while (freeCharArrayList_.isEmpty() && !terminated_) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}
		if (terminated_) {
			return null;
		} else {
			ArrayElement retElem = freeCharArrayList_.pollFirst();
			return retElem;
		}
	}
	
	synchronized public void returnArray(ArrayElement elem) {
		freeCharArrayList_.add(elem);
		notify();
	}
}