(function() {
    'use strict';
    
    // Track which buttons have been initialized to prevent duplicates
    const initializedButtons = new WeakSet();
    let clickOutsideHandler = null;
    let observer = null;
    let isInitialized = false;
    
    // Function to initialize dropdown menus
    function initializeDropdownMenus() {
        const actionButtons = document.querySelectorAll('.action-btn:not([data-dropdown-initialized])');
        
        if (actionButtons.length === 0) {
            return;
        }
        
        console.log('[Event.js] Initializing', actionButtons.length, 'new action buttons');

        actionButtons.forEach((button) => {
            // Mark button as initialized to prevent duplicate listeners
            button.setAttribute('data-dropdown-initialized', 'true');
            initializedButtons.add(button);
            
            button.addEventListener('click', function(event) {
                // Ngăn chặn sự kiện click lan ra ngoài
                event.stopPropagation();
                event.stopImmediatePropagation();

                // Tìm menu dropdown tương ứng
                const dropdownMenu = this.parentNode.querySelector('.dropdown-menu');
                if (!dropdownMenu) return;

                // Ẩn tất cả các menu khác
                document.querySelectorAll('.dropdown-menu').forEach(menu => {
                    if (menu !== dropdownMenu) {
                        menu.style.display = 'none';
                    }
                });

                // Chuyển đổi trạng thái hiển thị của menu hiện tại
                const isVisible = dropdownMenu.style.display === 'block';
                dropdownMenu.style.display = isVisible ? 'none' : 'block';
            });
        });
    }
    
    // Function to cleanup and reinitialize
    function cleanupAndReinitialize() {
        // Remove initialization markers from buttons that no longer exist
        const allButtons = document.querySelectorAll('.action-btn[data-dropdown-initialized]');
        allButtons.forEach(button => {
            if (!document.body.contains(button)) {
                button.removeAttribute('data-dropdown-initialized');
            }
        });
        
        // Initialize new buttons
        initializeDropdownMenus();
    }
    
    // Function to setup click outside handler (only once)
    function setupClickOutsideHandler() {
        if (clickOutsideHandler) {
            return; // Already setup
        }
        
        clickOutsideHandler = function(event) {
            if (!event.target.closest('.event-actions')) {
                document.querySelectorAll('.dropdown-menu').forEach(menu => {
                    menu.style.display = 'none';
                });
            }
        };
        
        // Use capture phase and make sure it doesn't interfere with button clicks
        document.addEventListener('click', clickOutsideHandler, true);
    }
    
    // Main initialization function
    function init() {
        if (isInitialized) {
            // Just initialize new buttons if any
            cleanupAndReinitialize();
            return;
        }
        
        console.log('[Event.js] Initializing dropdown menus system...');
        isInitialized = true;
        
        // Initialize immediately
        initializeDropdownMenus();
        
        // Setup click outside handler (only once)
        setupClickOutsideHandler();
        
        // Setup MutationObserver with debouncing to avoid excessive calls
        let mutationTimeout = null;
        observer = new MutationObserver(function(mutations) {
            // Clear existing timeout
            if (mutationTimeout) {
                clearTimeout(mutationTimeout);
            }
            
            // Debounce: wait 100ms after last mutation before reinitializing
            mutationTimeout = setTimeout(function() {
                let shouldReinit = false;
                
                mutations.forEach(function(mutation) {
                    if (mutation.type === 'childList') {
                        // Check if any added nodes contain action buttons
                        mutation.addedNodes.forEach(function(node) {
                            if (node.nodeType === 1) { // Element node
                                if (node.querySelector && node.querySelector('.action-btn')) {
                                    shouldReinit = true;
                                }
                                if (node.classList && node.classList.contains('action-btn')) {
                                    shouldReinit = true;
                                }
                            }
                        });
                    }
                });
                
                if (shouldReinit) {
                    console.log('[Event.js] New action buttons detected, reinitializing...');
                    cleanupAndReinitialize();
                }
            }, 100);
        });

        // Start observing - only watch for added/removed elements
        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
        
        // Handle saveEventBtn if it exists
        const saveEventBtn = document.getElementById('saveEventBtn');
        if (saveEventBtn && !saveEventBtn.hasAttribute('data-save-listener')) {
            saveEventBtn.setAttribute('data-save-listener', 'true');
            saveEventBtn.addEventListener('click', function() {
                if (typeof saveEvent === 'function') {
                    saveEvent();
                }
            });
        }
    }
    
    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
    
    // Also expose a manual init function for SPA navigation
    window.initEventDropdowns = function() {
        cleanupAndReinitialize();
    };
})();
function initCreateEventButton() {
    const btn = document.querySelector('.btn-create-event');
    if (!btn) return;

    btn.addEventListener('click', () => {
        document.getElementById('createEventModal').style.display = 'flex';
    });
}

// Gọi khi DOM ready
document.addEventListener("DOMContentLoaded", initCreateEventButton);

// SPA fragment reload
window.initCreateEventModal = initCreateEventButton;
