package com.campusmarketplace.service;

import com.campusmarketplace.model.BuyerRequest;
import com.campusmarketplace.model.Listing;
import com.campusmarketplace.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Starting data, copied from frontend/src/data/seedState.js (same ids and values).
 * STATIC: a utility class - only static methods, private constructor so no objects.
 */
public final class SeedData {

    private static final String PASSWORD = "password123";

    private SeedData() {
    }

    public static List<User> users() {
        List<User> users = new ArrayList<>();
        users.add(new User("u1", "Aarav Mehta", "aarav@campus.edu", PASSWORD, "+91 98765 43210",
                "Computer Science", "3rd Year", "seller",
                "Selling used gadgets and books from the hostel block.", "user", false));
        users.add(new User("u2", "Ananya Sharma", "ananya@campus.edu", PASSWORD, "+91 99887 66554",
                "Design", "2nd Year", "buyer",
                "Looking for practical items and affordable student deals.", "user", false));
        users.add(new User("u3", "Riya Kapoor", "riya@campus.edu", PASSWORD, "+91 98770 11223",
                "Commerce", "1st Year", "buyer",
                "Looking for affordable study essentials and books.", "user", false));
        users.add(new User("u4", "Karan Malhotra", "karan@campus.edu", PASSWORD, "+91 98111 22334",
                "Mechanical Engineering", "2nd Year", "buyer",
                "Interested in practical campus gear and sports items.", "user", false));
        users.add(new User("u5", "Mehak Arora", "mehak@campus.edu", PASSWORD, "+91 98990 44556",
                "Computer Applications", "3rd Year", "buyer",
                "Selling and finding useful items around campus.", "user", false));
        users.add(new User("u-admin", "Nikhil Partap Singh", "nikhilpartapsingh@gmail.com", PASSWORD, "",
                "", "", "seller", "Campus Marketplace administrator.", "admin", false));
        return users;
    }

    public static List<Listing> listings() {
        List<Listing> listings = new ArrayList<>();

        Listing l1 = listing("l1", "u1", "Study Table", "Furniture", 2400, "Good",
                "Compact wooden study table with drawer and cable slot.", "Hostel A", "Pending",
                "2026-08-20T09:30:00.000Z",
                "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=1200&q=80");
        l1.addRequestFirst(new BuyerRequest("r1", "u2", "Can I pick this up today evening?", 2400,
                "Pending", "2026-08-22T14:00:00.000Z"));
        listings.add(l1);

        listings.add(listing("l2", "u2", "HP Laptop Bag", "Accessories", 850, "Like New",
                "Lightweight padded bag for 15.6 inch laptops.", "Campus North Gate", "Available",
                "2026-08-22T13:15:00.000Z",
                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=1200&q=80"));
        listings.add(listing("l3", "u3", "Engineering Textbook Set", "Books", 1200, "Good",
                "First-year engineering textbooks with highlighted important topics.", "Library Block",
                "Available", "2026-08-25T10:00:00.000Z",
                "https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=1200&q=80"));
        listings.add(listing("l4", "u4", "Campus Bicycle", "Cycles", 7500, "Good",
                "Comfortable city bicycle, recently serviced and ready for campus rides.", "Sports Complex",
                "Available", "2026-08-27T08:45:00.000Z",
                "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=1200&q=80"));
        listings.add(listing("l5", "u5", "Study Desk Lamp", "Furniture", 500, "Like New",
                "Compact LED desk lamp with adjustable brightness for late-night study.", "Hostel C Common Room",
                "Available", "2026-08-29T17:20:00.000Z",
                "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=1200&q=80"));
        return listings;
    }

    /** Small helper so each seed listing also gets images = [imageUrl]. */
    private static Listing listing(String id, String ownerId, String title, String category, double price,
                                   String condition, String description, String location, String status,
                                   String createdAt, String imageUrl) {
        Listing listing = new Listing(id, ownerId, title, category, price, condition, description,
                location, status, createdAt, imageUrl);
        listing.setImages(List.of(imageUrl));
        return listing;
    }
}
