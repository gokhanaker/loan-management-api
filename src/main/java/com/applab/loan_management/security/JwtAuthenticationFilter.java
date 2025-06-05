package com.applab.loan_management.security;

import com.applab.loan_management.constants.Role;
import com.applab.loan_management.service.CustomerUserDetails;
import com.applab.loan_management.entity.Customer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    // Bypass authentication filter for login and register endpoints
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.equals("/api/auth/register") || path.equals("/api/auth/authenticate");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        userEmail = jwtUtil.extractUsername(jwt);

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Extract user information from JWT claims instead of database lookup
                Long userId = jwtUtil.extractUserId(jwt);
                Role role = jwtUtil.extractRole(jwt);
                
                if (userId != null && role != null) {
                    // Create a minimal Customer object for UserDetails (no DB call needed)
                    Customer customer = Customer.builder()
                            .id(userId)
                            .email(userEmail)
                            .role(role)
                            .build();
                    
                    CustomerUserDetails userDetails = new CustomerUserDetails(customer);
                    
                    // Validate token with the created UserDetails
                    if (jwtUtil.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                jwt,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception e) {
                // If JWT parsing fails, continue without authentication
                logger.debug("JWT parsing failed: " + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
