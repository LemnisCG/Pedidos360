package cl.duoc.pedidos360.usuario.service;

import cl.duoc.pedidos360.usuario.dto.AuthDtos.*;
import cl.duoc.pedidos360.usuario.model.Usuario;
import cl.duoc.pedidos360.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/** Registro local y login con contraseña cifrada mediante BCrypt. */
@Service
public class AuthService {
    private final UsuarioRepository repository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public AuthService(UsuarioRepository repository, JwtService jwtService) {
        this.repository = repository; this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // Si el correo ya existe por Google/Facebook/Discord/Microsoft, permitimos
        // agregar una contraseña local en vez de devolver un 409 innecesario.
        // Si ya tenía contraseña local, sí es un duplicado real.
        Usuario user = repository.findByEmailIgnoreCase(email).orElse(null);
        if (user != null) {
            if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Ya existe una cuenta con este correo. Inicia sesión o utiliza otro correo electrónico.");
            }

            user.setNombre(request.nombre().trim());
            user.setApellido(request.apellido().trim());
            user.setPasswordHash(encoder.encode(request.password()));
            user = repository.save(user);

            // La cuenta conserva en BD su proveedor social vinculado, pero esta sesión
            // fue autenticada/creada mediante contraseña local.
            return response(user, "local");
        }

        user = new Usuario();
        user.setNombre(request.nombre().trim());
        user.setApellido(request.apellido().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(request.password()));
        user.setProvider("local");
        user = repository.save(user);
        return response(user, "local");
    }

    public AuthResponse login(LoginRequest request) {
        Usuario user = repository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        if (user.getPasswordHash() == null || !encoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        // El claim provider describe cómo se autenticó esta sesión sin modificar
        // el proveedor social que pudiera estar vinculado en la base de datos.
        return response(user, "local");
    }

    public AuthResponse response(Usuario user) {
        return response(user, user.getProvider());
    }

    public AuthResponse response(Usuario user, String authProvider) {
        return new AuthResponse(jwtService.create(user, authProvider), new UserResponse(
                user.getId(), user.getNombre(), user.getApellido(), user.getEmail(), authProvider));
    }
}
