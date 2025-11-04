document.addEventListener('DOMContentLoaded', function() {
    console.log('[Event.js] DOM loaded, initializing dropdown menus...');
    
    // Function to initialize dropdown menus
    function initializeDropdownMenus() {
        const actionButtons = document.querySelectorAll('.action-btn');
        console.log('[Event.js] Found', actionButtons.length, 'action buttons');

        actionButtons.forEach((button, index) => {
            console.log('[Event.js] Setting up button', index);
            
            button.addEventListener('click', function(event) {
                console.log('[Event.js] Action button clicked');
                
                // Ngăn chặn sự kiện click lan ra ngoài
                event.stopPropagation();

                // Tìm menu dropdown tương ứng
                const dropdownMenu = this.parentNode.querySelector('.dropdown-menu');
                console.log('[Event.js] Found dropdown menu:', dropdownMenu);

                // Ẩn tất cả các menu khác
                document.querySelectorAll('.dropdown-menu').forEach(menu => {
                    if (menu !== dropdownMenu) {
                        menu.style.display = 'none';
                    }
                });

                // Chuyển đổi trạng thái hiển thị của menu hiện tại
                if (dropdownMenu) {
                    if (dropdownMenu.style.display === 'block') {
                        dropdownMenu.style.display = 'none';
                        console.log('[Event.js] Hiding dropdown');
                    } else {
                        dropdownMenu.style.display = 'block';
                        console.log('[Event.js] Showing dropdown');
                    }
                }
            });
        });
    }

    // Initialize immediately
    initializeDropdownMenus();

    // Also initialize when new content is loaded (for SPA)
    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            if (mutation.type === 'childList') {
                const actionButtons = document.querySelectorAll('.action-btn');
                if (actionButtons.length > 0) {
                    console.log('[Event.js] New content loaded, re-initializing dropdowns');
                    initializeDropdownMenus();
                }
            }
        });
    });

    // Start observing
    observer.observe(document.body, {
        childList: true,
        subtree: true
    });

    // Ẩn menu khi click bất cứ nơi nào ngoài menu
    document.addEventListener('click', function(event) {
        if (!event.target.closest('.event-actions')) {
            document.querySelectorAll('.dropdown-menu').forEach(menu => {
                menu.style.display = 'none';
            });
        }
    });
    
    // Only add event listener if saveEventBtn exists
    const saveEventBtn = document.getElementById('saveEventBtn');
    if (saveEventBtn) {
        saveEventBtn.addEventListener('click', function() {
            if (typeof saveEvent === 'function') {
                saveEvent();
            }
        });
    }
});