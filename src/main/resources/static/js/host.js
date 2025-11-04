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
        
        // Re-setup navigation after a delay to ensure DOM is ready
        setTimeout(() => {
            this.setupNavigation();
        }, 500);
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
        console.log('Setting up navigation for', navItems.length, 'items');
        navItems.forEach(item => {
            // Check if already has listener by checking data attribute
            if (item.dataset.navListener === 'true') {
                return; // Skip if already has listener
            }
            
            // Mark as having listener
            item.dataset.navListener = 'true';
            
            // Add event listener
            item.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                console.log('Nav item clicked:', item.id, item);
                this.handleNavigation(item);
            });
        });
    },

    // Handle navigation between menu items
    handleNavigation(navItem) {
        console.log('Navigation clicked:', navItem.id, navItem);
        
        // Xóa active hiện tại
        const navItems = document.querySelectorAll('.nav-item');
        navItems.forEach(nav => nav.classList.remove('active'));
        navItem.classList.add('active');

        // Xác định URL
        let url = "";
        let fragmentUrl = "";
        const navId = navItem.id;
        
        switch (navId) {
            case "nav-host":
                url = "/dashboard";
                fragmentUrl = "/fragment/dashboard";
                break;
            case "nav-events":
                url = "/events";
                fragmentUrl = "/fragment/events";
                break;
            case "nav-wallet":
                url = "/wallet";
                fragmentUrl = "/fragment/wallet";
                break;
            case "nav-settings":
                url = "/settings";
                fragmentUrl = "/fragment/settings";
                break;
            default:
                console.warn('Unknown navigation item:', navId);
                // Try to find wallet by checking if it has wallet icon or text
                const navText = navItem.querySelector("span")?.textContent?.toLowerCase() || "";
                if (navText.includes("ví") || navText.includes("wallet")) {
                    url = "/wallet";
                    fragmentUrl = "/fragment/wallet";
                }
                break;
        }

        console.log('Navigating to:', url, fragmentUrl);

        if (fragmentUrl) {
            this.loadFragment(fragmentUrl, url);
        } else {
            console.error('No fragment URL found for navigation item:', navId);
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
        console.log('Loading fragment:', fragmentUrl, 'URL:', newUrl);
        this.showLoadingState();
        fetch(fragmentUrl)
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.text();
            })
            .then(html => {
                console.log('Raw HTML received, length:', html.length);
                console.log('HTML preview (first 500 chars):', html.substring(0, 500));
                if (this.mainContent) {
                    // Use a temporary container to extract scripts
                    const tempDiv = document.createElement('div');
                    tempDiv.innerHTML = html;
                    
                    // Get all scripts from the fragment
                    const scripts = tempDiv.querySelectorAll('script');
                    const scriptsArray = Array.from(scripts);
                    console.log('Found', scriptsArray.length, 'scripts in fragment');
                    
                    // Also check for script tags in raw HTML
                    const scriptTagMatches = html.match(/<script[^>]*>[\s\S]*?<\/script>/gi);
                    console.log('Script tags found in raw HTML (regex):', scriptTagMatches ? scriptTagMatches.length : 0);
                    
                    let scriptsExecuted = false;
                    // If querySelector didn't find scripts but regex did, extract from regex
                    if (scriptsArray.length === 0 && scriptTagMatches && scriptTagMatches.length > 0) {
                        console.log('Scripts not found by querySelector, but found by regex. Extracting from regex...');
                        scriptTagMatches.forEach((scriptTag, index) => {
                            // Extract script content using regex
                            const contentMatch = scriptTag.match(/<script[^>]*>([\s\S]*?)<\/script>/i);
                            if (contentMatch && contentMatch[1]) {
                                const scriptContent = contentMatch[1];
                                console.log(`Executing script ${index + 1} extracted from regex, length:`, scriptContent.length);
                                try {
                                    (function() {
                                        eval(scriptContent);
                                    })();
                                    console.log(`Script ${index + 1} executed successfully from regex extraction`);
                                    scriptsExecuted = true;
                                } catch (e) {
                                    console.error(`Error executing script ${index + 1} from regex:`, e);
                                }
                            }
                        });
                    }
                    
                    // Remove scripts from HTML
                    scriptsArray.forEach(script => script.remove());
                    
                    // Insert HTML without scripts first (remove script tags if not already removed)
                    if (scriptsExecuted) {
                        // Scripts were executed from regex, so remove them from HTML
                        this.mainContent.innerHTML = html.replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '');
                    } else {
                        // Scripts will be executed below, so just use tempDiv HTML
                        this.mainContent.innerHTML = tempDiv.innerHTML;
                    }
                    console.log('HTML inserted, now executing scripts...');
                    
                    // Execute scripts after HTML is inserted (only if not already executed from regex)
                    if (!scriptsExecuted && scriptsArray.length > 0) {
                        scriptsArray.forEach((oldScript, index) => {
                            console.log(`Executing script ${index + 1}/${scriptsArray.length}`);
                            const newScript = document.createElement('script');
                            // Copy script attributes (except type if it's text/javascript)
                            Array.from(oldScript.attributes).forEach(attr => {
                                if (attr.name !== 'src' && attr.name !== 'type') {
                                    newScript.setAttribute(attr.name, attr.value);
                                }
                            });
                            // Copy script content using textContent for better execution
                            if (oldScript.src) {
                                newScript.src = oldScript.src;
                                newScript.async = false;
                            } else if (oldScript.innerHTML || oldScript.textContent) {
                                // Use textContent to ensure script executes
                                const scriptContent = oldScript.innerHTML || oldScript.textContent;
                                console.log('Script content length:', scriptContent.length);
                                // Create a function wrapper and execute immediately
                                try {
                                    // Execute script content directly using eval
                                    // This ensures the script runs in the current scope
                                    (function() {
                                        const scriptCode = scriptContent;
                                        eval(scriptCode);
                                    })();
                                    console.log(`Script ${index + 1} executed successfully`);
                                } catch (e) {
                                    console.error(`Error executing script ${index + 1}:`, e);
                                    console.error('Script content preview:', scriptContent.substring(0, 200));
                                    // Fallback: try appending script element
                                    newScript.textContent = scriptContent;
                                    document.head.appendChild(newScript);
                                }
                                return; // Skip appending since we already executed
                            }
                            // Only append if it's an external script
                            if (newScript.src) {
                                document.head.appendChild(newScript);
                            }
                        });
                    } // Close if (!scriptsExecuted && scriptsArray.length > 0)
                    
                    // Trigger wallet initialization if wallet page is loaded
                    if (fragmentUrl.includes('/wallet')) {
                        setTimeout(() => {
                            console.log('Wallet fragment detected, checking for initWallet...');
                            console.log('window.initWallet type:', typeof window.initWallet);
                            console.log('window.loadWalletData type:', typeof window.loadWalletData);
                            console.log('window.loadTransactionHistory type:', typeof window.loadTransactionHistory);
                            if (typeof window.initWallet === 'function') {
                                console.log('Calling window.initWallet()');
                                window.initWallet();
                            } else if (typeof initWallet === 'function') {
                                console.log('Calling initWallet() (non-window)');
                                initWallet();
                            } else {
                                // If initWallet not available, try to load wallet data directly
                                console.log('initWallet not found, trying alternative initialization');
                                if (typeof window.loadWalletData === 'function') {
                                    console.log('Calling window.loadWalletData()');
                                    window.loadWalletData();
                                }
                                if (typeof window.loadTransactionHistory === 'function') {
                                    console.log('Calling window.loadTransactionHistory()');
                                    window.loadTransactionHistory();
                                }
                            }
                        }, 500); // Increased timeout to ensure scripts are executed
                    }
                }
                this.attachDynamicEvents();
                // Update URL in browser
                if (newUrl) {
                    window.history.pushState(null, "", newUrl);
                    console.log('URL updated to:', newUrl);
                }

                // Initialize events page if it's the events fragment
                if (fragmentUrl.includes('/fragment/events') || fragmentUrl.includes('/events')) {
                    setTimeout(() => {
                        if (typeof window.initializeEventsPage === 'function') {
                            console.log('Calling initializeEventsPage after loading events fragment');
                            window.initializeEventsPage();
                        } else {
                            console.warn('initializeEventsPage function not found');
                        }
                    }, 100);
                }

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
        console.log('Initial page load, current path:', currentPath);
        let fragmentUrl = "";
        let activeId = "";
        switch (currentPath) {
            case "/":
            case "/dashboard":
            case "/organizer":
                fragmentUrl = "/fragment/dashboard";
                activeId = "nav-host";
                break;
            case "/events":
                fragmentUrl = "/fragment/events";
                activeId = "nav-events";
                // Initialize events page after loading
                setTimeout(() => {
                    if (typeof window.initializeEventsPage === 'function') {
                        window.initializeEventsPage();
                    }
                }, 100);
                break;
            case "/wallet":
                fragmentUrl = "/fragment/wallet";
                activeId = "nav-wallet";
                break;
            case "/settings":
                fragmentUrl = "/fragment/settings";
                activeId = "nav-settings";
                break;
            default:
                fragmentUrl = "/fragment/dashboard";
                activeId = "nav-host";
        }

        console.log('Loading initial fragment:', fragmentUrl, 'active:', activeId);
        this.loadFragment(fragmentUrl);
        document.querySelectorAll('.nav-item').forEach(nav => nav.classList.remove('active'));
        const activeNav = document.getElementById(activeId);
        if (activeNav) {
            activeNav.classList.add('active');
            console.log('Set active nav:', activeId);
        } else {
            console.warn('Active nav not found:', activeId);
        }
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
document.addEventListener('DOMContentLoaded', function() {
    const actionButtons = document.querySelectorAll('.action-btn');

    actionButtons.forEach(button => {
        button.addEventListener('click', function(event) {
            // Ngăn chặn sự kiện click lan ra ngoài
            event.stopPropagation();

            // Tìm menu dropdown tương ứng
            const dropdownMenu = this.parentNode.querySelector('.dropdown-menu');

            // Ẩn tất cả các menu khác
            document.querySelectorAll('.dropdown-menu').forEach(menu => {
                if (menu !== dropdownMenu) {
                    menu.style.display = 'none';
                }
            });

            // Chuyển đổi trạng thái hiển thị của menu hiện tại
            if (dropdownMenu.style.display === 'block') {
                dropdownMenu.style.display = 'none';
            } else {
                dropdownMenu.style.display = 'block';
            }
        });
    });

    // Ẩn menu khi click bất cứ nơi nào ngoài menu
    document.addEventListener('click', function(event) {
        document.querySelectorAll('.dropdown-menu').forEach(menu => {
            menu.style.display = 'none';
        });
    });
});


// Export for external use
window.HostDashboard = HostDashboard;