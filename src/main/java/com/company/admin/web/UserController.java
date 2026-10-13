package com.company.admin.web;

import com.company.admin.service.*;
import com.company.admin.web.form.UserForm;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/users")
public class UserController {
    private final UserService users;
    private final RoleService roles;
    public UserController(UserService users, RoleService roles) { this.users = users; this.roles = roles; }
    @GetMapping public String list(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("users", users.list(q)); model.addAttribute("q", q); return "users/list";
    }
    @GetMapping("/new") public String createForm(Model model, Authentication auth) {
        model.addAttribute("userForm", new UserForm()); return prepare(model, auth, null);
    }
    @GetMapping("/{id}/edit") public String editForm(@PathVariable long id, Model model, Authentication auth) {
        model.addAttribute("userForm", users.form(id)); return prepare(model, auth, id);
    }
    @PostMapping("/new") public String create(@Valid @ModelAttribute UserForm userForm, BindingResult errors, Model model,
                                             Authentication auth, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try { users.create(userForm); redirect.addFlashAttribute("success", "Пользователь создан"); return "redirect:/users"; }
            catch (BusinessException ex) { errors.reject("business", ex.getMessage()); }
            catch (DataIntegrityViolationException ex) { errors.reject("duplicate", "Логин или email уже используется"); }
        }
        userForm.setPassword(""); return prepare(model, auth, null);
    }
    @PostMapping("/{id}") public String update(@PathVariable long id, @Valid @ModelAttribute UserForm userForm,
             BindingResult errors, Model model, Authentication auth, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try { users.update(id, userForm); redirect.addFlashAttribute("success", "Изменения сохранены"); return "redirect:/users"; }
            catch (BusinessException ex) { errors.reject("business", ex.getMessage()); }
            catch (DataIntegrityViolationException ex) { errors.reject("duplicate", "Логин или email уже используется"); }
        }
        userForm.setPassword(""); return prepare(model, auth, id);
    }
    private String prepare(Model model, Authentication auth, Long id) {
        model.addAttribute("roles", roles.assignableRoles());
        model.addAttribute("canAssign", auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ASSIGN")));
        model.addAttribute("editing", id != null); model.addAttribute("userId", id); return "users/form";
    }
}
