/**
 * SpaRouter.js
 * Một class đơn giản để quản lý điều hướng trong Single-Page Application.
 */
class SpaRouter {
    /**
     * @param {Array} routes - Một mảng các đối tượng route. Ví dụ: [{ path: '/home', fragment: '/fragments/home.html', title: 'Trang chủ' }]
     * @param {string} mainContentId - ID của element sẽ chứa nội dung được tải động.
     */
    constructor(routes, mainContentId = '#main-content') {
        this.routes = routes;
        this.mainContent = document.querySelector(mainContentId);

        if (!this.mainContent) {
            console.error(`[SpaRouter] Không tìm thấy element nội dung chính với selector: ${mainContentId}`);
            return;
        }

        this.setupEventListeners();
        this.loadInitialRoute();
    }

    /**
     * Thiết lập các event listener cần thiết.
     */
    setupEventListeners() {
        // Lắng nghe sự kiện click trên toàn bộ document để bắt các link `data-link`
        document.addEventListener('click', e => {
            const link = e.target.closest('a[data-link]');
            if (link) {
                e.preventDefault(); // Ngăn trình duyệt tải lại trang
                this.navigateTo(link.getAttribute('href'));
            }
        });

        // Lắng nghe sự kiện khi người dùng nhấn nút back/forward của trình duyệt
        window.addEventListener('popstate', () => this.handlePopState());
    }

    /**
     * Điều hướng đến một đường dẫn (path) cụ thể.
     * @param {string} path - Đường dẫn cần điều hướng tới (ví dụ: '/host/setting').
     */
    // Trong SpaRouter.js
    navigateTo(path) {
        // Normalize path first
        const normalizedPath = path.split('?')[0].replace(/\/$/, '');
        console.log(`[Router] Bắt đầu điều hướng đến: ${path} (normalized: ${normalizedPath})`);
        
        const route = this.findRoute(normalizedPath);
        if (!route) {
            console.error(`[SpaRouter] Không tìm thấy route cho path: ${normalizedPath}`);
            console.error(`[SpaRouter] Available routes:`, this.routes.map(r => r.path));
            console.error(`[SpaRouter] Chuyển về trang mặc định.`);

            const defaultRoute = this.routes[0]; // Lấy route đầu tiên làm mặc định
            if (defaultRoute && defaultRoute.path !== normalizedPath) { // Tránh lặp vô hạn nếu chính route mặc định bị lỗi
                console.log(`[SpaRouter] Redirecting to default route: ${defaultRoute.path}`);
                this.navigateTo(defaultRoute.path);
            }
            return;
        }

        console.log(`[Router] ✅ Route found, navigating to: ${route.path}`);
        
        // Đẩy trạng thái mới vào history, cập nhật URL
        history.pushState({ path: normalizedPath }, route.title, normalizedPath);
        this.renderContent(route);
    }

    /**
     * Tải và hiển thị nội dung của một route.
     * @param {object} route - Đối tượng route cần hiển thị.
     */
    async renderContent(route) {
        this.mainContent.innerHTML = '<div class="loading-spinner"></div>'; // Hiển thị spinner

        try {
                // Lấy eventId từ URL hiện tại
            const pathParts = window.location.pathname.split('/');
            const eventId = pathParts[3]; // phần tử thứ 3 là số id
            
            if (!eventId || isNaN(eventId)) {
                throw new Error("Không tìm thấy Event ID trong URL để tải fragment.");
            }
            
            // Fragment URL đã có eventId trong app.js, không cần thêm nữa
            const fragmentUrl = route.fragment;

            console.log(`[Router] Bắt đầu fetch fragment từ: ${fragmentUrl}`); // <-- LOG 4 (QUAN TRỌNG NHẤT)
            const response = await fetch(fragmentUrl);
            if (!response.ok) {
                throw new Error(`Không thể tải fragment: ${response.statusText}`);
            }
            const html = await response.text();
            this.mainContent.innerHTML = html;
            document.title = route.title;

            // Chạy hàm JavaScript khởi tạo cho trang đó (nếu có)
            if (typeof route.initializer === 'function') {
                route.initializer();
            }

            // Cập nhật trạng thái 'active' cho link điều hướng
            this.updateActiveLink(route.path);

            const pageName = route.title.split(' | ')[0];
            if (typeof window.updateBreadcrumb === 'function') {
                window.updateBreadcrumb(pageName);
            }

        } catch (error) {
            console.error(`[SpaRouter] Lỗi khi render nội dung:`, error);
            this.mainContent.innerHTML = `<p class="error">Đã xảy ra lỗi khi tải trang. Vui lòng thử lại.</p>`;
        }
    }

    /**
     * Xử lý khi nhấn nút back/forward.
     */
    handlePopState() {
        const path = window.location.pathname;
        const route = this.findRoute(path);
        if (route) {
            this.renderContent(route);
        }
    }

    /**
     * Tải route ban đầu khi người dùng mới vào trang hoặc F5.
     */
    loadInitialRoute() {
        const path = window.location.pathname;
        console.log(`[Router] Tải route ban đầu cho path: ${path}`); // <-- LOG 7
        const route = this.findRoute(path);
        if (route) {
            this.renderContent(route);
        } else {
            // Nếu vào một URL không tồn tại, chuyển về route đầu tiên
            const defaultRoute = this.routes[0];
            if (defaultRoute) {
                // Dùng replaceState để không tạo thêm một mục history sai
                history.replaceState({ path: defaultRoute.path }, defaultRoute.title, defaultRoute.path);
                this.renderContent(defaultRoute);
            }
        }
    }

    /**
     * Tìm route tương ứng với một path.
     * @param {string} path - Đường dẫn cần tìm.
     * @returns {object|null} - Đối tượng route hoặc null nếu không tìm thấy.
     */
    findRoute(path) {
        // Normalize path (remove trailing slashes, handle query params)
        const normalizedPath = path.split('?')[0].replace(/\/$/, '');
        
        console.log(`[Router] Finding route for path: ${path} (normalized: ${normalizedPath})`);
        console.log(`[Router] Available routes:`, this.routes.map(r => r.path));
        
        const route = this.routes.find(route => {
            const routePath = route.path.split('?')[0].replace(/\/$/, '');
            const match = routePath === normalizedPath;
            if (match) {
                console.log(`[Router] ✅ Found matching route: ${route.path}`);
            }
            return match;
        });
        
        if (!route) {
            console.warn(`[Router] ❌ No route found for: ${normalizedPath}`);
        }
        
        return route || null;
    }

    /**
     * Cập nhật class 'active' cho link đang được chọn.
     * @param {string} path - Đường dẫn của trang hiện tại.
     */
    updateActiveLink(path) {
        // Xóa tất cả class active
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });
        
        // Thêm class active cho item tương ứng
        document.querySelectorAll('a[data-link]').forEach(link => {
            if (link.getAttribute('href') === path) {
                link.closest('.nav-item').classList.add('active');
            }
        });
    }
}