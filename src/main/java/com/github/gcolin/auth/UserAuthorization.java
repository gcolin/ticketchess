package com.github.gcolin.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_authorization",
        indexes = {
            @Index(name = "idx_user_auth_email", columnList = "email"),
            @Index(name = "idx_user_auth_lookup", columnList = "email,role,scope_type,scope_id,active")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_user_auth_grant",
                    columnNames = {"email", "role", "scope_type", "scope_id"})
        })
public class UserAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 64)
    private RoleCode role;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private AuthorizationScopeType scopeType = AuthorizationScopeType.GLOBAL;

    @Column(name = "scope_id")
    private Integer scopeId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "granted_by", length = 320)
    private String grantedBy;

    public UserAuthorization() {}

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (grantedBy != null) {
            grantedBy = grantedBy.trim().toLowerCase();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RoleCode getRole() {
        return role;
    }

    public void setRole(RoleCode role) {
        this.role = role;
    }

    public AuthorizationScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(AuthorizationScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(Integer scopeId) {
        this.scopeId = scopeId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }
}
