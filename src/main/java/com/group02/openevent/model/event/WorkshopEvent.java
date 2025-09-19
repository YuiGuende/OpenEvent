package com.group02.openevent.model.event;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("WORKSHOP")
public class WorkshopEvent  extends Event{

    private String topic;

    @Column(name = "materials_link")
    private String materialsLink;

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

    @Override
    public String toString() {
        return "WorkshopEvent{" +
                ", topic='" + topic + '\'' +
                ", materialsLink='" + materialsLink + '\'' +
                '}';
    }
}
