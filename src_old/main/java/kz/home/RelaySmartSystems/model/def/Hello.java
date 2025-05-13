package kz.home.RelaySmartSystems.model.def;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Hello {
    private String type;
    private String token;
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
}
