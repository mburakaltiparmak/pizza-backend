package com.example.pizza.config.actuator;

import com.example.pizza.config.actuator.ActuatorConfig.ApplicationStatisticsEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class ActuatorStatisticsInterceptor implements HandlerInterceptor {

    @Autowired(required = false)
    private ApplicationStatisticsEndpoint statisticsEndpoint;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (statisticsEndpoint != null) {
            statisticsEndpoint.incrementRequestCount();

            // Endpoint istatistiklerini güncelle
            String endpoint = request.getRequestURI();
            if (endpoint != null && !endpoint.startsWith("/actuator")) {
                statisticsEndpoint.updateEndpointStats(endpoint);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (statisticsEndpoint != null) {
            // Response status'a göre success/error sayacını güncelle
            int status = response.getStatus();
            if (status >= 200 && status < 400) {
                statisticsEndpoint.incrementSuccessCount();
            } else if (status >= 400) {
                statisticsEndpoint.incrementErrorCount();
            }
        }
    }
}


@Component
class ActuatorInterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private ActuatorStatisticsInterceptor statisticsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(statisticsInterceptor)
                .addPathPatterns("/api/**")  // Sadece API endpoint'lerini izle
                .excludePathPatterns("/actuator/**");  // Actuator endpoint'lerini hariç tut
    }
}