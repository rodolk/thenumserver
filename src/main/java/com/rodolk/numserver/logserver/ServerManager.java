package com.rodolk.numserver.logserver;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.rodolk.numserver.loggingprotocol.ApplicationProtocol;
import com.rodolk.numserver.loggingprotocol.ArrayProvider;
import com.rodolk.numserver.loggingprotocol.DataConsumer;
import com.rodolk.numserver.logserver.ConnectionHandler.ConnectionHandlerEvent;

public class ServerManager extends Observer implements ConnectionsListener.NewConnectionSubscriber {
	public static final int kMaxconnections_ = 5;
	public static final int kArrayProviderBuffers_ = kMaxconnections_ * 10;
	public static final short kPort_ = 4000;
	public static final int kMaxIntProcessors_ = 10;
	public static final String kFilename_ = "output.txt";
	private ExecutorService connectionHandlingExecutor_;
	private ExecutorService intArraysHandlingExecutor_;
	private int totalConnections_ = 0;
	private int totalClosedConnections_ = 0;
	private List<ConnectionHandler> connHandlerArr_ = new ArrayList<ConnectionHandler>();
	private List<IntProcessor> intProcessorArr_ = new ArrayList<IntProcessor>();
	private ArrayProvider connArrayProvider_;
	private ArrayProvider logArrayProvider_;
	private BufferQueue connArrayQueue_;
	private BufferQueue logArrayQueue_;
	private ConnectionsListener connListener_;
	private IntFileWriterThread fileWriterThread_;
	private boolean terminate_ = false;
	
	public ServerManager() {
		connectionHandlingExecutor_ = Executors.newFixedThreadPool(5);
		intArraysHandlingExecutor_  = Executors.newFixedThreadPool(10);
		connArrayProvider_ = new ArrayProvider(ApplicationProtocol.kArrayLen, kArrayProviderBuffers_);
		logArrayProvider_  = new ArrayProvider(ApplicationProtocol.kArrayLen, kArrayProviderBuffers_);
		connArrayQueue_    = new BufferQueue();
		logArrayQueue_     = new BufferQueue();
	}
	
	synchronized public boolean startAll() {
		if (terminate_) return false;
		IntStorageStrategy storageStrategy = new IntStorageTreeIntStrategy();
		for(int i = 0; i < kMaxIntProcessors_; i++) {
			IntProcessor processor = new IntProcessor(connArrayQueue_, logArrayQueue_, 
					connArrayProvider_, logArrayProvider_, storageStrategy);
			intProcessorArr_.add(processor);
			intArraysHandlingExecutor_.execute(processor);
		}
		try {
			fileWriterThread_ = new IntFileWriterThread(logArrayQueue_, logArrayProvider_, kFilename_);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		fileWriterThread_.start();
		connListener_ = new ConnectionsListener(kPort_);
		connListener_.subscribe(this);
		connListener_.start();
		return true;
	}
	
	public void execute() {
		while(!terminate_) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		terminateAll();
	}
	
	public void setTerminate() {
		terminate_ = true;
	}
	
	public void processEvent(Subject.Event evt) {
		if (evt.getType() == 1) {
			ConnectionHandlerEvent chEvt = (ConnectionHandlerEvent)evt;
			if (chEvt.getSubType() == 1) {
				setTerminate();
			} else if (chEvt.getSubType() == 2) {
				totalClosedConnections_++;
				if (totalClosedConnections_ == totalConnections_) {
					setTerminate();
				}
			}
		}
	}
	
	synchronized private boolean terminateAll() {
		for(ConnectionHandler connHandler : connHandlerArr_) {
			connHandler.setTerminate();
		}
		for(IntProcessor proc : intProcessorArr_) {
			proc.setTerminate();
		}
		fileWriterThread_.setTerminate();
		connArrayQueue_.setTerminated();
		logArrayQueue_.setTerminated();
		connArrayProvider_.setTerminated();
		logArrayProvider_.setTerminated();
		connListener_.setEnd();
		
		try {
			//Unblock ConnectionsListener thread blocked in accept
			Socket socket = new Socket("127.0.0.1", kPort_);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		connectionHandlingExecutor_.shutdown();
		intArraysHandlingExecutor_.shutdown();
		
		try {
			connListener_.join();
			fileWriterThread_.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	synchronized public void processConnection(Socket clientSocket) {
		if (terminate_) return;
		if (totalConnections_ < kMaxconnections_) {
			DataConsumer dataConsumer = new DataProcessor(connArrayQueue_);
			ConnectionHandler connHandler = new ConnectionHandler(clientSocket, connArrayProvider_, dataConsumer);
			connHandlerArr_.add(connHandler);
			connHandler.subscribe(this);
			connectionHandlingExecutor_.execute(connHandler);
			totalConnections_++;
		} else {
			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
