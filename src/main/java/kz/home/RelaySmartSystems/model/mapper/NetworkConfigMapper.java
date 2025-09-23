package kz.home.RelaySmartSystems.model.mapper;

import kz.home.RelaySmartSystems.model.dto.NetworkConfigDTO;
import kz.home.RelaySmartSystems.model.entity.*;
import org.springframework.stereotype.Service;

@Service
public class NetworkConfigMapper {
    public void cloudFromDto(CloudConfig cloud, NetworkConfigDTO.CloudDto dto) {
        if (cloud == null || dto == null) return;
        cloud.setAddress(dto.getAddress());
        cloud.setEnabled(dto.isEnabled());
    }

    public void ethFromDto(EthConfig eth, NetworkConfigDTO.EthDto dto) {
        if (eth == null || dto == null) return;
        eth.setEnabled(dto.isEnabled());
        eth.setDhcp(dto.isDhcp());
        eth.setIp(dto.getIp());
        eth.setNetmask(dto.getNetmask());
        eth.setGateway(dto.getGateway());
        eth.setDns(dto.getDns());
        eth.setEnableReset(dto.isEnableReset());
        eth.setResetGPIO(dto.getResetGPIO());
    }

    public void wifiFromDto(WifiConfig wifi, NetworkConfigDTO.WifiDto dto) {
        if (dto == null || wifi == null) return;
        wifi.setEnabled(dto.isEnabled());
        wifi.setDhcp(dto.isDhcp());
        wifi.setIp(dto.getIp());
        wifi.setNetmask(dto.getNetmask());
        wifi.setGateway(dto.getGateway());
        wifi.setDns(dto.getDns());
        wifi.setSsid(dto.getSsid());
        wifi.setPass(dto.getPass());
    }

    public void ftpFromDto(FtpConfig ftp, NetworkConfigDTO.FtpDto dto) {
        if (dto == null || ftp == null) return;
        ftp.setEnabled(dto.isEnabled());
        ftp.setUser(dto.getUser());
        ftp.setPass(dto.getPass());
    }

    public NetworkConfig fromDto(NetworkConfigDTO dto) {
        NetworkConfig entity = new NetworkConfig();
        entity.setNtpServer(dto.getNtpServer());
        entity.setNtpTZ(dto.getNtpTZ());
        entity.setOtaURL(dto.getOtaURL());

        if (dto.getCloud() != null) {
            CloudConfig cloud = new CloudConfig();
            cloud.setAddress(dto.getCloud().getAddress());
            cloud.setEnabled(dto.getCloud().isEnabled());
            entity.setCloud(cloud);
        }

        if (dto.getEth() != null) {
            EthConfig eth = new EthConfig();
            eth.setEnabled(dto.getEth().isEnabled());
            eth.setDhcp(dto.getEth().isDhcp());
            eth.setIp(dto.getEth().getIp());
            eth.setNetmask(dto.getEth().getNetmask());
            eth.setGateway(dto.getEth().getGateway());
            eth.setDns(dto.getEth().getDns());
            eth.setEnableReset(dto.getEth().isEnableReset());
            eth.setResetGPIO(dto.getEth().getResetGPIO());
            entity.setEth(eth);
        }

        if (dto.getWifi() != null) {
            WifiConfig wifi = new WifiConfig();
            wifi.setEnabled(dto.getWifi().isEnabled());
            wifi.setDhcp(dto.getWifi().isDhcp());
            wifi.setIp(dto.getWifi().getIp());
            wifi.setNetmask(dto.getWifi().getNetmask());
            wifi.setGateway(dto.getWifi().getGateway());
            wifi.setSsid(dto.getWifi().getSsid());
            wifi.setPass(dto.getWifi().getPass());
            entity.setWifi(wifi);
        }

        if (dto.getFtp() != null) {
            FtpConfig ftp = new FtpConfig();
            ftp.setEnabled(dto.getFtp().isEnabled());
            ftp.setUser(dto.getFtp().getUser());
            ftp.setPass(dto.getFtp().getPass());
            entity.setFtp(ftp);
        }

        return entity;
    }
}
