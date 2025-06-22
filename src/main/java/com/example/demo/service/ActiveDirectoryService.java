package com.example.demo.service;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.config.AdProperty;
import com.example.demo.util.RetryUtil;

import java.util.Hashtable;
import java.util.concurrent.Callable;

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
    protected LdapContext connect() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, adProperty.getProviderUrl());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, adProperty.getAdminPrincipal());
        env.put(Context.SECURITY_CREDENTIALS, adProperty.getPassword());

        env.put("java.naming.ldap.factory.socket", "javax.net.ssl.SSLSocketFactory");
        return new InitialLdapContext(env,null);
    }
}
