package com.abernathyclinic.medilabo_frontend.controller;

import com.abernathyclinic.medilabo_frontend.model.Diabetes;
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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/ui")
public class HomeController {

    @Autowired
    private RestTemplate restTemplate;

    private final String patientUrl = "http://localhost:8085/api/patient";
    private final String riskUrl = "http://localhost:8085/api/diabetes";

    @GetMapping("/")
    public String listPatients(Model model) {
        log.info("Getting all the patients");
        try {

            ResponseEntity<Patient[]> response = restTemplate.getForEntity(
                    patientUrl + "/all", Patient[].class);
            List<Patient> patients = Arrays.asList(response.getBody());
            log.info("Get all the patient " + patients.size());
            model.addAttribute("patients", patients);
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
        System.out.println("Fetched notes: " + noteList.size());
        model.addAttribute("notes", noteList);

        List<Diabetes> riskList = fetchDiabetesRisks(patientList, noteList);
        model.addAttribute("risks", riskList);

        return "add";
    }

    @PostMapping("/add")
    public String addPatient(@ModelAttribute Patient patient,
                             RedirectAttributes redirectAttributes) {
        try {
            restTemplate.postForEntity(patientUrl, patient, Patient.class);
            redirectAttributes.addFlashAttribute("success", "Patient added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add patient.");
        }
        return "redirect:/ui/add";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        //ResponseEntity<Patient> response = restTemplate.getForEntity(baseUrl + "/" + id, Patient.class);
        ResponseEntity<PatientHistory[]> response = restTemplate.getForEntity("http://localhost:8085/api/history/all", PatientHistory[].class);
        model.addAttribute("patient", response.getBody());
        return "edit";
    }

    @PostMapping("/update/{id}")
    public String updatePatient(@PathVariable Integer id, @ModelAttribute Patient patient) {
        restTemplate.put(patientUrl + "/" + id, patient);
        return "redirect:/ui/add";
    }

    private List<Diabetes> fetchDiabetesRisks(List<Patient> patients, List<PatientHistory> histories) {
        List<Diabetes> risks = new ArrayList<>();
        Map<Integer, List<PatientHistory>> historyGroups = histories.stream()
                .collect(Collectors.groupingBy(PatientHistory::getPatId));

        for (Patient patient : patients) {
            try {
                List<PatientHistory> patientHistories = historyGroups.get(patient.getId());
                if (patientHistories == null || patientHistories.isEmpty()) {
                    risks.add(new Diabetes(patient.getId(), "No history found"));
                    continue;
                }
                List<String> allNotes = patientHistories.stream()
                        .flatMap(h -> h.getNotes().stream())
                        .toList();
                ResponseEntity<String> response = restTemplate.getForEntity(
                        riskUrl + "/" + patient.getId(), String.class);

                String body = response.getBody();
                String riskLevel = body != null && body.contains(": ")
                        ? body.substring(body.lastIndexOf(":") + 3).trim()
                        : "Unknown";

                risks.add(new Diabetes(patient.getId(), riskLevel));
            } catch (Exception e) {
                log.error("Risk service failed for patient {}:{}", patient.getId(), e.getMessage());
                risks.add(new Diabetes(patient.getId(), "Unavailable"));
            }
        }
        return risks;
    }

    private List<Patient> fetchPatients() {
        try {
            ResponseEntity<Patient[]> response = restTemplate.getForEntity(
                    patientUrl + "/all", Patient[].class);
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