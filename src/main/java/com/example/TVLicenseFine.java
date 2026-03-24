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

    public String getReference() {return reference;}
    public String getFull_name() {return full_name;}
    public String getPostcode() {return postcode;}
    public String getAmount() {return amount.toString();}
    public String getDeadline() {return deadline;}

    public Long getId() {return id;}

    public void setReference(String reference) {this.reference = reference;}
    public void setFull_name(String full_name) {this.full_name = full_name;}
    public void setPostcode(String postcode) {this.postcode = postcode;}
    public void setAmount(BigDecimal amount) {this.amount = amount;}
    public void setDeadline(String deadline) {this.deadline = deadline;}

    public TVLicenseStatus getStatus() {
        return status;
    }

    public void setStatus(TVLicenseStatus status) {
        this.status = status;
    }
}
