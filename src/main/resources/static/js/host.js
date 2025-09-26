// Host Dashboard - Clean & Organized JavaScript
document.addEventListener('DOMContentLoaded', function() {
    HostDashboard.init();
});

const HostDashboard = {
    // Initialize dashboard
    init() {
        this.setupEventListeners();
        this.loadDashboardData();
        this.setupResponsiveBehavior();
    },

    // Setup all event listeners
    setupEventListeners() {
        this.setupNavigation();
        this.setupHeaderActions();
        this.setupEventActions();
        this.setupUtilityButtons();
        this.setupSidebarTooltips();
        this.setupMobileMenu();
        this.setupSidebarToggle();
    },

    // Navigation handling
    setupNavigation() {
        const navItems = document.querySelectorAll('.nav-item');
        navItems.forEach(item => {
            item.addEventListener('click', () => this.handleNavigation(item));
        });

        // Route bindings
        const navEvents = document.getElementById('nav-events');
        if (navEvents) navEvents.addEventListener('click', () => {
            window.location.href = '/events';
        });
        const navHost = document.getElementById('nav-host');
        if (navHost) navHost.addEventListener('click', () => {
            window.location.href = '/host';
        });
    },

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
    setupEventActions() {
        const actionBtns = document.querySelectorAll('.action-btn');
        actionBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.showEventActions(btn);
            });
        });

        // Click on event card navigates to events page
        const eventCards = document.querySelectorAll('.event-card');
        eventCards.forEach(card => {
            card.addEventListener('click', () => {
                window.location.href = '/events';
            });
        });
    },

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
                sidebar.classList.remove('collapsed');
                document.body.classList.remove('sidebar-collapsed');
                mainContent.style.marginLeft = '280px';
                mainContent.style.width = 'calc(100% - 280px)';
                this.showNotification('Sidebar đã được mở', 'info');
            });
        }
    },

    // Handle navigation between menu items
    handleNavigation(navItem) {
        const navItems = document.querySelectorAll('.nav-item');
        navItems.forEach(nav => nav.classList.remove('active'));
        navItem.classList.add('active');

        const navText = navItem.querySelector('span').textContent;
        this.updatePageTitle(navText);
        this.showLoadingState();

        // Simulate navigation delay
        setTimeout(() => this.hideLoadingState(), 500);
    },

    // Update page title based on navigation
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

    // Show event actions
    showEventActions(button) {
        const eventCard = button.closest('.event-card');
        const eventTitle = eventCard.querySelector('.event-title').textContent;
        this.showNotification(`Thao tác với sự kiện: ${eventTitle}`, 'info');
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

        // Add modal styles
        const style = document.createElement('style');
        style.textContent = `
            .help-modal {
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                z-index: 2000;
            }
            .modal-overlay {
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: rgba(0,0,0,0.5);
                display: flex;
                align-items: center;
                justify-content: center;
            }
            .modal-content {
                background: white;
                border-radius: 15px;
                max-width: 500px;
                width: 90%;
                max-height: 80vh;
                overflow-y: auto;
                box-shadow: 0 10px 30px rgba(0,0,0,0.3);
            }
            .modal-header {
                padding: 1.5rem;
                border-bottom: 1px solid #eee;
                display: flex;
                justify-content: space-between;
                align-items: center;
            }
            .modal-header h3 {
                margin: 0;
                color: #333;
                font-weight: 600;
            }
            .close-btn {
                background: none;
                border: none;
                font-size: 1.5rem;
                cursor: pointer;
                color: #666;
                transition: color 0.3s;
            }
            .close-btn:hover {
                color: #333;
            }
            .modal-body {
                padding: 1.5rem;
            }
            .modal-body h4 {
                color: #333;
                margin-bottom: 0.5rem;
                font-weight: 600;
            }
            .modal-body ul {
                margin-bottom: 1.5rem;
            }
            .modal-body li {
                margin-bottom: 0.5rem;
            }
        `;
        document.head.appendChild(style);

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
        const mainContent = document.querySelector('.main-content');
        mainContent.classList.add('loading');
    },

    hideLoadingState() {
        const mainContent = document.querySelector('.main-content');
        mainContent.classList.remove('loading');
    },

    // Setup responsive behavior
    setupResponsiveBehavior() {
        window.addEventListener('resize', () => {
            const sidebar = document.querySelector('.sidebar');
            const mainContent = document.querySelector('.main-content');

            if (window.innerWidth <= 768) {
                sidebar.classList.remove('open');
                sidebar.style.transform = 'translateX(-100%)';
                mainContent.style.marginLeft = '0';
                mainContent.style.width = '100%';
            } else {
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