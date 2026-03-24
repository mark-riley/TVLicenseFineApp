package com.example;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "tv_license_status")
public class TVLicenseStatus {

    @Id

    private Long status_id;
    private String status_label;

    public TVLicenseStatus() {}

    public TVLicenseStatus(Long status_id, String status_label)
    {
        this.status_id = status_id;
        this.status_label = status_label;
    }

    public Long getStatus_id() {
        return status_id;
    }

    public void setStatus_id(Long status_id) {
        this.status_id = status_id;
    }

    public String getStatus_label() {
        return status_label;
    }

    public void setStatus_label(String status_label) {
        this.status_label = status_label;
    }
}
