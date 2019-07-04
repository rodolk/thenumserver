package com.rodolk.numserver.logserver;

import com.rodolk.numserver.loggingprotocol.ArrayProvider;
import com.rodolk.numserver.loggingprotocol.DataConsumer;

/**
 * DataProcessor implements interface DataConsumer.
 * When a new ArrayElem is received as argument of processData, it adds it to the BufferQueue.
 * 
 * @author rodolk
 *
 */
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
