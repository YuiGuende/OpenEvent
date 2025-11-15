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
                const href = link.getAttribute('href');
                // Extract query params from href
                let queryParams = '';
                if (href.includes('?')) {
                    queryParams = '?' + href.split('?')[1];
                }
                this.navigateTo(href, queryParams);
            }
        });

        // Lắng nghe sự kiện khi người dùng nhấn nút back/forward của trình duyệt
        window.addEventListener('popstate', () => this.handlePopState());
    }

    /**
     * Điều hướng đến một đường dẫn (path) cụ thể.
     * @param {string} path - Đường dẫn cần điều hướng tới (ví dụ: '/host/setting').
     * @param {string} queryParams - Query parameters từ URL (ví dụ: '?formId=1').
     */
    // Trong SpaRouter.js
    navigateTo(path, queryParams = '') {
        // Normalize path first (remove query params for route matching)
        const normalizedPath = path.split('?')[0].replace(/\/$/, '');
        
        // Extract query params from path if not provided
        if (!queryParams && path.includes('?')) {
            queryParams = '?' + path.split('?')[1];
        }
        
        // Try to find route by matching path
        let route = this.findRoute(normalizedPath);
        
        // If route not found and it's a fragment URL, create a dynamic route
        if (!route && (normalizedPath.startsWith('/fragments/') || normalizedPath.startsWith('/forms/fragments/'))) {
            // For fragment URLs, create a route object with the full fragment URL
            route = {
                path: normalizedPath,
                fragment: normalizedPath + queryParams, // Include query params in fragment
                title: 'Form Statistics',
                isDynamic: true // Flag to indicate this is a dynamically created route
            };
            // Don't set queryParams since it's already in fragment
        }
        
        if (!route) {
            console.error(`[SpaRouter] Không tìm thấy route cho path: ${normalizedPath}`);
            const defaultRoute = this.routes[0]; // Lấy route đầu tiên làm mặc định
            if (defaultRoute && defaultRoute.path !== normalizedPath) { // Tránh lặp vô hạn nếu chính route mặc định bị lỗi
                this.navigateTo(defaultRoute.path);
            }
            return;
        }
        
        // Store query params for renderContent (only if route doesn't already have them in fragment)
        if (!route.isDynamic) {
            route.queryParams = queryParams;
        }
        
        // Đẩy trạng thái mới vào history, cập nhật URL (include query params in URL)
        const fullPath = normalizedPath + queryParams;
        history.pushState({ path: fullPath }, route.title, fullPath);
        this.renderContent(route);
    }

    /**
     * Tải và hiển thị nội dung của một route.
     * @param {object} route - Đối tượng route cần hiển thị.
     */
    async renderContent(route) {
        this.mainContent.innerHTML = '<div class="loading-spinner"></div>'; // Hiển thị spinner

        try {
            // Fragment URL - use the fragment from route
            let fragmentUrl = route.fragment;
            
            // If route is dynamic (created on the fly), fragment already has query params
            // Otherwise, append query params if available
            if (!route.isDynamic && route.queryParams) {
                // Check if fragment URL already has query params
                if (fragmentUrl.includes('?')) {
                    fragmentUrl += '&' + route.queryParams.substring(1); // Remove leading '?'
                } else {
                    fragmentUrl += route.queryParams;
                }
            }
            
            // For routes that need eventId, try to get it from URL (only if not already in fragment)
            // Also preserve formId from query params if it exists
            const currentUrl = new URL(window.location.href);
            const formIdParam = currentUrl.searchParams.get('formId');
            
            if (!fragmentUrl.includes('?id=') && !fragmentUrl.includes('&id=')) {
                // Try to get eventId from current URL path
                const pathParts = window.location.pathname.split('/');
                const eventId = pathParts[3]; // phần tử thứ 3 là số id
                
                if (eventId && !isNaN(eventId)) {
                    if (fragmentUrl.includes('?')) {
                        fragmentUrl += '&id=' + eventId;
                    } else {
                        fragmentUrl += '?id=' + eventId;
                    }
                }
            }
            
            // Add formId to fragment URL if it exists in query params and not already present
            if (formIdParam && !fragmentUrl.includes('formId=')) {
                if (fragmentUrl.includes('?')) {
                    fragmentUrl += '&formId=' + formIdParam;
                } else {
                    fragmentUrl += '?formId=' + formIdParam;
                }
            }

            const response = await fetch(fragmentUrl);
            if (!response.ok) {
                throw new Error(`Không thể tải fragment: ${response.statusText}`);
            }
            const html = await response.text();
            this.mainContent.innerHTML = html;
            document.title = route.title;

            // Execute scripts in the injected HTML
            this.executeScripts(this.mainContent);

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
        
        const route = this.routes.find(route => {
            const routePath = route.path.split('?')[0].replace(/\/$/, '');
            return routePath === normalizedPath;
        });
        
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

    /**
     * Execute scripts found in dynamically loaded HTML fragments.
     * innerHTML does not execute script tags by default, so we need to manually execute them.
     * @param {HTMLElement} container - The container element that contains the scripts.
     */
    executeScripts(container) {
        const scripts = container.querySelectorAll('script');
        scripts.forEach((oldScript) => {
            const newScript = document.createElement('script');
            Array.from(oldScript.attributes).forEach(attr => {
                newScript.setAttribute(attr.name, attr.value);
            });
            if (oldScript.src) {
                newScript.src = oldScript.src;
            } else {
                newScript.textContent = oldScript.textContent;
            }
            oldScript.parentNode.replaceChild(newScript, oldScript);
        });
    }
}