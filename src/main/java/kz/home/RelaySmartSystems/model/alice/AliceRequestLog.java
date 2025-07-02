package kz.home.RelaySmartSystems.model.alice;

import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@NoArgsConstructor
@Table(name = "alice_request_log")
public class AliceRequestLog {
    @Id
    @GeneratedValue
    private Long id;
    private Date datetime;
    @Column(length = 100)
    private String method;
    private String requestId;
    @Column(length = 500)
    private String token;
    private String username;
    @Lob
    private String request;
    @Lob
    private String response;
    @Column(length = 20)
    private String sourceIP;

    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    @PrePersist
    void datetime() {
        this.datetime = new Date();
    }
}
