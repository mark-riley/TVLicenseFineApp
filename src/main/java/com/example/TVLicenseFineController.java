package com.example;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/")
public class TVLicenseFineController {

    private final TVLicenseFineRepository repo;

    public TVLicenseFineController(TVLicenseFineRepository repo){this.repo = repo;}

    @GetMapping
    public String home(Model model) {
        return "fines/home";
    }

    @GetMapping("/test")
    public String test(Model model) {
        model.addAttribute("fines", repo.findAll());
        return "fines/test";
    }

    @GetMapping("/confirmation")
    public String confirmation(Model model) {
        model.addAttribute("fines", repo.findAll());
        return "fines/confirmation";
    }
    @GetMapping("/find")
    public String find(Model model) {
        return "fines/find";
    }

    @GetMapping("/make")
    public String make(Model model) { return "fines/make";}
}
