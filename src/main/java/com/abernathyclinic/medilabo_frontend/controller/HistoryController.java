package com.abernathyclinic.medilabo_frontend.controller;

import com.abernathyclinic.medilabo_frontend.model.PatientHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/ui/history")
public class HistoryController {
    @Autowired
    private RestTemplate restTemplate;
    private final String url = "http://localhost:8083/api/history";

    @GetMapping("/")
    public String viewHistory(Model model) {
        log.info("Fetching history for patient ID: {}");

        PatientHistory[] allNotes = restTemplate.getForObject(url + "/all", PatientHistory[].class);
        List<PatientHistory> filtered = Arrays.stream(allNotes)

                .collect(Collectors.toList());

        model.addAttribute("notes", filtered);
        model.addAttribute("note", new PatientHistory());

        return "history";
    }

    @PostMapping("/add")
    public String addNote(@ModelAttribute PatientHistory note, RedirectAttributes redirectAttributes) {
        if (note.getNotes() == null || note.getNotes().isEmpty() || note.getNotes().get(0).isBlank()) {
            log.warn("No notes found for patient ID: {}", note.getPatId());
            redirectAttributes.addFlashAttribute("error", "Note cannot be empty.");
            return "redirect:/ui/history/#addForm";
        }
        try {
            restTemplate.postForEntity(url, note, Void.class);
            redirectAttributes.addFlashAttribute("success", "Note added successfully.");
        } catch (RestClientException e){
            redirectAttributes.addFlashAttribute("error", "Failed to add note:" + e.getMessage());
        }
        return "redirect:/ui/history/#addForm";
    }

    @GetMapping("/add/{patId}")
    public String showAddPage(@PathVariable Integer patId, Model model) {
        PatientHistory history = new PatientHistory();
        history.setPatId(patId);
        model.addAttribute("note", history);
        return "history";
    }
}
