package com.example.demo;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.Hashtable;

public class ActiveDirectoryService {
    protected DirContext connect() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://your-ad-server:389");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "CN=Administrator,CN=Users,DC=sandbox,DC=local");
        env.put(Context.SECURITY_CREDENTIALS, "your-password");
        return new InitialDirContext(env);
    }
}
