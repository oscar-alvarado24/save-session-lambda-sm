package com.colombia.eps.helper.exceptions;

public class DynamoTableNotCreatedException extends RuntimeException {
  public DynamoTableNotCreatedException(String message) {
    super(message);
  }
}
