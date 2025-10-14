package com.abernathyclinic.medilabo_frontend.model;

import lombok.Data;

import java.util.List;

@Data
public class PatientHistory {
    private String _id;
    private Integer patId;
    private List<String> notes;
    private String fullName;
}
