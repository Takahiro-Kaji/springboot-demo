package com.example.demo.service;

import javax.naming.*;
import javax.naming.directory.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.config.AdProperty;
import com.example.demo.util.RetryUtil;

import java.util.Hashtable;
import java.util.function.Supplier;

/**
 * Active Directory操作のベースクラス
 * リトライ機能を組み込んでおり、子クラスで簡単にリトライ処理を使用できます
 */
@Service
public class ActiveDirectoryService {
    
    @Autowired
    private AdProperty adProperty;
    
    /**
     * Active Directoryに接続する
     */
    protected DirContext connect() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, adProperty.getProviderUrl());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, adProperty.getAdminPrincipal());
        env.put(Context.SECURITY_CREDENTIALS, adProperty.getPassword());

        env.put("java.naming.ldap.factory.socket", "javax.net.ssl.SSLSocketFactory");
        return new InitialDirContext(env);
    }
    
    /**
     * リトライ機能付きで操作を実行する（戻り値あり）
     * 
     * @param operation 実行する操作
     * @return 操作の結果
     */
    protected <T> T executeWithRetry(Supplier<T> operation) {
        return RetryUtil.retryOnError(operation);
    }
    
    /**
     * リトライ機能付きで操作を実行する（戻り値なし）
     * 
     * @param operation 実行する操作
     */
    protected void executeWithRetry(Runnable operation) {
        RetryUtil.retryOnError(() -> {
            operation.run();
            return null;
        });
    }
}
