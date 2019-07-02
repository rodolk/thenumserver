package com.rodolk.numserver.logserver;

import java.io.FileWriter;
import java.io.IOException;

import com.rodolk.numserver.loggingprotocol.ArrayProvider;

public class IntFileWriterThread extends Thread {
	BufferQueue inQueue_;
	ArrayProvider inArrayProvider_;
	FileWriter fileWriter_;
	boolean terminate_ = false;
	
	public IntFileWriterThread(BufferQueue inQueue, ArrayProvider inArrayProvider, String filename) throws IOException {
		inQueue_ = inQueue;
		inArrayProvider_ = inArrayProvider;
		fileWriter_ = new FileWriter(filename);
	}
	
	public void setTerminate() {
		terminate_ = true;
	}
	
	public void run() {
		while(!terminate_) {
			ArrayProvider.ArrayElement inArrElem = inQueue_.poll();
			if (inArrElem != null) {
				try {
					fileWriter_.write(inArrElem.array_, inArrElem.initialPos_, inArrElem.totalNumbers_ * inArrElem.numberLen_);
					fileWriter_.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				} finally {
					inArrayProvider_.returnArray(inArrElem);
				}
			}
		}
		try {
			fileWriter_.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
