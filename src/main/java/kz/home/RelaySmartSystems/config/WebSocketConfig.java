package kz.home.RelaySmartSystems.config;

import kz.home.RelaySmartSystems.controller.WebSocketHandler;
import kz.home.RelaySmartSystems.filters.JwtAuthorizationFilter;
import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.service.ControllerService;
import kz.home.RelaySmartSystems.service.RelayControllerService;
import kz.home.RelaySmartSystems.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    ControllerService controllerService;
    @Autowired
    RelayControllerService relayControllerService;
    @Autowired
    JwtAuthorizationFilter jwtAuthorizationFilter;
    @Autowired
    ApplicationEventPublisher eventPublisher;
    @Autowired
    UserService userService;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler(controllerService, relayControllerService, jwtAuthorizationFilter, userService), "/ws").setAllowedOrigins("*");
    //    registry.addHandler(new WebSocketHandlerUI(jwtAuthorizationFilter, controllerService, eventPublisher), "/wsui").setAllowedOrigins("*");
    }


}