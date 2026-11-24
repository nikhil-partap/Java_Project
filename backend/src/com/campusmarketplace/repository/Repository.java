package com.campusmarketplace.repository;

import com.campusmarketplace.model.BaseEntity;

import java.util.List;

/**
 * INTERFACE + GENERICS: a contract for storing any kind of entity.
 * The service only talks to this interface, so the storage could later be swapped
 * (e.g. for a database) without changing the service - that is ABSTRACTION.
 */
public interface Repository<T extends BaseEntity> {

    void add(T item);              // append at the end

    void addFirst(T item);         // insert at the front (newest first)

    T findById(String id);         // null when not found

    List<T> findAll();             // read-only snapshot

    boolean deleteById(String id); // true when something was removed

    int count();
}
