package com.rodolk.numserver.logserver;

import com.rodolk.numserver.loggingprotocol.ArrayProvider;

public class IntProcessor implements Runnable {
	BufferQueue inputQueue_;
	BufferQueue outputQueue_;
	ArrayProvider inputArrayProvider_;
	ArrayProvider outputArrayProvider_;
	IntStorageStrategy intStorage_;
	Boolean terminate_ = false;
	
	public IntProcessor(BufferQueue inQueue, BufferQueue outQueue, ArrayProvider in, 
			ArrayProvider out, IntStorageStrategy intStorage) {
		inputQueue_ = inQueue;
		outputQueue_ = outQueue;
		inputArrayProvider_ = in; 
		outputArrayProvider_ = out;
		intStorage_ = intStorage;
	}
	
	public void setTerminate() {
		terminate_ = true;
	}
	
	@Override
	public void run() {
		while(!terminate_) {
			ArrayProvider.ArrayElement arrayElemOut = outputArrayProvider_.getArray();
			if (arrayElemOut != null) {
				ArrayProvider.ArrayElement arrayElemIn = inputQueue_.poll();
				if (arrayElemIn != null) {
					int numIdx;
					int outNum = 0;
					for(numIdx = 0; numIdx < arrayElemIn.totalNumbers_; numIdx++) {
						boolean contains = intStorage_.contains(arrayElemIn.array_, 
										  (arrayElemIn.initialPos_ + (numIdx * arrayElemIn.numberLen_)), 
										  arrayElemIn.numberLen_);
						if (!contains) {
							for(int j = 0; j < arrayElemIn.numberLen_; j++) {
								arrayElemOut.array_[(outNum * arrayElemIn.numberLen_) + j] = 
									arrayElemIn.array_[(arrayElemIn.initialPos_ + (numIdx * arrayElemIn.numberLen_)) + j];
							}
							outNum++;
						}
					}
					arrayElemOut.setArrayValues(0, outNum, 10);
					outputQueue_.add(arrayElemOut);
					inputArrayProvider_.returnArray(arrayElemIn);
				} else {
					outputArrayProvider_.returnArray(arrayElemOut);
				}
			}
		}
	}
}
