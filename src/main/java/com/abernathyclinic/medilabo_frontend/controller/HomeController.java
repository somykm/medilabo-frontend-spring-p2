package com.abernathyclinic.medilabo_frontend.controller;

import org.springframework.ui.Model;
import com.abernathyclinic.medilabo_frontend.model.Patient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

@Slf4j
@Controller
public class HomeController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/patients")
    public String getPatients(Model model) {
        String url = "http://localhost:8085/api/patient/all";//gateway
        try {
            Patient[] patients = restTemplate.getForObject(url, Patient[].class);
            model.addAttribute("patients", Arrays.asList(patients));
        } catch (Exception e) {
            log.error("Error fetching patients: {}", e.getMessage());
            model.addAttribute("patients", Collections.emptyList());
            model.addAttribute("error", "Unable to fetch patient data.");
        }
        return "patients";
    }
}

