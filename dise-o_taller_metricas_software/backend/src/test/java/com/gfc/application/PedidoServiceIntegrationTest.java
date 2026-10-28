package com.gfc.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gfc.application.exception.StockInsuficienteException;
import com.gfc.domain.model.Cliente;
import com.gfc.domain.model.Comida;
import com.gfc.domain.model.ConfiguracionIngrediente;
import com.gfc.domain.model.Ingrediente;
import com.gfc.domain.model.TipoDocumento;
import com.gfc.domain.model.UnidadMedida;
import com.gfc.domain.port.CodificadorContrasena;
import com.gfc.infrastructure.persistence.ClienteRepository;
import com.gfc.infrastructure.persistence.ComidaRepository;
import com.gfc.infrastructure.persistence.IngredienteRepository;
import com.gfc.infrastructure.persistence.MovimientoInventarioRepository;
import com.gfc.infrastructure.persistence.PedidoRepository;
import com.gfc.web.dto.CrearPedidoRequest;
import com.gfc.web.dto.ResumenPedidoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PedidoServiceIntegrationTest {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ComidaRepository comidaRepository;

    @Autowired
    private IngredienteRepository ingredienteRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Autowired
    private CodificadorContrasena codificador;

    private Cliente cliente;
    private Ingrediente queso;
    private Comida hamburguesa;
    private ConfiguracionIngrediente configQueso;
    private long movimientosIniciales;

    @BeforeEach
    void setUp() {
        movimientosIniciales = movimientoInventarioRepository.count();
        cliente = clienteRepository.save(new Cliente(
                null, "test-cliente", "pass123", TipoDocumento.CC,
                "9999999999", "test@test.com", "Test Cliente", codificador));

        queso = ingredienteRepository.save(new Ingrediente(null, "Queso", 50, UnidadMedida.UNIDAD));
        hamburguesa = new Comida(null, "Hamburguesa", new BigDecimal("18000"), null);
        configQueso = hamburguesa.configurarIngrediente(null, queso, 2, true, true, true,
                new BigDecimal("1500"));
        hamburguesa = comidaRepository.save(hamburguesa);
    }

    @Test
    void crearPedidoConStockSuficienteDescuentaStock() {
        CrearPedidoRequest request = new CrearPedidoRequest(List.of(
                new CrearPedidoRequest.DetalleRequest(hamburguesa.getId(), 1, List.of())));

        ResumenPedidoResponse response = pedidoService.crearPedido("test-cliente", request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals(1, response.detalle().size());

        Ingrediente quesoRecargado = ingredienteRepository.findById(queso.getId()).orElseThrow();
        assertEquals(48, quesoRecargado.getStockActual());

        assertEquals(1, movimientoInventarioRepository.findAll().size() - movimientosIniciales);
    }

    @Test
    void crearPedidoConStockInsuficienteLanzaExcepcion() {
        Ingrediente poquito = ingredienteRepository.save(
                new Ingrediente(null, "Lechuga", 1, UnidadMedida.UNIDAD));
        Comida ensalada = new Comida(null, "Ensalada", new BigDecimal("9000"), null);
        ensalada.configurarIngrediente(null, poquito, 5, true, true, true, new BigDecimal("500"));
        ensalada = comidaRepository.save(ensalada);

        CrearPedidoRequest request = new CrearPedidoRequest(List.of(
                new CrearPedidoRequest.DetalleRequest(ensalada.getId(), 1, List.of())));

        assertThrows(StockInsuficienteException.class,
                () -> pedidoService.crearPedido("test-cliente", request));

        Ingrediente poquitoRecargado = ingredienteRepository.findById(poquito.getId()).orElseThrow();
        assertEquals(1, poquitoRecargado.getStockActual());
        assertEquals(0, movimientoInventarioRepository.count() - movimientosIniciales);
    }

    @Test
    void crearPedidoConPersonalizacionAdicionalDescuentaStockCorrectamente() {
        CrearPedidoRequest request = new CrearPedidoRequest(List.of(
                new CrearPedidoRequest.DetalleRequest(hamburguesa.getId(), 1, List.of(
                        new CrearPedidoRequest.PersonalizacionRequest(
                                configQueso.getId(), com.gfc.domain.model.TipoPersonalizacion.ADICIONAL, 2)))));

        ResumenPedidoResponse response = pedidoService.crearPedido("test-cliente", request);
        assertNotNull(response);

        Ingrediente quesoRecargado = ingredienteRepository.findById(queso.getId()).orElseThrow();
        assertEquals(46, quesoRecargado.getStockActual());
    }

    @Test
    void crearPedidoConPersonalizacionRetiradoDescuentaStockCorrectamente() {
        CrearPedidoRequest request = new CrearPedidoRequest(List.of(
                new CrearPedidoRequest.DetalleRequest(hamburguesa.getId(), 1, List.of(
                        new CrearPedidoRequest.PersonalizacionRequest(
                                configQueso.getId(), com.gfc.domain.model.TipoPersonalizacion.RETIRADO, 1)))));

        ResumenPedidoResponse response = pedidoService.crearPedido("test-cliente", request);
        assertNotNull(response);

        Ingrediente quesoRecargado = ingredienteRepository.findById(queso.getId()).orElseThrow();
        assertEquals(49, quesoRecargado.getStockActual());
    }
}
