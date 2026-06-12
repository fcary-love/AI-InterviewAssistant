package com.faceai.pdfreader.auth;

import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtService jwtService, AuthService authService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicPath(request.getRequestURI())) {
            return true;
        }
        try {
            String token = resolveToken(request);
            AuthUser tokenUser = jwtService.parseToken(token);
            AuthContext.set(authService.loadUser(tokenUser.id()));
            return true;
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(ex.getMessage())));
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private boolean isPublicPath(String uri) {
        return "/api/auth/login".equals(uri)
                || "/api/auth/register".equals(uri)
                || uri.startsWith("/files/");
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("请先登录");
        }
        return header.substring(7);
    }
}
