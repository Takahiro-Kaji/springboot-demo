package com.example.demo.service;

import javax.naming.*;
import javax.naming.directory.*;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class GroupMembershipService extends ActiveDirectoryService {
    
    private String findUserDN(String userCN) throws NamingException {
        DirContext ctx = connect();
        try {
            String searchBase = "CN=Users,DC=sandbox,DC=local";
            String searchFilter = "(&(objectClass=user)(cn=" + userCN + "))";
            
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{"distinguishedName"});
            
            NamingEnumeration<SearchResult> results = ctx.search(searchBase, searchFilter, searchControls);
            
            if (results.hasMore()) {
                SearchResult result = results.next();
                return result.getNameInNamespace();
            } else {
                throw new NamingException("User with CN '" + userCN + "' not found");
            }
        } finally {
            ctx.close();
        }
    }
    
    /**
     * 複数のユーザーCNを指定して、対応するDNを一括検索
     */
    private Map<String, String> findMultipleUserDNs(List<String> userCNs) throws NamingException {
        DirContext ctx = connect();
        try {
            Map<String, String> userCNToDN = new HashMap<>();
            List<String> notFoundUsers = new ArrayList<>();
            
            for (String userCN : userCNs) {
                try {
                    String userDN = findUserDN(userCN);
                    userCNToDN.put(userCN, userDN);
                } catch (NamingException e) {
                    notFoundUsers.add(userCN);
                }
            }
            
            if (!notFoundUsers.isEmpty()) {
                throw new NamingException("Users not found: " + String.join(", ", notFoundUsers));
            }
            
            return userCNToDN;
        } finally {
            ctx.close();
        }
    }
    
    /**
     * 複数のユーザーを一度にグループに追加
     */
    public void addMultipleUsersToGroup(List<String> userCNs, String groupCN) throws NamingException {
        DirContext ctx = connect();
        try {
            String groupDn = "CN=" + groupCN + ",CN=Users,DC=sandbox,DC=local";
            Map<String, String> userCNToDN = findMultipleUserDNs(userCNs);
            
            // 複数のユーザーを一度に追加
            ModificationItem[] mods = new ModificationItem[userCNToDN.size()];
            int index = 0;
            for (String userDN : userCNToDN.values()) {
                mods[index] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("member", userDN));
                index++;
            }
            
            ctx.modifyAttributes(groupDn, mods);
        } finally {
            ctx.close();
        }
    }
    
    /**
     * 複数のユーザーを一度にグループから削除
     */
    public void removeMultipleUsersFromGroup(List<String> userCNs, String groupCN) throws NamingException {
        DirContext ctx = connect();
        try {
            String groupDn = "CN=" + groupCN + ",CN=Users,DC=sandbox,DC=local";
            Map<String, String> userCNToDN = findMultipleUserDNs(userCNs);
            
            // 複数のユーザーを一度に削除
            ModificationItem[] mods = new ModificationItem[userCNToDN.size()];
            int index = 0;
            for (String userDN : userCNToDN.values()) {
                mods[index] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("member", userDN));
                index++;
            }
            
            ctx.modifyAttributes(groupDn, mods);
        } finally {
            ctx.close();
        }
    }
    
    public void addUserToGroup(String userCN, String groupCN) throws NamingException {
        DirContext ctx = connect();
        try {
            String groupDn = "CN=" + groupCN + ",CN=Users,DC=sandbox,DC=local";
            String userDn = findUserDN(userCN);

            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("member", userDn));
            ctx.modifyAttributes(groupDn, mods);
        } finally {
            ctx.close();
        }
    }

    public void removeUserFromGroup(String userCN, String groupCN) throws NamingException {
        DirContext ctx = connect();
        try {
            String groupDn = "CN=" + groupCN + ",CN=Users,DC=sandbox,DC=local";
            String userDn = findUserDN(userCN);

            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("member", userDn));
            ctx.modifyAttributes(groupDn, mods);
        } finally {
            ctx.close();
        }
    }
}
