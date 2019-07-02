package com.rodolk.numserver.loggingprotocol;

/**
 * DataConsumer is an interface for a class that subscribes to ApplicationProtocol to
 * consume the data read, parsed, and checkd for correctness.
 * This class must implement method processData.
 * 
 * @author rodolk
 *
 */
public interface DataConsumer {
	
	/**
	 * It processes and array element which contains the array with data received and 
	 * corresponding metadata.
	 * 
	 * @param arrayElem		Contains the array with data
	 * @param initialPos	Offset to read the array, always 0.
	 * @param totalNumbers	Total amount of numbers in the array
	 * @param numberLen		Length of each number including '\n'
	 */
	void processData(ArrayProvider.ArrayElement arrayElem, int initialPos, int totalNumbers, int numberLen);
}
