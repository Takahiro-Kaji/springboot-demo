package com.example.demo.service;

import javax.naming.*;
import javax.naming.directory.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

import com.example.demo.config.AdProperty;

/**
 * Active Directoryのグループメンバーシップ操作を提供するサービスクラス
 * ユーザーのグループへの追加・削除、複数ユーザーの一括操作をサポートします。
 */
@Service
public class GroupMembershipService extends ActiveDirectoryService {
    
    @Autowired
    private AdProperty adProperty;

    // 成功ログ（追加成功したユーザーDN）
    private final List<String> successLog = new ArrayList<>();

    // 失敗ログ（失敗したバッチのユーザーDNリスト）
    private final List<List<String>> failureLog = new ArrayList<>();

    /**
     * 指定されたユーザーCNに対応するDNを検索します
     * 
     * @param userCN 検索対象のユーザーCN
     * @return ユーザーのDN（Distinguished Name）
     * @throws NamingException ユーザーが見つからない場合、または検索中にエラーが発生した場合
     */
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
     * 複数のユーザーCNを指定して、対応するDNを一括検索します
     * 効率的な検索のため、OR条件を使用して一度のクエリで複数ユーザーを検索します
     * 
     * @param userCNs 検索対象のユーザーCNのリスト
     * @return ユーザーCNの順序に対応するDNのリスト
     * @throws NamingException 一部または全てのユーザーが見つからない場合、または検索中にエラーが発生した場合
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
     * 複数のユーザーを指定されたグループに100人ずつ追加します。
     * 成功・失敗のログは内部に記録され、コンソールには出力しません。
     * 
     * @param userCNs グループに追加するユーザーCNのリスト
     * @param groupCN 対象グループのCN
     * @throws NamingException 致命的なLDAPエラーが発生した場合（途中でも throw）
     */
    public void addMultipleUsersToGroup(List<String> userCNs, String groupCN) throws NamingException {
        try (DirContext ctx = connect()) {
            String groupDn = "CN=" + groupCN + "," + adProperty.getUsersDn();
            List<String> userDNs = findMultipleUserDNs(userCNs);

            int batchSize = 100;
            for (int i = 0; i < userDNs.size(); i += batchSize) {
                int end = Math.min(i + batchSize, userDNs.size());
                List<String> batch = userDNs.subList(i, end);

                ModificationItem[] mods = new ModificationItem[batch.size()];
                for (int j = 0; j < batch.size(); j++) {
                    mods[j] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("member", batch.get(j)));
                }

                try {
                    ctx.modifyAttributes(groupDn, mods);
                    successLog.addAll(batch);  // 成功ログに記録
                } catch (NamingException e) {
                    failureLog.add(new ArrayList<>(batch));  // 失敗ログに記録
                    // 続行（再throwしない）ことで部分成功を許容
                }
            }
        }
    }

    // 成功したユーザーDNの一覧を取得
    public List<String> getSuccessLog() {
        return Collections.unmodifiableList(successLog);
    }

    // 失敗したユーザーDNのバッチ一覧を取得
    public List<List<String>> getFailureLog() {
        return Collections.unmodifiableList(failureLog);
    }
    
    /**
     * 複数のユーザーを指定されたグループから1人ずつ削除します。
     * 各ユーザーに対して個別に modifyAttributes を実行します。
     *
     * @param userCNs グループから削除するユーザーCNのリスト
     * @param groupCN 対象グループのCN
     * @throws NamingException グループのDN取得や接続時に失敗した場合
     */
    public void removeMultipleUsersFromGroup(List<String> userCNs, String groupCN) throws NamingException {
        try (DirContext ctx = connect()) {
            String groupDn = "CN=" + groupCN + "," + adProperty.getUsersDn();
            List<String> userDNs = findMultipleUserDNs(userCNs);

            int batchSize = 100;
            for (int i = 0; i < userDNs.size(); i += batchSize) {
                int end = Math.min(i + batchSize, userDNs.size());
                List<String> batch = userDNs.subList(i, end);

                ModificationItem[] mods = new ModificationItem[batch.size()];
                for (int j = 0; j < batch.size(); j++) {
                    mods[j] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("member", batch.get(j)));
                }

                try {
                    ctx.modifyAttributes(groupDn, mods);
                    successLog.addAll(batch);  // 成功ログに記録
                } catch (NamingException e) {
                    failureLog.add(new ArrayList<>(batch));  // 失敗ログに記録
                    // 続行（再throwしない）ことで部分成功を許容
                }
            }
        }
    }

        // 削除成功したユーザーDN一覧
    public List<String> getRemoveSuccessLog() {
        return Collections.unmodifiableList(successLog);
    }

    // 削除失敗したユーザーDN一覧
    public List<String> getRemoveFailureLog() {
        return Collections.unmodifiableList(failureLog);
    }
    
    /**
     * 指定されたユーザーを指定されたグループに追加します
     * 
     * @param userCN グループに追加するユーザーのCN
     * @param groupCN 対象グループのCN
     * @throws NamingException ユーザーまたはグループが見つからない場合、または操作中にエラーが発生した場合
     */
    public void addUserToGroup(String userCN, String groupCN) throws NamingException {
        try (DirContext ctx = connect()) {
            String groupDn = "CN=" + groupCN + "," + adProperty.getUsersDn();
            String userDn = findUserDN(userCN);

            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("member", userDn));
            ctx.modifyAttributes(groupDn, mods);
        }
    }

    /**
     * 指定されたユーザーを指定されたグループから削除します
     * 
     * @param userCN グループから削除するユーザーのCN
     * @param groupCN 対象グループのCN
     * @throws NamingException ユーザーまたはグループが見つからない場合、または操作中にエラーが発生した場合
     */
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
