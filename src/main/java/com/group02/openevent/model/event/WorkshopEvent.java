package com.group02.openevent.model.event;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("WorkshopEvent")
public class WorkshopEvent  extends Event{

    private String topic;

    @Column(name = "materials_link")
    private String materialsLink;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "skill_level")
    private String skillLevel;

    @Column(name = "prerequisites")
    private String prerequisites;

    public WorkshopEvent() {
    }

    public WorkshopEvent(String topic, String materialsLink) {
        this.topic = topic;
        this.materialsLink = materialsLink;
    }
    // Getter & Setter



    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMaterialsLink() {
        return materialsLink;
    }

    public void setMaterialsLink(String materialsLink) {
        this.materialsLink = materialsLink;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public String getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(String skillLevel) {
        this.skillLevel = skillLevel;
    }

    public String getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(String prerequisites) {
        this.prerequisites = prerequisites;
    }

    @Override
    public String toString() {
        return "WorkshopEvent{" +
                ", topic='" + topic + '\'' +
                ", materialsLink='" + materialsLink + '\'' +
                '}';
    }
}
