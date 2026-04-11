package dev.ankit.platform.order_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class GatewayAuthFilter extends OncePerRequestFilter {

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Actuator + Internal — skip gateway check
        if (path.startsWith("/actuator/") ||
                path.startsWith("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Gateway secret verify karo
        String secret = request.getHeader(
                "X-Gateway-Secret");

        if (!gatewaySecret.equals(secret)) {
            log.warn("Direct service access blocked — " +
                            "path={}, ip={}",
                    path,
                    request.getRemoteAddr());
            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Direct service access not allowed\"," +
                            "\"message\":\"Please use API Gateway\"}");
            return;
        }

        // User headers extract karo
        String userId = request.getHeader("X-User-Id");
        String role   = request.getHeader("X-User-Role");
        String email  = request.getHeader("X-User-Email");

        if (userId != null && role != null) {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userId, null,
                            List.of(new SimpleGrantedAuthority(role))
                    );
            SecurityContextHolder.getContext()
                    .setAuthentication(auth);
            log.debug("Auth set from gateway — userId={}",
                    userId);
        }

        filterChain.doFilter(request, response);
    }
}