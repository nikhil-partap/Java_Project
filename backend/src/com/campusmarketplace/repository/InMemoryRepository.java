package com.campusmarketplace.repository;

import com.campusmarketplace.model.BaseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * INTERFACE IMPLEMENTATION: stores entities in memory (no database).
 * COLLECTIONS:
 *   - ArrayList keeps the display order,
 *   - ConcurrentHashMap gives fast, thread-safe lookup by id,
 *   - Iterator.remove() deletes safely while looping,
 *   - Collections.unmodifiableList hands out a read-only copy.
 * MULTITHREADING: methods that touch the list are synchronized because many HTTP
 * worker threads use the same repository at once.
 */
public class InMemoryRepository<T extends BaseEntity> implements Repository<T> {

    private final List<T> items = new ArrayList<>();
    private final Map<String, T> index = new ConcurrentHashMap<>();

    @Override
    public synchronized void add(T item) {
        items.add(item);
        index.put(item.getId(), item);
    }

    @Override
    public synchronized void addFirst(T item) {
        items.add(0, item);
        index.put(item.getId(), item);
    }

    @Override
    public T findById(String id) {
        if (id == null) {
            return null; // ConcurrentHashMap does not allow null keys
        }
        return index.get(id);
    }

    @Override
    public synchronized List<T> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    @Override
    public synchronized boolean deleteById(String id) {
        Iterator<T> it = items.iterator();
        while (it.hasNext()) {
            if (it.next().getId().equals(id)) {
                it.remove(); // ITERATOR REMOVAL: no ConcurrentModificationException
                index.remove(id);
                return true;
            }
        }
        return false;
    }

    @Override
    public int count() {
        return index.size();
    }
}
