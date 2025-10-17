package com.abernathyclinic.medilabo_frontend.controller;

import com.abernathyclinic.medilabo_frontend.model.PatientHistory;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
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
    public String listPatients(Model model, HttpServletRequest servletRequest) {
        String sessionCookie = getSessionCookie(servletRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sessionCookie);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        log.info("Fetching patient list for homepage");

        try {
            ResponseEntity<Patient[]> response = restTemplate.exchange(
                    baseUrl + "/all", HttpMethod.GET, entity, Patient[].class);
            List<Patient> patientList = Arrays.asList(response.getBody());
            model.addAttribute("patients", patientList);
        } catch (Exception e) {
            log.error("Access denied or failed to fetch patients", e);
            model.addAttribute("patients", Collections.emptyList());
            model.addAttribute("error", "Unable to fetch patient list.");
        }
        return "add";
    }

    private String getSessionCookie(HttpServletRequest servletRequest) {
        if (servletRequest.getCookies() != null) {
            for (Cookie cookie : servletRequest.getCookies()) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    return "JSESSIONID=" + cookie.getValue();
                }
            }
        }
        return null;
    }

    @GetMapping("/add")
    public String showAddForm(Model model, HttpServletRequest request) {
        model.addAttribute("patient", new Patient());

        String sessionCookie = getSessionCookie(request);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sessionCookie);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        List<Patient> patientList;
        try {
            ResponseEntity<Patient[]> response = restTemplate.exchange(
                    baseUrl + "/all", HttpMethod.GET, entity, Patient[].class);
            patientList = Arrays.asList(response.getBody());
            model.addAttribute("patients", patientList);
        } catch (Exception e) {
            patientList = Collections.emptyList();
            model.addAttribute("patients", patientList);
            model.addAttribute("error", "Unable to fetch patient list.");
        }

        List<PatientHistory> noteList;
        try {
            ResponseEntity<PatientHistory[]> response = restTemplate.exchange(
                    "http://localhost:8083/api/history/all", HttpMethod.GET, entity, PatientHistory[].class);
            noteList = Arrays.asList(response.getBody());

            Map<Integer, Patient> patientMap = patientList.stream()
                    .collect(Collectors.toMap(Patient::getId, Function.identity()));

            for (PatientHistory history : noteList) {
                Patient patient = patientMap.get(history.getPatId());
                history.setFullName(patient != null
                        ? patient.getFirstName() + " " + patient.getLastName()
                        : "Unknown");
            }

            model.addAttribute("notes", noteList);
        } catch (Exception e) {
            model.addAttribute("notes", Collections.emptyList());
            model.addAttribute("error", "Unable to fetch patient history.");
        }

        return "add";
    }

    @PostMapping("/add")
    public String addPatient(@ModelAttribute Patient patient,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest servletRequest) {
        log.info("Patient added: {}", patient);
        String sessionCookie = getSessionCookie(servletRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sessionCookie);
        HttpEntity<Patient> entity = new HttpEntity<>(patient, headers);
        try {
            restTemplate.exchange(baseUrl, HttpMethod.POST, entity, Patient.class);
            redirectAttributes.addFlashAttribute("Success", "Patient added successfully.");
        } catch (Exception e) {
            log.error("No patients found!", e);
        }
        return "redirect:/ui/add";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model,
                               HttpServletRequest servletRequest) {
        String sessionCookie = getSessionCookie(servletRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sessionCookie);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Patient> response = restTemplate.exchange(baseUrl + "/" + id, HttpMethod.GET, entity, Patient.class);
        model.addAttribute("patient", response.getBody());
        return "edit";
    }

    @PostMapping("/update/{id}")
    public String updatePatient(@PathVariable Integer id, @ModelAttribute Patient patient,
                                HttpServletRequest servletRequest) {
        String sessionCookie = getSessionCookie(servletRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sessionCookie);
        HttpEntity<Patient> entity = new HttpEntity<>(patient, headers);

        restTemplate.exchange(baseUrl + "/" + id, HttpMethod.PUT, entity, Void.class);
        return "redirect:/ui/add";
    }

}