package com.github.gcolin.event;

public class ChessEventException extends Exception {

    private final int status;
    private final String error;

    public ChessEventException(int status, String error) {
        super(error);
        this.status = status;
        this.error = error;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
