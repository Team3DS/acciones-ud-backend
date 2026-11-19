package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class UserIdHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank() &&
                SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails details = new User(userId, "password",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    details, null, details.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
