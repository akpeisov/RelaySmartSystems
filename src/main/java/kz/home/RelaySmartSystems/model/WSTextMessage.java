package kz.home.RelaySmartSystems.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WSTextMessage {
    String type;
    Object payload;
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

}
