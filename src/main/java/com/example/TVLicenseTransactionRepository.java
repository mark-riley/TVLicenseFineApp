package com.example;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TVLicenseTransactionRepository extends CrudRepository<TVLicenseTransaction, Long> {

    List<TVLicenseTransaction> findByFine(TVLicenseFine fine);

    @Query("SELECT t FROM TVLicenseTransaction t WHERE t.client_transaction_id = :clientId")
    Optional<TVLicenseTransaction> findByClientTransactionId(@Param("clientId") String clientId);

    @Query("SELECT t FROM TVLicenseTransaction t WHERE t.processor_token = :token")
    Optional<TVLicenseTransaction> findByProcessorToken(@Param("token") String token);
}