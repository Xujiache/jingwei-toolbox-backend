package com.xujiache.jingweitoolbox.config;

import com.xujiache.jingweitoolbox.auth.UserRepository;
import com.xujiache.jingweitoolbox.auth.UserRole;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class BootstrapAdmin {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String identifier;
    private final String password;
    private final String displayName;

    public BootstrapAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.identifier:}") String identifier,
            @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.display-name:系统管理员}") String displayName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.identifier = identifier == null ? "" : identifier.trim().toLowerCase(java.util.Locale.ROOT);
        this.password = password == null ? "" : password;
        this.displayName = displayName;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureAdmin() {
        if (identifier.isBlank() || password.length() < 8) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_IDENTIFIER/PASSWORD 必须配置且密码至少 8 位");
        }
        Optional<?> existing = userRepository.findByIdentifier(identifier);
        if (existing.isEmpty()) {
            userRepository.insert(identifier, passwordEncoder.encode(password), displayName, UserRole.ADMIN);
        }
    }
}
