package com.company.admin.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Refreshes permissions from storage before URL and method authorization on every authenticated request. */
public class SessionAccessRefreshFilter extends OncePerRequestFilter {
    private final DatabaseUserDetailsService users;
    public SessionAccessRefreshFilter(DatabaseUserDetailsService users) { this.users = users; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            try {
                var current = users.loadUserByUsername(authentication.getName());
                boolean passwordChanged = authentication.getPrincipal() instanceof SecurityUser previous
                        && !previous.getPassword().equals(current.getPassword());
                if (!current.isEnabled() || passwordChanged) {
                    clear(request);
                } else {
                    var refreshed = UsernamePasswordAuthenticationToken.authenticated(current, null, current.getAuthorities());
                    refreshed.setDetails(authentication.getDetails());
                    SecurityContextHolder.getContext().setAuthentication(refreshed);
                }
            } catch (UsernameNotFoundException ex) { clear(request); }
        }
        chain.doFilter(request, response);
    }
    private void clear(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}
