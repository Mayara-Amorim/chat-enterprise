package br.com.dialogosistemas.auth_service.api.filter;

import br.com.dialogosistemas.auth_service.domain.gateway.TenantGateway;
import br.com.dialogosistemas.auth_service.domain.model.Tenant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final TenantGateway tenantGateway;
    private final String masterKey;

    public ApiKeyAuthenticationFilter(TenantGateway tenantGateway,
                                      @Value("${auth.master-key}") String masterKey) {
        this.tenantGateway = tenantGateway;
        this.masterKey = masterKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/api/auth/tenants")) {
            String masterKeyHeader = request.getHeader("X-Master-Key");
            if (masterKeyHeader == null || !MessageDigest.isEqual(masterKeyHeader.getBytes(), masterKey.getBytes())) {
                sendUnauthorized(response, "Invalid master key");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || apiKey.isBlank()) {
            sendUnauthorized(response, "API key required");
            return;
        }

        String hash = Tenant.hashApiKey(apiKey);
        var tenant = tenantGateway.findByApiKeyHash(hash);
        if (tenant.isEmpty() || !tenant.get().isActive()) {
            sendUnauthorized(response, "Invalid or inactive API key");
            return;
        }

        request.setAttribute("tenantId", tenant.get().getId());
        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
