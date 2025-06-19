package com.example.demo.service;

import com.example.demo.config.AdProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * GroupMembershipServiceのテストクラス
 */
@ExtendWith(MockitoExtension.class)
class GroupMembershipServiceTest {

    @Mock
    private AdProperty adProperty;

    @Mock
    private DirContext mockContext;

    @Mock
    private NamingEnumeration<SearchResult> mockSearchResults;

    @Mock
    private SearchResult mockSearchResult;

    @Mock
    private Attributes mockAttributes;

    @Mock
    private Attribute mockAttribute;

    @InjectMocks
    private GroupMembershipService groupMembershipService;

    @BeforeEach
    void setUp() {
        // AdPropertyのモック設定
        when(adProperty.getUsersDn()).thenReturn("OU=Users,DC=example,DC=com");
        when(adProperty.getObjectDn(anyString())).thenReturn("CN=TestGroup,OU=Users,DC=example,DC=com");
    }

    @Test
    void testAddUserToGroup_Success() throws Exception {
        // 準備
        String userCN = "testUser";
        String groupCN = "testGroup";
        String userDN = "CN=testUser,OU=Users,DC=example,DC=com";
        String groupDN = "CN=testGroup,OU=Users,DC=example,DC=com";

        // ユーザー検索のモック設定
        when(mockSearchResults.hasMore()).thenReturn(true);
        when(mockSearchResult.getNameInNamespace()).thenReturn(userDN);
        when(mockSearchResults.next()).thenReturn(mockSearchResult);

        // メソッドの実行
        groupMembershipService.addUserToGroup(userCN, groupCN);

        // 検証
        verify(mockContext, times(1)).modifyAttributes(eq(groupDN), any(ModificationItem[].class));
    }

    @Test
    void testRemoveUserFromGroup_Success() throws Exception {
        // 準備
        String userCN = "testUser";
        String groupCN = "testGroup";
        String userDN = "CN=testUser,OU=Users,DC=example,DC=com";
        String groupDN = "CN=testGroup,OU=Users,DC=example,DC=com";

        // ユーザー検索のモック設定
        when(mockSearchResults.hasMore()).thenReturn(true);
        when(mockSearchResult.getNameInNamespace()).thenReturn(userDN);
        when(mockSearchResults.next()).thenReturn(mockSearchResult);

        // メソッドの実行
        groupMembershipService.removeUserFromGroup(userCN, groupCN);

        // 検証
        verify(mockContext, times(1)).modifyAttributes(eq(groupDN), any(ModificationItem[].class));
    }

    @Test
    void testAddMultipleUsersToGroup_Success() throws Exception {
        // 準備
        List<String> userCNs = Arrays.asList("user1", "user2", "user3");
        String groupCN = "testGroup";
        String groupDN = "CN=testGroup,OU=Users,DC=example,DC=com";

        // 複数ユーザー検索のモック設定
        when(mockSearchResults.hasMore()).thenReturn(true, true, true, false);
        when(mockSearchResult.getNameInNamespace()).thenReturn(
            "CN=user1,OU=Users,DC=example,DC=com",
            "CN=user2,OU=Users,DC=example,DC=com",
            "CN=user3,OU=Users,DC=example,DC=com"
        );
        when(mockSearchResult.getAttributes()).thenReturn(mockAttributes);
        when(mockAttributes.get("cn")).thenReturn(mockAttribute);
        when(mockAttribute.get()).thenReturn("user1", "user2", "user3");
        when(mockSearchResults.next()).thenReturn(mockSearchResult);

        // メソッドの実行
        groupMembershipService.addMultipleUsersToGroup(userCNs, groupCN);

        // 検証
        verify(mockContext, times(1)).modifyAttributes(eq(groupDN), any(ModificationItem[].class));
        
        // ログの検証
        List<String> successLog = groupMembershipService.getSuccessLog();
        assertEquals(3, successLog.size());
        assertTrue(successLog.containsAll(userCNs));
        
        List<String> failureLog = groupMembershipService.getFailureLog();
        assertEquals(0, failureLog.size());
    }

    @Test
    void testRemoveMultipleUsersFromGroup_Success() throws Exception {
        // 準備
        List<String> userCNs = Arrays.asList("user1", "user2", "user3");
        String groupCN = "testGroup";
        String groupDN = "CN=testGroup,OU=Users,DC=example,DC=com";

        // 複数ユーザー検索のモック設定
        when(mockSearchResults.hasMore()).thenReturn(true, true, true, false);
        when(mockSearchResult.getNameInNamespace()).thenReturn(
            "CN=user1,OU=Users,DC=example,DC=com",
            "CN=user2,OU=Users,DC=example,DC=com",
            "CN=user3,OU=Users,DC=example,DC=com"
        );
        when(mockSearchResult.getAttributes()).thenReturn(mockAttributes);
        when(mockAttributes.get("cn")).thenReturn(mockAttribute);
        when(mockAttribute.get()).thenReturn("user1", "user2", "user3");
        when(mockSearchResults.next()).thenReturn(mockSearchResult);

        // メソッドの実行
        groupMembershipService.removeMultipleUsersFromGroup(userCNs, groupCN);

        // 検証
        verify(mockContext, times(1)).modifyAttributes(eq(groupDN), any(ModificationItem[].class));
        
        // ログの検証
        List<String> successLog = groupMembershipService.getSuccessLog();
        assertEquals(3, successLog.size());
        assertTrue(successLog.containsAll(userCNs));
        
        List<String> failureLog = groupMembershipService.getFailureLog();
        assertEquals(0, failureLog.size());
    }

    @Test
    void testAddMultipleUsersToGroup_WithFailure() throws Exception {
        // 準備
        List<String> userCNs = Arrays.asList("user1", "user2", "user3");
        String groupCN = "testGroup";

        // 複数ユーザー検索のモック設定
        when(mockSearchResults.hasMore()).thenReturn(true, true, true, false);
        when(mockSearchResult.getNameInNamespace()).thenReturn(
            "CN=user1,OU=Users,DC=example,DC=com",
            "CN=user2,OU=Users,DC=example,DC=com",
            "CN=user3,OU=Users,DC=example,DC=com"
        );
        when(mockSearchResult.getAttributes()).thenReturn(mockAttributes);
        when(mockAttributes.get("cn")).thenReturn(mockAttribute);
        when(mockAttribute.get()).thenReturn("user1", "user2", "user3");
        when(mockSearchResults.next()).thenReturn(mockSearchResult);

        // modifyAttributesで例外をスロー
        doThrow(new NamingException("Test error")).when(mockContext).modifyAttributes(anyString(), any(ModificationItem[].class));

        // メソッドの実行と例外の検証
        assertThrows(NamingException.class, () -> {
            groupMembershipService.addMultipleUsersToGroup(userCNs, groupCN);
        });

        // ログの検証
        List<String> successLog = groupMembershipService.getSuccessLog();
        assertEquals(0, successLog.size());
        
        List<String> failureLog = groupMembershipService.getFailureLog();
        assertEquals(3, failureLog.size());
        assertTrue(failureLog.containsAll(userCNs));
    }

    @Test
    void testUserNotFound() throws Exception {
        // 準備
        String userCN = "nonexistentUser";
        String groupCN = "testGroup";

        // ユーザーが見つからない場合のモック設定
        when(mockSearchResults.hasMore()).thenReturn(false);

        // メソッドの実行と例外の検証
        assertThrows(NamingException.class, () -> {
            groupMembershipService.addUserToGroup(userCN, groupCN);
        });
    }

    @Test
    void testClearLogs() {
        // 準備
        List<String> userCNs = Arrays.asList("user1", "user2");
        String groupCN = "testGroup";

        // ログをクリア
        groupMembershipService.clearLogs();

        // 検証
        List<String> successLog = groupMembershipService.getSuccessLog();
        List<String> failureLog = groupMembershipService.getFailureLog();
        
        assertEquals(0, successLog.size());
        assertEquals(0, failureLog.size());
    }

    @Test
    void testGetSuccessLog_Unmodifiable() {
        // 準備
        List<String> successLog = groupMembershipService.getSuccessLog();

        // 検証：変更不可のリストであることを確認
        assertThrows(UnsupportedOperationException.class, () -> {
            successLog.add("test");
        });
    }

    @Test
    void testGetFailureLog_Unmodifiable() {
        // 準備
        List<String> failureLog = groupMembershipService.getFailureLog();

        // 検証：変更不可のリストであることを確認
        assertThrows(UnsupportedOperationException.class, () -> {
            failureLog.add("test");
        });
    }
} 