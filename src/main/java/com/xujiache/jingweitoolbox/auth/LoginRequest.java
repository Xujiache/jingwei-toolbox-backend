package com.xujiache.jingweitoolbox.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank @Size(max = 128) String identifier,
        @NotBlank @Size(max = 256) String password) {
}
