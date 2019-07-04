package com.rodolk.numserver.logserver;

import java.net.Socket;

import com.rodolk.numserver.loggingprotocol.ApplicationProtocol;
import com.rodolk.numserver.loggingprotocol.ArrayProvider;
import com.rodolk.numserver.loggingprotocol.DataConsumer;
import com.rodolk.numserver.loggingprotocol.ProtocolException;

/**
 * A ConnectionHandler handles one socket connection with an ApplicationProtocol
 * When the tread runs, it calls execute in the Application Protocol and the call only returns
 * when the connection finishes. A connection can finish because of erroneous data, because the client closed
 * the connection, because of a socket error, or because client sent 'terminate' and the whole 
 * application is shutting down.
 * This class inherits from Subject an notifies of the events occurred.
 * A DataConsumer is subscribed to the ApplicationProtocol to receive arrays with numbers
 * read from the connection.
 * 
 * @author rodolk
 *
 */
public class ConnectionHandler extends Subject implements Runnable {
    private ApplicationProtocol protocol_;
    private boolean error_ = false;
    private boolean terminated_ = false;
    
    public class ConnectionHandlerEvent extends Subject.Event {
        private int subType_ = 0;
        public ConnectionHandlerEvent(int subType) {
            subType_ = subType;
            setType(1);
        }
        public int getSubType() {
            return subType_;
        }
    }
    
    public ConnectionHandler(Socket socket, ArrayProvider provider, DataConsumer dataConsumer) {
        try {
            protocol_ = new ApplicationProtocol(socket, provider);
            protocol_.dataConsumerSubscribe(dataConsumer);
        } catch (ProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            error_ = true;
        }
    }
    
    public void setTerminate() {
        terminated_ = true;
        protocol_.setTerminate();
    }

    @Override
    public void run() {
        if (error_) return;
        
        try {
            protocol_.execute();
            if (terminated_) {
                ConnectionHandlerEvent evt = new ConnectionHandlerEvent(3); //Connection closed because application terminated
                notify(evt);
            } else {
                ConnectionHandlerEvent evt = new ConnectionHandlerEvent(2); //Connection closed by client
                notify(evt);
            }
            
        } catch (ProtocolException e) {
            if (e.getError() == ProtocolException.ErrorCode.END) {
                ConnectionHandlerEvent evt = new ConnectionHandlerEvent(1); //Process terminated by client
                notify(evt);
            } else {
                e.printStackTrace();
            }
        }
    }
}
