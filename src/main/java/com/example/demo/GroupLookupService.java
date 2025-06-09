package com.example.demo;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;

public class GroupLookupService extends ActiveDirectoryService {
    public void listGroups() throws NamingException {
        DirContext ctx = connect();
        try {
            String base = "CN=Users,DC=sandbox,DC=local";
            String filter = "(objectClass=group)";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration<SearchResult> results = ctx.search(base, filter, sc);
            while (results.hasMore()) {
                SearchResult sr = results.next();
                System.out.println("Group: " + sr.getNameInNamespace());
            }
        } finally {
            ctx.close();
        }
    }
}
