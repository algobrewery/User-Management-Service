package com.userapi.exception;

/**
 * Exception thrown when request validation fails.
 * This includes missing required headers, invalid formats, etc.
 */
public class InvalidRequestException extends RuntimeException {
    
    public InvalidRequestException(String message) {
        super(message);
    }
    
    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
