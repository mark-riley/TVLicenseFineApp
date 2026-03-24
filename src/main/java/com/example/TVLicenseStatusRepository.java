package com.example;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TVLicenseStatusRepository extends CrudRepository<TVLicenseStatus, Long> {
    // This interface allows you to run .save(), .findAll(), and .count() on statuses
}