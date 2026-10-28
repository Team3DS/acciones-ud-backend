package com.gfc.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gfc.domain.model.Rol;
import com.gfc.domain.model.Usuario;
import com.gfc.domain.port.CodificadorContrasena;
import com.gfc.infrastructure.persistence.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class CuentaBajaApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CodificadorContrasena codificador;

    private String login(String identificador, String contrasena) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"" + identificador + "\",\"contrasena\":\"" + contrasena + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"token\":\"") + 9;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    @Test
    void bajaClienteDevuelve204() throws Exception {
        String nombre = "baja_cli_" + System.nanoTime();
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + nombre + "\",\"contrasena\":\"pass123\","
                                + "\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"" + (System.nanoTime() % 1000000000L)
                                + "\",\"correo\":\"" + nombre + "@test.com\",\"nombre\":\"Test\"}"))
                .andExpect(status().isCreated());

        String token = login(nombre, "pass123");

        mockMvc.perform(post("/api/cuenta/baja")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void bajaEmpleadoDevuelve204() throws Exception {
        String nombre = "baja_emp_" + System.nanoTime();
        usuarioRepository.save(new Usuario(null, nombre, "pass123", Rol.EMPLEADO, codificador));

        String token = login(nombre, "pass123");

        mockMvc.perform(post("/api/cuenta/baja")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void bajaSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/cuenta/baja"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void trasBajaTokenDevuelve401() throws Exception {
        String nombre = "baja_token_" + System.nanoTime();
        usuarioRepository.save(new Usuario(null, nombre, "pass123", Rol.CLIENTE, codificador));

        String token = login(nombre, "pass123");

        mockMvc.perform(post("/api/cuenta/baja")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pedidos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void trasBajaLoginMismasCredencialesDevuelve401() throws Exception {
        String nombre = "baja_login_" + System.nanoTime();
        usuarioRepository.save(new Usuario(null, nombre, "pass123", Rol.EMPLEADO, codificador));

        String token = login(nombre, "pass123");

        mockMvc.perform(post("/api/cuenta/baja")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"" + nombre + "\",\"contrasena\":\"pass123\"}"))
                .andExpect(status().isUnauthorized());
    }
}
