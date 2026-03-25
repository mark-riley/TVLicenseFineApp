package com.example;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tv_license_fines")
public class TVLicenseFine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;
    private String full_name;
    private String postcode;
    private BigDecimal amount;
    private String deadline;

    @ManyToOne(optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private TVLicenseStatus status;

    public TVLicenseFine() {}

    public TVLicenseFine(String reference, String full_name, String postcode, BigDecimal amount, String deadline, TVLicenseStatus status){
        this.reference = reference;
        this.full_name = full_name;
        this.postcode = postcode;
        this.amount = amount;
        this.deadline = deadline;
        this.status = status;
    }

    public Long getId() {return id;}
    public String getReference() {return reference;}
    public String getFull_name() {return full_name;}
    public String getPostcode() {return postcode;}
    public String getDeadline() {return deadline;}

    // Returns string for the UI
    public String getAmount() {return amount.toString();}

    // Helper for calculations
    public BigDecimal getAmountValue() {return amount;}

    public void setStatus(TVLicenseStatus status) {this.status = status;}
    public TVLicenseStatus getStatus() {return status;}
}