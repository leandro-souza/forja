package br.com.forja.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "profiles")
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private Long userId;
    private String name;
    private LocalDate birthDate;
    private String sex;
    private Double heightCm;
    private Double weightKg;
    private String goal;
    private Integer trainingDays;
    private String trainingStart;
    private String trainingEnd;
    private String jobActivity;
    private Integer monthsOff;
    private Boolean backPain;
    private Boolean kneePain;
    private Boolean machinesOnly;
    private Boolean lactoseIntolerant;
    @Column(columnDefinition = "TEXT")
    private String restrictions;
    @Column(columnDefinition = "TEXT")
    private String surgeries;
    @Column(columnDefinition = "TEXT")
    private String medications;
    @Column(columnDefinition = "TEXT")
    private String conditions;
    private Double sleepHours;
    private String gym;
    private Boolean parqAlert;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private Integer kcalAdjust;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public Integer getTrainingDays() { return trainingDays; }
    public void setTrainingDays(Integer trainingDays) { this.trainingDays = trainingDays; }
    public String getTrainingStart() { return trainingStart; }
    public void setTrainingStart(String trainingStart) { this.trainingStart = trainingStart; }
    public String getTrainingEnd() { return trainingEnd; }
    public void setTrainingEnd(String trainingEnd) { this.trainingEnd = trainingEnd; }
    public String getJobActivity() { return jobActivity; }
    public void setJobActivity(String jobActivity) { this.jobActivity = jobActivity; }
    public Integer getMonthsOff() { return monthsOff; }
    public void setMonthsOff(Integer monthsOff) { this.monthsOff = monthsOff; }
    public Boolean getBackPain() { return backPain; }
    public void setBackPain(Boolean backPain) { this.backPain = backPain; }
    public Boolean getKneePain() { return kneePain; }
    public void setKneePain(Boolean kneePain) { this.kneePain = kneePain; }
    public Boolean getMachinesOnly() { return machinesOnly; }
    public void setMachinesOnly(Boolean machinesOnly) { this.machinesOnly = machinesOnly; }
    public Boolean getLactoseIntolerant() { return lactoseIntolerant; }
    public void setLactoseIntolerant(Boolean lactoseIntolerant) { this.lactoseIntolerant = lactoseIntolerant; }
    public String getRestrictions() { return restrictions; }
    public void setRestrictions(String restrictions) { this.restrictions = restrictions; }
    public String getSurgeries() { return surgeries; }
    public void setSurgeries(String surgeries) { this.surgeries = surgeries; }
    public String getMedications() { return medications; }
    public void setMedications(String medications) { this.medications = medications; }
    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }
    public Double getSleepHours() { return sleepHours; }
    public void setSleepHours(Double sleepHours) { this.sleepHours = sleepHours; }
    public String getGym() { return gym; }
    public void setGym(String gym) { this.gym = gym; }
    public Boolean getParqAlert() { return parqAlert; }
    public void setParqAlert(Boolean parqAlert) { this.parqAlert = parqAlert; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Integer getKcalAdjust() { return kcalAdjust; }
    public void setKcalAdjust(Integer kcalAdjust) { this.kcalAdjust = kcalAdjust; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
