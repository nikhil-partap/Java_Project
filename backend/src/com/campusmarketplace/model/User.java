package com.campusmarketplace.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OOP: a class with private fields (ENCAPSULATION), a constructor, getters/setters.
 * INHERITANCE: extends BaseEntity. POLYMORPHISM: overrides toMap().
 * The role field ("user" / "admin") decides what a person may do.
 */
public class User extends BaseEntity {

    private String name;
    private final String email;
    private final String password;
    private String phone;
    private String department;
    private String year;
    private String mode;       // "buyer" | "seller"
    private String bio;
    private final String role; // "user" | "admin"
    private boolean blocked;

    public User(String id, String name, String email, String password, String phone,
                String department, String year, String mode, String bio, String role, boolean blocked) {
        super(id); // calls the BaseEntity constructor
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.department = department;
        this.year = year;
        this.mode = mode;
        this.bio = bio;
        this.role = role;
        this.blocked = blocked;
    }

    /** The password never leaves this class; others can only check it. */
    public boolean checkPassword(String attempt) {
        return password.equals(attempt);
    }

    public String getEmail() { return email; }
    public synchronized boolean isBlocked() { return blocked; }

    public synchronized void setName(String name) { this.name = name; }
    public synchronized void setPhone(String phone) { this.phone = phone; }
    public synchronized void setDepartment(String department) { this.department = department; }
    public synchronized void setYear(String year) { this.year = year; }
    public synchronized void setMode(String mode) { this.mode = mode; }
    public synchronized void setBio(String bio) { this.bio = bio; }

    /** Flips the blocked flag; synchronized so two admins clicking at once stay consistent. */
    public synchronized void toggleBlocked() { this.blocked = !this.blocked; }

    @Override
    public synchronized Map<String, Object> toMap() {
        // LinkedHashMap keeps insertion order so the JSON looks tidy. No password!
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", getId());
        map.put("name", name);
        map.put("email", email);
        map.put("phone", phone);
        map.put("department", department);
        map.put("year", year);
        map.put("mode", mode);
        map.put("bio", bio);
        map.put("role", role);
        map.put("isBlocked", blocked); // AUTOBOXING: boolean -> Boolean
        return map;
    }
}
