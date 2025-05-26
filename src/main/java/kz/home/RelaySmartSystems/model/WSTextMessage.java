package kz.home.RelaySmartSystems.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.java.Log;

@Log
@Getter
public class WSTextMessage {
    String type;
    Object payload;

    public WSTextMessage(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public String makeMessage() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.info(e.getLocalizedMessage());
            return null;
        }
    }

    public static String send(String type, Object payload) {
        return new WSTextMessage(type, payload).makeMessage();
    }
}
