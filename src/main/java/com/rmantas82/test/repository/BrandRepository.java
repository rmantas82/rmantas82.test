package com.rmantas82.test.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.rmantas82.test.entity.Brand;

/**
 * Repository interface for Brand entity.
 * This interface defines methods for brand-related database operations.
 */
@Repository
public interface BrandRepository {

    /**
     * Find all brands.
     *
     * @return a list of all brands
     */
    List<Brand> findAll();

    /**
     * Find a brand by its ID.
     *
     * @param id the ID of the brand to find
     * @return an Optional containing the brand if found, or empty if not found
     */
    Optional<Brand> findById(Long id);

    /**
     * Save a brand.
     *
     * @param brand the brand to save
     * @return the saved brand
     */
    Brand save(Brand brand);

    /**
     * Delete a brand by its ID.
     *
     * @param id the ID of the brand to delete
     */
    void deleteById(Long id);
}
