package kz.home.RelaySmartSystems.model.alice;

public class ResponseError {
    private String request_id;
    private String status = "error";
    private String message;

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
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

    public ResponseError(String request_id, String status, String message) {
        this.request_id = request_id;
        this.status = status;
        this.message = message;
    }

    public ResponseError(String request_id, String message) {
        this.request_id = request_id;
        this.message = message;
    }
}
