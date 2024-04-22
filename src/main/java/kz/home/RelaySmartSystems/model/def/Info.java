package kz.home.RelaySmartSystems.model.def;

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


    public Integer getFreememory() {
        return freememory;
    }

    public void setFreememory(Integer freememory) {
        this.freememory = freememory;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public Integer getUptimeraw() {
        return uptimeraw;
    }

    public void setUptimeraw(Integer uptimeraw) {
        this.uptimeraw = uptimeraw;
    }

    public String getCurdate() {
        return curdate;
    }

    public void setCurdate(String curdate) {
        this.curdate = curdate;
    }

    public String getDevicename() {
        return devicename;
    }

    public void setDevicename(String devicename) {
        this.devicename = devicename;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getRssi() {
        return rssi;
    }

    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }

    public String getEthip() {
        return ethip;
    }

    public void setEthip(String ethip) {
        this.ethip = ethip;
    }

    public String getWifiip() {
        return wifiip;
    }

    public void setWifiip(String wifiip) {
        this.wifiip = wifiip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
