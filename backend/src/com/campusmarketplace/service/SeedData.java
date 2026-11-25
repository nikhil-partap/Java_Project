package com.campusmarketplace.service;

import com.campusmarketplace.model.BuyerRequest;
import com.campusmarketplace.model.Listing;
import com.campusmarketplace.model.User;

import java.util.ArrayList;
import java.util.List;

/** Initial demo users and listings loaded on first run. */
public final class SeedData {

    private SeedData() {
    }

    public static List<User> users() {
        List<User> list = new ArrayList<>();
        list.add(new User("u1", "Aarav Mehta", "aarav@campus.edu", "password123", "CSE", "seller", "user", false));
        list.add(new User("u2", "Ananya Sharma", "ananya@campus.edu", "password123", "Design", "buyer", "user", false));
        list.add(new User("u3", "Riya Kapoor", "riya@campus.edu", "password123", "Commerce", "buyer", "user", false));
        list.add(new User("u4", "Karan Malhotra", "karan@campus.edu", "password123", "Mechanical", "buyer", "user", false));
        list.add(new User("u-admin", "Nikhil Partap Singh", "nikhilpartapsingh@gmail.com", "password123", "Admin", "seller", "admin", false));
        return list;
    }

    public static List<Listing> listings() {
        List<Listing> list = new ArrayList<>();
        Listing l1 = new Listing("l1", "u1", "Study Table", "Furniture", 2400, "Good", "Hostel A", "Pending", "2026-08-20 09:30");
        l1.addRequest(new BuyerRequest("r1", "u2", "Can I pick this up today evening?", 2400, "2026-08-22 14:00"));
        list.add(l1);

        list.add(new Listing("l2", "u2", "HP Laptop Bag", "Accessories", 850, "Like New", "North Gate", "Available", "2026-08-22 13:15"));
        list.add(new Listing("l3", "u3", "Engineering Textbooks", "Books", 1200, "Good", "Library Block", "Available", "2026-08-25 10:00"));
        list.add(new Listing("l4", "u4", "Campus Bicycle", "Cycles", 7500, "Good", "Sports Complex", "Available", "2026-08-27 08:45"));
        list.add(new Listing("l5", "u1", "LED Desk Lamp", "Furniture", 500, "Like New", "Hostel C", "Available", "2026-08-29 17:20"));
        return list;
    }
}
