package com.abernathyclinic.medilabo_frontend.controller;

import com.abernathyclinic.medilabo_frontend.model.PatientHistory;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/ui")
public class HomeController {

    @Autowired
    private RestTemplate restTemplate;

    private final String baseUrl = "http://localhost:8085/api/patient";

    @GetMapping("/")
    public String listPatients(Model model) {
        try {
            ResponseEntity<Patient[]> response = restTemplate.getForEntity(
                    baseUrl + "/all", Patient[].class);
            model.addAttribute("patients", Arrays.asList(response.getBody()));
        } catch (Exception e) {
            model.addAttribute("patients", Collections.emptyList());
            model.addAttribute("error", "Unable to fetch patient list.");
        }
        return "add";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("patient", new Patient());

        List<Patient> patientList = fetchPatients();
        model.addAttribute("patients", patientList);

        List<PatientHistory> noteList = fetchNotes(patientList);
        model.addAttribute("notes", noteList);

        return "add";
    }

    @PostMapping("/add")
    public String addPatient(@ModelAttribute Patient patient,
                             RedirectAttributes redirectAttributes) {
        try {
            restTemplate.postForEntity(baseUrl, patient, Patient.class);
            redirectAttributes.addFlashAttribute("success", "Patient added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add patient.");
        }
        return "redirect:/ui/add";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        ResponseEntity<Patient> response = restTemplate.getForEntity(baseUrl + "/" + id, Patient.class);
        model.addAttribute("patient", response.getBody());
        return "edit";
    }

    @PostMapping("/update/{id}")
    public String updatePatient(@PathVariable Integer id, @ModelAttribute Patient patient) {
        restTemplate.put(baseUrl + "/" + id, patient);
        return "redirect:/ui/add";
    }

    // Helpers
    private List<Patient> fetchPatients() {
        try {
            ResponseEntity<Patient[]> response = restTemplate.getForEntity(
                    baseUrl + "/all", Patient[].class);
            return Arrays.asList(response.getBody());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    private List<PatientHistory> fetchNotes(List<Patient> patients) {
        try {
            ResponseEntity<PatientHistory[]> response = restTemplate.getForEntity(
                    "http://localhost:8085/api/history/all", PatientHistory[].class);
            Map<Integer, Patient> patientMap = patients.stream()
                    .collect(Collectors.toMap(Patient::getId, Function.identity()));
            List<PatientHistory> notes = Arrays.asList(response.getBody());
            for (PatientHistory note : notes) {
                Patient p = patientMap.get(note.getPatId());
                note.setFullName(p != null ? p.getFirstName() + " " + p.getLastName() : "Unknown");
            }
            return notes;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}