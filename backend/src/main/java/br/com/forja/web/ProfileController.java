package br.com.forja.web;

import br.com.forja.domain.Measurement;
import br.com.forja.domain.Profile;
import br.com.forja.domain.Protocol;
import br.com.forja.domain.Repositories;
import br.com.forja.domain.SessionLog;
import br.com.forja.domain.SetLog;
import br.com.forja.service.TrainingService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Avaliacao inicial (anamnese), medidas corporais e copia de seguranca. */
@RestController
@RequestMapping("/api")
public class ProfileController {
    private final Repositories.Profiles profiles;
    private final Repositories.Measurements measurements;
    private final Repositories.Protocols protocols;
    private final Repositories.Sessions sessions;
    private final Repositories.Sets sets;
    private final TrainingService training;

    public ProfileController(Repositories.Profiles profiles, Repositories.Measurements measurements, Repositories.Protocols protocols,
                             Repositories.Sessions sessions, Repositories.Sets sets, TrainingService training) {
        this.profiles = profiles;
        this.measurements = measurements;
        this.protocols = protocols;
        this.sessions = sessions;
        this.sets = sets;
        this.training = training;
    }

    public record ProfileView(Profile profile, boolean hasProtocol) {}

    public record Backup(Profile profile, List<Measurement> measurements, List<Protocol> protocols, List<SessionLog> sessions, List<SetLog> sets) {}

    @GetMapping("/profile")
    public ProfileView get(@RequestAttribute Long userId) {
        return new ProfileView(profiles.findByUserId(userId).orElse(null), training.current(userId).isPresent());
    }

    /** Salva a anamnese. Na primeira vez, registra o peso informado e gera o Protocolo 1. */
    @PutMapping("/profile")
    @Transactional
    public ProfileView save(@RequestAttribute Long userId, @RequestBody Profile in, @RequestParam(defaultValue = "PEITO") String firstFocus) {
        if (in.getHeightCm() == null || in.getWeightKg() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe altura e peso.");
        Profile existing = profiles.findByUserId(userId).orElse(null);
        in.setId(existing == null ? null : existing.getId());
        in.setUserId(userId);
        in.setKcalAdjust(existing == null || existing.getKcalAdjust() == null ? 0 : existing.getKcalAdjust());
        in.setUpdatedAt(Instant.now());
        Profile saved = profiles.save(in);
        if (training.current(userId).isEmpty()) {
            List<Measurement> all = measurements.findByUserIdOrderByDateAsc(userId);
            boolean hasToday = all.stream().anyMatch(m -> m.getDate().equals(LocalDate.now()));
            if (!hasToday) {
                Measurement m = new Measurement();
                m.setUserId(userId);
                m.setDate(LocalDate.now());
                m.setWeightKg(saved.getWeightKg());
                m.setNotes("Avaliação inicial");
                measurements.save(m);
            }
            training.createFirst(userId, saved, firstFocus);
        }
        return new ProfileView(saved, true);
    }

    @GetMapping("/measurements")
    public List<Measurement> list(@RequestAttribute Long userId) {
        return measurements.findByUserIdOrderByDateAsc(userId);
    }

    @PostMapping("/measurements")
    @Transactional
    public Measurement add(@RequestAttribute Long userId, @RequestBody Measurement m) {
        m.setId(null);
        m.setUserId(userId);
        if (m.getDate() == null) m.setDate(LocalDate.now());
        Measurement saved = measurements.save(m);
        // O peso mais recente passa a valer para os calculos da dieta
        measurements.findByUserIdOrderByDateAsc(userId).stream().filter(x -> x.getWeightKg() != null)
                .max(Comparator.comparing(Measurement::getDate)).ifPresent(latest ->
                        profiles.findByUserId(userId).ifPresent(p -> { p.setWeightKg(latest.getWeightKg()); profiles.save(p); }));
        return saved;
    }

    @DeleteMapping("/measurements/{id}")
    public void delete(@RequestAttribute Long userId, @PathVariable Long id) {
        measurements.findById(id).filter(m -> userId.equals(m.getUserId())).ifPresent(measurements::delete);
    }

    @GetMapping("/backup")
    public Backup export(@RequestAttribute Long userId) {
        return new Backup(profiles.findByUserId(userId).orElse(null), measurements.findByUserIdOrderByDateAsc(userId),
                protocols.findByUserIdOrderByNumberAsc(userId), sessions.findByUserIdOrderByDateAscIdAsc(userId), sets.findByUserIdOrderByIdAsc(userId));
    }

    /** Substitui todos os dados do usuario pelo conteudo de uma copia de seguranca. */
    @PostMapping("/backup/restore")
    @Transactional
    public Map<String, Integer> restore(@RequestAttribute Long userId, @RequestBody Backup b) {
        sets.deleteAll(sets.findByUserIdOrderByIdAsc(userId));
        sessions.deleteAll(sessions.findByUserIdOrderByDateAscIdAsc(userId));
        protocols.deleteAll(protocols.findByUserIdOrderByNumberAsc(userId));
        measurements.deleteAll(measurements.findByUserIdOrderByDateAsc(userId));
        profiles.findByUserId(userId).ifPresent(profiles::delete);
        profiles.flush();
        if (b.profile() != null) {
            Profile p = b.profile();
            p.setId(null);
            p.setUserId(userId);
            profiles.save(p);
        }
        if (b.measurements() != null) for (Measurement m : b.measurements()) { m.setId(null); m.setUserId(userId); measurements.save(m); }
        Map<Long, Long> protocolIds = new HashMap<>(), sessionIds = new HashMap<>();
        if (b.protocols() != null) for (Protocol p : b.protocols()) {
            Long old = p.getId();
            p.setId(null);
            p.setUserId(userId);
            protocolIds.put(old, protocols.save(p).getId());
        }
        if (b.sessions() != null) for (SessionLog s : b.sessions()) {
            Long old = s.getId();
            s.setId(null);
            s.setUserId(userId);
            s.setProtocolId(protocolIds.get(s.getProtocolId()));
            sessionIds.put(old, sessions.save(s).getId());
        }
        int n = 0;
        if (b.sets() != null) for (SetLog s : b.sets()) {
            s.setId(null);
            s.setUserId(userId);
            s.setProtocolId(protocolIds.get(s.getProtocolId()));
            s.setSessionId(sessionIds.get(s.getSessionId()));
            sets.save(s);
            n++;
        }
        return Map.of("sessions", sessionIds.size(), "sets", n);
    }
}
