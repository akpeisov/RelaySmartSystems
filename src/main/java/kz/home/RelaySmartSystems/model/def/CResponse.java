package kz.home.RelaySmartSystems.model.def;

public class CResponse {
    private String message;

    public CResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
