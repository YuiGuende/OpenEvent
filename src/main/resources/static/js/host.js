// Host Dashboard - Clean & Organized JavaScript
document.addEventListener('DOMContentLoaded', function() {
    HostDashboard.init();
});

const HostDashboard = {
    // Initialize dashboard
    init() {
        this.mainContent = document.querySelector('.main-content');
        this.setupEventListeners();
        this.setupResponsiveBehavior();
        this.loadInitialPage();
    },

    // Setup all event listeners
    setupEventListeners() {
        this.setupNavigation();
        this.setupHeaderActions();
        this.setupUtilityButtons();
        this.setupSidebarTooltips();
        this.setupMobileMenu();
        this.setupSidebarToggle();
    },

    // Navigation handling

    // Header actions (live status, share, user menu)
    setupHeaderActions() {
        // Live status toggle
        const liveStatus = document.querySelector('.live-status');
        if (liveStatus) {
            liveStatus.addEventListener('click', () => this.toggleLiveStatus());
        }

        // Share button
        const shareBtn = document.querySelector('.btn-share');
        if (shareBtn) {
            shareBtn.addEventListener('click', () => this.shareOrganizerPage());
        }

        // View homepage button
        const viewHomepageBtn = document.querySelector('.btn-view-homepage');
        if (viewHomepageBtn) {
            viewHomepageBtn.addEventListener('click', () => this.viewOrganizerHomepage());
        }

        // User avatar
        const userAvatar = document.querySelector('.user-avatar');
        if (userAvatar) {
            userAvatar.addEventListener('click', () => this.showUserMenu());
        }
    },

    // Event-specific actions

    // Utility buttons (FAB, collapse sidebar)
    setupUtilityButtons() {
        // FAB (Floating Action Button)
        const fab = document.querySelector('.fab');
        if (fab) {
            fab.addEventListener('click', () => this.showHelpModal());
        }

        // Collapse sidebar button
        const collapseBtn = document.querySelector('.collapse-btn');
        if (collapseBtn) {
            collapseBtn.addEventListener('click', () => this.toggleSidebar());
        }
    },

    // Setup tooltips for collapsed sidebar
    setupSidebarTooltips() {
        const navItems = document.querySelectorAll('.nav-item');
        navItems.forEach(item => {
            const span = item.querySelector('span');
            if (span) {
                item.setAttribute('data-tooltip', span.textContent);
            }
        });
    },

    // Setup mobile menu functionality
    setupMobileMenu() {
        const mobileMenuBtn = document.querySelector('.mobile-menu-btn');
        const sidebar = document.querySelector('.sidebar');
        
        if (mobileMenuBtn && sidebar) {
            mobileMenuBtn.addEventListener('click', () => {
                sidebar.classList.toggle('open');
            });

            // Close sidebar when clicking outside on mobile
            document.addEventListener('click', (e) => {
                if (window.innerWidth <= 768 && 
                    !sidebar.contains(e.target) && 
                    !mobileMenuBtn.contains(e.target) && 
                    sidebar.classList.contains('open')) {
                    sidebar.classList.remove('open');
                }
            });
        }
    },

    // Setup sidebar toggle functionality
    setupSidebarToggle() {
        const sidebarToggleBtn = document.querySelector('.sidebar-toggle-btn');
        const sidebar = document.querySelector('.sidebar');
        const mainContent = document.querySelector('.main-content');
        
        if (sidebarToggleBtn && sidebar) {
            sidebarToggleBtn.addEventListener('click', () => {
                if (window.innerWidth > 768) {
                    sidebar.classList.remove('collapsed');
                    document.body.classList.remove('sidebar-collapsed');
                    mainContent.style.marginLeft = '280px';
                    mainContent.style.width = 'calc(100% - 280px)';
                    this.showNotification('Sidebar đã được mở', 'info');
                } else {
                    sidebar.classList.add('open');
                }
            });
        }
    },
    setupNavigation() {
        const navItems = document.querySelectorAll('.nav-item');
        navItems.forEach(item => {
            item.addEventListener('click', () => this.handleNavigation(item));
        });
    },

    // Handle navigation between menu items
    handleNavigation(navItem) {
        // Xóa active hiện tại
        const navItems = document.querySelectorAll('.nav-item');
        navItems.forEach(nav => nav.classList.remove('active'));
        navItem.classList.add('active');

        // Xác định URL
        let url = "";
        let fragmentUrl = "";
        switch (navItem.id) {
            case "nav-host":
                url = "/dashboard";
                fragmentUrl = "/fragment/dashboard";
                break;
            case "nav-events":
                url = "/events";
                fragmentUrl = "/fragment/events";
                break;
            case "nav-settings":
                url = "/settings";
                fragmentUrl = "/fragment/settings";
                break;
        }

        if (fragmentUrl) {
            this.loadFragment(fragmentUrl, url);
        }

        const navText = navItem.querySelector("span")?.textContent || "Open.events";
        this.updatePageTitle(navText);
    },
    attachDynamicEvents() {
        // Xử lý nút Tạo sự kiện
        const createBtn = document.querySelector(".btn-create-event");
        const modal = document.getElementById("createEventModal");
        const closeBtn = modal?.querySelector(".close");

        if (createBtn && modal) {
            createBtn.addEventListener("click", () => {
                modal.style.display = "flex";
            });
        }

        if (closeBtn && modal) {
            closeBtn.addEventListener("click", () => {
                modal.style.display = "none";
            });
        }

        // Click bên ngoài để đóng modal
        window.addEventListener("click", (e) => {
            if (modal && e.target === modal) {
                modal.style.display = "none";
            }
        });
    },

    loadFragment(fragmentUrl, newUrl) {
        this.showLoadingState();
        fetch(fragmentUrl)
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.text();
            })
            .then(html => {
                if (this.mainContent) this.mainContent.innerHTML = html;
                this.attachDynamicEvents();
                if (newUrl) history.pushState(null, "", newUrl);

            })
            .catch(err => {
                console.error("Lỗi load fragment:", err);
                if (this.mainContent)
                    this.mainContent.innerHTML = `<div class="error">Không thể tải nội dung.</div>`;
            })
            .finally(() => this.hideLoadingState());
    },
    loadInitialPage() {
        const currentPath = window.location.pathname;
        let fragmentUrl = "";
        let activeId = "";
        switch (currentPath) {
            case "/":
            case "/dashboard":
                fragmentUrl = "/fragment/dashboard";
                activeId = "nav-host";
                break;
            case "/events":
                fragmentUrl = "/fragment/events";
                activeId = "nav-events";
                break;
            case "/settings":
                fragmentUrl = "/fragment/settings";
                activeId = "nav-settings";
                break;
            default:
                fragmentUrl = "/fragment/dashboard";
                activeId = "nav-host";
        }

        this.loadFragment(fragmentUrl);
        document.querySelectorAll('.nav-item').forEach(nav => nav.classList.remove('active'));
        const activeNav = document.getElementById(activeId);
        if (activeNav) activeNav.classList.add('active');
    },





    //Update page title based on navigation
    updatePageTitle(navText) {
        const pageTitle = document.querySelector('.page-title');
        if (!pageTitle) return;

        const titles = {
            'Bảng điều khiển nhà tổ chức': 'duc le - Bảng điều khiển',
            'Sự kiện': 'duc le - Quản lý sự kiện',
            'Cài đặt': 'duc le - Cài đặt',
            'Thiết kế trang sự kiện': 'duc le - Thiết kế trang sự kiện'
        };

        pageTitle.textContent = titles[navText] || 'duc le - Dashboard';
    },

    // Toggle live status
    toggleLiveStatus() {
        const liveStatus = document.querySelector('.live-status');
        const isLive = liveStatus.textContent.includes('Trực tiếp');

        if (isLive) {
            liveStatus.innerHTML = '<i class="fas fa-eye-slash"></i><span>• Đã tắt - Nhấp để bật</span>';
            liveStatus.style.backgroundColor = 'rgba(220, 53, 69, 0.2)';
            this.showNotification('Đã tắt chế độ trực tiếp', 'info');
        } else {
            liveStatus.innerHTML = '<i class="fas fa-eye"></i><span>• Trực tiếp - Nhấp để gỡ bỏ</span>';
            liveStatus.style.backgroundColor = 'rgba(255, 255, 255, 0.1)';
            this.showNotification('Đã bật chế độ trực tiếp', 'success');
        }
    },


    // Share organizer page
    shareOrganizerPage() {
        const shareUrl = 'https://app.hi.events/organizer/duc-le';

        if (navigator.share) {
            navigator.share({
                title: 'Trang nhà tổ chức - duc le',
                text: 'Xem trang nhà tổ chức của duc le trên hi.events',
                url: shareUrl
            });
        } else {
            navigator.clipboard.writeText(shareUrl).then(() => {
                this.showNotification('Đã sao chép link vào clipboard', 'success');
            }).catch(() => {
                this.showNotification('Không thể sao chép link', 'error');
            });
        }
    },

    // View organizer homepage
    viewOrganizerHomepage() {
        const homepageUrl = 'https://app.hi.events/organizer/duc-le';
        window.open(homepageUrl, '_blank');
    },

    // Show user dropdown menu
    showUserMenu() {
        const dropdown = this.createDropdown([
            { icon: 'fas fa-user', text: 'Hồ sơ' },
            { icon: 'fas fa-cog', text: 'Cài đặt' },
            { icon: 'fas fa-sign-out-alt', text: 'Đăng xuất' }
        ]);

        const userAvatar = document.querySelector('.user-avatar');
        userAvatar.style.position = 'relative';
        userAvatar.appendChild(dropdown);

        this.setupDropdownClose(dropdown, userAvatar);
    },

    // Create dropdown menu
    createDropdown(items) {
        const dropdown = document.createElement('div');
        dropdown.className = 'user-dropdown';
        dropdown.style.cssText = `
            position: absolute;
            top: 100%;
            right: 0;
            background: white;
            border-radius: 10px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.15);
            padding: 0.5rem 0;
            min-width: 150px;
            z-index: 1001;
        `;

        items.forEach(item => {
            const itemEl = document.createElement('div');
            itemEl.className = 'dropdown-item';
            itemEl.innerHTML = `<i class="${item.icon}"></i><span>${item.text}</span>`;
            itemEl.style.cssText = `
                padding: 0.75rem 1rem;
                display: flex;
                align-items: center;
                gap: 0.75rem;
                cursor: pointer;
                transition: background-color 0.3s;
            `;
            itemEl.addEventListener('mouseenter', () => {
                itemEl.style.backgroundColor = '#f8f9fa';
            });
            itemEl.addEventListener('mouseleave', () => {
                itemEl.style.backgroundColor = 'transparent';
            });
            dropdown.appendChild(itemEl);
        });

        return dropdown;
    },

    // Setup dropdown close behavior
    setupDropdownClose(dropdown, parent) {
        setTimeout(() => {
            document.addEventListener('click', function closeDropdown(e) {
                if (!parent.contains(e.target)) {
                    dropdown.remove();
                    document.removeEventListener('click', closeDropdown);
                }
            });
        }, 100);
    },

    // Show help modal
    showHelpModal() {
        const modal = this.createModal('Trợ giúp', `
            <h4>Các tính năng chính:</h4>
            <ul>
                <li><strong>Bảng điều khiển:</strong> Xem tổng quan về sự kiện và doanh số</li>
                <li><strong>Quản lý sự kiện:</strong> Tạo và chỉnh sửa sự kiện</li>
                <li><strong>Cài đặt:</strong> Cấu hình tài khoản và thông tin</li>
                <li><strong>Thiết kế trang:</strong> Tùy chỉnh giao diện trang sự kiện</li>
            </ul>
            <h4>Liên hệ hỗ trợ:</h4>
            <p>Email: support@hi.events<br>Hotline: 1900-xxxx</p>
        `);

        document.body.appendChild(modal);
        this.setupModalClose(modal);
    },


    // Create modal
    createModal(title, content) {
        const modal = document.createElement('div');
        modal.className = 'help-modal';
        modal.innerHTML = `
            <div class="modal-overlay">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>${title}</h3>
                        <button class="close-btn">&times;</button>
                    </div>
                    <div class="modal-body">${content}</div>
                </div>
            </div>
        `;



        return modal;
    },

    // Setup modal close behavior
    setupModalClose(modal) {
        const closeBtn = modal.querySelector('.close-btn');
        const overlay = modal.querySelector('.modal-overlay');

        closeBtn.addEventListener('click', () => modal.remove());
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) modal.remove();
        });
    },

    // Toggle sidebar
    toggleSidebar() {
        const sidebar = document.querySelector('.sidebar');
        const mainContent = document.querySelector('.main-content');

        if (window.innerWidth > 768) {
            // Desktop behavior
            sidebar.classList.toggle('collapsed');
            document.body.classList.toggle('sidebar-collapsed');

            // Xử lý width và margin cho main content
            if (sidebar.classList.contains('collapsed')) {
                mainContent.style.marginLeft = '0';
                mainContent.style.width = '100%';
                this.showNotification('Sidebar đã được thu gọn', 'info');
            } else {
                mainContent.style.marginLeft = '280px';
                mainContent.style.width = 'calc(100% - 280px)';
                this.showNotification('Sidebar đã được mở rộng', 'info');
            }
        } else {
            // Mobile behavior
            sidebar.classList.toggle('open');
        }
    },

    // Load dashboard data
    loadDashboardData() {
        this.showLoadingState();
        setTimeout(() => {
            this.updateMetrics();
            this.hideLoadingState();
        }, 1000);
    },

    // Update metrics with animation
    updateMetrics() {
        const metrics = {
            totalSales: '$1,250.00',
            productsSold: 45,
            attendees: 120,
            totalOrders: 23,
            totalTax: '$125.00',
            totalFees: '$62.50'
        };

        const selectors = [
            '.metric-card:nth-child(1) .metric-value',
            '.metric-card:nth-child(2) .metric-value',
            '.metric-card:nth-child(3) .metric-value',
            '.metric-card:nth-child(4) .metric-value',
            '.metric-card:nth-child(5) .metric-value',
            '.metric-card:nth-child(6) .metric-value'
        ];

        const values = Object.values(metrics);

        selectors.forEach((selector, index) => {
            this.animateMetricValue(selector, values[index]);
        });
    },

    // Animate metric value (robust for both string and number inputs)
    animateMetricValue(selector, newValue) {
        const element = document.querySelector(selector);
        if (!element) return;

        const startValue = element.textContent;
        const parseNumber = (val) => {
            if (val == null) return 0; // null hoặc undefined

            if (typeof val === 'number') return val;

            // Ép về string để tránh lỗi
            const cleaned = String(val).replace(/[$,\s]/g, '');
            const parsed = parseFloat(cleaned);

            return isNaN(parsed) ? 0 : parsed;
        };

        const isNumeric = !isNaN(parseNumber(startValue));

        if (isNumeric) {
            const start = parseNumber(startValue);
            const end = parseNumber(newValue);
            const duration = 1000;
            const startTime = performance.now();

            const animate = (currentTime) => {
                const elapsed = currentTime - startTime;
                const progress = Math.min(elapsed / duration, 1);

                const current = start + (end - start) * progress;
                const newValueIsCurrency = (typeof newValue === 'string') && newValue.includes('$');
                element.textContent = newValueIsCurrency ? 
                    `$${current.toFixed(2)}` : 
                    Math.round(current).toString();

                if (progress < 1) {
                    requestAnimationFrame(animate);
                }
            };

            requestAnimationFrame(animate);
        } else {
            element.textContent = String(newValue);
        }
    },

    // Show/hide loading states
    showLoadingState() {
        if (this.mainContent) {
            this.mainContent.classList.add('loading');
        }
    },

    hideLoadingState() {
        if (this.mainContent) {
            this.mainContent.classList.remove('loading');
        }
    },

    // Setup responsive behavior
    setupResponsiveBehavior() {
        window.addEventListener('resize', () => {
            const sidebar = document.querySelector('.sidebar');
            const mainContent = document.querySelector('.main-content');

            if (window.innerWidth <= 768) {
                // Mobile behavior
                sidebar.classList.remove('collapsed', 'open');
                sidebar.style.transform = 'translateX(-100%)';
                mainContent.style.marginLeft = '0';
                mainContent.style.width = '100%';
                document.body.classList.remove('sidebar-collapsed');
            } else {
                // Desktop behavior
                sidebar.classList.remove('open');
                sidebar.style.transform = 'translateX(0)';
                
                if (sidebar.classList.contains('collapsed')) {
                    mainContent.style.marginLeft = '0';
                    mainContent.style.width = '100%';
                } else {
                    mainContent.style.marginLeft = '280px';
                    mainContent.style.width = 'calc(100% - 280px)';
                }
            }
        });
    },

    // Show notification
    showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;

        const colors = {
            success: '#28a745',
            error: '#dc3545',
            info: '#6f42c1'
        };

        notification.style.cssText = `
            position: fixed;
            top: 90px;
            right: 20px;
            background: ${colors[type]};
            color: white;
            padding: 1rem 1.5rem;
            border-radius: 10px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.15);
            z-index: 2000;
            animation: slideIn 0.3s ease-out;
            font-weight: 500;
        `;

        // Add animation
        if (!document.querySelector('#notification-style')) {
            const style = document.createElement('style');
            style.id = 'notification-style';
            style.textContent = `
                @keyframes slideIn {
                    from {
                        transform: translateX(100%);
                        opacity: 0;
                    }
                    to {
                        transform: translateX(0);
                        opacity: 1;
                    }
                }
            `;
            document.head.appendChild(style);
        }

        document.body.appendChild(notification);

        // Auto remove after 3 seconds
        setTimeout(() => {
            notification.style.animation = 'slideIn 0.3s ease-out reverse';
            setTimeout(() => notification.remove(), 300);
        }, 3000);
    },

    // Utility functions
    formatCurrency(amount) {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(amount);
    },

    formatDate(date) {
        return new Intl.DateTimeFormat('vi-VN', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        }).format(date);
    }
};


// Export for external use
window.HostDashboard = HostDashboard;