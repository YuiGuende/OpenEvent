package com.group02.openevent.model.event;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("FESTIVAL")
public class FestivalEvent extends Event {

    private String culture;

    @Column(columnDefinition = "TEXT")
    private String highlight;

    @Column(name = "festival_theme")
    private String festivalTheme; // Cultural, Sports, Arts, Technology, etc.

    @Column(name = "number_of_activities")
    private Integer numberOfActivities; // Number of activities/booths

    @Column(name = "target_audience")
    private String targetAudience; // Students, Faculty, Alumni, Public

    @Column(name = "registration_required")
    private Boolean registrationRequired;

    @Column(name = "festival_type")
    private String festivalType; // Annual, Special Event, Competition

    @Column(name = "main_organizer")
    private String mainOrganizer; // Department or club organizing

    @Column(name = "expected_attendees")
    private Integer expectedAttendees;
    

    public FestivalEvent() {
    }

    public FestivalEvent(String culture, String highlight, String festivalTheme,
                         Integer numberOfActivities, String targetAudience,
                         Boolean registrationRequired, String festivalType,
                         String mainOrganizer, Integer expectedAttendees) {
        this.culture = culture;
        this.highlight = highlight;
        this.festivalTheme = festivalTheme;
        this.numberOfActivities = numberOfActivities;
        this.targetAudience = targetAudience;
        this.registrationRequired = registrationRequired;
        this.festivalType = festivalType;
        this.mainOrganizer = mainOrganizer;
        this.expectedAttendees = expectedAttendees;
    }

    // Getters & Setters
    public String getCulture() {
        return culture;
    }

    public void setCulture(String culture) {
        this.culture = culture;
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    public String getFestivalTheme() {
        return festivalTheme;
    }

    public void setFestivalTheme(String festivalTheme) {
        this.festivalTheme = festivalTheme;
    }

    public Integer getNumberOfActivities() {
        return numberOfActivities;
    }

    public void setNumberOfActivities(Integer numberOfActivities) {
        this.numberOfActivities = numberOfActivities;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public Boolean getRegistrationRequired() {
        return registrationRequired;
    }

    public void setRegistrationRequired(Boolean registrationRequired) {
        this.registrationRequired = registrationRequired;
    }

    public String getFestivalType() {
        return festivalType;
    }

    public void setFestivalType(String festivalType) {
        this.festivalType = festivalType;
    }

    public String getMainOrganizer() {
        return mainOrganizer;
    }

    public void setMainOrganizer(String mainOrganizer) {
        this.mainOrganizer = mainOrganizer;
    }

    public Integer getExpectedAttendees() {
        return expectedAttendees;
    }

    public void setExpectedAttendees(Integer expectedAttendees) {
        this.expectedAttendees = expectedAttendees;
    }

    @Override
    public String toString() {
        return "FestivalEvent{" +
                "culture='" + culture + '\'' +
                ", highlight='" + highlight + '\'' +
                ", festivalTheme='" + festivalTheme + '\'' +
                ", numberOfActivities=" + numberOfActivities +
                ", targetAudience='" + targetAudience + '\'' +
                ", registrationRequired=" + registrationRequired +
                ", festivalType='" + festivalType + '\'' +
                ", mainOrganizer='" + mainOrganizer + '\'' +
                ", expectedAttendees=" + expectedAttendees +
                '}';
    }
}
