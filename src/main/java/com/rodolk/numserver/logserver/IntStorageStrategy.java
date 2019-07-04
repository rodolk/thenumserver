package com.rodolk.numserver.logserver;

/**
 * This is an abstract class that allows use of different strategies to store and read elements.
 * It implements Strategy pattern.
 * 
 * @author rodolk
 *
 */
public abstract class IntStorageStrategy {
    /**
     * Determines if a number passed as an array of characters exists in this container.
     * If it exists it returns 'true'. If it doesn't exist, this method returns 'false' and inserts
     * the passed number. Next time same number is passed, it will return 'true'.
     * Only characters of digits must be passed without new line or other characters.
     * 
     * @param array     array that contains the digits of the number
     * @param offset    offset within the passed array where first digit is located
     * @param len       len of number digits not including new line characters
     * @return          true if number exists
     *                  false if number does not exist
     *                  
     */
    synchronized public boolean contains(char[] array, int offset, int len) {
        return hasValue(array, offset, len);
    }
    
    protected abstract boolean hasValue(char[] array, int offset, int len);
    public abstract int getSize();
}
