package com.group02.openevent.dto.department;

import com.group02.openevent.model.department.ArticleStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleDTO {
    
    private Long articleId;
    private String title;
    private String content;
    private String imageUrl;
    private ArticleStatus status;
    private LocalDateTime publishedAt;
    private String departmentName;
    private Long departmentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields for display
    public String getFormattedPublishedDate() {
        if (publishedAt == null) return "Chưa xuất bản";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return publishedAt.format(formatter);
    }
    
    public String getFormattedCreatedDate() {
        if (createdAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return createdAt.format(formatter);
    }
    
    public String getStatusLabel() {
        return status == ArticleStatus.PUBLISHED ? "Đã xuất bản" : "Bản nháp";
    }
    
    public String getStatusBadgeClass() {
        return status == ArticleStatus.PUBLISHED ? "badge-success" : "badge-secondary";
    }
    
    public String getShortContent() {
        if (content == null) return "";
        return content.length() > 150 ? content.substring(0, 150) + "..." : content;
    }
}
