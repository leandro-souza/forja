package org.springframework.web.server;
import org.springframework.http.HttpStatus;
public class ResponseStatusException extends RuntimeException { public final HttpStatus status; public ResponseStatusException(HttpStatus s, String m){ super(m); status=s; } }
