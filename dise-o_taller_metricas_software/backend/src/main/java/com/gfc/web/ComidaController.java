package com.gfc.web;

import com.gfc.application.ComidaService;
import com.gfc.web.dto.ComidaDetalleResponse;
import com.gfc.web.dto.ComidaResumenResponse;
import com.gfc.web.dto.CrearComidaRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comidas")
public class ComidaController {
    private final ComidaService comidaService;

    public ComidaController(ComidaService comidaService) {
        this.comidaService = comidaService;
    }

    @PostMapping
    public ResponseEntity<ComidaResumenResponse> crear(@RequestBody CrearComidaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comidaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ComidaResumenResponse> actualizar(@PathVariable Long id,
                                                             @RequestBody CrearComidaRequest request) {
        return ResponseEntity.ok(comidaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        comidaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ComidaResumenResponse> listar() {
        return comidaService.listar();
    }

    @GetMapping("/{id}")
    public ComidaDetalleResponse obtener(@PathVariable Long id) {
        return comidaService.obtener(id);
    }
}
