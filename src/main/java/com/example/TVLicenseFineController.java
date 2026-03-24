package com.example;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/")
public class TVLicenseFineController {

    private final TVLicenseFineRepository repo;

    public TVLicenseFineController(TVLicenseFineRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String home(Model model) {
        return "fines/find";
    }

    @PostMapping("/find-fine")
    public String processSearch(@RequestParam String reference, @RequestParam String postcode, Model model) {
        List<TVLicenseFine> fines = repo.findByReferenceAndPostcode(reference, postcode);

        if (!fines.isEmpty()) {
            // Send the first matching fine to the HTML
            model.addAttribute("fine", fines.get(0));
            return "fines/make";
        }
        return "fines/find";
    }


    @GetMapping("/make")
    public String make(Model model) {
        return "fines/make";
    }

    @GetMapping("/confirmation")
    public String confirmation(Model model) {
        model.addAttribute("fines", repo.findAll());
        return "fines/confirmation";
    }

    @GetMapping("/test")
    public String test(Model model) {
        model.addAttribute("fines", repo.findAll());
        return "fines/test";
    }
}
