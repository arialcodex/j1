package de.drucken.service;

import javax.print.PrintException;

public class PrintServiceUnavailableException extends PrintException {

    public PrintServiceUnavailableException(String message) {
        super(message);
    }

    public PrintServiceUnavailableException(String message, Exception cause) {
        super(message, cause);
    }
}
