package com.example.demo.service;

import javax.naming.*;
import javax.naming.directory.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.config.AdProperty;
import java.util.*;

@Service
public class GroupManagementService extends ActiveDirectoryService {
    
    @Autowired
    private AdProperty adProperty;
    
    public void createGroup(String groupCN) throws NamingException {
        try (DirContext ctx = connect()) {
            Attributes attrs = new BasicAttributes(true);

            // objectClass を定義（必須）
            Attribute objClass = new BasicAttribute("objectClass");
            objClass.add("top");
            objClass.add("group");  // グループの場合
            attrs.put(objClass);

            // sAMAccountName（必須、ログオン名などに利用）
            attrs.put("sAMAccountName", groupCN);

            // 一般的な説明属性（任意）
            attrs.put("description", "This is a test group created via LDAP"+ groupCN);

            // グループの種類（任意：514 = セキュリティ有効、ドメイングローバルグループ）
            attrs.put("groupType", String.valueOf(0x00000002 | 0x80000000)); 
            // 0x00000002 = グローバルグループ, 0x80000000 = セキュリティ有効

            // メール属性（任意）
            attrs.put("mail", groupCN.toLowerCase() + "@sandbox.local");

            // 管理者がよく使う表示名（任意）
            attrs.put("displayName", groupCN);

            // distinguishedName を使ってオブジェクト作成
            String dn = adProperty.getObjectDn(groupCN);

            // 管理者の設定
            attrs.put("managedBy", adProperty.getAdminPrincipal());

            ctx.createSubcontext(dn, attrs);
        }
    }

    public void deleteGroup(String groupCN) throws NamingException {
        try (DirContext ctx = connect()) {
            String dn = adProperty.getObjectDn(groupCN);
            ctx.destroySubcontext(dn);
        }
    }

    public void renameGroup(String oldCN, String newCN) throws NamingException {
        try (DirContext ctx = connect()) {
            String oldDn = adProperty.getObjectDn(oldCN);
            String newDn = adProperty.getObjectDn(newCN);
            ctx.rename(oldDn, newDn);
        }
    }
    
    /**
     * セキュリティグループのメンバー一覧を取得
     * 1000人ずつ取得して、全メンバーを取得する
     * 
     * @param groupCN グループのCN
     * @return メンバーのDN一覧
     * @throws NamingException
     */
    public List<String> getGroupMembers(String groupCN) throws NamingException {
        try (DirContext ctx = connect()) {
            String groupDn = adProperty.getObjectDn(groupCN);
            List<String> allMembers = new ArrayList<>();
            
            // ページング用の変数
            int pageSize = 1000;
            byte[] cookie = null;
            
            for (byte[] cookie = null; ; ) {
                // ページング制御
                Control[] controls = new Control[]{
                    new PagedResultsControl(pageSize, cookie, Control.CRITICAL)
                };
                ctx.setRequestControls(controls);

                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(SearchControls.OBJECT_SCOPE);
                searchControls.setReturningAttributes(new String[]{"member"});

                NamingEnumeration<SearchResult> results = ctx.search(groupDn, "(objectClass=group)", searchControls);

                if (results.hasMore()) {
                    SearchResult result = results.next();
                    Attribute memberAttr = result.getAttributes().get("member");

                    if (memberAttr != null) {
                        totalCount += memberAttr.size();
                    }
                }

                // 次のページ用 cookie を取得
                byte[] nextCookie = null;
                Control[] responseControls = ctx.getResponseControls();
                if (responseControls != null) {
                    for (Control control : responseControls) {
                        if (control instanceof PagedResultsResponseControl) {
                            nextCookie = ((PagedResultsResponseControl) control).getCookie();
                            break;
                        }
                    }
                }

                if (nextCookie == null || nextCookie.length == 0) {
                    break;
                }

                cookie = nextCookie;
            }  

            
            return allMembers;
        }
    }
    
    /**
     * セキュリティグループのメンバー数を取得
     * 1000人を超えるグループでも正確な数を取得するため、ページング制御を使用
     * 
     * @param groupCN グループのCN
     * @return メンバー数
     * @throws NamingException
     */
    public int getGroupMemberCount(String groupCN) throws NamingException {
    try (DirContext ctx = connect()) {
        String groupDn = adProperty.getObjectDn(groupCN);
        int totalCount = 0;
        int pageSize = 1000;
        byte[] cookie = null;

        for (byte[] cookie = null; cookie == null || cookie.length > 0; ) {
            // ページング制御
            Control[] controls = new Control[]{
                new PagedResultsControl(pageSize, cookie, Control.CRITICAL)
            };
            ctx.setRequestControls(controls);

            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.OBJECT_SCOPE);
            searchControls.setReturningAttributes(new String[]{"member"});
            searchControls.setCountLimit(pageSize); // 明示的に設定

            NamingEnumeration<SearchResult> results = ctx.search(groupDn, "(objectClass=group)", searchControls);

            if (results.hasMore()) {
                SearchResult result = results.next();
                Attribute memberAttr = result.getAttributes().get("member");

                if (memberAttr != null) {
                    int pageCount = memberAttr.size();
                    totalCount += pageCount;
                    System.out.println("Page count: " + pageCount + ", Total so far: " + totalCount);
                }
            }

            // 次ページの cookie を取得
            byte[] nextCookie = null;
            Control[] responseControls = ctx.getResponseControls();
            if (responseControls != null) {
                for (Control control : responseControls) {
                    if (control instanceof PagedResultsResponseControl) {
                        nextCookie = ((PagedResultsResponseControl) control).getCookie();
                        break;
                    }
                }
            }

            // 次ループ用に cookie 更新
            cookie = nextCookie;
        }


        return totalCount;
        }
    }
}