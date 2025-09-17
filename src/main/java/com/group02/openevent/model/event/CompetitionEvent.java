package com.group02.openevent.model.event;

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

    public CompetitionEvent() {
    }

    public CompetitionEvent( String competitionType, String rules, String prizePool) {
        this.competitionType = competitionType;
        this.rules = rules;
        this.prizePool = prizePool;
    }
    // Getter & Setter


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
