package com.example.demo.service;

import javax.naming.*;
import javax.naming.directory.*;

import org.springframework.stereotype.Service;

import java.util.Hashtable;

@Service
public class ActiveDirectoryService {
    protected DirContext connect() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldaps://" + System.getenv("AD_HOST") + ":636");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "CN=Administrator,CN=Users,DC=sandbox,DC=local");
        env.put(Context.SECURITY_CREDENTIALS, System.getenv("AD_PASSWORD"));

        env.put("java.naming.ldap.factory.socket", "javax.net.ssl.SSLSocketFactory");
        return new InitialDirContext(env);
    }
}
