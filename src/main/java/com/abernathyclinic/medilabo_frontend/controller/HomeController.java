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

        List<Patient> patientList;
        try {
            ResponseEntity<Patient[]> response = restTemplate.getForEntity(baseUrl + "/all", Patient[].class);
            patientList = Arrays.asList(response.getBody());
            model.addAttribute("patients", patientList);
        } catch (Exception e) {
            patientList = Collections.emptyList();
            model.addAttribute("patients", patientList);
            model.addAttribute("error", "Unable to fetch patient list.");
        }

        List<PatientHistory> noteList;
        try {
            PatientHistory[] allNotes = restTemplate.getForObject("http://localhost:8083/api/history/all", PatientHistory[].class);
            noteList = Arrays.asList(allNotes);

            // Enrich notes with fullName
            Map<Integer, Patient> patientMap = patientList.stream()
                    .collect(Collectors.toMap(Patient::getId, Function.identity()));

            for (PatientHistory history : noteList) {
                Patient patient = patientMap.get(history.getPatId());
                if (patient != null) {
                    history.setFullName(patient.getFirstName() + " " + patient.getLastName());
                } else {
                    history.setFullName("Unknown");
                }
            }

            model.addAttribute("notes", noteList);
        } catch (Exception e) {
            model.addAttribute("notes", Collections.emptyList());
            model.addAttribute("error", "Unable to fetch patient history.");
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