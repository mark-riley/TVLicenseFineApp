package com.example;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TVLicenseFineRepository extends CrudRepository<TVLicenseFine, Long> {

}


//    @Query("SELECT c FROM Course c WHERE " +
//            "LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
//            "LOWER(c.instructor) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
//            "LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
//    List<Course> searchAllFields(@Param("searchTerm") String searchTerm);