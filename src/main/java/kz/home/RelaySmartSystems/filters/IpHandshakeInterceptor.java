package kz.home.RelaySmartSystems.filters;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class IpHandshakeInterceptor implements HandshakeInterceptor {

    public static final String CLIENT_IP_ADDRESS_KEY = "clientIpAddress";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // Попытаемся получить реальный IP клиента из нескольких заголовков, которые обычно выставляет nginx или другие прокси.
        String ipAddress = null;

        // 1. X-Real-IP (часто устанавливается nginx)
        ipAddress = request.getHeaders().getFirst("X-Real-IP");
        if (isEmptyOrUnknown(ipAddress)) {
            // 2. X-Forwarded-For может содержать список адресов: client, proxy1, proxy2
            String xff = request.getHeaders().getFirst("X-Forwarded-For");
            if (!isEmptyOrUnknown(xff)) {
                // берём первый адрес в списке
                String[] parts = xff.split(",");
                if (parts.length > 0) {
                    ipAddress = parts[0].trim();
                }
            }
        }

        if (isEmptyOrUnknown(ipAddress)) {
            // 3. дополнительные заголовки, которые иногда используют прокси/балансировщики
            String[] otherHeaders = {"Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR"};
            for (String h : otherHeaders) {
                String val = request.getHeaders().getFirst(h);
                if (!isEmptyOrUnknown(val)) {
                    ipAddress = val;
                    break;
                }
            }
        }

        if (isEmptyOrUnknown(ipAddress)) {
            // 4. fallback на адрес, с которого пришло соединение к приложению (может быть IP nginx)
            if (request.getRemoteAddress() != null) {
                ipAddress = request.getRemoteAddress().toString();
            }
        }

        attributes.put(CLIENT_IP_ADDRESS_KEY, ipAddress);
        return true;
    }

    private boolean isEmptyOrUnknown(String s) {
        return s == null || s.trim().isEmpty() || "unknown".equalsIgnoreCase(s.trim());
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Не используется
    }
}
