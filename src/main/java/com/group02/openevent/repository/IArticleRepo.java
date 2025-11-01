package com.group02.openevent.repository;


import com.group02.openevent.model.department.Article;
import com.group02.openevent.model.department.ArticleStatus;
import com.group02.openevent.model.department.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IArticleRepo extends JpaRepository<Article, Long> {
    
    // Find articles by department
    Page<Article> findByDepartment(Department department, Pageable pageable);
    
    // Find articles by department and status
    Page<Article> findByDepartmentAndStatus(Department department, ArticleStatus status, Pageable pageable);
    
    // Find published articles (for public view)
    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);
    
    // Count articles by department
    long countByDepartment(Department department);
    
    // Count articles by department and status
    long countByDepartmentAndStatus(Department department, ArticleStatus status);
}
