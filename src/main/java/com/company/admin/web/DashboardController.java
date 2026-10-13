package com.company.admin.web;

import com.company.admin.service.DashboardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final DashboardService dashboard;
    private final boolean demo;
    public DashboardController(DashboardService dashboard, @Value("${app.bootstrap.demo:false}") boolean demo) { this.dashboard = dashboard; this.demo = demo; }
    @GetMapping("/login") public String login(Model model) { model.addAttribute("demoMode", demo); return "login"; }
    @GetMapping("/") public String dashboard(Model model) {
        var stats = dashboard.stats();
        model.addAttribute("userCount", stats.userCount()); model.addAttribute("enabledCount", stats.enabledCount());
        model.addAttribute("roleCount", stats.roleCount()); model.addAttribute("auditCount", stats.auditCount());
        return "dashboard";
    }
}
