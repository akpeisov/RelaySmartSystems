package kz.home.RelaySmartSystems.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WSTextMessage {
    String type;
    Object payload;

    public WSTextMessage() {
    }

    public WSTextMessage(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    public String makeMessage() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static String send(String type, Object payload) {
        return new WSTextMessage(type, payload).makeMessage();
    }
}
