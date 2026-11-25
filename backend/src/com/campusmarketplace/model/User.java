package com.campusmarketplace.model;

public class User extends BaseEntity {

    private final String name;
    private final String email;
    private final String password;
    private final String department;
    private String mode; // "buyer" or "seller"
    private final String role; // "user" or "admin"
    private boolean blocked;

    public User(String id, String name, String email, String password,
                String department, String mode, String role, boolean blocked) {
        super(id);
        this.name = name;
        this.email = email;
        this.password = password;
        this.department = department;
        this.mode = mode;
        this.role = role;
        this.blocked = blocked;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public String getRole() { return role; }
    public synchronized String getMode() { return mode; }
    public synchronized void setMode(String mode) { this.mode = mode; }
    public synchronized boolean isBlocked() { return blocked; }
    public synchronized void toggleBlocked() { this.blocked = !this.blocked; }

    public boolean checkPassword(String attempt) {
        return password.equals(attempt);
    }

    @Override
    public synchronized String toFileLine() {
        return String.join("|", getId(), name, email, password, department,
                mode, role, String.valueOf(blocked));
    }

    public static User fromFileLine(String[] f) {
        return new User(f[0], f[1], f[2], f[3], f[4], f[5], f[6], Boolean.parseBoolean(f[7]));
    }
}
