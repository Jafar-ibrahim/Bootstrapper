package org.example.bootstrapper;
import lombok.extern.log4j.Log4j2;
import org.example.bootstrapper.Service.Network.NetworkService;
import org.example.bootstrapper.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@Log4j2
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class })
public class BootstrapperApplication implements CommandLineRunner {
	@Autowired
	private NetworkService networkService;
	@Autowired
	private UserService userService;

	public static void main(String[] args) {
		SpringApplication.run(BootstrapperApplication.class, args);
	}

	@Override
	public void run(String... args){
		log.info("Bootstrapper is up and running.....");
		networkService.init();
		userService.createInitialAdminIfNotExists();
	}
}
