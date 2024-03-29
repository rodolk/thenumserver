package com.rodolk.numserver.loggingprotocol;

import java.util.LinkedList;

/**
 * ArrayProvider contains pre-allocated arrays to be used by other classes that need
 * to read data or process data. This is necessary to improve performance and avoid
 * continuous allocation and free of memory which also generates memory fragmentation.
 * freeCharArrayList_ is a LinkedList of ArrayElement that are retrieved by clients,
 * When they are not used anymore, ArrayElement's are returned and added to the end of 
 * the linked list. 
 * 
 * @author rodolk
 *
 */
public class ArrayProvider {
    private LinkedList<ArrayElement> freeCharArrayList_;
    private int quantity_;
    private int arrayLen_;
    private boolean terminated_ = false;
    
    /**
     * ArrayElement contains the array of characters and metadata:
     * <ul>
     * <li> initialPos_     is the offset of data in the array
     * <li> totalNumbers_   is the amount of numbers with new line characters in the array
     * <li> numberLen_      is the length of each number including new line characters
     * <li> newLineLen_     is the length of new line characters
     * </ul>
     * 
     * @author rodolk
     *
     */
    public class ArrayElement {
        public char[] array_ = null;
        public int initialPos_ = 0;
        public int totalNumbers_ = 0;
        public int numberLen_ = 0; //Length of each number including new line characters
        public int newLineLen_ = 0; //Length of new line
        
        private ArrayElement(int nlLen) {
            newLineLen_ = nlLen;
        }
        public void setArray(char[] charArray, int initialPos, int totalNumbers, int numberLen) {
            array_ = charArray;
            initialPos_ = initialPos;
            totalNumbers_ = totalNumbers;
            numberLen_ = numberLen;
        }
        public void setArrayValues(int initialPos, int totalNumbers, int numberLen) {
            initialPos_ = initialPos;
            totalNumbers_ = totalNumbers;
            numberLen_ = numberLen;
        }
    }
    
    public ArrayProvider(int len, int initialValue, int newLineLen) {
        freeCharArrayList_ =  new LinkedList<ArrayElement>();
        quantity_ = initialValue;
        arrayLen_ = len;
        while(0 < initialValue--) {
            ArrayElement elem = new ArrayElement(newLineLen);
            elem.array_ = new char[len];
            freeCharArrayList_.add(elem);
        }
    }
    
    synchronized public void setTerminated() {
        terminated_ = true;
        notifyAll();
    }

    synchronized public ArrayElement getArray() {
        while (freeCharArrayList_.isEmpty() && !terminated_) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        }
        if (terminated_) {
            return null;
        } else {
            ArrayElement retElem = freeCharArrayList_.pollFirst();
            return retElem;
        }
    }
    
    synchronized public void returnArray(ArrayElement elem) {
        freeCharArrayList_.add(elem);
        notify();
    }
}
