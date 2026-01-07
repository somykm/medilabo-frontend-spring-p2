package com.abernathyclinic.medilabo_frontend.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Diabetes {
    private Integer patId;
    private String expectedRisks;
}