package com.company.admin.web;

import com.company.admin.domain.Permission;
import com.company.admin.service.*;
import com.company.admin.web.form.RoleForm;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/roles")
public class RoleController {
    private final RoleService roles;
    public RoleController(RoleService roles) { this.roles = roles; }
    @GetMapping public String list(Model model) { model.addAttribute("roles", roles.list()); return "roles/list"; }
    @GetMapping("/new") public String createForm(Model model) { model.addAttribute("roleForm", new RoleForm()); return prepare(model, null); }
    @GetMapping("/{id}/edit") public String editForm(@PathVariable long id, Model model, RedirectAttributes redirect) {
        try { model.addAttribute("roleForm", roles.form(id)); return prepare(model, id); }
        catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); return "redirect:/roles"; }
    }
    @PostMapping("/new") public String create(@Valid @ModelAttribute RoleForm roleForm, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try { roles.create(roleForm); redirect.addFlashAttribute("success", "Роль создана"); return "redirect:/roles"; }
            catch (BusinessException ex) { errors.reject("business", ex.getMessage()); }
            catch (DataIntegrityViolationException ex) { errors.reject("duplicate", "Название роли уже используется"); }
        }
        return prepare(model, null);
    }
    @PostMapping("/{id}") public String update(@PathVariable long id, @Valid @ModelAttribute RoleForm roleForm,
                    BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try { roles.update(id, roleForm); redirect.addFlashAttribute("success", "Права роли сохранены"); return "redirect:/roles"; }
            catch (BusinessException ex) { errors.reject("business", ex.getMessage()); }
            catch (DataIntegrityViolationException ex) { errors.reject("duplicate", "Название роли уже используется"); }
        }
        return prepare(model, id);
    }
    private String prepare(Model model, Long id) {
        model.addAttribute("permissions", Permission.values()); model.addAttribute("editing", id != null);
        model.addAttribute("roleId", id); return "roles/form";
    }
}
