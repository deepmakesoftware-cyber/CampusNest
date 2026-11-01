package com.campusnest;

import org.springframework.boot.SpringApplication;

public class TestCampusNestApplication {

	public static void main(String[] args) {
		SpringApplication.from(CampusNestApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
