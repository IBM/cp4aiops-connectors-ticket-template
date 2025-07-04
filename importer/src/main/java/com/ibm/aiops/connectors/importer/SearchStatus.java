package com.ibm.aiops.connectors.importer;

public class SearchStatus {
    enum Status {
        OK, ERROR, INFO, WARNING
    }

    protected String message = null;
    protected Throwable throwable = null;
    protected Status status = null;

    public SearchStatus(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public SearchStatus(Status status, String message, Throwable throwable) {
        this(status, message);
        this.throwable = throwable;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}