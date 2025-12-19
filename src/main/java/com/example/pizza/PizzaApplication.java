package com.example.pizza;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PizzaApplication {

	public static void main(String[] args) {
		// Burada server.address parametresini açıkça belirliyoruz
		System.setProperty("server.address", "0.0.0.0");
		SpringApplication.run(PizzaApplication.class, args);
	}
	
}