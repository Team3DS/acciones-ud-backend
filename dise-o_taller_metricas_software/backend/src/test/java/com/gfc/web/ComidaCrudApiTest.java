package com.gfc.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ComidaCrudApiTest {

    @Autowired
    private MockMvc mockMvc;

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
    void crearComidaComoEmpleadoDevuelve201() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(post("/api/comidas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Prueba\",\"precio\":10000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Prueba"))
                .andExpect(jsonPath("$.precio").value(10000))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void crearComidaConIngredientesDevuelve201() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(post("/api/comidas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Con extras\",\"precio\":15000,"
                                + "\"ingredientes\":[{\"ingredienteId\":1,\"cantidadBase\":1,"
                                + "\"incluidoPorDefecto\":true,\"permiteAdicional\":true,"
                                + "\"permiteRetirar\":true,\"precioAdicional\":1000}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Con extras"));
    }

    @Test
    void crearComidaComoClienteDevuelve403() throws Exception {
        String token = login("cliente", "cliente123");

        mockMvc.perform(post("/api/comidas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nope\",\"precio\":5000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearComidaSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/comidas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sin auth\",\"precio\":5000}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearComidaNombreVacioDevuelve400() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(post("/api/comidas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"precio\":10000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearComidaIngredienteInexistenteDevuelve404() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(post("/api/comidas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"precio\":10000,"
                                + "\"ingredientes\":[{\"ingredienteId\":99999,\"cantidadBase\":1,"
                                + "\"incluidoPorDefecto\":true,\"permiteAdicional\":true,"
                                + "\"permiteRetirar\":true,\"precioAdicional\":1000}]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarComidaDevuelve200() throws Exception {
        String token = login("empleado", "empleado123");

        MvcResult created = mockMvc.perform(post("/api/comidas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Original\",\"precio\":10000}"))
                .andExpect(status().isCreated())
                .andReturn();
        String body = created.getResponse().getContentAsString();
        Long id = body.contains("\"id\":") ? Long.parseLong(
                body.substring(body.indexOf("\"id\":") + 5, body.indexOf(",", body.indexOf("\"id\":")))) : 0L;

        mockMvc.perform(put("/api/comidas/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Actualizada\",\"precio\":20000,"
                                + "\"ingredientes\":[{\"ingredienteId\":1,\"cantidadBase\":2,"
                                + "\"incluidoPorDefecto\":true,\"permiteAdicional\":true,"
                                + "\"permiteRetirar\":false,\"precioAdicional\":500}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Actualizada"))
                .andExpect(jsonPath("$.precio").value(20000));
    }

    @Test
    void actualizarComidaInexistenteDevuelve404() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(put("/api/comidas/99999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"precio\":10000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarComidaComoClienteDevuelve403() throws Exception {
        String token = login("cliente", "cliente123");

        mockMvc.perform(put("/api/comidas/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"precio\":10000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void eliminarComidaDevuelve204() throws Exception {
        String token = login("empleado", "empleado123");

        MvcResult created = mockMvc.perform(post("/api/comidas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Para borrar\",\"precio\":5000}"))
                .andExpect(status().isCreated())
                .andReturn();
        String body = created.getResponse().getContentAsString();
        Long id = Long.parseLong(body.substring(body.indexOf("\"id\":") + 5,
                body.indexOf(",", body.indexOf("\"id\":"))));

        mockMvc.perform(delete("/api/comidas/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/comidas/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarComidaReferenciadaEnPedidoDevuelve409() throws Exception {
        String empToken = login("empleado", "empleado123");

        MvcResult created = mockMvc.perform(post("/api/comidas")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Con pedido\",\"precio\":18000,"
                                + "\"ingredientes\":[{\"ingredienteId\":1,\"cantidadBase\":1,"
                                + "\"incluidoPorDefecto\":true,\"permiteAdicional\":false,"
                                + "\"permiteRetirar\":false,\"precioAdicional\":0}]}"))
                .andExpect(status().isCreated())
                .andReturn();
        String body = created.getResponse().getContentAsString();
        Long comidaId = Long.parseLong(body.substring(body.indexOf("\"id\":") + 5,
                body.indexOf(",", body.indexOf("\"id\":"))));

        String cliToken = login("cliente", "cliente123");
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + cliToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"detalles\":[{\"comidaId\":" + comidaId
                                + ",\"cantidad\":1,\"personalizaciones\":[]}]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/comidas/" + comidaId)
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminarComidaSinTokenDevuelve401() throws Exception {
        mockMvc.perform(delete("/api/comidas/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void eliminarComidaInexistenteDevuelve404() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(delete("/api/comidas/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
