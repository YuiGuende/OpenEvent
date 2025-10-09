package com.group02.openevent.model.dto;

public class OrganizationDTO {
    private Long orgId;
    private String orgName;
    private String description;
    private String website;
    private String email;
    private String phone;
    private String address;
    private String imageUrl; // Dùng để hiển thị logo của nhà tổ chức

    // Constructor mặc định
    public OrganizationDTO() {
    }

    // Constructor đầy đủ tham số để dễ dàng mapping từ Entity
    public OrganizationDTO(Long orgId, String orgName, String description, String website, String email, String phone, String address, String imageUrl) {
        this.orgId = orgId;
        this.orgName = orgName;
        this.description = description;
        this.website = website;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.imageUrl = imageUrl;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
