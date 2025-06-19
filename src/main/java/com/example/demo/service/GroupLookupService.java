package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.config.AdProperty;

@Service
public class GroupLookupService extends ActiveDirectoryService {
    
    @Autowired
    private AdProperty adProperty;
    
    public List<String> listGroups() throws NamingException {
        try (DirContext ctx = connect()) {
            String base = adProperty.getUsersDn();
            String filter = "(objectClass=group)";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration<SearchResult> results = ctx.search(base, filter, sc);
            List<String> groupNames = new ArrayList<>();
            while (results.hasMore()) {
                SearchResult sr = results.next();
                System.out.println(sr.getNameInNamespace());
                groupNames.add(sr.getNameInNamespace());
            }
            return groupNames;
        }
    }
}
