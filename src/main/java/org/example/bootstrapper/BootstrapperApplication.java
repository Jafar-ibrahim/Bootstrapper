package org.example.bootstrapper;
import lombok.extern.log4j.Log4j2;
import org.example.bootstrapper.services.network.NetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@Log4j2
@SpringBootApplication
public class BootstrapperApplication implements CommandLineRunner {
	@Autowired
	private NetworkService networkService;

	public static void main(String[] args) {
		SpringApplication.run(BootstrapperApplication.class, args);
	}

	@Override
	public void run(String... args){
		log.info("Bootstrapper is up and running.....");
		networkService.run();
	}
}
