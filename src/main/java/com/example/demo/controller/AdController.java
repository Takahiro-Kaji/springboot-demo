package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;

import com.example.demo.service.GroupLookupService;
import com.example.demo.service.GroupManagementService;
import com.example.demo.service.GroupMembershipService;

import javax.naming.NamingException;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/groups")
public class AdController {

    private final GroupManagementService managementService = new GroupManagementService();
    private final GroupLookupService lookupService = new GroupLookupService();
    private final GroupMembershipService membershipService = new GroupMembershipService();

    @GetMapping("/ref")
    public List<String> listGroups() throws NamingException {
        List<String> groupNames = new ArrayList<>();
        lookupService.listGroups(); 
        return groupNames;
    }

    @PostMapping("/{groupName}")
    public String createGroup(@PathVariable String groupName) throws NamingException {
        managementService.createGroup(groupName);
        return "Group created: " + groupName;
    }

    @DeleteMapping("/{groupName}")
    public String deleteGroup(@PathVariable String groupName) throws NamingException {
        managementService.deleteGroup(groupName);
        return "Group deleted: " + groupName;
    }

    @PutMapping("/{oldName}/rename/{newName}")
    public String renameGroup(@PathVariable String oldName, @PathVariable String newName) throws NamingException {
        managementService.renameGroup(oldName, newName);
        return "Group renamed from " + oldName + " to " + newName;
    }

    @PostMapping("/{groupName}/members/{userName}")
    public String addUserToGroup(@PathVariable String userName, @PathVariable String groupName) throws NamingException {
        membershipService.addUserToGroup(userName, groupName);
        return "User " + userName + " added to group " + groupName;
    }

    @DeleteMapping("/{groupName}/members/{userName}")
    public String removeUserFromGroup(@PathVariable String userName, @PathVariable String groupName) throws NamingException {
        membershipService.removeUserFromGroup(userName, groupName);
        return "User " + userName + " removed from group " + groupName;
    }
}
