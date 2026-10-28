package com.gfc.infrastructure.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gfc.domain.model.Rol;
import com.gfc.domain.model.TipoDocumento;
import com.gfc.infrastructure.persistence.ClienteRepository;
import com.gfc.infrastructure.persistence.ComidaRepository;
import com.gfc.infrastructure.persistence.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DatosDemoSeederTest {

    @Autowired
    private DatosDemoSeeder seeder;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ComidaRepository comidaRepository;

    @Test
    void semillaEmpleadosCreados() {
        var empleado = usuarioRepository.findByNombreUsuario("empleado");
        assertTrue(empleado.isPresent());
        assertEquals(Rol.EMPLEADO, empleado.get().getRol());
    }

    @Test
    void semillaClienteCreado() {
        var cliente = clienteRepository.findByNombreUsuario("cliente");
        assertTrue(cliente.isPresent());
        assertEquals(Rol.CLIENTE, cliente.get().getRol());
        assertEquals(TipoDocumento.CC, cliente.get().getTipoDocumento());
    }

    @Test
    void semillaEmpresaCreada() {
        var empresa = clienteRepository.findByNombreUsuario("empresa");
        assertTrue(empresa.isPresent());
        assertEquals(Rol.CLIENTE, empresa.get().getRol());
        assertEquals(TipoDocumento.NIT, empresa.get().getTipoDocumento());
    }

    @Test
    void catalogoContieneDoceComidas() {
        assertTrue(comidaRepository.count() >= 12);
    }

    @Test
    void semillaEsIdempotente() {
        long usuariosAntes = usuarioRepository.count();
        long comidasAntes = comidaRepository.count();

        seeder.run();

        assertEquals(usuariosAntes, usuarioRepository.count());
        assertEquals(comidasAntes, comidaRepository.count());
    }
}
