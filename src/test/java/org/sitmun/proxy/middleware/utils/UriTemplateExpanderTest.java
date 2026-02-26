package org.sitmun.proxy.middleware.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.proxy.middleware.utils.UriTemplateExpander.ExpandedResult;

class UriTemplateExpanderTest {

  @Test
  @DisplayName("expand handles simple variable expansion")
  void expandHandlesSimpleVariableExpansion() {
    String template = "https://api.example.com/users/{userId}";
    Map<String, String> params = Map.of("userId", "123");

    String result = UriTemplateExpander.expand(template, params);

    assertEquals("https://api.example.com/users/123", result);
  }

  @Test
  @DisplayName("expand handles multiple variables")
  void expandHandlesMultipleVariables() {
    String template = "https://api.example.com/users/{userId}/posts/{postId}";
    Map<String, String> params = Map.of("userId", "123", "postId", "456");

    String result = UriTemplateExpander.expand(template, params);

    assertEquals("https://api.example.com/users/123/posts/456", result);
  }

  @Test
  @DisplayName("expand handles query parameter operators")
  void expandHandlesQueryParameterOperators() {
    String template = "https://api.example.com/search{?q,limit}";
    Map<String, String> params = Map.of("q", "test", "limit", "10");

    String result = UriTemplateExpander.expand(template, params);

    assertEquals("https://api.example.com/search?q=test&limit=10", result);
  }

  @Test
  @DisplayName("expand handles path operators")
  void expandHandlesPathOperators() {
    String template = "https://api.example.com/{+path}";
    Map<String, String> params = Map.of("path", "users/123/posts");

    String result = UriTemplateExpander.expand(template, params);

    assertEquals("https://api.example.com/users/123/posts", result);
  }

  @Test
  @DisplayName("expand returns original template when parameters are empty")
  void expandReturnsOriginalTemplateWhenParametersAreEmpty() {
    String template = "https://api.example.com/users/{userId}";
    Map<String, String> params = Map.of();

    String result = UriTemplateExpander.expand(template, params);

    assertEquals(template, result);
  }

  @Test
  @DisplayName("expand throws exception when template is null")
  void expandThrowsExceptionWhenTemplateIsNull() {
    Map<String, String> params = Map.of("userId", "123");

    assertThrows(IllegalArgumentException.class, () -> UriTemplateExpander.expand(null, params));
  }

  @Test
  @DisplayName("expand throws exception when parameters are null")
  void expandThrowsExceptionWhenParametersAreNull() {
    String template = "https://api.example.com/users/{userId}";

    assertThrows(IllegalArgumentException.class, () -> UriTemplateExpander.expand(template, null));
  }

  @Test
  @DisplayName("expandWithUsedVariables tracks which variables were used")
  void expandWithUsedVariablesTracksWhichVariablesWereUsed() {
    String template = "https://api.example.com/users/{userId}/posts/{postId}";
    Map<String, String> params = Map.of("userId", "123", "postId", "456", "unused", "789");

    ExpandedResult result = UriTemplateExpander.expandWithUsedVariables(template, params);

    assertEquals("https://api.example.com/users/123/posts/456", result.getUri());
    assertEquals(Set.of("userId", "postId"), result.getUsedVariables());
  }

  @Test
  @DisplayName("hasTemplateVariables returns true for valid template")
  void hasTemplateVariablesReturnsTrueForValidTemplate() {
    String template = "https://api.example.com/users/{userId}";

    boolean result = UriTemplateExpander.hasTemplateVariables(template);

    assertTrue(result);
  }

  @Test
  @DisplayName("hasTemplateVariables returns false for string without variables")
  void hasTemplateVariablesReturnsFalseForStringWithoutVariables() {
    String template = "https://api.example.com/users";

    boolean result = UriTemplateExpander.hasTemplateVariables(template);

    assertFalse(result);
  }

  @Test
  @DisplayName("getVariableNames returns set of variable names")
  void getVariableNamesReturnsSetOfVariableNames() {
    String template = "https://api.example.com/users/{userId}/posts/{postId}";

    Set<String> result = UriTemplateExpander.getVariableNames(template);

    assertEquals(Set.of("userId", "postId"), result);
  }

  @Test
  @DisplayName("expand handles URL encoding correctly")
  void expandHandlesUrlEncodingCorrectly() {
    String template = "https://api.example.com/search{?query}";
    Map<String, String> params = Map.of("query", "hello world & goodbye");

    String result = UriTemplateExpander.expand(template, params);

    assertTrue(result.contains("hello%20world"));
  }

  @Test
  @DisplayName("expand with mutable map does not modify original")
  void expandWithMutableMapDoesNotModifyOriginal() {
    String template = "https://api.example.com/users/{userId}";
    Map<String, String> params = new HashMap<>();
    params.put("userId", "123");
    params.put("extra", "value");

    UriTemplateExpander.expand(template, params);

    assertEquals(2, params.size());
    assertTrue(params.containsKey("extra"));
  }
}
