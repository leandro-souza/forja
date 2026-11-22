package br.com.forja.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "session_logs")
public class SessionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long protocolId;
    private String workoutCode;
    @Column(name = "trained_on")
    private LocalDate date;
    @Column(name = "week_number")
    private Integer week;
    private Integer backPain;
    private Integer energy;
    private Integer durationMin;
    @Column(columnDefinition = "TEXT")
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProtocolId() { return protocolId; }
    public void setProtocolId(Long protocolId) { this.protocolId = protocolId; }
    public String getWorkoutCode() { return workoutCode; }
    public void setWorkoutCode(String workoutCode) { this.workoutCode = workoutCode; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Integer getWeek() { return week; }
    public void setWeek(Integer week) { this.week = week; }
    public Integer getBackPain() { return backPain; }
    public void setBackPain(Integer backPain) { this.backPain = backPain; }
    public Integer getEnergy() { return energy; }
    public void setEnergy(Integer energy) { this.energy = energy; }
    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
