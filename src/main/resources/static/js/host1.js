// ===========================
// Host Dashboard JS
// ===========================
document.addEventListener('DOMContentLoaded', function() {
    HostDashboard.init();
});

const HostDashboard = {
    init() {
        this.mainContent = document.querySelector('.main-content');
        this.setupEventListeners();
        this.setupResponsiveBehavior();
        this.loadInitialPage();
    },

    setupEventListeners() {
        this.setupNavigation();
        this.setupHeaderActions();
        this.setupUtilityButtons();
        this.setupSidebarTooltips();
        this.setupMobileMenu();
        this.setupSidebarToggle();
    },

    // =======================
    // NAVIGATION HANDLING
    // =======================
    setupNavigation() {
        const navItems = document.querySelectorAll('.nav-item');
        navItems.forEach(item => {
            item.addEventListener('click', () => this.handleNavigation(item));
        });
    },

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

    loadFragment(fragmentUrl, newUrl) {
        this.showLoadingState();
        fetch(fragmentUrl)
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.text();
            })
            .then(html => {
                if (this.mainContent) this.mainContent.innerHTML = html;
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

    // =======================
    // HEADER, SIDEBAR, UI LOGIC
    // =======================
    setupHeaderActions() {
        const liveStatus = document.querySelector('.live-status');
        if (liveStatus) {
            liveStatus.addEventListener('click', () => this.toggleLiveStatus());
        }

        const viewHomepageBtn = document.querySelector('.btn-view-homepage');
        if (viewHomepageBtn) {
            viewHomepageBtn.addEventListener('click', () => this.viewOrganizerHomepage());
        }
    },

    setupUtilityButtons() {
        const collapseBtn = document.querySelector('.collapse-btn');
        if (collapseBtn) {
            collapseBtn.addEventListener('click', () => this.toggleSidebar());
        }
    },

    setupSidebarTooltips() {
        document.querySelectorAll('.nav-item').forEach(item => {
            const span = item.querySelector('span');
            if (span) item.setAttribute('data-tooltip', span.textContent);
        });
    },

    setupMobileMenu() {
        const mobileMenuBtn = document.querySelector('.mobile-menu-btn');
        const sidebar = document.querySelector('.sidebar');
        if (!mobileMenuBtn || !sidebar) return;

        mobileMenuBtn.addEventListener('click', () => {
            sidebar.classList.toggle('open');
        });

        document.addEventListener('click', (e) => {
            if (window.innerWidth <= 768 &&
                !sidebar.contains(e.target) &&
                !mobileMenuBtn.contains(e.target) &&
                sidebar.classList.contains('open')) {
                sidebar.classList.remove('open');
            }
        });
    },

    setupSidebarToggle() {
        const sidebarToggleBtn = document.querySelector('.sidebar-toggle-btn');
        const sidebar = document.querySelector('.sidebar');
        const mainContent = document.querySelector('.main-content');

        if (sidebarToggleBtn && sidebar) {
            sidebarToggleBtn.addEventListener('click', () => {
                sidebar.classList.toggle('collapsed');
                document.body.classList.toggle('sidebar-collapsed');

                if (sidebar.classList.contains('collapsed')) {
                    mainContent.style.marginLeft = '0';
                    mainContent.style.width = '100%';
                } else {
                    mainContent.style.marginLeft = '280px';
                    mainContent.style.width = 'calc(100% - 280px)';
                }
            });
        }
    },

    // =======================
    // UTILS
    // =======================
    showLoadingState() {
        if (this.mainContent) {
            this.mainContent.innerHTML = `<div class="loading">Đang tải...</div>`;
        }
    },

    hideLoadingState() {},

    updatePageTitle(title) {
        document.title = `${title} - Open.events`;
    },

    toggleLiveStatus() {
        const liveStatus = document.querySelector('.live-status');
        if (!liveStatus) return;

        const isLive = liveStatus.textContent.includes('Trực tiếp');
        if (isLive) {
            liveStatus.innerHTML = '<i class="fas fa-eye-slash"></i><span>• Đã tắt - Nhấp để bật</span>';
        } else {
            liveStatus.innerHTML = '<i class="fas fa-eye"></i><span>• Trực tiếp - Nhấp để gỡ bỏ</span>';
        }
    },

    viewOrganizerHomepage() {
        window.open('https://app.hi.events/organizer/duc-le', '_blank');
    },

    setupResponsiveBehavior() {
        window.addEventListener('resize', () => {
            const sidebar = document.querySelector('.sidebar');
            const mainContent = document.querySelector('.main-content');
            if (!sidebar || !mainContent) return;

            if (window.innerWidth <= 768) {
                sidebar.classList.remove('collapsed', 'open');
                mainContent.style.marginLeft = '0';
                mainContent.style.width = '100%';
            } else if (!sidebar.classList.contains('collapsed')) {
                mainContent.style.marginLeft = '280px';
                mainContent.style.width = 'calc(100% - 280px)';
            }
        });
    }
};

window.HostDashboard = HostDashboard;
