package com.example.service;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;

import org.springframework.stereotype.Service;

@Service
public class GroupLookupService extends ActiveDirectoryService {
    public NamingEnumeration<SearchResult> listGroups() throws NamingException {
        DirContext ctx = connect();
        try {
            String base = "CN=Users,DC=sandbox,DC=local";
            String filter = "(objectClass=group)";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

           return ctx.search(base, filter, sc);
        } finally {
            ctx.close();
        }
    }
}
