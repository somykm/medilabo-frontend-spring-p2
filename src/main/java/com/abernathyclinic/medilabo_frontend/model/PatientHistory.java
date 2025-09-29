package com.abernathyclinic.medilabo_frontend.model;

import lombok.Data;

@Data
public class PatientHistory {
    private Integer patId;
    private String note;
    private String firstName;
    private String lastname;
}
