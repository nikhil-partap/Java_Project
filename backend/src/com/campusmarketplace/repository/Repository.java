package com.campusmarketplace.repository;

import com.campusmarketplace.model.BaseEntity;
import java.util.List;

/** Generic repository contract for storing entities. */
public interface Repository<T extends BaseEntity> {
    void add(T item);
    T findById(String id);
    List<T> findAll();
    boolean deleteById(String id);
    int count();
}
