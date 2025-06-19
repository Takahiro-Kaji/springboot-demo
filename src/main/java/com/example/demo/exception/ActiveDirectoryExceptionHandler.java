package com.example.demo.exception;

import javax.naming.NamingException;

/**
 * Active Directory操作のエラーハンドリングを行うユーティリティクラス
 */
public class ActiveDirectoryExceptionHandler {
    
    /**
     * NamingExceptionをActiveDirectoryExceptionに変換
     */
    public static ActiveDirectoryException handleNamingException(NamingException e, String operation) {
        return new ActiveDirectoryException("Active Directory操作に失敗しました: " + operation + " - " + e.getMessage(), e);
    }
    
    /**
     * ユーザーが見つからない場合の例外を生成
     */
    public static ActiveDirectoryException userNotFound(String userCN) {
        return new ActiveDirectoryException("ユーザーが見つかりません: " + userCN);
    }
    
    /**
     * 複数のユーザーが見つからない場合の例外を生成
     */
    public static ActiveDirectoryException multipleUsersNotFound(String notFoundUsers) {
        return new ActiveDirectoryException("一部のユーザーが見つかりません: " + notFoundUsers);
    }
    
    /**
     * グループが見つからない場合の例外を生成
     */
    public static ActiveDirectoryException groupNotFound(String groupCN) {
        return new ActiveDirectoryException("グループが見つかりません: " + groupCN);
    }
    
    /**
     * グループ操作失敗の場合の例外を生成
     */
    public static ActiveDirectoryException groupOperationFailed(String groupCN, String operation) {
        return new ActiveDirectoryException("グループ操作に失敗しました: " + operation + " - " + groupCN);
    }
} 