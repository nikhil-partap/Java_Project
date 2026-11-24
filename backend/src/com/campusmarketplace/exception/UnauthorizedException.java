package com.campusmarketplace.exception;

/** EXCEPTIONS + INHERITANCE: HTTP 401. Wrong email or password. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(401, message); // super(...) calls the ApiException constructor
    }
}
