package com.rodolk.numserver.logserver;

/**
 * This class uses a strategy that is good for the exercise. Considering it needs to store 
 * up to 1,000,000,000 numbers, each number can be represented with a bit in an array of Bytes.
 * We use 125MB for all numbers and have time complexity O(1). Two birds with one shot!
 * 
 * @author rodolk
 *
 */
public class IntStorageBitArrayStrategy extends IntStorageStrategy {
    public static final int kMaxSizeBitArray_ = 1000000000;
    public static final int kSizeByteArray_ = 125000000;
    private final int kSizeByte_ = 8;
    private int size_ = 0;
    private byte[] indexArray_ = null;
    
    public IntStorageBitArrayStrategy() {
        indexArray_ = new byte[kSizeByteArray_];  
    }

    /**
     * This implementation will determine the bit position of the received int value
     * in the array. Then it checks if the bit is set or not.
     * If it is not set, it sets it and returns false. Otherwise, it just returns true.
     * 
     * @param   array the array containing the characters representing the integer
     * @param   offset of this number in the array
     * @param   length of the number
     * 
     * @return  false   if the number doesn't exist. The number is added.
     *          true    if the number exists.
     * 
     */
    @Override
    protected boolean hasValue(char[] array, int offset, int len) {
        int value = 0;
        int tmp;
        int pos = 0;
        for(pos = 0;pos < len; pos++) {
            tmp = array[offset + pos] - '0';
            value += tmp * Math.pow(10, (len - 1 - pos));
        }
        
        int arrayPosition = (int)Math.floor(value / kSizeByte_);
        int bitPosition = value % kSizeByte_;
        byte testValue = (byte) (1 << bitPosition);
        
        if ((indexArray_[arrayPosition] & testValue) == 0) {
            indexArray_[arrayPosition] |= testValue;
            size_++;
            return false;
        } else {
            return true;
        }
    }

    @Override
    public int getSize() {
        return size_;
    }

}
