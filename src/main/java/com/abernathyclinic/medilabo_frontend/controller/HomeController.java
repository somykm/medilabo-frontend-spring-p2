package com.abernathyclinic.medilabo_frontend.controller;

import org.springframework.ui.Model;
import com.abernathyclinic.medilabo_frontend.model.Patient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Slf4j
@Controller
public class HomeController {
    private final RestTemplate restTemplate;
    @Autowired
    public HomeController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/patients")
    public String getPatients(Model model) {
        String url = "http://localhost:8080/api/patients";
        Patient[] patients = restTemplate.getForObject(url, Patient[].class);
        model.addAttribute("patients", Arrays.asList(patients));
        return "patients";
    }
}

