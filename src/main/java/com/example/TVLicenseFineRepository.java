package com.example;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TVLicenseFineRepository extends CrudRepository<TVLicenseFine, Long> {
    // Use 'TVLicenseFine' (the class name), not 'tv_license_fines' (the table name)

    @Query("SELECT c FROM TVLicenseFine c WHERE LOWER(c.reference) LIKE LOWER(CONCAT('%', :reference, '%')) OR LOWER(c.postcode) LIKE LOWER(CONCAT('%', :postcode, '%'))")
    List<TVLicenseFine> findByReferenceAndPostcode(String reference, String postcode);
}


//    @Query("SELECT c FROM Course c WHERE " +
//            "LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
//            "LOWER(c.instructor) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
//            "LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
//    List<Course> searchAllFields(@Param("searchTerm") String searchTerm);