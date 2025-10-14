/**
 * Sidebar Manager - Quản lý sidebar đóng/mở
 */
class SidebarManager {
    constructor() {
        this.sidebar = document.getElementById('sidebar');
        this.sidebarToggle = document.getElementById('sidebarToggle');
        this.sidebarToggleMobile = document.getElementById('sidebarToggleMobile');
        this.sidebarCollapse = document.getElementById('sidebarCollapse');
        this.mainWrapper = document.getElementById('main-wrapper');
        
        this.isCollapsed = false;
        this.isMobileMenuOpen = false;
        
        this.init();
    }
    
    init() {
        console.log('🔧 Initializing SidebarManager...');
        
        // Check if elements exist
        if (!this.sidebar || !this.sidebarToggle) {
            console.error('❌ Sidebar elements not found!');
            return;
        }
        
        this.setupEventListeners();
        this.loadSidebarState();
        
        console.log('✅ SidebarManager initialized');
    }
    
    setupEventListeners() {
        // Desktop toggle button
        if (this.sidebarToggle) {
            this.sidebarToggle.addEventListener('click', () => {
                this.toggleSidebar();
            });
        }
        
        // Mobile toggle button
        if (this.sidebarToggleMobile) {
            this.sidebarToggleMobile.addEventListener('click', () => {
                this.toggleMobileMenu();
            });
        }
        
        // Collapse button
        if (this.sidebarCollapse) {
            this.sidebarCollapse.addEventListener('click', () => {
                this.toggleCollapse();
            });
        }
        
        // Close mobile menu when clicking outside
        document.addEventListener('click', (e) => {
            if (this.isMobileMenuOpen && 
                !this.sidebar.contains(e.target) && 
                !this.sidebarToggle.contains(e.target)) {
                this.closeMobileMenu();
            }
        });
        
        // Handle window resize
        window.addEventListener('resize', () => {
            this.handleResize();
        });
    }
    
    toggleSidebar() {
        console.log('🔄 Toggling sidebar...');
        this.isCollapsed = !this.isCollapsed;
        this.updateSidebarState();
        this.saveSidebarState();
    }
    
    toggleMobileMenu() {
        console.log('📱 Toggling mobile menu...');
        this.isMobileMenuOpen = !this.isMobileMenuOpen;
        this.updateMobileMenuState();
    }
    
    toggleCollapse() {
        console.log('📐 Toggling collapse...');
        this.toggleSidebar();
    }
    
    updateSidebarState() {
        console.log('🔄 Updating sidebar state. isCollapsed:', this.isCollapsed);
        
        if (this.isCollapsed) {
            this.sidebar.classList.add('collapsed');
            this.mainWrapper.classList.add('expanded'); // Sử dụng class 'expanded' thay vì 'sidebar-collapsed'
            console.log('✅ Added collapsed and expanded classes');
            
            // Update collapse button icon
            if (this.sidebarCollapse) {
                const icon = this.sidebarCollapse.querySelector('i');
                if (icon) {
                    icon.className = 'bi bi-chevron-right';
                }
            }
        } else {
            this.sidebar.classList.remove('collapsed');
            this.mainWrapper.classList.remove('expanded'); // Sử dụng class 'expanded' thay vì 'sidebar-collapsed'
            console.log('✅ Removed collapsed and expanded classes');
            
            // Update collapse button icon
            if (this.sidebarCollapse) {
                const icon = this.sidebarCollapse.querySelector('i');
                if (icon) {
                    icon.className = 'bi bi-chevron-left';
                }
            }
        }
        
        console.log('📊 Current classes - Sidebar:', this.sidebar.className);
        console.log('📊 Current classes - MainWrapper:', this.mainWrapper.className);
    }
    
    updateMobileMenuState() {
        if (this.isMobileMenuOpen) {
            this.sidebar.classList.add('mobile-open');
            document.body.classList.add('sidebar-mobile-open');
        } else {
            this.sidebar.classList.remove('mobile-open');
            document.body.classList.remove('sidebar-mobile-open');
        }
    }
    
    closeMobileMenu() {
        this.isMobileMenuOpen = false;
        this.updateMobileMenuState();
    }
    
    handleResize() {
        // Auto-close mobile menu on desktop
        if (window.innerWidth >= 992 && this.isMobileMenuOpen) {
            this.closeMobileMenu();
        }
    }
    
    saveSidebarState() {
        localStorage.setItem('sidebarCollapsed', this.isCollapsed.toString());
    }
    
    loadSidebarState() {
        const saved = localStorage.getItem('sidebarCollapsed');
        if (saved !== null) {
            this.isCollapsed = saved === 'true';
            this.updateSidebarState();
        }
    }
}

// Initialize sidebar manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 Initializing sidebar...');
    window.sidebarManager = new SidebarManager();
});

// Make it globally accessible
window.SidebarManager = SidebarManager;
