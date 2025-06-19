package com.example.demo.util;

import com.example.demo.exception.ActiveDirectoryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.naming.CommunicationException;
import javax.naming.ServiceUnavailableException;
import javax.naming.TimeLimitExceededException;
import javax.naming.directory.AttributeInUseException;
import javax.naming.NamingException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RetryUtilのテストクラス
 */
@ExtendWith(MockitoExtension.class)
class RetryUtilTest {

    @Test
    void testRetryOnError_SuccessOnFirstAttempt() {
        // 準備
        AtomicInteger callCount = new AtomicInteger(0);
        
        // メソッドの実行
        String result = RetryUtil.retryOnError(() -> {
            callCount.incrementAndGet();
            return "success";
        });

        // 検証
        assertEquals("success", result);
        assertEquals(1, callCount.get());
    }

    @Test
    void testRetryOnError_SuccessOnRetry() {
        // 準備
        AtomicInteger callCount = new AtomicInteger(0);
        
        // メソッドの実行
        String result = RetryUtil.retryOnError(() -> {
            int count = callCount.incrementAndGet();
            if (count == 1) {
                throw new CommunicationException("Connection failed");
            }
            return "success";
        });

        // 検証
        assertEquals("success", result);
        assertEquals(2, callCount.get());
    }

    @Test
    void testRetryOnError_SuccessOnThirdAttempt() {
        // 準備
        AtomicInteger callCount = new AtomicInteger(0);
        
        // メソッドの実行
        String result = RetryUtil.retryOnError(() -> {
            int count = callCount.incrementAndGet();
            if (count <= 2) {
                throw new ServiceUnavailableException("Service unavailable");
            }
            return "success";
        });

        // 検証
        assertEquals("success", result);
        assertEquals(3, callCount.get());
    }

    @Test
    void testRetryOnError_FailureAfterMaxRetries() {
        // 準備
        AtomicInteger callCount = new AtomicInteger(0);
        
        // メソッドの実行と例外の検証
        ActiveDirectoryException exception = assertThrows(ActiveDirectoryException.class, () -> {
            RetryUtil.retryOnError(() -> {
                callCount.incrementAndGet();
                throw new CommunicationException("Connection failed");
            });
        });

        // 検証
        assertEquals("操作が失敗しました", exception.getMessage());
        assertEquals(3, callCount.get());
    }

    @Test
    void testRetryOnError_NonRetryableError_ThrowsImmediately() {
        // 準備
        AtomicInteger callCount = new AtomicInteger(0);
        
        // メソッドの実行と例外の検証
        NamingException exception = assertThrows(NamingException.class, () -> {
            RetryUtil.retryOnError(() -> {
                callCount.incrementAndGet();
                throw new NamingException("Non-retryable error");
            });
        });

        // 検証
        assertEquals("Non-retryable error", exception.getMessage());
        assertEquals(1, callCount.get()); // リトライされない
    }

    @Test
    void testRetryOnError_RetryableExceptions() {
        // リトライ可能な例外のテスト
        testRetryableException(new CommunicationException("Connection failed"));
        testRetryableException(new ServiceUnavailableException("Service unavailable"));
        testRetryableException(new TimeLimitExceededException("Timeout"));
        testRetryableException(new AttributeInUseException("Attribute in use"));
    }

    private void testRetryableException(Exception retryableException) {
        // 準備
        AtomicInteger callCount = new AtomicInteger(0);
        
        // メソッドの実行と例外の検証
        ActiveDirectoryException exception = assertThrows(ActiveDirectoryException.class, () -> {
            RetryUtil.retryOnError(() -> {
                callCount.incrementAndGet();
                throw retryableException;
            });
        });

        // 検証
        assertEquals("操作が失敗しました", exception.getMessage());
        assertEquals(3, callCount.get()); // 3回リトライされる
    }

    @Test
    void testRetryOnError_InterruptedException() {
        // 準備
        AtomicInteger callCount = new AtomicInteger(0);
        
        // メソッドの実行と例外の検証
        ActiveDirectoryException exception = assertThrows(ActiveDirectoryException.class, () -> {
            RetryUtil.retryOnError(() -> {
                callCount.incrementAndGet();
                if (callCount.get() == 1) {
                    throw new CommunicationException("Connection failed");
                }
                // 2回目の試行でInterruptedExceptionをスロー
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted");
            });
        });

        // 検証
        assertEquals("リトライが中断されました", exception.getMessage());
        assertEquals(1, callCount.get());
        assertTrue(Thread.currentThread().isInterrupted());
        
        // テスト後の状態をリセット
        Thread.interrupted();
    }

    @Test
    void testRetryOnError_VoidOperation() {
        // 準備
        AtomicInteger callCount = new AtomicInteger(0);
        
        // メソッドの実行
        RetryUtil.retryOnError(() -> {
            callCount.incrementAndGet();
            if (callCount.get() == 1) {
                throw new CommunicationException("Connection failed");
            }
            // 何もしない（void操作）
        });

        // 検証
        assertEquals(2, callCount.get());
    }

    @Test
    void testRetryOnError_ComplexRetryScenario() {
        // 準備
        AtomicInteger callCount = new AtomicInteger(0);
        
        // メソッドの実行
        String result = RetryUtil.retryOnError(() -> {
            int count = callCount.incrementAndGet();
            switch (count) {
                case 1:
                    throw new CommunicationException("Connection failed");
                case 2:
                    throw new ServiceUnavailableException("Service unavailable");
                case 3:
                    return "success after retries";
                default:
                    throw new RuntimeException("Unexpected");
            }
        });

        // 検証
        assertEquals("success after retries", result);
        assertEquals(3, callCount.get());
    }

    @Test
    void testRetryOnError_MixedRetryableAndNonRetryableErrors() {
        // 準備
        AtomicInteger callCount = new AtomicInteger(0);
        
        // メソッドの実行と例外の検証
        NamingException exception = assertThrows(NamingException.class, () -> {
            RetryUtil.retryOnError(() -> {
                int count = callCount.incrementAndGet();
                if (count == 1) {
                    throw new CommunicationException("Connection failed"); // リトライ可能
                } else {
                    throw new NamingException("Non-retryable error"); // リトライ不可能
                }
            });
        });

        // 検証
        assertEquals("Non-retryable error", exception.getMessage());
        assertEquals(2, callCount.get()); // 1回リトライしてから非リトライ可能エラーで終了
    }

    @Test
    void testRetryOnError_ReturnNull() {
        // 準備
        AtomicInteger callCount = new AtomicInteger(0);
        
        // メソッドの実行
        Object result = RetryUtil.retryOnError(() -> {
            callCount.incrementAndGet();
            if (callCount.get() == 1) {
                throw new CommunicationException("Connection failed");
            }
            return null;
        });

        // 検証
        assertNull(result);
        assertEquals(2, callCount.get());
    }

    @Test
    void testRetryOnError_ReturnComplexObject() {
        // 準備
        AtomicInteger callCount = new AtomicInteger(0);
        TestObject expectedObject = new TestObject("test", 123);
        
        // メソッドの実行
        TestObject result = RetryUtil.retryOnError(() -> {
            callCount.incrementAndGet();
            if (callCount.get() == 1) {
                throw new CommunicationException("Connection failed");
            }
            return expectedObject;
        });

        // 検証
        assertEquals(expectedObject, result);
        assertEquals(2, callCount.get());
    }

    // テスト用の内部クラス
    private static class TestObject {
        private final String name;
        private final int value;

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestObject that = (TestObject) obj;
            return value == that.value && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() + 31 * value;
        }
    }
} 