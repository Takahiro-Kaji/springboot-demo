package com.example.demo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * リトライ機能を提供するユーティリティクラス
 * Callable<T>ベースで統一されたリトライ処理を提供します
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
     * リトライ例外クラス
     */
    public static class RetryException extends Exception {
        public RetryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * 指定されたタスクをリトライ可能なエラーが発生した場合にリトライする
     * 
     * @param task 実行するタスク
     * @return タスクの結果
     * @throws RetryException リトライ後も失敗した場合
     */
    public static <T> T retry(Callable<T> task) throws RetryException {
        int maxAttempts = MAX_RETRIES;
        List<Exception> exceptions = new ArrayList<>();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return task.call();
            } catch (Exception e) {
                if (!isRetryable(e)) {
                    throw new RetryException("Non-retryable exception", e);
                }
                exceptions.add(e);
                if (attempt < maxAttempts) {
                    logger.warn("操作が失敗しました（試行回数: {}）: {}", attempt, e.getMessage());
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RetryException("リトライが中断されました", ie);
                    }
                }
            }
        }

        Exception lastException = exceptions.get(exceptions.size() - 1);
        logger.error("操作が最終的に失敗しました（試行回数: {}）", maxAttempts, lastException);
        throw new RetryException("All retry attempts failed", lastException);
    }
    
    /**
     * NamingExceptionをスローする可能性のあるタスクをリトライし、最終的にNamingExceptionを再スローする
     * 
     * @param task 実行するタスク
     * @return タスクの結果
     * @throws NamingException 操作が失敗した場合
     */
    public static <T> T retryNaming(Callable<T> task) throws NamingException {
        try {
            return retry(task);
        } catch (RetryException e) {
            Throwable cause = e.getCause();
            if (cause instanceof NamingException ne) {
                throw ne;
            }
            throw new RuntimeException(e);
        }
    }
    
    /**
     * リトライ可能なエラーかどうかを判定
     */
    private static boolean isRetryable(Exception e) {
        Throwable cause = e instanceof RetryException ? e.getCause() : e;

        return cause instanceof javax.naming.CommunicationException
            || cause instanceof javax.naming.ServiceUnavailableException
            || cause instanceof javax.naming.TimeLimitExceededException;
    }
} 