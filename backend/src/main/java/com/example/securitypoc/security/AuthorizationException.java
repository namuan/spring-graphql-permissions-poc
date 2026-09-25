package com.example.securitypoc.security;

public class AuthorizationException extends RuntimeException {
    public AuthorizationException() {
        super("Access denied");
    }
}
