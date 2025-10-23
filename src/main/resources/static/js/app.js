document.addEventListener('DOMContentLoaded', () => {
    console.log('📄 DOM Content Loaded - Starting SPA initialization...');
    
    // Lấy eventId từ URL hiện tại, ví dụ: /manage/event/10/...
    const pathParts = window.location.pathname.split('/');
    const eventId = pathParts[3]; // phần tử thứ 3 là số id

    console.log('🔍 Current URL:', window.location.pathname);
    console.log('🔍 Path parts:', pathParts);
    console.log('🔍 Extracted eventId:', eventId);

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
            path: `/manage/event/${eventId}/check-in`,
            fragment: `/fragments/check-in?id=${eventId}`,
            title: 'Check-in',
            initializer: function() {
                console.log('Initializing check-in page...');
                // Initialize check-in functionality
            }
        }
    ];

    console.log('🚀 Initializing SPA Router with eventId:', eventId);
    console.log('📋 Routes:', appRoutes);
    
    // Initialize SPA Router
    window.spaRouter = new SpaRouter(appRoutes, '#main-content');
    console.log('✅ SPA Router initialized successfully');
});

// Fallback initialization if DOM is already loaded
if (document.readyState === 'loading') {
    console.log('⏳ DOM still loading, waiting for DOMContentLoaded...');
} else {
    console.log('⚡ DOM already loaded, initializing immediately...');
    // Re-run the initialization
    const pathParts = window.location.pathname.split('/');
    const eventId = pathParts[3];
    
    if (eventId && !isNaN(eventId)) {
        console.log('🔄 Fallback initialization with eventId:', eventId);
        // This will be handled by the DOMContentLoaded event above
    }
}
