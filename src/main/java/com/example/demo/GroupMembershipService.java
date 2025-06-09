package com.example.demo;

import javax.naming.*;
import javax.naming.directory.*;

public class GroupMembershipService extends ActiveDirectoryService {
    public void addUserToGroup(String userCN, String groupCN) throws NamingException {
        DirContext ctx = connect();
        try {
            String groupDn = "CN=" + groupCN + ",CN=Users,DC=sandbox,DC=local";
            String userDn = "CN=" + userCN + ",CN=Users,DC=sandbox,DC=local";

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
            String userDn = "CN=" + userCN + ",CN=Users,DC=sandbox,DC=local";

            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("member", userDn));
            ctx.modifyAttributes(groupDn, mods);
        } finally {
            ctx.close();
        }
    }
}
