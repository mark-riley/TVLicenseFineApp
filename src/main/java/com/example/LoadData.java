package com.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class LoadData {

    @Bean
    public CommandLineRunner dataLoader(TVLicenseStatusRepository repo) {
        return args -> {
//            repo.save(new TVLicenseStatus(1L, "Not Paid"));
//            repo.save(new TVLicenseStatus(2L, "Partially Paid"));
//            repo.save(new TVLicenseStatus(3L, "Paid"));
//            repo.save(new TVLicenseStatus(4L, "Overdue"));
//            repo.save(new TVLicenseStatus(5L, "Canceled"));
//            repo.save(new TVLicenseStatus(6L, "Being Disputed"));

        };
    }


    @Bean
    public CommandLineRunner initData(TVLicenseStatusRepository statusRepo, TVLicenseFineRepository fineRepo) {
        return args -> {
            TVLicenseStatus paidStatus = statusRepo.findById(1L).orElseThrow();
            fineRepo.save(new TVLicenseFine("Test2", "Test2", "Test2", new BigDecimal("22.34"), "Test2", paidStatus));
        };
    }
}
