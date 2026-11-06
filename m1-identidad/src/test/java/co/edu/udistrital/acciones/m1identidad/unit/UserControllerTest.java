package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in.UserController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserControllerTest {

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController();
    }

    @Test
    void healthReturnsOk() {
        String result = controller.health();

        assertEquals("OK", result);
    }
}