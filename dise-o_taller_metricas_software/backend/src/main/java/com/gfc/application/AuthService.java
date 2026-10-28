package com.gfc.application;

import com.gfc.application.exception.ConflictoDatosException;
import com.gfc.application.exception.CredencialesInvalidasException;
import com.gfc.application.exception.RecursoNoEncontradoException;
import com.gfc.domain.model.Cliente;
import com.gfc.domain.model.Usuario;
import com.gfc.domain.port.CodificadorContrasena;
import com.gfc.infrastructure.persistence.ClienteRepository;
import com.gfc.infrastructure.persistence.UsuarioRepository;
import com.gfc.infrastructure.security.jwt.JwtService;
import com.gfc.web.dto.AuthResponse;
import com.gfc.web.dto.LoginRequest;
import com.gfc.web.dto.RegistroRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro y login: valida datos antes de tocar el dominio y emite el JWT de sesion. */
@Service
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final CodificadorContrasena codificador;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                        CodificadorContrasena codificador, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.codificador = codificador;
        this.jwtService = jwtService;
    }

    /**
     * Crea un {@link Cliente} y devuelve el token de sesion (registro con login automatico).
     *
     * @throws ConflictoDatosException si el nombre de usuario, documento o correo ya existen.
     */
    @Transactional
    public AuthResponse registrarCliente(RegistroRequest request) {
        if (usuarioRepository.findByNombreUsuario(request.nombreUsuario()).isPresent()) {
            throw new ConflictoDatosException("El nombre de usuario ya esta en uso");
        }
        if (clienteRepository.existsByDocumento(request.numeroDocumento())) {
            throw new ConflictoDatosException("El documento ya esta registrado");
        }
        if (clienteRepository.existsByEmail(request.correo())) {
            throw new ConflictoDatosException("El correo ya esta registrado");
        }
        Cliente cliente = new Cliente(null, request.nombreUsuario(), request.contrasena(),
                request.tipoDocumento(), request.numeroDocumento(), request.correo(), request.nombre(),
                codificador);
        clienteRepository.save(cliente);
        return respuestaPara(cliente);
    }

    /**
     * Busca al usuario por nombreUsuario y, si no aparece, por correo (solo aplica a clientes).
     *
     * @throws CredencialesInvalidasException si no existe o la contrasena no coincide.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(request.identificador())
                .or(() -> clienteRepository.findByEmail(request.identificador()).map(c -> (Usuario) c))
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario o contrasena incorrectos"));
        if (!usuario.isActivo()) {
            throw new CredencialesInvalidasException("Usuario o contrasena incorrectos");
        }
        if (!usuario.comprobarContraseña(request.contrasena())) {
            throw new CredencialesInvalidasException("Usuario o contrasena incorrectos");
        }
        return respuestaPara(usuario);
    }

    private AuthResponse respuestaPara(Usuario usuario) {
        String token = jwtService.generarToken(usuario);
        if (usuario instanceof Cliente cliente) {
            return new AuthResponse(token, cliente.getNombreUsuario(), cliente.getRol().name(),
                    cliente.getIdCliente(), cliente.getNombre(), cliente.getDocumento(), cliente.getEmail());
        }
        return new AuthResponse(token, usuario.getNombreUsuario(), usuario.getRol().name(),
                null, null, null, null);
    }

    @Transactional
    public void darseDeBaja(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        usuario.darseDeBaja();
        usuarioRepository.save(usuario);
    }
}
