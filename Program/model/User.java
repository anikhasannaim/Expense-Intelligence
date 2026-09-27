package Program.model;

import Program.enums.UserRole;

public class User {
    private final String id;
    private String fullName;
    private final String username;
    private String passwordHash;
    private final UserRole role;

    public User(String id, String fullName, String username, String passwordHash, UserRole role) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String v) {
        fullName = v;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String v) {
        passwordHash = v;
    }

    public UserRole getRole() {
        return role;
    }
}
