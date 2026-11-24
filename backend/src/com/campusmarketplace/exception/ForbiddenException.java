package com.campusmarketplace.exception;

/** EXCEPTIONS + INHERITANCE: HTTP 403. User is blocked by an administrator. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(403, message); // super(...) calls the ApiException constructor
    }
}
