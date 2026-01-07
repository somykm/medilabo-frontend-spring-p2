package com.abernathyclinic.medilabo_frontend.model;

import lombok.Data;

@Data
public class Patient {
    private Integer id;
    private String firstName;
    private String lastName;
    private String birthdate;
    private String gender;
    private String address;
    private String phone;
}