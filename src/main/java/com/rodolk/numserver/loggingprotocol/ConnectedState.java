package com.rodolk.numserver.loggingprotocol;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class ConnectedState extends State {

	ArrayProvider.ArrayElement arrayElem_;
	private int nextOffset_ = 0;

	public ConnectedState(ApplicationProtocol prot) {
		super(prot);

		arrayElem_ = protocol_.arrayProvider_.getArray();
	}
	
	@Override
	public State process() throws ProtocolException {
		int len = 0;
		State nextState = this;
		
		try {
			len = protocol_.inBuffReader_.read(arrayElem_.array_, nextOffset_, protocol_.kArrayLen - nextOffset_);
			len += nextOffset_;
			if (len != -1 && len < protocol_.kMinThreshold) {
				//If read a small amount of data, try to read as much data as possible in a buffer
				protocol_.socket_.setSoTimeout(200); //Should be enough for a big RTT

				try {
					int len2 = protocol_.inBuffReader_.read(arrayElem_.array_, len, protocol_.kArrayLen - len);
					if (len2 > 0) {
						len += len2;
					}
				} catch (SocketTimeoutException excepTO) {
				}
				protocol_.socket_.setSoTimeout(30000); //Set previous timeout
			}
		} catch (SocketTimeoutException e2) {
			e2.printStackTrace();
			protocol_.arrayProvider_.returnArray(arrayElem_);
			throw new ProtocolException("Socket timeout", ProtocolException.ErrorCode.SOCKET_ERROR);
		} catch (SocketException e1) {
			e1.printStackTrace();
			protocol_.arrayProvider_.returnArray(arrayElem_);
			throw new ProtocolException("Error setting socket timeout", ProtocolException.ErrorCode.SOCKET_ERROR);
		} catch (IOException e) {
			e.printStackTrace();
			protocol_.arrayProvider_.returnArray(arrayElem_);
			throw new ProtocolException("Error reading", ProtocolException.ErrorCode.READER_ERROR);
		} 
		
		if (len == -1) {
			protocol_.arrayProvider_.returnArray(arrayElem_);
			throw new ProtocolException("Closed", ProtocolException.ErrorCode.CLOSED);
		} else {
			int i;
			int totalNumbers = 0;
			int nextNumber = -1;
			boolean terminate = false;
			for(i = 0; i < len && !terminate; i++) {
				if ((i + 1) % (protocol_.kIntLen + 1) != 0) {
					if (arrayElem_.array_[i] < '0' || arrayElem_.array_[i] > '9') {
						if ((i + 1) % (protocol_.kIntLen + 1) == 1 && arrayElem_.array_[i] == 't') {
							terminate = true;
						} else {
							if (protocol_.dataConsumer_ != null && totalNumbers > 0) {
								protocol_.dataConsumer_.processData(arrayElem_, 0, 
										totalNumbers, (protocol_.kIntLen + 1));
							} else {
								protocol_.arrayProvider_.returnArray(arrayElem_);
							}
							throw new ProtocolException("Erroneous data", ProtocolException.ErrorCode.READER_ERROR);
						}
					} // else OK
				} else {
					if (arrayElem_.array_[i] != '\n') {
						if (protocol_.dataConsumer_ != null && totalNumbers > 0) {
							protocol_.dataConsumer_.processData(arrayElem_, 0, 
									totalNumbers, (protocol_.kIntLen + 1));
						} else {
							protocol_.arrayProvider_.returnArray(arrayElem_);
						}
						throw new ProtocolException("Erroneous data", ProtocolException.ErrorCode.READER_ERROR);
					} else {
						nextNumber = i + 1;
						totalNumbers++;
					}
				}
			}
			if (!terminate) {
				ArrayProvider.ArrayElement nextArrayElem = protocol_.arrayProvider_.getArray();
				if (nextNumber == -1) {
					nextNumber = 0;
				}
				nextOffset_ = 0;
				if (nextNumber < len) {
					int copyIdx;
					for(copyIdx = nextNumber; copyIdx < len; copyIdx++) {
						nextArrayElem.array_[copyIdx - nextNumber] = arrayElem_.array_[copyIdx];
						nextOffset_++;
					}
				}
				if (protocol_.dataConsumer_ != null && totalNumbers > 0) {
					protocol_.dataConsumer_.processData(arrayElem_, 0, 
							totalNumbers, (protocol_.kIntLen + 1));
				} else {
					protocol_.arrayProvider_.returnArray(arrayElem_);
				}
				arrayElem_ = nextArrayElem;
			} else {
				nextState = new TerminateState(protocol_, arrayElem_.array_, i - 1, len - 1);
				if (protocol_.dataConsumer_ != null && totalNumbers > 0) {
					protocol_.dataConsumer_.processData(arrayElem_, 0, 
							totalNumbers, (protocol_.kIntLen + 1));
				} else {
					protocol_.arrayProvider_.returnArray(arrayElem_);
				}
			}
		}
		return nextState;
	}
}
