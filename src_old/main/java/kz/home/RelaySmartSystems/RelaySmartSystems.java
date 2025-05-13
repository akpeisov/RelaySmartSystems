package kz.home.RelaySmartSystems;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@ComponentScan("kz.home.mqttdb.model")
//@EnableJpaRepositories(basePackages = "kz.home")
@EnableJpaAuditing
@EnableScheduling
@EntityScan(basePackages = {"kz.home.RelaySmartSystems.model", "kz.home.RelaySmartSystems.model.relaycontroller"})

public class RelaySmartSystems {

	public static void main(String[] args) {
		SpringApplication.run(RelaySmartSystems.class, args);
	}

}
