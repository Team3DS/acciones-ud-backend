package com.gfc.infrastructure.security.jwt;

import com.gfc.domain.model.Usuario;
import com.gfc.infrastructure.persistence.UsuarioRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Adapta Usuario a UserDetails; la contrasena real se valida con Usuario.comprobarContraseña,
 * no con el PasswordEncoder de Spring Security. */
@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new UsernameNotFoundException(nombreUsuario));
        if (!usuario.isActivo()) {
            throw new UsernameNotFoundException(nombreUsuario);
        }
        return User.builder()
                .username(usuario.getNombreUsuario())
                .password("N/A")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())))
                .build();
    }
}
