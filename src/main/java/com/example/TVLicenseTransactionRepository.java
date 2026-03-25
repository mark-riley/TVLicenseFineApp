package com.example;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TVLicenseTransactionRepository extends CrudRepository<TVLicenseTransaction, Long> {
    List<TVLicenseTransaction> findByFine(TVLicenseFine fine);
}