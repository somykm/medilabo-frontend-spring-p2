package com.abernathyclinic.medilabo_frontend.controller;

import com.abernathyclinic.medilabo_frontend.model.Patient;
import com.abernathyclinic.medilabo_frontend.model.PatientHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
@RequestMapping("/ui/history")
public class HistoryController {

    @Autowired
    private RestTemplate restTemplate;

    private final String historyUrl = "http://localhost:8085/api/history";
    private final String patientUrl = "http://localhost:8085/api/patient/all";

    @GetMapping
    public String redirectToHistorySlash() {
        return "redirect:/ui/history/";
    }

    @GetMapping("/")
    public String showHistory(Model model) {
        List<PatientHistory> notes = fetchNotes();
        List<Patient> patients = fetchPatients();

        Map<Integer, Patient> patientMap = patients.stream()
                .collect(Collectors.toMap(Patient::getId, Function.identity()));

        for (PatientHistory note : notes) {
            Patient p = patientMap.get(note.getPatId());
            note.setFullName(p != null ? p.getFirstName() + " " + p.getLastName() : "Unknown");
        }

        model.addAttribute("notes", notes);
        model.addAttribute("note", new PatientHistory());
        return "history";
    }

    @PostMapping("/add")
    public String addNote(@ModelAttribute PatientHistory note,
                          RedirectAttributes redirectAttributes) {
        if (note.getNotes() == null || note.getNotes().isEmpty() || note.getNotes().get(0).isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Note cannot be empty.");
            return "redirect:/ui/history/#addForm";
        }

        HttpEntity<PatientHistory> entity = new HttpEntity<>(note);

        try {
            restTemplate.postForEntity(historyUrl, entity, Void.class);
            redirectAttributes.addFlashAttribute("success", "Note added.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add note.");
        }

        return "redirect:/ui/history";
    }

    @GetMapping("/add/{patId}")
    public String showAddNoteForm(@PathVariable Integer patId, Model model) {
        PatientHistory note = new PatientHistory();
        note.setPatId(patId);
        model.addAttribute("note", note);

        List<PatientHistory> notes = fetchNotes();
        List<Patient> patients = fetchPatients();

        Map<Integer, Patient> patientMap = patients.stream()
                .collect(Collectors.toMap(Patient::getId, Function.identity()));

        for (PatientHistory history : notes) {
            Patient patient = patientMap.get(history.getPatId());
            if (patient != null) {
                history.setFullName(patient.getFirstName() + " " + patient.getLastName());
            }
        }

        model.addAttribute("notes", notes);
        return "history";
    }

    // Helpers
    private List<PatientHistory> fetchNotes() {
        try {
            ResponseEntity<PatientHistory[]> response = restTemplate.getForEntity(
                    historyUrl + "/all", PatientHistory[].class);
            return Arrays.asList(response.getBody());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Patient> fetchPatients() {
        try {
            ResponseEntity<Patient[]> response = restTemplate.getForEntity(
                    patientUrl, Patient[].class);
            return Arrays.asList(response.getBody());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}