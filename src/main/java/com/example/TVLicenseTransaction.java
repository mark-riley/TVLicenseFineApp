package com.example;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tv_license_transactions")
public class TVLicenseTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transaction_id;

    @ManyToOne
    @JoinColumn(name = "fine_id")
    private TVLicenseFine fine;
    private BigDecimal amount_paid;
    private String processor_type;
    private String processor_token;
    private LocalDateTime created_at;

    public TVLicenseTransaction() {
    }

    public TVLicenseTransaction(TVLicenseFine fine, BigDecimal amount_paid, String processor_type, String processor_token, LocalDateTime created_at) {
        this.fine = fine;
        this.amount_paid = amount_paid;
        this.processor_type = processor_type;
        this.processor_token = processor_token;
        this.created_at = created_at;
    }




    public BigDecimal getAmount_paid() {
        return amount_paid;
    }

    public void setAmount_paid(BigDecimal amount_paid) {
        this.amount_paid = amount_paid;
    }

    public String getProcessor_type() {
        return processor_type;
    }

    public void setProcessor_type(String processor_type) {
        this.processor_type = processor_type;
    }

    public String getProcessor_token() {
        return processor_token;
    }

    public void setProcessor_token(String processor_token) {
        this.processor_token = processor_token;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    public TVLicenseFine getFine() {
        return fine;
    }

    public void setFine(TVLicenseFine fine) {
        this.fine = fine;
    }
}
