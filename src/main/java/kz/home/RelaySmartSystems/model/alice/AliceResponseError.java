package kz.home.RelaySmartSystems.model.alice;

public class AliceResponseError {
    private String requestId;
    private String status = "error";
    private String message;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AliceResponseError(String requestId, String status, String message) {
        this.requestId = requestId;
        this.status = status;
        this.message = message;
    }

    public AliceResponseError(String requestId, String message) {
        this.requestId = requestId;
        this.message = message;
    }
}
