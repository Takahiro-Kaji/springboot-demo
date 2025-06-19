package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "ad")
public class AdProperty {
    
    private String host;
    private String password;
    private String adminPrincipal;
    private int port = 636;
    private String baseDn = "DC=sandbox,DC=local";
    private String usersOu = "CN=Users";
    
    /**
     * 完全なプロバイダーURLを取得
     */
    public String getProviderUrl() {
        return "ldaps://" + host + ":" + port;
    }
    
    /**
     * ユーザーOUの完全なDNを取得
     */
    public String getUsersDn() {
        return usersOu + "," + baseDn;
    }
    
    /**
     * 指定されたCNの完全なDNを取得（ユーザーやグループ用）
     */
    public String getObjectDn(String cn) {
        return "CN=" + cn + "," + getUsersDn();
    }
} 