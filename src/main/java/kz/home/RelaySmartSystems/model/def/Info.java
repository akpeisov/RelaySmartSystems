package kz.home.RelaySmartSystems.model.def;

public class Info {
    private Integer freememory;
    private String uptime;
    private String curdate;
    private String devicename;
    private String version;
    private Integer rssi;
    private String ethip;
    private String wifiip;
    private String id;

    public String getId() {
        return id;
    }

    public Integer getFreememory() {
        return freememory;
    }

    public String getUptime() {
        return uptime;
    }

    public String getCurdate() {
        return curdate;
    }

    public String getDevicename() {
        return devicename;
    }

    public String getVersion() {
        return version;
    }

    public Integer getRssi() {
        return rssi;
    }

    public String getEthip() {
        return ethip;
    }

    public String getWifiip() {
        return wifiip;
    }
}
