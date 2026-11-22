package org.springframework.http;
public enum HttpStatus { NOT_FOUND(404), BAD_REQUEST(400), FORBIDDEN(403), CONFLICT(409), UNAUTHORIZED(401); public final int code; HttpStatus(int c){code=c;} }
