package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m3catalogomercados.application.service.CatalogoService;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Mercado;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/markets", "/api/v1/catalog/markets"})
public class MercadoController {
    private final CatalogoService catalogo;

    public MercadoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    public List<Mercado> listar(@RequestParam(defaultValue = "true") Boolean enabled) {
        return catalogo.mercados(enabled);
    }

    @GetMapping("/{code}")
    public Mercado detalle(@PathVariable String code) {
        return catalogo.mercado(code);
    }
}
