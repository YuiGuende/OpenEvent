package com.group02.openevent.model.event;

import com.group02.openevent.model.enums.CompetitionFormat;
import jakarta.persistence.*;

@Entity
@DiscriminatorValue("COMPETITION")
public class CompetitionEvent  extends Event{

    @Column(name = "competition_type")
    private String competitionType;

    @Column(columnDefinition = "TEXT")
    private String rules;

    @Column(name = "prize_pool")
    private String prizePool;

    @Column(columnDefinition = "TEXT")
    private String eligibility; // điều kiện tham gia

    @Enumerated(EnumType.STRING)
    @Column(name = "format")
    private CompetitionFormat format; // SOLO hoặc TEAM, CLASS, UNIVERSITY

    @Column(columnDefinition = "TEXT")
    private String judgingCriteria; // tiêu chí chấm điểm


    public CompetitionEvent() {
    }


    // Getter & Setter


    public String getEligibility() {
        return eligibility;
    }

    public void setEligibility(String eligibility) {
        this.eligibility = eligibility;
    }

    public CompetitionFormat getFormat() {
        return format;
    }

    public void setFormat(CompetitionFormat format) {
        this.format = format;
    }

    public String getJudgingCriteria() {
        return judgingCriteria;
    }

    public void setJudgingCriteria(String judgingCriteria) {
        this.judgingCriteria = judgingCriteria;
    }

    public String getCompetitionType() {
        return competitionType;
    }

    public void setCompetitionType(String competitionType) {
        this.competitionType = competitionType;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public String getPrizePool() {
        return prizePool;
    }

    public void setPrizePool(String prizePool) {
        this.prizePool = prizePool;
    }

    @Override
    public String toString() {
        return "CompetitionEvent{" +
                ", competitionType='" + competitionType + '\'' +
                ", rules='" + rules + '\'' +
                ", prizePool='" + prizePool + '\'' +
                '}';
    }
}
