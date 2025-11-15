document.addEventListener('DOMContentLoaded', () => {
    // Lấy eventId từ URL hiện tại, ví dụ: /manage/event/10/...
    const pathParts = window.location.pathname.split('/');
    const eventId = pathParts[3]; // phần tử thứ 3 là số id

    if (!eventId || isNaN(eventId)) {
        console.error("❌ Không tìm thấy eventId trong URL!");
        return;
    }

    const appRoutes = [
        {
            path: `/manage/event/${eventId}/getting-started`,
            fragment: `/fragments/getting-started?id=${eventId}`,
            title: 'Bắt đầu',

        },
        {
            path: `/manage/event/${eventId}/ticket`,
            fragment: `/fragments/ticket?id=${eventId}`,
            title: 'Vé',
            initializer: function() {
                console.log('Initializing ticket page...');
                // Initialize ticket functionality
                if (typeof window.initializeTicketForm === 'function') {
                    window.initializeTicketForm();
                }
            }
        },
        {
            path: `/manage/event/${eventId}/settings`,
            fragment: `/fragments/update-event?id=${eventId}`,
            title: 'Cài đặt Sự kiện',
            initializer: window.initializeEventFormListeners,
        },
        {
            path: `/manage/event/${eventId}/dashboard-event`,
            fragment: `/fragments/dashboard-event?id=${eventId}`,
            title: 'DashBoard',
            initializer: function() {
                console.log('Initializing dashboard page...');
                // Gọi function khởi tạo dashboard
                if (typeof window.initializeDashboard === 'function') {
                    window.initializeDashboard();
                }
            }
        },
        {
            path: `/manage/event/${eventId}/request-form`,
            fragment: `/api/requests/form?eventId=${eventId}`,
            title: 'Yêu cầu',
            initializer: function() {
                console.log('[SPA Router] Initializing request form page...');
                // Wait for HTML to be injected into DOM, then initialize
                // Use requestAnimationFrame for better timing with DOM updates
                requestAnimationFrame(() => {
                    setTimeout(() => {
                        if (typeof window.initializeRequestForm === 'function') {
                            console.log('[SPA Router] Calling initializeRequestForm...');
                            window.initializeRequestForm();
                        } else {
                            console.warn('[SPA Router] initializeRequestForm not found, waiting for script to load...');
                            // Retry if script hasn't loaded yet (shouldn't happen if included in manager-event.html)
                            setTimeout(() => {
                                if (typeof window.initializeRequestForm === 'function') {
                                    console.log('[SPA Router] Found initializeRequestForm on retry, calling it...');
                                    window.initializeRequestForm();
                                } else {
                                    console.error('[SPA Router] initializeRequestForm still not found. Check if request-form.js is loaded.');
                                }
                            }, 200);
                        }
                    }, 50); // Small delay to ensure DOM is ready
                });
            }
        },
        {
            path: `/manage/event/${eventId}/orders`,
            fragment: `/fragments/orders?id=${eventId}`,
            title: `Đơn Hàng`,
            initializer: function () {
                console.log('Orders page loaded - initializing from orders.js');
                // Wait for orders.js to load and initialize
                setTimeout(() => {
                    if (typeof window.initializeOrders === 'function') {
                        console.log('Found initializeOrders, calling it...');
                        window.initializeOrders();
                    } else {
                        console.warn('initializeOrders function not found');
                    }
                }, 200);
            }
        },
        {
            path: `/manage/event/${eventId}/attendees`,
            fragment: `/fragments/attendees?id=${eventId}`,
            title: `Người tham dự`,
            initializer: function () {
                // Modals are now in layout (global), just initialize and setup search
                if (typeof window.initializeModals === 'function') {
                    window.initializeModals();
                }
                if (typeof window.setupAutoSearch === 'function') {
                    setTimeout(() => window.setupAutoSearch(), 300);
                }
            }
        },
        {
            path: `/manage/event/${eventId}/statis-forms`,
            fragment: `/fragments/statis-forms?id=${eventId}`,
            title: 'Thống kê Form',
            initializer: function() {
                console.log('Initializing create forms page...');
            }
        },
        {
            path: `/manage/event/${eventId}/volunteers`,
            fragment: `/fragments/volunteers?id=${eventId}`,
            title: `Tình nguyện viên`,
            initializer: function () {
                console.log('Initializing volunteers page...');
            }
        },
        {
            path: `/manage/event/${eventId}/create-forms`,
            fragment: `/fragments/create-forms?id=${eventId}`,
            title: 'Tạo Form',
            initializer: function() {
                console.log('Initializing create forms page...');
            }
        },
        {
            path: `/manage/event/${eventId}/qr-codes`,
            fragment: `/fragments/qr-codes?id=${eventId}`,
            title: 'QR Codes',
            initializer: function() {
                console.log('Initializing QR codes page...');
            }
        },
        {
            path: `/manage/event/${eventId}/volunteer-create-form`,
            fragment: `/fragments/volunteer-create-form?id=${eventId}`,
            title: 'Tạo Form Tình nguyện viên',
            initializer: function() {
                console.log('Initializing volunteer create form page...');
            }
        },
        {
            path: `/manage/event/${eventId}/volunteer-requests`,
            fragment: `/fragments/volunteer-requests?id=${eventId}`,
            title: 'Xem yêu cầu duyệt Tình nguyện viên',
            initializer: function() {
                console.log('Initializing volunteer requests page...');
            }
        },
        {
            path: `/manage/event/${eventId}/notification`,
            fragment: `/fragments/notification?id=${eventId}`,
            title: 'Thông báo',
            initializer: function() {
                console.log('Initializing notification page...');
                // Notification page scripts are already in the fragment
            }
        }
    ];

    // Initialize SPA Router
    window.spaRouter = new SpaRouter(appRoutes, '#main-content');
});

// Fallback initialization if DOM is already loaded
if (document.readyState !== 'loading') {
    // This will be handled by the DOMContentLoaded event above
}
