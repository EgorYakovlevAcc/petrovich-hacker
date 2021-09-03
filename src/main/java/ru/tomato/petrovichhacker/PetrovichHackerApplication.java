package ru.tomato.petrovichhacker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PetrovichHackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetrovichHackerApplication.class, args);
    }

}
