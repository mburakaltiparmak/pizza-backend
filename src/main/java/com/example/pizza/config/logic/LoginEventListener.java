package com.example.pizza.config.logic;

import com.example.pizza.service.user.UserService;
import com.example.pizza.service.user.SupabaseUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class LoginEventListener implements ApplicationListener<AuthenticationSuccessEvent> {
    private static final Logger logger = LoggerFactory.getLogger(LoginEventListener.class);

    private final UserService userService;
    private final SupabaseUserService supabaseUserService;

    public LoginEventListener(UserService userService, SupabaseUserService supabaseUserService) {
        this.userService = userService;
        this.supabaseUserService = supabaseUserService;
    }

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        logger.info("Authentication success event received: {}", event.getAuthentication().getName());

        // Check the type of authentication
        if (event.getAuthentication().getPrincipal() instanceof UserDetails) {
            // Regular login or Supabase authentication
            UserDetails userDetails = (UserDetails) event.getAuthentication().getPrincipal();
            String email = userDetails.getUsername();

            logger.info("Updating login time for user: {}", email);
            userService.updateLoginTime(email);

            // Additional information can be retrieved from custom user details if needed
            if (userDetails.getAuthorities() != null) {
                logger.debug("User authorities: {}", userDetails.getAuthorities());
            }
        } else {
            logger.warn("Unknown principal type: {}",
                    event.getAuthentication().getPrincipal() != null ?
                            event.getAuthentication().getPrincipal().getClass().getName() : "null");
        }
    }
}