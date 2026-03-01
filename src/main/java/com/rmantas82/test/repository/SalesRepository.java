package com.rmantas82.test.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.rmantas82.test.entity.Sales;

/**
 * Repository interface for Sales entity.
 */
@Repository
public interface SalesRepository {
    /**
     * Find all sales.
     *
     * @return a list of all sales
     */
    List<Sales> findAll();

    /**
     * Find a sale by its ID.
     *
     * @param id the ID of the sale to find
     * @return an Optional containing the sale if found, or empty if not found
     */
    Optional<Sales> findById(Long id);

}
