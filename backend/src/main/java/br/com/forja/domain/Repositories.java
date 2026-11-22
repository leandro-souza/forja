package br.com.forja.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorios Spring Data, agrupados por serem todos triviais. */
public final class Repositories {
    private Repositories() {}

    public interface Users extends JpaRepository<User, Long> {
        Optional<User> findByEmail(String email);
    }

    public interface Tokens extends JpaRepository<AuthToken, Long> {
        Optional<AuthToken> findByToken(String token);
    }

    public interface Profiles extends JpaRepository<Profile, Long> {
        Optional<Profile> findByUserId(Long userId);
    }

    public interface Measurements extends JpaRepository<Measurement, Long> {
        List<Measurement> findByUserIdOrderByDateAsc(Long userId);
    }

    public interface Protocols extends JpaRepository<Protocol, Long> {
        List<Protocol> findByUserIdOrderByNumberAsc(Long userId);
    }

    public interface Sessions extends JpaRepository<SessionLog, Long> {
        List<SessionLog> findByUserIdOrderByDateAscIdAsc(Long userId);
    }

    public interface Sets extends JpaRepository<SetLog, Long> {
        List<SetLog> findByUserIdOrderByIdAsc(Long userId);
        List<SetLog> findBySessionId(Long sessionId);
    }
}
