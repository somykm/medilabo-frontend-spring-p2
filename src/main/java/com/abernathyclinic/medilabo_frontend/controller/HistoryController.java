package com.abernathyclinic.medilabo_frontend.controller;

import com.abernathyclinic.medilabo_frontend.model.Patient;
import com.abernathyclinic.medilabo_frontend.model.PatientHistory;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

    private final String historyUrl = "http://localhost:8085/api/history";
    private final String patientUrl = "http://localhost:8085/api/patient";

    @GetMapping
    public String redirectToHistorySlash() {
        return "redirect:/ui/history/";
    }

    @GetMapping("/")
    public String showHistory(HttpServletRequest request, Model model) {
        log.info("Fetching history notes and patient list for UI display.");

        List<PatientHistory> notes = fetchNotes(request);
        List<Patient> patients = fetchPatients(request);

        Map<Integer, Patient> patientMap = patients.stream()
                .collect(Collectors.toMap(Patient::getId, Function.identity()));

        for (PatientHistory note : notes) {
            Patient patient = patientMap.get(note.getPatId());
            if (patient != null) {
                note.setFullName(patient.getFirstName() + " " + patient.getLastName());
            }
        }
        model.addAttribute("notes", notes);
        model.addAttribute("note", new PatientHistory());
        return "history";
    }

    @PostMapping("/add")
    public String addNote(HttpServletRequest request,
                          @ModelAttribute PatientHistory note,
                          RedirectAttributes redirectAttributes) {
        log.info("Trying to add note for patient ID:{}", note.getPatId());
        if (note.getNotes() == null || note.getNotes().isEmpty() || note.getNotes().get(0).isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Note cannot be empty.");
            return "redirect:/ui/history/#addForm";
        }

        String authToken = getCookieValue(request, "AUTH_TOKEN");
        HttpHeaders headers = new HttpHeaders();
        if (authToken != null) {
            headers.add(HttpHeaders.COOKIE, "AUTH_TOKEN=" + authToken);
        }

        HttpEntity<PatientHistory> entity = new HttpEntity<>(note, headers);
        try {
            restTemplate.postForEntity(historyUrl, entity, Void.class);
            log.info("Note successfully added for patient with Id:{}", note.getPatId());
        } catch (Exception e) {
            log.error("Failed to add note for patient ID {}: {}", note.getPatId(), e.getMessage());
        }

        return "redirect:/ui/history";
    }

    @GetMapping("/add/{patId}")
    public String showAddNoteForm(HttpServletRequest request,
                                  @PathVariable Integer patId, Model model) {
        PatientHistory note = new PatientHistory();
        note.setPatId(patId);

        model.addAttribute("note", note);
        log.info("Preparing add-note form for patient ID: {}", patId);
        List<PatientHistory> notes = fetchNotes(request);
        List<Patient> patients = fetchPatients(request);

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

    private List<PatientHistory> fetchNotes(HttpServletRequest request) {
        try {
            String jsessionid = getCookieValue(request, "AUTH_TOKEN");
            HttpHeaders headers = new HttpHeaders();
            if (jsessionid != null) {
                headers.add(HttpHeaders.COOKIE, "AUTH_TOKEN=" + jsessionid);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<PatientHistory[]> response = restTemplate.exchange(
                    historyUrl + "/all",
                    HttpMethod.GET,
                    entity,
                    PatientHistory[].class
            );

            return Arrays.asList(response.getBody());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Patient> fetchPatients(HttpServletRequest request) {
        try {
            String jsessionid = getCookieValue(request, "AUTH_TOKEN");
            HttpHeaders headers = new HttpHeaders();
            if (jsessionid != null) {
                headers.add(HttpHeaders.COOKIE, "AUTH_TOKEN=" + jsessionid);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Patient[]> response = restTemplate.exchange(
                    patientUrl,
                    HttpMethod.GET,
                    entity,
                    Patient[].class
            );
            return Arrays.asList(response.getBody());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}