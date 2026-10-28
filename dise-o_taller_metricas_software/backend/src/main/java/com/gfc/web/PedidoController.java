package com.gfc.web;

import com.gfc.application.PedidoService;
import com.gfc.web.dto.CrearPedidoRequest;
import com.gfc.web.dto.ResumenPedidoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Creacion de pedidos; requiere rol CLIENTE (ver SecurityConfig). */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /**
     * {@code POST /api/pedidos} — crea el pedido, descuenta el inventario consumido y devuelve
     * el resumen de pago (201, 401, 404, 409). El cliente se resuelve del JWT, no del body.
     */
    @PostMapping
    public ResponseEntity<ResumenPedidoResponse> crear(@RequestBody CrearPedidoRequest request,
                                                         Authentication authentication) {
        ResumenPedidoResponse resumen = pedidoService.crearPedido(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resumen);
    }

    @GetMapping
    public ResponseEntity<java.util.List<ResumenPedidoResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(pedidoService.listarPedidos(authentication.getName()));
    }
}
