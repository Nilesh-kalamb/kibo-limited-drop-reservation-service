package com.kibo.drop.config;

import com.kibo.drop.domain.Drop;
import com.kibo.drop.repository.DropRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import java.time.Instant;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seed(DropRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Drop("Limited Sneakers", 100, 100,
                        Instant.now().minusSeconds(10)));
                repository.save(new Drop("Concert Tickets", 50, 50,
                        Instant.now().minusSeconds(10)));
                repository.save(new Drop("VIP Tables", 20, 20,
                        Instant.now().minusSeconds(10)));
            }
        };
    }
}
