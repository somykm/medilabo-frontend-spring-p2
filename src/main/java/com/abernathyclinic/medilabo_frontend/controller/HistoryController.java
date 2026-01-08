package com.abernathyclinic.medilabo_frontend.controller;

import com.abernathyclinic.medilabo_frontend.model.Patient;
import com.abernathyclinic.medilabo_frontend.model.PatientHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/ui/history")
public class HistoryController {

    @Autowired
    private RestTemplate restTemplate;
    //    private final String historyUrl = "http://medilabo-gateway:8085/api/history";
//    private final String patientUrl = "http://medilabo-gateway:8085/api/patient";
    private final String historyUrl = "http://localhost:8085/api/history";
    private final String patientUrl = "http://localhost:8085/api/patient";

    @GetMapping
    public String redirectToHistorySlash() {
        return "redirect:http://localhost:8085/ui/history/";
    }

    @GetMapping("/")
    public String showHistory(Model model) {
        log.info("Loading history page");

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
    public String addNote(@ModelAttribute PatientHistory note, RedirectAttributes redirectAttributes) {
        log.info("Adding note for patient {}", note.getPatId());
        if (note.getNotes() == null || note.getNotes().isEmpty() || note.getNotes().get(0).isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Note cannot be empty.");
            return "redirect:http://localhost:8085/ui/history//#addForm";
        }
        try {
            restTemplate.postForObject(historyUrl, note, Void.class);
            redirectAttributes.addFlashAttribute("success", "Note added successfully.");
        } catch (Exception e) {
            log.error("Failed to add note for {}: {}", note.getPatId(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to add note.");
        }
        return "redirect:http://localhost:8085/ui/history";
    }

    @GetMapping("/add/{patId}")
    public String showAddNoteForm(@PathVariable Integer patId, Model model) {

        log.info("Preparing add-note form for patient {}", patId);

        PatientHistory note = new PatientHistory();
        note.setPatId(patId);
        model.addAttribute("note", note);

        List<PatientHistory> notes = fetchNotes();
        List<Patient> patients = fetchPatients();

        Map<Integer, Patient> patientMap = patients.stream()
                .collect(Collectors.toMap(Patient::getId, Function.identity()));

        for (PatientHistory h : notes) {
            Patient p = patientMap.get(h.getPatId());
            h.setFullName(p != null ? p.getFirstName() + " " + p.getLastName() : "Unknown");
        }

        model.addAttribute("notes", notes);

        return "history";
    }

    private List<PatientHistory> fetchNotes() {
        try {
            PatientHistory[] response = restTemplate.getForObject(
                    historyUrl + "/all",
                    PatientHistory[].class
            );
            return response != null ? Arrays.asList(response) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch notes: {}", e.getMessage());
            return Collections.emptyList();
        }
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
}
