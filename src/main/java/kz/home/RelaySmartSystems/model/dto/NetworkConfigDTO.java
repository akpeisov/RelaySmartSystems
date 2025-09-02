package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkConfigDTO {
    private UUID id;
    private String ntpServer;
    private String ntpTZ;
    private String otaURL;

    private CloudDto cloud;
    private EthDto eth;
    private WifiDto wifi;
    private FtpDto ftp;

    @Data
    public static class CloudDto {
        private String address;
        private boolean enabled;
    }

    @Data
    public static class EthDto {
        private boolean enabled;
        private boolean dhcp;
        private String ip;
        private String netmask;
        private String gateway;
        private String dns;
        private boolean enableReset;
        private Integer resetGPIO;
    }

    @Data
    public static class WifiDto {
        private boolean enabled;
        private boolean dhcp;
        private String ip;
        private String netmask;
        private String gateway;
        private String ssid;
        private String pass;
    }

    @Data
    public static class FtpDto {
        private boolean enabled;
        private String user;
        private String pass;
    }
}

