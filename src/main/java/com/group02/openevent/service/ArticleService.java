package com.group02.openevent.service;


import com.group02.openevent.dto.department.ArticleDTO;
import com.group02.openevent.model.department.Article;
import com.group02.openevent.model.department.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

public interface ArticleService {

    ArticleDTO createArticle(ArticleDTO articleDTO, Long departmentAccountId, MultipartFile imageFile);

    ArticleDTO updateArticle(Long articleId, ArticleDTO articleDTO, Long departmentAccountId, MultipartFile imageFile);

    void deleteArticle(Long articleId, Long departmentAccountId);

    ArticleDTO getArticleById(Long articleId);

    ArticleDTO saveArticleWithImage(ArticleDTO articleDTO, MultipartFile imageFile);

    ArticleDTO saveArticle(ArticleDTO articleDTO);

    void deleteArticle(Long articleId);

    ArticleDTO publishArticle(Long articleId, Long departmentAccountId);

    ArticleDTO unpublishArticle(Long articleId, Long departmentAccountId);

    ArticleDTO convertToDTO(Article article);

    Page<ArticleDTO> getArticlesByDepartment(Long departmentAccountId, Pageable pageable);

    Page<ArticleDTO> getArticlesByDepartment(Long departmentAccountId, ArticleStatus status, Pageable pageable);

    Page<ArticleDTO> getArticlesByDepartmentAndStatus(Long departmentAccountId, ArticleStatus status, Pageable pageable);

    Page<ArticleDTO> getPublishedArticles(Pageable pageable);

    ArticleDTO publishArticle(Long articleId);
}
