package br.com.forja.web;

import br.com.forja.domain.AuthToken;
import br.com.forja.domain.Measurement;
import br.com.forja.domain.Profile;
import br.com.forja.domain.Repositories;
import br.com.forja.domain.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sistema pessoal: o primeiro cadastro cria o dono da instalacao e, depois dele,
 * novos cadastros ficam bloqueados (a menos que ALLOW_SIGNUP=true).
 */
@RestController
@RequestMapping("/api")
public class AuthController {
    private final Repositories.Users users;
    private final Repositories.Tokens tokens;
    private final Repositories.Profiles profiles;
    private final Repositories.Measurements measurements;
    private final ObjectMapper mapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    @Value("${ALLOW_SIGNUP:false}")
    private boolean allowSignup;

    /** JSON opcional com perfil e medidas iniciais do dono, aplicado no primeiro cadastro. Fica fora do codigo-fonte. */
    @Value("${FORJA_SEED:}")
    private String seed;

    public AuthController(Repositories.Users users, Repositories.Tokens tokens, Repositories.Profiles profiles,
                          Repositories.Measurements measurements, ObjectMapper mapper) {
        this.users = users;
        this.tokens = tokens;
        this.profiles = profiles;
        this.measurements = measurements;
        this.mapper = mapper;
    }

    public record Credentials(String email, String password, String name) {}

    public record Session(String token, String name) {}

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/auth/status")
    public Map<String, Boolean> status() {
        return Map.of("signupOpen", users.count() == 0 || allowSignup);
    }

    @PostMapping("/auth/register")
    @Transactional
    public Session register(@RequestBody Credentials c) {
        boolean first = users.count() == 0;
        if (!first && !allowSignup) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este sistema já tem um dono. Entre com sua conta.");
        if (c.email() == null || !c.email().contains("@")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um e-mail válido.");
        if (c.password() == null || c.password().length() < 8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha precisa ter pelo menos 8 caracteres.");
        String email = c.email().trim().toLowerCase();
        if (users.findByEmail(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
        User u = new User();
        u.setEmail(email);
        u.setName(c.name() == null || c.name().isBlank() ? email : c.name().trim());
        u.setPasswordHash(encoder.encode(c.password()));
        u.setCreatedAt(Instant.now());
        u = users.save(u);
        if (first && seed != null && !seed.isBlank()) applySeed(u.getId());
        return open(u);
    }

    @PostMapping("/auth/login")
    public Session login(@RequestBody Credentials c) {
        User u = users.findByEmail(c.email() == null ? "" : c.email().trim().toLowerCase())
                .filter(x -> c.password() != null && encoder.matches(c.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos."));
        return open(u);
    }

    private Session open(User u) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        AuthToken t = new AuthToken();
        t.setToken(HexFormat.of().formatHex(bytes));
        t.setUserId(u.getId());
        t.setCreatedAt(Instant.now());
        tokens.save(t);
        return new Session(t.getToken(), u.getName());
    }

    private void applySeed(Long userId) {
        try {
            JsonNode root = mapper.readTree(seed);
            if (root.has("profile")) {
                Profile p = mapper.treeToValue(root.get("profile"), Profile.class);
                p.setId(null);
                p.setUserId(userId);
                p.setUpdatedAt(Instant.now());
                profiles.save(p);
            }
            if (root.has("measurements")) {
                for (JsonNode n : root.get("measurements")) {
                    Measurement m = mapper.treeToValue(n, Measurement.class);
                    m.setId(null);
                    m.setUserId(userId);
                    measurements.save(m);
                }
            }
        } catch (Exception e) {
            // Semente invalida nao deve impedir o cadastro
        }
    }
}
