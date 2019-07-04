package com.rodolk.numserver.logserver;

import com.rodolk.numserver.loggingprotocol.ArrayProvider;

/**
 * This class reads ArrayElement's from a BufferQueue and determines if the numbers in the array are
 * unique or duplicate by calling the contains method of IntStorageStrategy. Unique numbers are stored
 * in the container.
 * Then it creates a new ArrayElement with unique numbers and it adds it to an output BufferQueue.
 * 
 * @author rodolk
 *
 */
public class IntProcessor implements Runnable {
    BufferQueue inputQueue_;
    BufferQueue outputQueue_;
    ArrayProvider inputArrayProvider_;
    ArrayProvider outputArrayProvider_;
    IntStorageStrategy intStorage_;
    Boolean terminate_ = false;
    Statistics.StatisticsCounter statsCounter_;
    
    public IntProcessor(BufferQueue inQueue, BufferQueue outQueue, ArrayProvider in, 
            ArrayProvider out, IntStorageStrategy intStorage, Statistics.StatisticsCounter statsCounter) {
        inputQueue_ = inQueue;
        outputQueue_ = outQueue;
        inputArrayProvider_ = in; 
        outputArrayProvider_ = out;
        intStorage_ = intStorage;
        statsCounter_ = statsCounter;
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
                        //Ask storage if integer exists. Remove new line characters
                        boolean contains = intStorage_.contains(arrayElemIn.array_, 
                                          (arrayElemIn.initialPos_ + (numIdx * arrayElemIn.numberLen_)), 
                                          arrayElemIn.numberLen_ - arrayElemIn.newLineLen_);
                        if (!contains) {
                            for(int j = 0; j < arrayElemIn.numberLen_; j++) {
                                arrayElemOut.array_[(outNum * arrayElemIn.numberLen_) + j] = 
                                    arrayElemIn.array_[(arrayElemIn.initialPos_ + (numIdx * arrayElemIn.numberLen_)) + j];
                            }
                            statsCounter_.incUnique();
                            outNum++;
                        } else {
                            statsCounter_.incDuplicate();
                        }
                    }
                    arrayElemOut.setArrayValues(0, outNum, arrayElemIn.numberLen_);
                    outputQueue_.add(arrayElemOut);
                    inputArrayProvider_.returnArray(arrayElemIn);
                } else {
                    outputArrayProvider_.returnArray(arrayElemOut);
                }
            }
        }
    }
}
