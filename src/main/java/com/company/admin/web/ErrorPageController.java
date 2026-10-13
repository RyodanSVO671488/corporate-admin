package com.company.admin.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class ErrorPageController {
    @RequestMapping("/error/403") @ResponseStatus(HttpStatus.FORBIDDEN)
    public String denied() { return "error/403"; }
}
