package com.example.demo.exception;

/**
 * Active Directory操作で発生する例外を表すカスタム例外クラス
 */
public class ActiveDirectoryException extends RuntimeException {
    
    public ActiveDirectoryException(String message) {
        super(message);
    }
    
    public ActiveDirectoryException(String message, Throwable cause) {
        super(message, cause);
    }
} 