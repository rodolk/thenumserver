package com.rodolk.numserver.logserver;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.rodolk.numserver.loggingprotocol.ApplicationProtocol;
import com.rodolk.numserver.loggingprotocol.ArrayProvider;
import com.rodolk.numserver.loggingprotocol.DataConsumer;
import com.rodolk.numserver.logserver.ConnectionHandler.ConnectionHandlerEvent;

/**
 * This class manages the whole server.
 * startAll must be called before calling execute()
 * 
 * @author rodolk
 *
 */
public class ServerManager extends Observer implements ConnectionsListener.NewConnectionSubscriber {
    public static final int kMaxconnections_ = 5;
    public static final int kArrayProviderBuffers_ = kMaxconnections_ * 10;
    public static final short kPort_ = 4000;
    public static final int kMaxIntProcessors_ = 10;
    public static final String kFilename_ = "numbers.log";
    public static final int kSamplePeriodMS_ = 10000;
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
    private Statistics statsInstance_ = null;
    private Integer syncObject_ = Integer.valueOf(1);
    
    public ServerManager() {
        connectionHandlingExecutor_ = Executors.newFixedThreadPool(5);
        intArraysHandlingExecutor_  = Executors.newFixedThreadPool(10);
        connArrayProvider_ = new ArrayProvider(ApplicationProtocol.kArrayLen, kArrayProviderBuffers_, ApplicationProtocol.getNewLineLen());
        logArrayProvider_  = new ArrayProvider(ApplicationProtocol.kArrayLen, kArrayProviderBuffers_, ApplicationProtocol.getNewLineLen());
        connArrayQueue_    = new BufferQueue();
        logArrayQueue_     = new BufferQueue();
        Statistics.initialize(kMaxIntProcessors_);
        statsInstance_ = Statistics.getInstance();
    }
    
    synchronized public boolean startAll() {
        if (terminate_) return false;
        IntStorageStrategy storageStrategy = new IntStorageBitArrayStrategy();
        Statistics.StatisticsCounter[] statsCounterArr = statsInstance_.getStatisticsCounterArray();
        for(int i = 0; i < kMaxIntProcessors_; i++) {
            IntProcessor processor = new IntProcessor(connArrayQueue_, logArrayQueue_, 
                    connArrayProvider_, logArrayProvider_, storageStrategy, statsCounterArr[i]);
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
        synchronized(syncObject_) {
            while(!terminate_) {
                try {
                    syncObject_.wait(kSamplePeriodMS_);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                statsInstance_.setSnapshot();
                long periodDuplicates = statsInstance_.getPeriodDuplicateCounter();
                long periodUniques = statsInstance_.getPeriodUniqueCounter();
                long uniqueTotal = statsInstance_.getTotalUniqueCounter();
                
                System.out.println(new Date() + " - Received " + periodUniques + " unique numbers, " + 
                        periodDuplicates + " duplicates. Unique total: " + uniqueTotal);
            }
        }
        
        terminateAll();
        
        statsInstance_.setSnapshot();
        long periodDuplicates = statsInstance_.getPeriodDuplicateCounter();
        long periodUniques = statsInstance_.getPeriodUniqueCounter();
        long uniqueTotal = statsInstance_.getTotalUniqueCounter();
        
        System.out.println(new Date() + " - Final report---Received " + periodUniques + " unique numbers, " + 
                periodDuplicates + " duplicates. Unique total: " + uniqueTotal);
    }
    
    public void setTerminate() {
        synchronized(syncObject_) {
            terminate_ = true;
            syncObject_.notify();
        }
    }
    
    public void processEvent(Subject.Event evt) {
        if (evt.getType() == 1) {
            ConnectionHandlerEvent chEvt = (ConnectionHandlerEvent)evt;
            if (chEvt.getSubType() == 1) {
                totalClosedConnections_++;
                setTerminate();
            } else if (chEvt.getSubType() == 2) {
                totalClosedConnections_++;
                if (totalClosedConnections_ == totalConnections_) {
                    setTerminate();
                }
            } else if (chEvt.getSubType() == 3) {
                totalClosedConnections_++;
            }
        }
    }
    
    synchronized private boolean terminateAll() {
        //First stop all connections
        for(ConnectionHandler connHandler : connHandlerArr_) {
            connHandler.setTerminate();
        }
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
        try {
            if (!connectionHandlingExecutor_.awaitTermination(60, TimeUnit.SECONDS)) {
                connectionHandlingExecutor_.shutdownNow();
            }
        } catch (InterruptedException ex) {
            System.out.println("Exception waiting for connection handling threads to terminate");
            connectionHandlingExecutor_.shutdownNow();
        }
        
        //Check queues are empty
        
        //A little dirty but this is the end
        while(!connArrayQueue_.isEmpty()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        //A little dirty but this is the end
        while(!logArrayQueue_.isEmpty()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        //A little dirty but this is the end
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for(IntProcessor proc : intProcessorArr_) {
            proc.setTerminate();
        }

        connArrayQueue_.setTerminated();
        logArrayQueue_.setTerminated();
        connArrayProvider_.setTerminated();
        logArrayProvider_.setTerminated();
        intArraysHandlingExecutor_.shutdown();
        
        try {
            if (!intArraysHandlingExecutor_.awaitTermination(60, TimeUnit.SECONDS)) {
                intArraysHandlingExecutor_.shutdownNow();
            }
        } catch (InterruptedException ex) {
            System.out.println("Exception waiting for numbers handling threads to terminate");
            intArraysHandlingExecutor_.shutdownNow();
        }

        fileWriterThread_.setTerminate();
        
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
