package com.gfc.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ComidaApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listarComidasDevuelveArregloConAlMenosCuatro() throws Exception {
        mockMvc.perform(get("/api/comidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    void obtenerComidaPorIdDevuelveDetalle() throws Exception {
        mockMvc.perform(get("/api/comidas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").isNotEmpty())
                .andExpect(jsonPath("$.precio").isNumber())
                .andExpect(jsonPath("$.ingredientes").isArray())
                .andExpect(jsonPath("$.ingredientes.length()").value(8));
    }

    @Test
    void obtenerComidaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/comidas/999"))
                .andExpect(status().isNotFound());
    }
}
