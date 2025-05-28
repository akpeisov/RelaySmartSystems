package kz.home.RelaySmartSystems.model.def;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Info {
    private Integer freememory;
    private String uptime;
    private Integer uptimeraw;
    private String curdate;
    private String devicename;
    private String description;
    private String version;
    private Integer rssi;
    private String ethip;
    private String wifiip;
    private String mac;

//    public String getJson() throws JsonProcessingException {
//        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
//        return ow.writeValueAsString(this);
//    }
}
