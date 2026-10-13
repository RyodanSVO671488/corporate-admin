package com.company.admin.web;

import com.company.admin.service.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuditController {
    private final AuditService audit;
    public AuditController(AuditService audit) { this.audit = audit; }
    @GetMapping("/audit") public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("entries", audit.list(page)); return "audit";
    }
}
