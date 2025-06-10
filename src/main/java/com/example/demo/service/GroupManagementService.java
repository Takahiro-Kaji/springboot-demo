package com.example.demo.service;

import javax.naming.*;
import javax.naming.directory.*;

import org.springframework.stereotype.Service;

@Service
public class GroupManagementService extends ActiveDirectoryService {
    public void createGroup(String groupCN) throws NamingException {
        DirContext ctx = connect();
        try {
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
            String dn = "CN=" + groupCN + ",CN=Users,DC=sandbox,DC=local";
            ctx.createSubcontext(dn, attrs);

        } finally {
            ctx.close();
        }
    }

    public void deleteGroup(String groupCN) throws NamingException {
        DirContext ctx = connect();
        try {
            String dn = "CN=" + groupCN + ",CN=Users,DC=sandbox,DC=local";
            ctx.destroySubcontext(dn);
        } finally {
            ctx.close();
        }
    }

    public void renameGroup(String oldCN, String newCN) throws NamingException {
        DirContext ctx = connect();
        try {
            String oldDn = "CN=" + oldCN + ",CN=Users,DC=sandbox,DC=local";
            String newDn = "CN=" + newCN + ",CN=Users,DC=sandbox,DC=local";
            ctx.rename(oldDn, newDn);
        } finally {
            ctx.close();
        }
    }
}