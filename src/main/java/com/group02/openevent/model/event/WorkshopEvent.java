package com.group02.openevent.model.event;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("WORKSHOP")
public class WorkshopEvent  extends Event{


    @Column(nullable = false)
    private String speaker;

    private String topic;

    @Column(name = "materials_link")
    private String materialsLink;

    public WorkshopEvent() {
    }

    public WorkshopEvent( String speaker, String topic, String materialsLink) {
        this.speaker = speaker;
        this.topic = topic;
        this.materialsLink = materialsLink;
    }
    // Getter & Setter




    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

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

    @Override
    public String toString() {
        return "WorkshopEvent{" +
                ", speaker='" + speaker + '\'' +
                ", topic='" + topic + '\'' +
                ", materialsLink='" + materialsLink + '\'' +
                '}';
    }
}
