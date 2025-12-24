package com.tibia.app.config;

import com.tibia.app.domain.entity.User;
import com.tibia.app.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SessionService sessionService;

    public WebConfig(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Interceptor para verificar autenticação
        registry.addInterceptor(new AuthInterceptor(sessionService))
                .addPathPatterns("/parties/**", "/profile/**", "/characters/**", "/betting/**")
                .excludePathPatterns("/auth/**", "/css/**", "/js/**", "/images/**");

        // Interceptor para forçar criação de senha
        registry.addInterceptor(new PasswordRequiredInterceptor(sessionService))
                .addPathPatterns("/parties/**", "/profile/**", "/characters/**", "/betting/**")
                .excludePathPatterns("/auth/**", "/css/**", "/js/**", "/images/**");
    }

    /**
     * Interceptor para verificar autenticação em rotas protegidas
     */
    static class AuthInterceptor implements HandlerInterceptor {

        private final SessionService sessionService;

        AuthInterceptor(SessionService sessionService) {
            this.sessionService = sessionService;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {

            if (!sessionService.isAuthenticated()) {
                response.sendRedirect("/auth/login?redirect=" + request.getRequestURI());
                return false;
            }

            return true;
        }
    }

    /**
     * Interceptor para forçar usuários sem senha a criar uma
     */
    static class PasswordRequiredInterceptor implements HandlerInterceptor {

        private final SessionService sessionService;

        PasswordRequiredInterceptor(SessionService sessionService) {
            this.sessionService = sessionService;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {

            if (sessionService.isAuthenticated()) {
                User user = sessionService.getCurrentUser().orElse(null);
                if (user != null && !user.hasPassword()) {
                    response.sendRedirect("/auth/set-password");
                    return false;
                }
            }

            return true;
        }
    }
}
