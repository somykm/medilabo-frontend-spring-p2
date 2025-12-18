package com.abernathyclinic.medilabo_frontend.controller;

import com.abernathyclinic.medilabo_frontend.model.Diabetes;
import com.abernathyclinic.medilabo_frontend.model.PatientHistory;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

    private final String patientUrl = "http://medilabo-gateway:8085/api/patient";
    private final String riskUrl = "http://medilabo-gateway:8085/api/diabetes";
    private final String historyUrl = "http://medilabo-gateway:8085/api/history";


    @GetMapping("/")
    public String listPatients(HttpServletRequest request, Model model) {
        log.info("Getting all the patients");
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication() != null ?
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()
                : "anonymous";
        model.addAttribute("username", username);
        String jsessionid = getCookieValue(request, "AUTH_TOKEN");

        HttpHeaders headers = new HttpHeaders();
        if (jsessionid != null) {
            headers.add(HttpHeaders.COOKIE, "AUTH_TOKEN=" + jsessionid);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Patient[]> response = restTemplate.exchange(
                    patientUrl + "/all",
                    HttpMethod.GET,
                    entity,
                    Patient[].class
            );
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
    public String showAddForm(HttpServletRequest request, Model model) {
        model.addAttribute("patient", new Patient());

        List<Patient> patientList = fetchPatients(request);
        model.addAttribute("patients", patientList);

        List<PatientHistory> noteList = fetchNotes(request, patientList);
        System.out.println("Fetched notes: " + noteList.size());
        model.addAttribute("notes", noteList);

        List<Diabetes> riskList = fetchDiabetesRisks(request, patientList, noteList);
        model.addAttribute("risks", riskList);

        return "add";
    }

    @PostMapping("/add")
    public String addPatient(HttpServletRequest request, @ModelAttribute Patient patient,
                             RedirectAttributes redirectAttributes) {
        try {
            String jsessionid = getCookieValue(request, "AUTH_TOKEN");
            HttpHeaders headers = new HttpHeaders();
            if (jsessionid != null) {
                headers.add(HttpHeaders.COOKIE, "AUTH_TOKEN=" + jsessionid);
            }
            HttpEntity<Patient> entity = new HttpEntity<>(patient, headers);
            restTemplate.postForEntity(patientUrl, entity, Patient.class);
            redirectAttributes.addFlashAttribute("success", "Patient added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add patient.");
        }
        return "redirect:/ui/add";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(HttpServletRequest request, @PathVariable Integer id, Model model) {
        String jsessionid = getCookieValue(request, "AUTH_TOKEN");
        HttpHeaders headers = new HttpHeaders();
        if (jsessionid != null) {
            headers.add(HttpHeaders.COOKIE, "AUTH_TOKEN=" + jsessionid);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Patient> response = restTemplate.exchange(
                patientUrl + "/" + id,
                HttpMethod.GET,
                entity,
                Patient.class
        );

        model.addAttribute("patient", response.getBody());
        return "edit";
    }

    @PostMapping("/update/{id}")
    public String updatePatient(HttpServletRequest request,
                                @PathVariable Integer id,
                                @ModelAttribute Patient patient) {
        String jsessionid = getCookieValue(request, "AUTH_TOKEN");
        HttpHeaders headers = new HttpHeaders();
        if (jsessionid != null) {
            headers.add(HttpHeaders.COOKIE, "AUTH_TOKEN=" + jsessionid);
        }
        HttpEntity<Patient> entity = new HttpEntity<>(patient, headers);
        restTemplate.exchange(patientUrl + "/" + id, HttpMethod.PUT, entity, Void.class);
        return "redirect:/ui/add";
    }

    private Patient getPatientById(HttpServletRequest request, Integer id) {
        String jsessionid = getCookieValue(request, "AUTH_TOKEN");
        HttpHeaders headers = new HttpHeaders();
        if (jsessionid != null) {
            headers.add(HttpHeaders.COOKIE, "AUTH_TOKEN=" + jsessionid);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Patient> response = restTemplate.exchange(
                    patientUrl + "/" + id,
                    HttpMethod.GET,
                    entity,
                    Patient.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch patient {}: {}", id, e.getMessage());
            return null;
        }
    }

    private List<Diabetes> fetchDiabetesRisks(HttpServletRequest request,
                                              List<Patient> patients,
                                              List<PatientHistory> histories) {
        List<Diabetes> risks = new ArrayList<>();
        Map<Integer, List<PatientHistory>> historyGroups = histories.stream()
                .collect(Collectors.groupingBy(PatientHistory::getPatId));

        String jsessionid = getCookieValue(request, "AUTH_TOKEN");
        HttpHeaders headers = new HttpHeaders();
        if (jsessionid != null) {
            headers.add(HttpHeaders.COOKIE, "AUTH_TOKEN=" + jsessionid);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (Patient patient : patients) {
            try {
                Patient p = getPatientById(request, patient.getId());
                if (p == null) {
                    risks.add(new Diabetes(patient.getId(), "Patient not found"));
                    continue;
                }

                List<PatientHistory> patientHistories = historyGroups.get(patient.getId());
                if (patientHistories == null || patientHistories.isEmpty()) {
                    risks.add(new Diabetes(patient.getId(), "No history found"));
                    continue;
                }

                ResponseEntity<String> response = restTemplate.exchange(
                        riskUrl + "/" + patient.getId(),
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                String body = response.getBody();
                log.info("Diabetes service raw response for patient {}: {}", patient.getId(), body);

                String riskLevel = body != null && body.contains(":")
                        ? body.substring(body.lastIndexOf(":") + 1).trim()
                        : "Unavailable";

                risks.add(new Diabetes(patient.getId(), riskLevel));
            } catch (Exception e) {
                log.error("Risk service failed for patient {}: {}", patient.getId(), e.getMessage());
                risks.add(new Diabetes(patient.getId(), "Unavailable"));
            }
        }
        return risks;
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
                    patientUrl + "/all",
                    HttpMethod.GET,
                    entity,
                    Patient[].class
            );
            return Arrays.asList(response.getBody());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<PatientHistory> fetchNotes(HttpServletRequest request, List<Patient> patients) {
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

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}