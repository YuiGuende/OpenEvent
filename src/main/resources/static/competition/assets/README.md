# Competition Assets Directory

## 📁 Cấu trúc thư mục
```
/competition/assets/
├── css/                    # CSS files cho competition theme
│   ├── bootstrap.min.css
│   ├── style.css
│   ├── responsive.css
│   ├── competition-main.css
│   └── ...
├── js/                     # JavaScript files
│   ├── bootstrap.min.js
│   ├── jquery.min.js
│   ├── competition-main.js
│   └── ...
├── fonts/                  # Font files
├── img/                    # Images
│   ├── competition-specific/  # Ảnh riêng cho competition
│   ├── gallery/           # Ảnh gallery
│   ├── speaker/           # Ảnh speakers/judges
│   └── team/              # Ảnh team
└── competition-test.html  # File test UI
```

## 🎯 Cách sử dụng

### 1. Test UI trực tiếp
```bash
# Mở file test trong browser
open /Users/quangminhnguyen/OpenEvent/src/main/resources/static/competition/assets/competition-test.html
```

### 2. Sử dụng trong Spring Boot
```java
// Controller endpoint
@GetMapping("/competition/{id}")
public String competitionHome(@PathVariable Long id, Model model) {
    // Add mock data to model
    return "competition/competitionHome";
}
```

### 3. Custom CSS
- File chính: `/competition/assets/css/competition-main.css`
- Theme colors: `/css/competition-theme.css`
- Responsive: `/competition/assets/css/responsive.css`

## 🎨 Competition Theme Colors
- Primary: `#ff6b35` (Orange)
- Secondary: `#004e89` (Blue)
- Accent: `#ffd23f` (Yellow)
- Success: `#06ffa5` (Green)

## 📝 Notes
- Tất cả đường dẫn đã được cập nhật từ `/music/assets/` thành `/competition/assets/`
- File test sử dụng mock data để hiển thị UI
- Responsive design cho mobile và desktop
- Tương thích với Spring Boot + Thymeleaf