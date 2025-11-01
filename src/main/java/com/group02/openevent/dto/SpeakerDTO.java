package com.group02.openevent.dto;

public class SpeakerDTO {
    private String name;
    private String role;     // lấy từ defaultRole
    private String imageUrl;
    private String profile;

    // Default constructor
    public SpeakerDTO() {
    }

    public SpeakerDTO(String name, String role, String imageUrl, String profile) {
        this.name = name;
        this.role = role;
        this.imageUrl = imageUrl;
        this.profile = profile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
