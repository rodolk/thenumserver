package com.rodolk.numserver.logserver;

import com.rodolk.numserver.loggingprotocol.ArrayProvider;
import com.rodolk.numserver.loggingprotocol.DataConsumer;

public class DataProcessor implements DataConsumer {
    private BufferQueue queue_;
    
    public DataProcessor(BufferQueue queue) {
        queue_ = queue;
    }
    
    @Override
    public void processData(ArrayProvider.ArrayElement arrayElem, int initialPos, int totalNumbers, int numberLen) {
        arrayElem.setArrayValues(initialPos, totalNumbers, numberLen);
        queue_.add(arrayElem);
    }

}
