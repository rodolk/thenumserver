package com.rodolk.numserver.loggingprotocol;

public interface DataConsumer {
	void processData(ArrayProvider.ArrayElement arrayElem, int initialPos, int totalNumbers, int numberLen);
}
