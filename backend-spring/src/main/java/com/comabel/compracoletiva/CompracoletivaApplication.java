package com.comabel.compracoletiva;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CompracoletivaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CompracoletivaApplication.class, args);
	}

}
