package gov.nist.hit.iz.ws.server.exception;

import gov.nist.healthcare.tools.core.transport.TransportServerException;

public class MessageTooLargeException extends TransportServerException {
	private static final long serialVersionUID = 1L;

	public MessageTooLargeException() {
		super("MessageTooLargeFaultException");
	}

	public MessageTooLargeException(String s) {
		super(s);
	}

}
