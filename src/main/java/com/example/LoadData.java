package com.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoadData {

    @Bean
    CommandLineRunner initDatabase(TVLicenseFineRepository repository) {
        return args -> {
//            This is data for testing.  I have commented it out to stop the database re-seeding
//            itself every time the app compiles.
//            repository.save(new Course("Spring Boot Basics", "Mark"));
//            repository.save(new Course("REST APIs", "Alastair"));
//            repository.save(new Course("How To Use Azure Databases", "MarkR"));
            repository.save(new TVLicenseFine("Test", "Test", "Test", 12.34, "Test", 2));

        };
    }
}
