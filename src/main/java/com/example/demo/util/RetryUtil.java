package com.example.demo.util;

import com.example.demo.exception.ActiveDirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * リトライ機能を提供するユーティリティクラス
 * 3回リトライ、1秒間隔の固定パターンで動作します
 */
public class RetryUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryUtil.class);
    
    /**
     * 最大リトライ回数
     */
    private static final int MAX_RETRIES = 3;
    
    /**
     * リトライ間隔（ミリ秒）
     */
    private static final long RETRY_DELAY = 1000;
    
    /**
     * 指定された操作をリトライ可能なエラーが発生した場合にリトライする
     * 3回リトライ、1秒間隔で実行されます
     * 
     * @param operation 実行する操作
     * @return 操作の結果
     * @throws ActiveDirectoryException リトライ後も失敗した場合
     */
    public static <T> T retryOnError(Supplier<T> operation) {
        int attempts = 0;
        
        while (attempts < MAX_RETRIES) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempts++;
                
                if (isRetryableError(e) && attempts < MAX_RETRIES) {
                    logger.warn("操作が失敗しました（試行回数: {}）: {}", attempts, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ActiveDirectoryException("リトライが中断されました", ie);
                    }
                } else {
                    logger.error("操作が最終的に失敗しました（試行回数: {}）", attempts, e);
                    throw new ActiveDirectoryException("操作が失敗しました", e);
                }
            }
        }
        
        throw new ActiveDirectoryException("操作が最大試行回数に達しました");
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