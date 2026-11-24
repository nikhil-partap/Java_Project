package com.campusmarketplace.exception;

/** EXCEPTIONS + INHERITANCE: HTTP 404. Requested item does not exist. */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(404, message); // super(...) calls the ApiException constructor
    }
}
