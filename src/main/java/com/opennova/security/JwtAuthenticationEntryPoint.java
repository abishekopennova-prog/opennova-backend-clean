package com.opennova.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        
        System.err.println("=== JWT Authentication Entry Point ===");
        System.err.println("Request URI: " + request.getRequestURI());
        System.err.println("Request Method: " + request.getMethod());
        System.err.println("Authorization Header: " + request.getHeader("Authorization"));
        System.err.println("Auth Exception: " + authException.getMessage());
        
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        String message = authException.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = "Full authentication is required to access this resource";
        }
        
        response.getOutputStream().println("{ \"error\": \"Unauthorized\", \"message\": \"" + message + "\" }");
    }
}