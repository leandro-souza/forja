package br.com.forja.config;

import br.com.forja.domain.Repositories;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Exige um token de sessao valido em toda a API, exceto login, cadastro e verificacao de saude. */
@Component
public class AuthFilter extends OncePerRequestFilter {
    private final Repositories.Tokens tokens;

    public AuthFilter(Repositories.Tokens tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || path.startsWith("/api/auth/") || path.equals("/api/health")) {
            chain.doFilter(request, response);
            return;
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            var token = tokens.findByToken(header.substring(7));
            if (token.isPresent()) {
                request.setAttribute("userId", token.get().getUserId());
                chain.doFilter(request, response);
                return;
            }
        }
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Sessão expirada. Entre novamente.\"}");
    }
}
