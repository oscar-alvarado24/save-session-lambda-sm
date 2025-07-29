package com.colombia.eps.session.helper.exceptions;

public class DynamoTableNotCreatedException extends RuntimeException {
    public DynamoTableNotCreatedException(String message) {
        super(message);
    }
}
