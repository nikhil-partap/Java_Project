package com.campusmarketplace.repository;

import com.campusmarketplace.model.BaseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe in-memory repository using ArrayList and ConcurrentHashMap. */
public class InMemoryRepository<T extends BaseEntity> implements Repository<T> {

    private final List<T> items = new ArrayList<>();
    private final Map<String, T> index = new ConcurrentHashMap<>();

    @Override
    public synchronized void add(T item) {
        items.add(item);
        index.put(item.getId(), item);
    }

    @Override
    public T findById(String id) {
        return id == null ? null : index.get(id);
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
                it.remove();
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
