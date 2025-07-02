package kz.home.RelaySmartSystems.model.def;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Info {
    private Integer freeMemory;
    private String uptime;
    private Integer uptimeRaw;
    private String curdate;
    private String name;
    private String description;
    private String version;
    private Integer wifiRSSI;
    private String ethIP;
    private String wifiIP;
    private String mac;

//    public String getJson() throws JsonProcessingException {
//        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
//        return ow.writeValueAsString(this);
//    }
}
