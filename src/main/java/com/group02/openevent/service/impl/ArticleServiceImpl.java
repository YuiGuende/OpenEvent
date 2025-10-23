package com.group02.openevent.service.impl;


import com.group02.openevent.dto.department.ArticleDTO;
import com.group02.openevent.model.department.Article;
import com.group02.openevent.model.department.ArticleStatus;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.repository.IArticleRepo;
import com.group02.openevent.repository.IDepartmentRepo;
import com.group02.openevent.service.ArticleService;
import com.group02.openevent.util.CloudinaryUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final IArticleRepo articleRepo;
    private final IDepartmentRepo departmentRepo;
    private final CloudinaryUtil cloudinaryUtil;

    @Override
    public ArticleDTO createArticle(ArticleDTO articleDTO, Long departmentAccountId, MultipartFile imageFile) {
        Department department = departmentRepo.findByAccountId(departmentAccountId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                imageUrl = cloudinaryUtil.uploadFile(imageFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Article article = Article.builder()
                .title(articleDTO.getTitle())
                .content(articleDTO.getContent())
                .imageUrl(imageUrl != null ? imageUrl : articleDTO.getImageUrl())
                .status(articleDTO.getStatus() != null ? articleDTO.getStatus() : ArticleStatus.DRAFT)
                .department(department)
                .build();

        Article savedArticle = articleRepo.save(article);
        return convertToDTO(savedArticle);
    }

    @Override
    public ArticleDTO updateArticle(Long articleId, ArticleDTO articleDTO, Long departmentAccountId, MultipartFile imageFile) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        // Verify that the article belongs to this department
        if (!article.getDepartment().getAccountId().equals(departmentAccountId)) {
            throw new RuntimeException("Unauthorized: Article does not belong to this department");
        }

        article.setTitle(articleDTO.getTitle());
        article.setContent(articleDTO.getContent());

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = null;
            try {
                imageUrl = cloudinaryUtil.uploadFile(imageFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            article.setImageUrl(imageUrl);
        } else if (articleDTO.getImageUrl() != null) {
            article.setImageUrl(articleDTO.getImageUrl());
        }

        if (articleDTO.getStatus() != null) {
            article.setStatus(articleDTO.getStatus());
            if (articleDTO.getStatus() == ArticleStatus.PUBLISHED && article.getPublishedAt() == null) {
                article.setPublishedAt(LocalDateTime.now());
            }
        }

        Article updatedArticle = articleRepo.save(article);
        return convertToDTO(updatedArticle);
    }

    @Override
    public void deleteArticle(Long articleId, Long departmentAccountId) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        // Verify that the article belongs to this department
        if (!article.getDepartment().getAccountId().equals(departmentAccountId)) {
            throw new RuntimeException("Unauthorized: Article does not belong to this department");
        }

        articleRepo.delete(article);
    }

    @Override
    public ArticleDTO getArticleById(Long articleId) {
        return articleRepo.findById(articleId)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Article not found"));
    }

    @Override
    public ArticleDTO saveArticleWithImage(ArticleDTO articleDTO, MultipartFile imageFile) {
        Department department = departmentRepo.findById(articleDTO.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        Article article;
        if (articleDTO.getArticleId() != null) {
            // Update existing article
            article = articleRepo.findById(articleDTO.getArticleId())
                    .orElseThrow(() -> new RuntimeException("Article not found"));
            article.setTitle(articleDTO.getTitle());
            article.setContent(articleDTO.getContent());
        } else {
            // Create new article
            article = Article.builder()
                    .title(articleDTO.getTitle())
                    .content(articleDTO.getContent())
                    .status(articleDTO.getStatus() != null ? articleDTO.getStatus() : ArticleStatus.DRAFT)
                    .department(department)
                    .build();
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = null;
            try {
                imageUrl = cloudinaryUtil.uploadFile(imageFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            article.setImageUrl(imageUrl);
        }

        Article savedArticle = articleRepo.save(article);
        return convertToDTO(savedArticle);
    }

    @Override
    public ArticleDTO saveArticle(ArticleDTO articleDTO) {
        Department department = departmentRepo.findById(articleDTO.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        Article article;
        if (articleDTO.getArticleId() != null) {
            // Update existing article
            article = articleRepo.findById(articleDTO.getArticleId())
                    .orElseThrow(() -> new RuntimeException("Article not found"));
            article.setTitle(articleDTO.getTitle());
            article.setContent(articleDTO.getContent());
            article.setImageUrl(articleDTO.getImageUrl());
        } else {
            // Create new article
            article = Article.builder()
                    .title(articleDTO.getTitle())
                    .content(articleDTO.getContent())
                    .imageUrl(articleDTO.getImageUrl())
                    .status(articleDTO.getStatus() != null ? articleDTO.getStatus() : ArticleStatus.DRAFT)
                    .department(department)
                    .build();
        }

        Article savedArticle = articleRepo.save(article);
        return convertToDTO(savedArticle);
    }

    @Override
    public void deleteArticle(Long articleId) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));
        articleRepo.delete(article);
    }

    @Override
    public ArticleDTO publishArticle(Long articleId) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        article.setStatus(ArticleStatus.PUBLISHED);
        article.setPublishedAt(LocalDateTime.now());

        Article updatedArticle = articleRepo.save(article);
        return convertToDTO(updatedArticle);
    }

    @Override
    public Page<ArticleDTO> getArticlesByDepartment(Long departmentAccountId, Pageable pageable) {
        Department department = departmentRepo.findById(departmentAccountId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        return articleRepo.findByDepartment(department, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<ArticleDTO> getArticlesByDepartment(Long departmentAccountId, ArticleStatus status, Pageable pageable) {
        Department department = departmentRepo.findById(departmentAccountId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        if (status == null) {
            return articleRepo.findByDepartment(department, pageable)
                    .map(this::convertToDTO);
        }

        return articleRepo.findByDepartmentAndStatus(department, status, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<ArticleDTO> getArticlesByDepartmentAndStatus(Long departmentAccountId, ArticleStatus status, Pageable pageable) {
        Department department = departmentRepo.findById(departmentAccountId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        return articleRepo.findByDepartmentAndStatus(department, status, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<ArticleDTO> getPublishedArticles(Pageable pageable) {
        return articleRepo.findByStatus(ArticleStatus.PUBLISHED, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public ArticleDTO publishArticle(Long articleId, Long departmentAccountId) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        if (!article.getDepartment().getAccountId().equals(departmentAccountId)) {
            throw new RuntimeException("Unauthorized: Article does not belong to this department");
        }

        article.setStatus(ArticleStatus.PUBLISHED);
        article.setPublishedAt(LocalDateTime.now());

        Article updatedArticle = articleRepo.save(article);
        return convertToDTO(updatedArticle);
    }

    @Override
    public ArticleDTO unpublishArticle(Long articleId, Long departmentAccountId) {
        Article article = articleRepo.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        if (!article.getDepartment().getAccountId().equals(departmentAccountId)) {
            throw new RuntimeException("Unauthorized: Article does not belong to this department");
        }

        article.setStatus(ArticleStatus.DRAFT);

        Article updatedArticle = articleRepo.save(article);
        return convertToDTO(updatedArticle);
    }

    @Override
    public ArticleDTO convertToDTO(Article article) {
        return ArticleDTO.builder()
                .articleId(article.getArticleId())
                .title(article.getTitle())
                .content(article.getContent())
                .imageUrl(article.getImageUrl())
                .status(article.getStatus())
                .publishedAt(article.getPublishedAt())
                .departmentName(article.getDepartment().getDepartmentName())
                .departmentId(article.getDepartment().getAccountId())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }
}
