package com.example.demo.service;

import javax.naming.*;
import javax.naming.directory.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.config.AdProperty;

import java.util.Hashtable;

@Service
public class ActiveDirectoryService {
    
    @Autowired
    private AdProperty adProperty;
    
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
}
