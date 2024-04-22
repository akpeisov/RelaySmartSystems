package kz.home.RelaySmartSystems.controller;

import kz.home.RelaySmartSystems.model.TestMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/test")
public class SocketController {
    private final WebSocketHandler socketHandler;

    public SocketController(WebSocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    @PostMapping("/ws")
    public ResponseEntity<?> testWS(@RequestBody TestMessage testMessage) {
        String response;
        response = socketHandler.sendMessageToUser(testMessage.getId(), testMessage.getMessage());
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/sec")
    public String secureEndpoint(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        if (username != null) {
            return "Привет, " + username + "! Этот эндпоинт требует авторизации.";
        } else {
            return "Ошибка авторизации!";
        }
    }

    @GetMapping("/pub")
    public String publicEndpoint() {
        return "Этот эндпоинт доступен публично";
    }
}
