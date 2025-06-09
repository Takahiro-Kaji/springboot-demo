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
            Attribute objClass = new BasicAttribute("objectClass");
            objClass.add("top");
            objClass.add("group");
            attrs.put(objClass);
            attrs.put("sAMAccountName", groupCN);

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