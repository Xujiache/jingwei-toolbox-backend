package com.xujiache.jingweitoolbox.auth;

import com.xujiache.jingweitoolbox.common.security.PrincipalType;
import java.util.Set;

public record PrincipalData(PrincipalType principalType, String userId, String displayName, Set<String> scopes) {
}
