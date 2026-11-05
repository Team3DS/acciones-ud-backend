package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}