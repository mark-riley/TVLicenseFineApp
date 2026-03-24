package com.example;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;

@Entity
public class TVLicenseFine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;
    private String full_name;
    private String postcode;
    private BigDecimal amount;
    private String deadline;
    private Integer status_id;

    public TVLicenseFine() {}

    public TVLicenseFine(String reference, String full_name, String postcode, BigDecimal amount, String deadline, Integer status_id){
        this.reference = reference;
        this.full_name = full_name;
        this.postcode = postcode;
        this.amount = amount;
        this.deadline = deadline;
        this.status_id = status_id;
    }

    public String getReference() {return reference;}
    public String getFull_name() {return full_name;}
    public String getPostcode() {return postcode;}
    public String getAmount() {return amount.toString();}
    public String getDeadline() {return deadline;}
    public String getStatus_id() {return status_id.toString();}

    public Long getId() {return id;}

    public void setReference(String reference) {this.reference = reference;}
    public void setFull_name(String full_name) {this.full_name = full_name;}
    public void setPostcode(String postcode) {this.postcode = postcode;}
    public void setAmount(BigDecimal amount) {this.amount = amount;}
    public void setDeadline(String deadline) {this.deadline = deadline;}
    public void setStatus_id(Integer status_id) {this.status_id = status_id;}
}
