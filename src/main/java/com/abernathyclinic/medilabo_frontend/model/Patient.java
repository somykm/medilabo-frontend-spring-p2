package com.abernathyclinic.medilabo_frontend.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Patient {
    private Integer id;
    private String firstName;
    private String lastName;
    private LocalDate birthdate;
    private String gender;
    private String address;
    private String phone;

}
