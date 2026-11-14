package co.edu.udistrital.acciones.m4datosmercado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("co.edu.udistrital.acciones.m4datosmercado.domain")
@EnableJpaRepositories("co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.persistence")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}