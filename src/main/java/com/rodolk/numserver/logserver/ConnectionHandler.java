package com.rodolk.numserver.logserver;

import java.net.Socket;

import com.rodolk.numserver.loggingprotocol.ApplicationProtocol;
import com.rodolk.numserver.loggingprotocol.ArrayProvider;
import com.rodolk.numserver.loggingprotocol.DataConsumer;
import com.rodolk.numserver.loggingprotocol.ProtocolException;

public class ConnectionHandler extends Subject implements Runnable {
    private ApplicationProtocol protocol_;
    private boolean error_ = false;
    
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
        protocol_.setTerminate();
    }

    @Override
    public void run() {
        if (error_) return;
        
        try {
            protocol_.execute();
            ConnectionHandlerEvent evt = new ConnectionHandlerEvent(2); //Connection closed
            notify(evt);
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
