package com.example.demo.service;

import javax.naming.*;
import javax.naming.directory.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

import com.example.demo.config.AdProperty;

@Service
public class GroupMembershipService extends ActiveDirectoryService {
    
    @Autowired
    private AdProperty adProperty;
    
    private String findUserDN(String userCN) throws NamingException {
        try (DirContext ctx = connect()) {
            String searchBase = adProperty.getUsersDn();
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
        }
    }
    
    /**
     * 複数のユーザーCNを指定して、対応するDNを一括検索
     */
    private List<String> findMultipleUserDNs(List<String> userCNs) throws NamingException {
        try (DirContext ctx = connect()) {
            List<String> userDNs = new ArrayList<>();
            Map<String, String> userCNToDN = new HashMap<>();
            
            if (userCNs.isEmpty()) {
                return userDNs;
            }
            
            // OR条件で複数のユーザーを一度に検索
            StringBuilder searchFilter = new StringBuilder("(&(objectClass=user)(|");
            for (String userCN : userCNs) {
                searchFilter.append("(cn=").append(userCN).append(")");
            }
            searchFilter.append("))");
            
            String searchBase = adProperty.getUsersDn();
            
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{"distinguishedName", "cn"});
            
            NamingEnumeration<SearchResult> results = ctx.search(searchBase, searchFilter.toString(), searchControls);
            
            // 検索結果をマップに格納
            while (results.hasMore()) {
                SearchResult result = results.next();
                String dn = result.getNameInNamespace();
                String cn = result.getAttributes().get("cn").get().toString();
                userCNToDN.put(cn, dn);
            }
            
            // 見つからないユーザーをチェック
            List<String> notFoundUsers = new ArrayList<>();
            for (String userCN : userCNs) {
                if (!userCNToDN.containsKey(userCN)) {
                    notFoundUsers.add(userCN);
                }
            }
            
            if (!notFoundUsers.isEmpty()) {
                throw new NamingException("Users not found: " + String.join(", ", notFoundUsers));
            }
            
            // 元の順序を保持してDNのリストを作成
            for (String userCN : userCNs) {
                userDNs.add(userCNToDN.get(userCN));
            }
            
            return userDNs;
        }
    }
    
    /**
     * 複数のユーザーを一度にグループに追加
     */
    public void addMultipleUsersToGroup(List<String> userCNs, String groupCN) throws NamingException {
        try (DirContext ctx = connect()) {
            String groupDn = "CN=" + groupCN + "," + adProperty.getUsersDn();
            List<String> userDNs = findMultipleUserDNs(userCNs);
            
            // 複数のユーザーを一度に追加
            ModificationItem[] mods = new ModificationItem[userDNs.size()];
            int index = 0;
            for (String userDN : userDNs) {
                mods[index] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("member", userDN));
                index++;
            }
            
            ctx.modifyAttributes(groupDn, mods);
        }
    }
    
    /**
     * 複数のユーザーを一度にグループから削除
     */
    public void removeMultipleUsersFromGroup(List<String> userCNs, String groupCN) throws NamingException {
        try (DirContext ctx = connect()) {
            String groupDn = "CN=" + groupCN + "," + adProperty.getUsersDn();
            List<String> userDNs = findMultipleUserDNs(userCNs);
            
            // 複数のユーザーを一度に削除
            ModificationItem[] mods = new ModificationItem[userDNs.size()];
            int index = 0;
            for (String userDN : userDNs) {
                mods[index] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("member", userDN));
                index++;
            }
            
            ctx.modifyAttributes(groupDn, mods);
        }
    }
    
    public void addUserToGroup(String userCN, String groupCN) throws NamingException {
        try (DirContext ctx = connect()) {
            String groupDn = "CN=" + groupCN + "," + adProperty.getUsersDn();
            String userDn = findUserDN(userCN);

            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("member", userDn));
            ctx.modifyAttributes(groupDn, mods);
        }
    }

    public void removeUserFromGroup(String userCN, String groupCN) throws NamingException {
        try (DirContext ctx = connect()) {
            String groupDn = "CN=" + groupCN + "," + adProperty.getUsersDn();
            String userDn = findUserDN(userCN);

            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("member", userDn));
            ctx.modifyAttributes(groupDn, mods);
        }
    }
}
