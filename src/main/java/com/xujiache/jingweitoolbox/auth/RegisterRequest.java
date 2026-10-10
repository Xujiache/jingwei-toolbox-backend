package com.xujiache.jingweitoolbox.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 128) String identifier,
        @NotBlank @Size(min = 8, max = 256) String password,
        @Size(max = 128) String displayName) {
}
