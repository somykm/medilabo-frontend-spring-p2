package com.abernathyclinic.medilabo_frontend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import com.abernathyclinic.medilabo_frontend.model.Patient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/ui")
public class HomeController {

    @Autowired
    private RestTemplate restTemplate;

    private final String baseUrl = "http://localhost:8081/api/patient";

    @GetMapping("/")
    public String listPatients(Model model) {
        log.info("Fetching patient list for homepage");
        Patient[] response = restTemplate.getForObject(baseUrl + "/all", Patient[].class);
        List<Patient> patientList = Arrays.asList(response);
        model.addAttribute("patients", patientList);
        return "add";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("patient", new Patient());
        try {
            ResponseEntity<Patient[]> response = restTemplate.getForEntity(baseUrl + "/all", Patient[].class);
            model.addAttribute("patients", Arrays.asList(response.getBody()));
        } catch (Exception e) {
            model.addAttribute("patients", Collections.emptyList());
            model.addAttribute("error", "Unable to fetch patient list.");
        }
        return "add";
    }

    @PostMapping("/add")
    public String addPatient(@ModelAttribute Patient patient, RedirectAttributes redirectAttributes) {
        log.info("Patient added: {}", patient);
        try {
            restTemplate.postForEntity(baseUrl, patient, Patient.class);
            redirectAttributes.addFlashAttribute("Success","Patient added successfully.");
        } catch (Exception e) {
            log.error("No patients found!", e);
        }
        return "redirect:/ui/add";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Patient patient = restTemplate.getForObject(baseUrl + "/" + id, Patient.class);
        model.addAttribute("patient", patient);
        return "edit";
    }

    @PostMapping("/update/{id}")
    public String updatePatient(@PathVariable Integer id, @ModelAttribute Patient patient) {
        restTemplate.put(baseUrl + "/" + id, patient);
        return "redirect:/ui/add";
    }
}