package com.example.demo.util;

import com.example.demo.exception.ActiveDirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * リトライ機能を提供するユーティリティクラス
 */
public class RetryUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryUtil.class);
    
    /**
     * デフォルトの最大リトライ回数
     */
    private static final int DEFAULT_MAX_RETRIES = 3;
    
    /**
     * デフォルトのリトライ間隔（ミリ秒）
     */
    private static final long DEFAULT_RETRY_DELAY = 1000;
    
    /**
     * 指定された操作をリトライ可能なエラーが発生した場合にリトライする
     */
    public static <T> T retryOnError(Supplier<T> operation, String operationName) {
        return retryOnError(operation, operationName, DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY);
    }
    
    /**
     * 指定された操作をリトライ可能なエラーが発生した場合にリトライする（カスタム設定）
     */
    public static <T> T retryOnError(Supplier<T> operation, String operationName, int maxRetries, long retryDelay) {
        int attempts = 0;
        
        while (attempts < maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempts++;
                
                if (isRetryableError(e) && attempts < maxRetries) {
                    logger.warn("{} が失敗しました（試行回数: {}）: {}", operationName, attempts, e.getMessage());
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ActiveDirectoryException("リトライが中断されました", ie);
                    }
                } else {
                    logger.error("{} が最終的に失敗しました（試行回数: {}）", operationName, attempts, e);
                    throw new ActiveDirectoryException(operationName + " が失敗しました", e);
                }
            }
        }
        
        throw new ActiveDirectoryException(operationName + " が最大試行回数に達しました");
    }
    
    /**
     * リトライ可能なエラーかどうかを判定
     */
    private static boolean isRetryableError(Exception e) {
        // 接続エラーや一時的なエラーはリトライ可能
        String message = e.getMessage().toLowerCase();
        return message.contains("connection") || 
               message.contains("timeout") || 
               message.contains("temporary") ||
               message.contains("service unavailable");
    }
} 