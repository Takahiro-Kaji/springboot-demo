package com.example.demo.service;

import com.example.demo.config.AdProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * GroupManagementServiceのテストクラス
 */
@ExtendWith(MockitoExtension.class)
class GroupManagementServiceTest {

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
    private Attribute mockMemberAttribute;

    @Mock
    private NamingEnumeration<?> mockMemberEnumeration;

    @InjectMocks
    private GroupManagementService groupManagementService;

    @BeforeEach
    void setUp() {
        // AdPropertyのモック設定
        doReturn("CN=TestGroup,OU=Users,DC=example,DC=com").when(adProperty).getObjectDn(anyString());
        doReturn("CN=Admin,OU=Users,DC=example,DC=com").when(adProperty).getAdminPrincipal();
    }

    @Test
    void testCreateGroup_Success() throws Exception {
        // 準備
        String groupCN = "testGroup";
        String groupDN = "CN=testGroup,OU=Users,DC=example,DC=com";

        // メソッドの実行
        groupManagementService.createGroup(groupCN);

        // 検証
        verify(mockContext, times(1)).createSubcontext(eq(groupDN), any(Attributes.class));
    }

    @Test
    void testDeleteGroup_Success() throws Exception {
        // 準備
        String groupCN = "testGroup";
        String groupDN = "CN=testGroup,OU=Users,DC=example,DC=com";

        // メソッドの実行
        groupManagementService.deleteGroup(groupCN);

        // 検証
        verify(mockContext, times(1)).destroySubcontext(eq(groupDN));
    }

    @Test
    void testRenameGroup_Success() throws Exception {
        // 準備
        String oldCN = "oldGroup";
        String newCN = "newGroup";
        String oldDN = "CN=oldGroup,OU=Users,DC=example,DC=com";
        String newDN = "CN=newGroup,OU=Users,DC=example,DC=com";

        // メソッドの実行
        groupManagementService.renameGroup(oldCN, newCN);

        // 検証
        verify(mockContext, times(1)).rename(eq(oldDN), eq(newDN));
    }

    @Test
    void testGetGroupMembers_Success() throws Exception {
        // 準備
        String groupCN = "testGroup";
        String groupDN = "CN=testGroup,OU=Users,DC=example,DC=com";
        List<String> expectedMembers = Arrays.asList(
            "CN=user1,OU=Users,DC=example,DC=com",
            "CN=user2,OU=Users,DC=example,DC=com"
        );

        // 検索結果のモック設定
        doReturn(true, false).when(mockSearchResults).hasMore();
        doReturn(mockAttributes).when(mockSearchResult).getAttributes();
        doReturn(mockMemberAttribute).when(mockAttributes).get("member");
        doReturn(mockMemberEnumeration).when(mockMemberAttribute).getAll();
        doReturn(true, true, false).when(mockMemberEnumeration).hasMore();
        doReturn(
            "CN=user1,OU=Users,DC=example,DC=com",
            "CN=user2,OU=Users,DC=example,DC=com"
        ).when(mockMemberEnumeration).next();
        doReturn(mockSearchResult).when(mockSearchResults).next();

        // レスポンスコントロールのモック設定（ページング終了）
        doReturn(null).when(mockContext).getResponseControls();

        // メソッドの実行
        List<String> result = groupManagementService.getGroupMembers(groupCN);

        // 検証
        assertEquals(expectedMembers.size(), result.size());
        assertTrue(result.containsAll(expectedMembers));
    }

    @Test
    void testGetGroupMembers_WithPaging() throws Exception {
        // 準備
        String groupCN = "testGroup";
        String groupDN = "CN=testGroup,OU=Users,DC=example,DC=com";
        List<String> expectedMembers = Arrays.asList(
            "CN=user1,OU=Users,DC=example,DC=com",
            "CN=user2,OU=Users,DC=example,DC=com"
        );

        // 1ページ目の検索結果
        doReturn(true, false).when(mockSearchResults).hasMore();
        doReturn(mockAttributes).when(mockSearchResult).getAttributes();
        doReturn(mockMemberAttribute).when(mockAttributes).get("member");
        doReturn(mockMemberEnumeration).when(mockMemberAttribute).getAll();
        doReturn(true, false).when(mockMemberEnumeration).hasMore();
        doReturn("CN=user1,OU=Users,DC=example,DC=com").when(mockMemberEnumeration).next();
        doReturn(mockSearchResult).when(mockSearchResults).next();

        // 2ページ目の検索結果
        doReturn(true, false).when(mockSearchResults).hasMore();
        doReturn(true, false).when(mockMemberEnumeration).hasMore();
        doReturn("CN=user2,OU=Users,DC=example,DC=com").when(mockMemberEnumeration).next();

        // ページングコントロールのモック設定
        PagedResultsResponseControl mockResponseControl = mock(PagedResultsResponseControl.class);
        doReturn(new byte[0]).when(mockResponseControl).getCookie(); // 終了を示す空のcookie
        doReturn(new Control[]{mockResponseControl}).when(mockContext).getResponseControls();

        // メソッドの実行
        List<String> result = groupManagementService.getGroupMembers(groupCN);

        // 検証
        assertEquals(expectedMembers.size(), result.size());
        assertTrue(result.containsAll(expectedMembers));
    }

    @Test
    void testGetGroupMemberCount_Success() throws Exception {
        // 準備
        String groupCN = "testGroup";
        String groupDN = "CN=testGroup,OU=Users,DC=example,DC=com";
        int expectedCount = 1500;

        // 検索結果のモック設定
        doReturn(true, false).when(mockSearchResults).hasMore();
        doReturn(mockAttributes).when(mockSearchResult).getAttributes();
        doReturn(mockMemberAttribute).when(mockAttributes).get("member");
        doReturn(expectedCount).when(mockMemberAttribute).size();
        doReturn(mockSearchResult).when(mockSearchResults).next();

        // レスポンスコントロールのモック設定（ページング終了）
        doReturn(null).when(mockContext).getResponseControls();

        // メソッドの実行
        int result = groupManagementService.getGroupMemberCount(groupCN);

        // 検証
        assertEquals(expectedCount, result);
    }

    @Test
    void testGetGroupMemberCount_WithPaging() throws Exception {
        // 準備
        String groupCN = "testGroup";
        String groupDN = "CN=testGroup,OU=Users,DC=example,DC=com";
        int expectedTotalCount = 2500;

        // 1ページ目の検索結果
        doReturn(true, false).when(mockSearchResults).hasMore();
        doReturn(mockAttributes).when(mockSearchResult).getAttributes();
        doReturn(mockMemberAttribute).when(mockAttributes).get("member");
        doReturn(1000).when(mockMemberAttribute).size();
        doReturn(mockSearchResult).when(mockSearchResults).next();

        // 2ページ目の検索結果
        doReturn(true, false).when(mockSearchResults).hasMore();
        doReturn(1500).when(mockMemberAttribute).size();

        // ページングコントロールのモック設定
        PagedResultsResponseControl mockResponseControl = mock(PagedResultsResponseControl.class);
        doReturn(new byte[0]).when(mockResponseControl).getCookie(); // 終了を示す空のcookie
        doReturn(new Control[]{mockResponseControl}).when(mockContext).getResponseControls();

        // メソッドの実行
        int result = groupManagementService.getGroupMemberCount(groupCN);

        // 検証
        assertEquals(expectedTotalCount, result);
    }

    @Test
    void testCreateGroup_WithException() throws Exception {
        // 準備
        String groupCN = "testGroup";

        // createSubcontextで例外をスロー
        doThrow(new NamingException("Group already exists")).when(mockContext).createSubcontext(anyString(), any(Attributes.class));

        // メソッドの実行と例外の検証
        assertThrows(NamingException.class, () -> {
            groupManagementService.createGroup(groupCN);
        });
    }

    @Test
    void testDeleteGroup_WithException() throws Exception {
        // 準備
        String groupCN = "testGroup";

        // destroySubcontextで例外をスロー
        doThrow(new NamingException("Group not found")).when(mockContext).destroySubcontext(anyString());

        // メソッドの実行と例外の検証
        assertThrows(NamingException.class, () -> {
            groupManagementService.deleteGroup(groupCN);
        });
    }

    @Test
    void testRenameGroup_WithException() throws Exception {
        // 準備
        String oldCN = "oldGroup";
        String newCN = "newGroup";

        // renameで例外をスロー
        doThrow(new NamingException("Group not found")).when(mockContext).rename(anyString(), anyString());

        // メソッドの実行と例外の検証
        assertThrows(NamingException.class, () -> {
            groupManagementService.renameGroup(oldCN, newCN);
        });
    }

    @Test
    void testGetGroupMembers_WithException() throws Exception {
        // 準備
        String groupCN = "testGroup";

        // searchで例外をスロー
        doThrow(new NamingException("Group not found")).when(mockContext).search(anyString(), anyString(), any(SearchControls.class));

        // メソッドの実行と例外の検証
        assertThrows(NamingException.class, () -> {
            groupManagementService.getGroupMembers(groupCN);
        });
    }

    @Test
    void testGetGroupMemberCount_WithException() throws Exception {
        // 準備
        String groupCN = "testGroup";

        // searchで例外をスロー
        doThrow(new NamingException("Group not found")).when(mockContext).search(anyString(), anyString(), any(SearchControls.class));

        // メソッドの実行と例外の検証
        assertThrows(NamingException.class, () -> {
            groupManagementService.getGroupMemberCount(groupCN);
        });
    }

    @Test
    void testGetGroupMembers_EmptyGroup() throws Exception {
        // 準備
        String groupCN = "testGroup";
        String groupDN = "CN=testGroup,OU=Users,DC=example,DC=com";

        // 空のグループの検索結果
        doReturn(true, false).when(mockSearchResults).hasMore();
        doReturn(mockAttributes).when(mockSearchResult).getAttributes();
        doReturn(null).when(mockAttributes).get("member"); // member属性が存在しない
        doReturn(mockSearchResult).when(mockSearchResults).next();

        // レスポンスコントロールのモック設定（ページング終了）
        doReturn(null).when(mockContext).getResponseControls();

        // メソッドの実行
        List<String> result = groupManagementService.getGroupMembers(groupCN);

        // 検証
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetGroupMemberCount_EmptyGroup() throws Exception {
        // 準備
        String groupCN = "testGroup";
        String groupDN = "CN=testGroup,OU=Users,DC=example,DC=com";

        // 空のグループの検索結果
        doReturn(true, false).when(mockSearchResults).hasMore();
        doReturn(mockAttributes).when(mockSearchResult).getAttributes();
        doReturn(null).when(mockAttributes).get("member"); // member属性が存在しない
        doReturn(mockSearchResult).when(mockSearchResults).next();

        // レスポンスコントロールのモック設定（ページング終了）
        doReturn(null).when(mockContext).getResponseControls();

        // メソッドの実行
        int result = groupManagementService.getGroupMemberCount(groupCN);

        // 検証
        assertEquals(0, result);
    }
} 