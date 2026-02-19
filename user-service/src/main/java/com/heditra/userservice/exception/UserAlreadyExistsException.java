package com.heditra.userservice.exception;

public class UserAlreadyExistsException extends BusinessException {

    public UserAlreadyExistsException(String field, String value) {
        super("USER_ALREADY_EXISTS", "User already exists with " + field + ": " + value);
    }
}
