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
class IngredienteCrudApiTest {

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
    void crearIngredienteDevuelve201() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(post("/api/ingredientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ingrediente Nuevo\",\"unidadMedida\":\"UNIDAD\",\"stockActual\":50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombreIngrediente").value("Ingrediente Nuevo"))
                .andExpect(jsonPath("$.unidadMedida").value("UNIDAD"))
                .andExpect(jsonPath("$.stockActual").value(50));
    }

    @Test
    void crearIngredienteDuplicadoDevuelve409() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(post("/api/ingredientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ingrediente Unico\",\"unidadMedida\":\"GRAMO\",\"stockActual\":100}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/ingredientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ingrediente Unico\",\"unidadMedida\":\"GRAMO\",\"stockActual\":50}"))
                .andExpect(status().isConflict());
    }

    @Test
    void crearIngredienteNombreVacioDevuelve400() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(post("/api/ingredientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"unidadMedida\":\"UNIDAD\",\"stockActual\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearIngredienteStockNegativoDevuelve400() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(post("/api/ingredientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Negativo\",\"unidadMedida\":\"UNIDAD\",\"stockActual\":-5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearIngredienteUnidadInvalidaDevuelve400() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(post("/api/ingredientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"BadUnit\",\"unidadMedida\":\"NOEXISTE\",\"stockActual\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearIngredienteSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/ingredientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"SinAuth\",\"unidadMedida\":\"UNIDAD\",\"stockActual\":10}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearIngredienteComoClienteDevuelve403() throws Exception {
        String token = login("cliente", "cliente123");

        mockMvc.perform(post("/api/ingredientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nope\",\"unidadMedida\":\"UNIDAD\",\"stockActual\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarIngredienteDevuelve200() throws Exception {
        String token = login("empleado", "empleado123");

        MvcResult created = mockMvc.perform(post("/api/ingredientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Para Actualizar\",\"unidadMedida\":\"GRAMO\",\"stockActual\":200}"))
                .andExpect(status().isCreated())
                .andReturn();
        String body = created.getResponse().getContentAsString();
        Long id = Long.parseLong(body.substring(body.indexOf("\"id\":") + 5,
                body.indexOf(",", body.indexOf("\"id\":"))));

        mockMvc.perform(put("/api/ingredientes/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Actualizado\",\"unidadMedida\":\"KILOGRAMO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombreIngrediente").value("Actualizado"))
                .andExpect(jsonPath("$.unidadMedida").value("KILOGRAMO"));
    }

    @Test
    void actualizarIngredienteInexistenteDevuelve404() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(put("/api/ingredientes/99999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarIngredienteNombreDuplicadoDevuelve409() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(post("/api/ingredientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Existe A\",\"unidadMedida\":\"UNIDAD\",\"stockActual\":10}"))
                .andExpect(status().isCreated());

        MvcResult created = mockMvc.perform(post("/api/ingredientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Existe B\",\"unidadMedida\":\"UNIDAD\",\"stockActual\":10}"))
                .andExpect(status().isCreated())
                .andReturn();
        String body = created.getResponse().getContentAsString();
        Long id = Long.parseLong(body.substring(body.indexOf("\"id\":") + 5,
                body.indexOf(",", body.indexOf("\"id\":"))));

        mockMvc.perform(put("/api/ingredientes/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Existe A\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminarIngredienteNoUsadoDevuelve204() throws Exception {
        String token = login("empleado", "empleado123");

        MvcResult created = mockMvc.perform(post("/api/ingredientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Para Borrar\",\"unidadMedida\":\"UNIDAD\",\"stockActual\":0}"))
                .andExpect(status().isCreated())
                .andReturn();
        String body = created.getResponse().getContentAsString();
        Long id = Long.parseLong(body.substring(body.indexOf("\"id\":") + 5,
                body.indexOf(",", body.indexOf("\"id\":"))));

        mockMvc.perform(delete("/api/ingredientes/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminarIngredienteEnUsoDevuelve409() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(delete("/api/ingredientes/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminarIngredienteInexistenteDevuelve404() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(delete("/api/ingredientes/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarIngredienteSinTokenDevuelve401() throws Exception {
        mockMvc.perform(delete("/api/ingredientes/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarIngredientesDevuelveArreglo() throws Exception {
        String token = login("empleado", "empleado123");

        mockMvc.perform(get("/api/ingredientes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(19)));
    }
}
