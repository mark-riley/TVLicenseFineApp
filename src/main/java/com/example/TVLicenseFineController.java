package com.example;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/")
public class TVLicenseFineController {

    private final TVLicenseFineRepository repo;

    public TVLicenseFineController(TVLicenseFineRepository repo){this.repo = repo;}

    @GetMapping
    public String list(Model model) {
        model.addAttribute("fines", repo.findAll());
        return "fines/list";
    }
}
