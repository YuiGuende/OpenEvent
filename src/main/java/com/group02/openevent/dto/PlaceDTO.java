package com.group02.openevent.dto;

public class PlaceDTO {
    private String placeName;
    private String building;

    public PlaceDTO(String placeName, String building) {
        this.placeName = placeName;
        this.building = building;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }
}
