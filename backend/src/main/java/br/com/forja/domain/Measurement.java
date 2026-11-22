package br.com.forja.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "measurements")
public class Measurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    @Column(name = "measured_on")
    private LocalDate date;
    private Double weightKg;
    private Double bodyFatPct;
    private Double chest;
    private Double shoulders;
    private Double arm;
    private Double waist;
    private Double hip;
    private Double thigh;
    private Double calf;
    @Column(columnDefinition = "TEXT")
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public Double getBodyFatPct() { return bodyFatPct; }
    public void setBodyFatPct(Double bodyFatPct) { this.bodyFatPct = bodyFatPct; }
    public Double getChest() { return chest; }
    public void setChest(Double chest) { this.chest = chest; }
    public Double getShoulders() { return shoulders; }
    public void setShoulders(Double shoulders) { this.shoulders = shoulders; }
    public Double getArm() { return arm; }
    public void setArm(Double arm) { this.arm = arm; }
    public Double getWaist() { return waist; }
    public void setWaist(Double waist) { this.waist = waist; }
    public Double getHip() { return hip; }
    public void setHip(Double hip) { this.hip = hip; }
    public Double getThigh() { return thigh; }
    public void setThigh(Double thigh) { this.thigh = thigh; }
    public Double getCalf() { return calf; }
    public void setCalf(Double calf) { this.calf = calf; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
