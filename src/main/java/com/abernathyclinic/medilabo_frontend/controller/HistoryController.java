package com.abernathyclinic.medilabo_frontend.controller;

import com.abernathyclinic.medilabo_frontend.model.PatientHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/ui")
public class HistoryController {
    @Autowired
    private RestTemplate restTemplate;


    @GetMapping("/history/{patId}")
    public String viewHistory(@PathVariable Integer patId, Model model) {
        PatientHistory[] notes = restTemplate.getForObject("http://localhost:8085/api/history/all", PatientHistory[].class);
        List<PatientHistory> filtered = Arrays.stream(notes)
                .filter(n -> n.getPatId().equals(patId))
                .collect(Collectors.toList());
        model.addAttribute("notes", filtered);
        return "history"; // create history.html
    }

    @PostMapping("/history/add")
    public String addNote(@ModelAttribute PatientHistory note, RedirectAttributes redirectAttributes) {
        restTemplate.postForEntity("http://localhost:8085/api/history/", note, PatientHistory.class);
        redirectAttributes.addFlashAttribute("success", "Note added successfully.");
        return "redirect:/ui/history/" + note.getPatId();
    }
}
