package com.abernathyclinic.medilabo_frontend.controller;

import com.abernathyclinic.medilabo_frontend.model.Diabetes;
import com.abernathyclinic.medilabo_frontend.model.Patient;
import com.abernathyclinic.medilabo_frontend.model.PatientHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/ui")
public class HomeController {

    @Autowired
    private RestTemplate restTemplate;
    private final String patientUrl = "http://medilabo-gateway:8085/api/patient";
    private final String riskUrl = "http://medilabo-gateway:8085/api/diabetes";
    private final String historyUrl = "http://medilabo-gateway:8085/api/history";

    @GetMapping("/")
    public String home() {
        return "redirect:/ui/add";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {

        model.addAttribute("patient", new Patient());

        List<Patient> patients = fetchPatients();
        model.addAttribute("patients", patients);

        List<PatientHistory> notes = fetchNotes(patients);
        model.addAttribute("notes", notes);

        List<Diabetes> risks = fetchDiabetesRisks(patients, notes);
        model.addAttribute("risks", risks);

        return "add";
    }

    @PostMapping("/add")
    public String addPatient(@ModelAttribute Patient patient) {
        try {
            restTemplate.postForObject(patientUrl, patient, Patient.class);
            log.info("Patient added successfully");
        } catch (Exception e) {
            log.error("Failed to add patient: {}", e.getMessage());
        }
        return "redirect:/ui/add";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        try {
            Patient patient = restTemplate.getForObject(
                    patientUrl + "/" + id,
                    Patient.class
            );
            model.addAttribute("patient", patient);
        } catch (Exception e) {
            log.error("Failed to load patient {}: {}", id, e.getMessage());
        }
        return "edit";
    }

    @PostMapping("/update/{id}")
    public String updatePatient(@PathVariable Integer id,
                                @ModelAttribute Patient patient) {

        try {
            restTemplate.put(patientUrl + "/" + id, patient);
            log.info("Patient {} updated", id);
        } catch (Exception e) {
            log.error("Failed to update patient {}: {}", id, e.getMessage());
        }

        return "redirect:/ui/add/";
    }

    private List<Patient> fetchPatients() {
        try {
            Patient[] response = restTemplate.getForObject(
                    patientUrl + "/all",
                    Patient[].class
            );
            return response != null ? Arrays.asList(response) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch patients: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<PatientHistory> fetchNotes(List<Patient> patients) {
        try {
            PatientHistory[] response = restTemplate.getForObject(
                    historyUrl + "/all",
                    PatientHistory[].class
            );

            List<PatientHistory> notes = response != null
                    ? Arrays.asList(response)
                    : Collections.emptyList();

            Map<Integer, Patient> patientMap = patients.stream()
                    .collect(Collectors.toMap(Patient::getId, Function.identity()));

            for (PatientHistory note : notes) {
                Patient p = patientMap.get(note.getPatId());
                note.setFullName(p != null ? p.getFirstName() + " " + p.getLastName() : "Unknown");
            }

            return notes;

        } catch (Exception e) {
            log.error("Failed to fetch notes: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Diabetes> fetchDiabetesRisks(List<Patient> patients,
                                              List<PatientHistory> histories) {

        List<Diabetes> risks = new ArrayList<>();

        Map<Integer, List<PatientHistory>> historyGroups =
                histories.stream().collect(Collectors.groupingBy(PatientHistory::getPatId));

        for (Patient patient : patients) {
            try {
                List<PatientHistory> patientHistories = historyGroups.get(patient.getId());

                if (patientHistories == null || patientHistories.isEmpty()) {
                    risks.add(new Diabetes(patient.getId(), "No history found"));
                    continue;
                }

                String response = restTemplate.getForObject(
                        riskUrl + "/" + patient.getId(),
                        String.class
                );

                String riskLevel = (response != null && response.contains(":"))
                        ? response.substring(response.lastIndexOf(":") + 1).trim()
                        : "Unavailable";

                risks.add(new Diabetes(patient.getId(), riskLevel));

            } catch (Exception e) {
                log.error("Failed to fetch diabetes risk for {}: {}", patient.getId(), e.getMessage());
                risks.add(new Diabetes(patient.getId(), "Unavailable"));
            }
        }

        return risks;
    }
}
