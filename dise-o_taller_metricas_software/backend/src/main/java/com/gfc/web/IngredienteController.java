package com.gfc.web;

import com.gfc.application.IngredienteService;
import com.gfc.application.InventarioService;
import com.gfc.web.dto.IngredienteRequest;
import com.gfc.web.dto.IngredienteResponse;
import com.gfc.web.dto.MovimientoRequest;
import com.gfc.web.dto.MovimientoResultadoResponse;
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
@RequestMapping("/api/ingredientes")
public class IngredienteController {
    private final InventarioService inventarioService;
    private final IngredienteService ingredienteService;

    public IngredienteController(InventarioService inventarioService, IngredienteService ingredienteService) {
        this.inventarioService = inventarioService;
        this.ingredienteService = ingredienteService;
    }

    @GetMapping
    public List<IngredienteResponse> listar() {
        return inventarioService.listar();
    }

    @PostMapping
    public ResponseEntity<IngredienteResponse> crear(@RequestBody IngredienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingredienteService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngredienteResponse> actualizar(@PathVariable Long id,
                                                            @RequestBody IngredienteRequest request) {
        return ResponseEntity.ok(ingredienteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ingredienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/entradas")
    public ResponseEntity<MovimientoResultadoResponse> entrada(@PathVariable Long id,
                                                                 @RequestBody MovimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.registrarEntrada(id, request));
    }

    @PostMapping("/{id}/salidas")
    public ResponseEntity<MovimientoResultadoResponse> salida(@PathVariable Long id,
                                                                @RequestBody MovimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.registrarSalida(id, request));
    }
}
