package com.example.service;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;

import org.springframework.stereotype.Service;

@Service
public class GroupLookupService extends ActiveDirectoryService {
    public List<String> listGroups() throws NamingException {
        DirContext ctx = connect();
        try {
            String base = "CN=Users,DC=sandbox,DC=local";
            String filter = "(objectClass=group)";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration<SearchResult> results = ctx.search(base, filter, sc);
            List<String> groupNames = new ArrayList<>();
            while (results.hasMore()) {
                SearchResult sr = results.next();
                groupNames.add(sr.getNameInNamespace());
            }
            return groupNames;
        } finally {
            ctx.close();
        }
    }
}
