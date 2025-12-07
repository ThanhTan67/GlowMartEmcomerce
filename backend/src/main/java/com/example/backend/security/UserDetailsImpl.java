package com.example.backend.security;

import com.example.backend.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class UserDetailsImpl implements UserDetails {
    @Serial
    private static final long serialVersionUID = 1L;

    private final User user;

    public UserDetailsImpl(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.getRole() == null || user.getRole().getName() == null) {
            return Collections.emptyList();
        }
        return Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getName())
        );
    }

    public int getTokenVersion() {
        Integer version = user.getTokenVersion();
        return version != null ? version : 0;
    }

    public UUID getId() {
        return user.getId();
    }

    @Override
    public String getPassword() {
        return user.getHashedPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        Boolean locked = user.getLocked();
        return locked == null || !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        Boolean enabled = user.getEnabled();
        return enabled != null && enabled;
    }
}
