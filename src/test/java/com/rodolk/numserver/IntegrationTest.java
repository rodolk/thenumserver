package com.rodolk.numserver;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.rodolk.numserver.loggingprotocol.ApplicationProtocol;
import com.rodolk.numserver.loggingprotocol.ArrayProvider;
import com.rodolk.numserver.loggingprotocol.DataConsumer;
import com.rodolk.numserver.logserver.BufferQueue;
import com.rodolk.numserver.logserver.ConnectionHandler;
import com.rodolk.numserver.logserver.ConnectionsListener;
import com.rodolk.numserver.logserver.DataProcessor;
import com.rodolk.numserver.logserver.IntFileWriterThread;
import com.rodolk.numserver.logserver.IntProcessor;
import com.rodolk.numserver.logserver.IntStorageBitArrayStrategy;
import com.rodolk.numserver.logserver.IntStorageStrategy;
import com.rodolk.numserver.logserver.IntStorageTreeIntStrategy;
import com.rodolk.numserver.logserver.Observer;
import com.rodolk.numserver.logserver.ServerManager;
import com.rodolk.numserver.logserver.Statistics;
import com.rodolk.numserver.logserver.Subject;
import com.rodolk.numserver.logserver.ConnectionHandler.ConnectionHandlerEvent;

public class IntegrationTest {
    final static int kNumbers = 2000000; //2M
    public int storage1Tot = 0;
    public int storage2Tot = 0;
    public int numProcessed = 0;
    public int numWrong = 0;
    public long totalUniqueCounter_ = 0;
    public long totalPeriodUniqueCounter_ = 0;
    public long totalPeriodDuplicateCounter_ = 0;

    public class IntFileWriterThread extends Thread {
        BufferQueue inQueue_;
        ArrayProvider inArrayProvider_;
        FileWriter fileWriter_;
        boolean terminate_ = false;
        public IntStorageTreeIntStrategy store_ = new IntStorageTreeIntStrategy();
        public int outNum = 0;
        
        public IntFileWriterThread(BufferQueue inQueue, ArrayProvider inArrayProvider, String filename) throws IOException {
            inQueue_ = inQueue;
            inArrayProvider_ = inArrayProvider;
        }
        
        public void setTerminate() {
            terminate_ = true;
        }
        
        public void run() {
            while(!terminate_) {
                ArrayProvider.ArrayElement inArrElem = inQueue_.poll();
                if (inArrElem != null) {
                    int numIdx;
                    for(numIdx = 0; numIdx < inArrElem.totalNumbers_; numIdx++) {
                        boolean contains = store_.contains(inArrElem.array_, 
                                          (inArrElem.initialPos_ + (numIdx * inArrElem.numberLen_)), 
                                          inArrElem.numberLen_ - inArrElem.newLineLen_);
                        if (contains != false) {
                            numWrong++;
                        }
                        outNum++;
                    }
                    
                    inArrayProvider_.returnArray(inArrElem);
                }
            }
            storage2Tot = store_.getSize();
            numProcessed = outNum;
        }
    }

    public class ServerManagerTest extends Observer implements ConnectionsListener.NewConnectionSubscriber {
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
        IntStorageStrategy storageStrategyCopy;
        private Statistics statsInstance_ = null;
        private Integer syncObject_ = Integer.valueOf(1);
        
        public ServerManagerTest() {
            connectionHandlingExecutor_ = Executors.newFixedThreadPool(5);
            intArraysHandlingExecutor_  = Executors.newFixedThreadPool(10);
            ApplicationProtocol.systemNewLine = false;
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
            storageStrategyCopy = storageStrategy;
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
                        syncObject_.wait(10000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    statsInstance_.setSnapshot();
                    long periodDuplicates = statsInstance_.getPeriodDuplicateCounter();
                    long periodUniques = statsInstance_.getPeriodUniqueCounter();
                    long uniqueTotal = statsInstance_.getTotalUniqueCounter();
                    
                    //For the test
                    totalPeriodUniqueCounter_ += periodUniques; 
                    totalPeriodDuplicateCounter_ += periodDuplicates;
                    
                    System.out.println(new Date() + " - Received " + periodUniques + "unique numbers, " + 
                            periodDuplicates + "duplicates. Unique total: " + uniqueTotal);
                }
            }
            totalUniqueCounter_ = statsInstance_.getTotalUniqueCounter();
            
            storage1Tot = storageStrategyCopy.getSize();
            
            terminateAll();
        }
        
        public void setTerminate() {
            synchronized(syncObject_) {
                terminate_ = true;
            }
            syncObject_.notify();
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
            System.out.println("Terminating all");
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
        
    public class TestClient extends Thread {
        public int amount_ = kNumbers;
        public int type_ = 0;
        public int numLen = 9;
        public int newLineEnd = 2;
        
        public void run() {
            try {
                Socket socket = new Socket("127.0.0.1", 4000);
                OutputStream os = socket.getOutputStream();
                if (type_ == 0) {
                    for(int i = 0; i < amount_; i++) {
                        String valString = String.format("%09d\r\n",  i);
                        char[] arr = valString.toCharArray();
                        for(int j =0; j < (numLen + newLineEnd); j++) {
                            os.write(arr[j]);
                        }
                    }
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    char[] terminateArray = {'t', 'e', 'r', 'm', 'i', 'n', 'a', 't', 'e', '\r', '\n'};
                    for(int j =0; j < (numLen + newLineEnd); j++) {
                        os.write(terminateArray[j]);
                    }
                } else if (type_ == 1) {
                    for(int i = amount_ - 1; i >= 0; i--) {
                        String valString = String.format("%09d\r\n",  i);
                        char[] arr = valString.toCharArray();
                        for(int j =0; j < (numLen + newLineEnd); j++) {
                            os.write(arr[j]);
                        }
                    }
                } else if (type_ == 2) {
                    for(int i = 10000; i < amount_ - 10000; i++) {
                        String valString = String.format("%09d\r\n",  i);
                        char[] arr = valString.toCharArray();
                        for(int j =0; j < (numLen + newLineEnd); j++) {
                            os.write(arr[j]);
                        }
                    }
                }
                socket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    @Test
    public void test() {
        ServerManagerTest sm = new ServerManagerTest();
        sm.startAll();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        TestClient client1 = new TestClient();
        client1.type_ = 0;
        client1.start();
        TestClient client2 = new TestClient();
        client2.type_ = 1;
        client2.start();
        TestClient client3 = new TestClient();
        client3.type_ = 2;
        client3.start();
        TestClient client4 = new TestClient();
        client4.type_ = 0;
        client4.start();
        TestClient client5 = new TestClient();
        client5.type_ = 1;
        client5.start();

        sm.execute();
        
        assertEquals(storage1Tot, storage2Tot);
        assertEquals(storage2Tot, kNumbers);
        assertEquals(numProcessed, kNumbers);
        assertEquals(numWrong, 0);
        assertEquals(totalUniqueCounter_, kNumbers);
        assertEquals(totalPeriodUniqueCounter_, kNumbers);
        assertEquals(totalPeriodDuplicateCounter_, (kNumbers * 4 - 20000));

        System.out.println("Total duplicates: " + totalPeriodDuplicateCounter_);
        
        System.out.println("Server execution finished");
    }

}
