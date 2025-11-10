// Events Manager - Handles events page search, filter, and initialization
// This file is loaded in host.html and functions are exposed to window

(function() {
    'use strict';
    
    console.log('[Events Manager] Script loaded');
    
    // Event search and filter variables (module scope)
    var searchTimeout = null;
    var searchAbortController = null;
    var isLoadingEvents = false;
    var lastSearchValue = ''; // Track last search value to avoid duplicate requests
    var lastSortFilter = ''; // Track last sort filter to avoid duplicate requests
    
    // Function to show loading state
    function showEventsLoading() {
        const eventsList = document.querySelector('.events-list');
        if (eventsList && !isLoadingEvents) {
            isLoadingEvents = true;
            const loadingHtml = `
                <div class="events-loading" style="text-align: center; padding: 40px; color: #666;">
                    <div style="display: inline-block; width: 40px; height: 40px; border: 4px solid #f3f3f3; border-top: 4px solid #6f42c1; border-radius: 50%; animation: spin 1s linear infinite;"></div>
                    <p style="margin-top: 20px;">Đang tải sự kiện...</p>
                </div>
            `;
            eventsList.innerHTML = loadingHtml;
            
            // Add spin animation if not exists
            if (!document.querySelector('#events-loading-style')) {
                const style = document.createElement('style');
                style.id = 'events-loading-style';
                style.textContent = `
                    @keyframes spin {
                        0% { transform: rotate(0deg); }
                        100% { transform: rotate(360deg); }
                    }
                `;
                document.head.appendChild(style);
            }
        }
    }
    
    // Function to hide loading state
    function hideEventsLoading() {
        isLoadingEvents = false;
    }
    
    // Function to load events with filters - expose to window
    window.loadEventsFragment = function loadEventsFragment(search, sortFilter, tabFilter, forceLoad) {
        // Normalize search value (trim and convert to string)
        const normalizedSearch = (search || '').trim();
        const normalizedSortFilter = sortFilter || '';
        
        // Avoid duplicate requests: only load if values actually changed
        if (!forceLoad && normalizedSearch === lastSearchValue && normalizedSortFilter === lastSortFilter) {
            console.log('[Events Manager] Skipping duplicate request - values unchanged:', {
                search: normalizedSearch,
                sortFilter: normalizedSortFilter
            });
            return;
        }
        
        // Update last values
        lastSearchValue = normalizedSearch;
        lastSortFilter = normalizedSortFilter;
        
        console.log('[Events Manager] loadEventsFragment called with:', { 
            search: normalizedSearch, 
            sortFilter: normalizedSortFilter, 
            tabFilter 
        });
        
        // Show loading state
        showEventsLoading();
        
        const params = new URLSearchParams();
        if (normalizedSearch) {
            params.set('search', normalizedSearch);
        }
        if (normalizedSortFilter) {
            params.set('sortFilter', normalizedSortFilter);
        }
        // Note: tabFilter is not used in backend currently, but kept for future use
        
        const url = `/fragment/events${params.toString() ? '?' + params.toString() : ''}`;
        console.log('[Events Manager] Loading URL:', url);
        
        // Abort previous request if exists
        if (searchAbortController) {
            searchAbortController.abort();
        }
        searchAbortController = new AbortController();
        
        fetch(url, { signal: searchAbortController.signal })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.text();
            })
            .then(html => {
                const mainContent = document.querySelector('.main-content');
                if (mainContent) {
                    mainContent.innerHTML = html;
                    hideEventsLoading();
                    
                    console.log('[Events Manager] Fragment loaded, re-initializing...');
                    
                    // Re-initialize events page after loading
                    setTimeout(() => {
                        if (typeof window.initializeEventsPage === 'function') {
                            console.log('[Events Manager] Calling initializeEventsPage...');
                            window.initializeEventsPage();
                        } else {
                            console.warn('[Events Manager] initializeEventsPage not found after fragment load');
                        }
                    }, 100);
                }
            })
            .catch(error => {
                hideEventsLoading();
                if (error.name === 'AbortError') {
                    console.log('[Events Manager] Search request aborted');
                    return;
                }
                console.error('[Events Manager] Error loading events:', error);
                const eventsList = document.querySelector('.events-list');
                if (eventsList) {
                    eventsList.innerHTML = `
                        <div class="events-error" style="text-align: center; padding: 60px 20px; color: #dc3545;">
                            <i class="fas fa-exclamation-triangle" style="font-size: 48px; margin-bottom: 20px;"></i>
                            <p style="font-size: 18px; margin: 0;">Đã xảy ra lỗi khi tải sự kiện. Vui lòng thử lại.</p>
                            <button onclick="window.location.reload()" style="margin-top: 20px; padding: 10px 20px; background: #6f42c1; color: white; border: none; border-radius: 5px; cursor: pointer;">
                                Tải lại trang
                            </button>
                        </div>
                    `;
                }
            });
    };
    
    // Setup auto-search - expose to window
    window.setupEventSearch = function setupEventSearch() {
        console.log('[Events Manager] setupEventSearch called');
        
        // Clean up previous listeners
        if (searchAbortController) {
            searchAbortController.abort();
        }
        searchAbortController = new AbortController();
        
        // Remove old listeners by cloning elements (prevents duplicates)
        const searchInput = document.getElementById('eventSearchInput');
        if (searchInput && !searchInput.dataset.listenerAttached) {
            searchInput.dataset.listenerAttached = 'true';
            
            // Store the current value to compare
            let currentValue = searchInput.value || '';
            
            searchInput.addEventListener('input', (e) => {
                const newValue = e.target.value || '';
                const oldValue = currentValue;
                
                // Clear previous timeout
                clearTimeout(searchTimeout);
                
                // Only proceed if value actually changed (after trimming)
                if (newValue.trim() === oldValue.trim()) {
                    console.log('[Events Manager] Search value unchanged, skipping request');
                    return;
                }
                
                // Determine if user is deleting (value is getting shorter)
                const isDeleting = newValue.length < oldValue.length;
                
                // Update current value
                currentValue = newValue;
                
                // Debounce: wait longer when deleting to reduce requests
                const delay = isDeleting ? 700 : 500;
                
                searchTimeout = setTimeout(function() {
                    const trimmedValue = newValue.trim();
                    const sortFilter = document.getElementById('eventSortFilter')?.value || '';
                    
                    // Double check: only send if value is different from last search
                    if (trimmedValue !== lastSearchValue || sortFilter !== lastSortFilter) {
                        console.log('[Events Manager] Auto-search triggered:', trimmedValue);
                        if (typeof window.loadEventsFragment === 'function') {
                            window.loadEventsFragment(trimmedValue, sortFilter, '');
                        }
                    } else {
                        console.log('[Events Manager] Auto-search skipped - no change in search/filter');
                    }
                }, delay);
            });
            console.log('[Events Manager] Search input listener attached');
        }
        
        // Setup sort filter change
        const sortFilter = document.getElementById('eventSortFilter');
        if (sortFilter && !sortFilter.dataset.listenerAttached) {
            sortFilter.dataset.listenerAttached = 'true';
            sortFilter.addEventListener('change', function(e) {
                const searchValue = document.getElementById('eventSearchInput')?.value.trim() || '';
                const newSortFilter = e.target.value || '';
                
                // Only send request if filter actually changed
                if (newSortFilter === lastSortFilter && searchValue === lastSearchValue) {
                    console.log('[Events Manager] Filter unchanged, skipping request');
                    return;
                }
                
                console.log('[Events Manager] Filter changed:', newSortFilter);
                if (typeof window.loadEventsFragment === 'function') {
                    window.loadEventsFragment(searchValue, newSortFilter, '');
                }
            });
            console.log('[Events Manager] Sort filter listener attached');
        }
        
        // Setup tab clicks (if tabs exist)
        const tabs = document.querySelectorAll('.event-tab');
        tabs.forEach(function(tab) {
            if (!tab.dataset.listenerAttached) {
                tab.dataset.listenerAttached = 'true';
                tab.addEventListener('click', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    
                    // Remove active class from all tabs
                    document.querySelectorAll('.event-tab').forEach(function(t) { 
                        t.classList.remove('active'); 
                    });
                    // Add active class to clicked tab
                    tab.classList.add('active');
                    
                    const searchValue = document.getElementById('eventSearchInput')?.value.trim() || '';
                    const sortFilter = document.getElementById('eventSortFilter')?.value || '';
                    const tabFilter = tab.dataset.tab || '';
                    console.log('[Events Manager] Tab clicked:', tabFilter);
                    if (typeof window.loadEventsFragment === 'function') {
                        window.loadEventsFragment(searchValue, sortFilter, tabFilter);
                    }
                });
                console.log('[Events Manager] Tab listener attached for tab:', tab.dataset.tab);
            }
        });
    };
    
    // Function to initialize event form validation and modal - expose to window
    window.initializeEventsPage = function initializeEventsPage() {
        console.log('[Events Manager] === Initializing Events Page ===');
        
        // Reset listener flags to allow re-initialization after fragment reload
        const searchInput = document.getElementById('eventSearchInput');
        const sortFilter = document.getElementById('eventSortFilter');
        const tabs = document.querySelectorAll('.event-tab');
        
        if (searchInput) searchInput.removeAttribute('data-listener-attached');
        if (sortFilter) sortFilter.removeAttribute('data-listener-attached');
        tabs.forEach(tab => tab.removeAttribute('data-listener-attached'));
        
        // Reset last search values to sync with current input values after fragment reload
        // This prevents skipping legitimate requests after fragment reloads
        if (searchInput) {
            lastSearchValue = (searchInput.value || '').trim();
        }
        if (sortFilter) {
            lastSortFilter = (sortFilter.value || '');
        }
        
        // Setup search and filters
        if (typeof window.setupEventSearch === 'function') {
            window.setupEventSearch();
        }
        
        const modal = document.getElementById('createEventModal');
        const openBtn = document.querySelector('.btn-create-event');
        const closeBtn = document.querySelector('.close');

        // Open modal
        if (openBtn) {
            // Remove existing listener to avoid duplicates
            const existingHandler = openBtn._modalHandler;
            if (existingHandler) {
                openBtn.removeEventListener('click', existingHandler);
            }
            
            function handleOpenModal() {
                console.log('[Events Manager] Opening modal');
                if (modal) {
                    modal.style.display = 'block';
                    document.body.style.overflow = 'hidden';
                    // Setup form validation when modal opens
                    setupFormValidation();
                }
            }
            
            openBtn._modalHandler = handleOpenModal;
            openBtn.addEventListener('click', handleOpenModal);
        }

        // Close modal
        function closeModal() {
            if (modal) {
                modal.style.display = 'none';
                document.body.style.overflow = 'auto';
            }
        }

        if (closeBtn) {
            const existingCloseHandler = closeBtn._closeHandler;
            if (existingCloseHandler) {
                closeBtn.removeEventListener('click', existingCloseHandler);
            }
            closeBtn._closeHandler = closeModal;
            closeBtn.addEventListener('click', closeModal);
        }

        // Close modal when clicking outside
        if (modal) {
            const existingModalHandler = modal._modalClickHandler;
            if (existingModalHandler) {
                modal.removeEventListener('click', existingModalHandler);
            }
            
            function handleModalClick(event) {
                if (event.target === modal) {
                    closeModal();
                }
            }
            
            modal._modalClickHandler = handleModalClick;
            modal.addEventListener('click', handleModalClick);
        }

        // Setup form validation using event delegation
        function setupFormValidation() {
            console.log('[Events Manager] Setting up form validation...');
            
            // Use event delegation on document to catch form submit anywhere
            // Remove existing listener first to avoid duplicates
            const existingSubmitHandler = document._formSubmitHandler;
            if (existingSubmitHandler) {
                document.removeEventListener('submit', existingSubmitHandler, true);
            }
            
            function handleFormSubmitDelegate(e) {
                // Only handle form submit from createEventModal
                const form = e.target;
                if (!form || form.closest('#createEventModal') === null) {
                    return; // Not our form, let it submit normally
                }
                
                console.log('[Events Manager] Form submit from createEventModal detected');
                handleFormSubmit(e);
            }
            
            document._formSubmitHandler = handleFormSubmitDelegate;
            document.addEventListener('submit', handleFormSubmitDelegate, true);
        }

        function handleFormSubmit(e) {
            console.log('[Events Manager] === Form submit event triggered ===');
            
            // Find inputs by name attribute (Thymeleaf converts th:field to name)
            const form = e.target;
            const title = form.querySelector('input[name="title"]');
            const enrollDeadline = form.querySelector('input[name="enrollDeadline"]');
            const startsAt = form.querySelector('input[name="startsAt"]');
            const endsAt = form.querySelector('input[name="endsAt"]');

            console.log('[Events Manager] Found inputs:', {
                title: !!title,
                enrollDeadline: !!enrollDeadline,
                startsAt: !!startsAt,
                endsAt: !!endsAt,
                titleValue: title?.value,
                enrollDeadlineValue: enrollDeadline?.value,
                startsAtValue: startsAt?.value,
                endsAtValue: endsAt?.value
            });

            // Validate title
            if (!title || !title.value.trim()) {
                e.preventDefault();
                e.stopPropagation();
                alert('Vui lòng nhập tên sự kiện');
                if (title) title.focus();
                return false;
            }

            // Validate startsAt (required)
            if (!startsAt || !startsAt.value) {
                e.preventDefault();
                e.stopPropagation();
                alert('Vui lòng chọn ngày và giờ bắt đầu');
                if (startsAt) startsAt.focus();
                return false;
            }

            // Validate enrollDeadline < startsAt
            if (enrollDeadline && enrollDeadline.value && startsAt && startsAt.value) {
                const enrollDeadlineDate = new Date(enrollDeadline.value);
                const startsAtDate = new Date(startsAt.value);
                
                console.log('[Events Manager] Checking enrollDeadline < startsAt:', {
                    enrollDeadline: enrollDeadline.value,
                    startsAt: startsAt.value,
                    enrollDeadlineDate: enrollDeadlineDate,
                    startsAtDate: startsAtDate,
                    isValid: enrollDeadlineDate < startsAtDate
                });
                
                if (enrollDeadlineDate >= startsAtDate) {
                    e.preventDefault();
                    e.stopPropagation();
                    alert('Hạn đăng ký sự kiện phải nhỏ hơn ngày giờ bắt đầu');
                    enrollDeadline.focus();
                    return false;
                }
            }

            // Validate startsAt < endsAt
            if (endsAt && endsAt.value && startsAt && startsAt.value) {
                const startsAtDate = new Date(startsAt.value);
                const endsAtDate = new Date(endsAt.value);
                
                console.log('[Events Manager] Checking startsAt < endsAt:', {
                    startsAt: startsAt.value,
                    endsAt: endsAt.value,
                    startsAtDate: startsAtDate,
                    endsAtDate: endsAtDate,
                    isValid: startsAtDate < endsAtDate
                });
                
                if (startsAtDate >= endsAtDate) {
                    e.preventDefault();
                    e.stopPropagation();
                    alert('Ngày giờ bắt đầu phải nhỏ hơn ngày giờ kết thúc');
                    startsAt.focus();
                    return false;
                }
            }

            // Validate enrollDeadline < endsAt (if both are provided)
            if (enrollDeadline && enrollDeadline.value && endsAt && endsAt.value) {
                const enrollDeadlineDate = new Date(enrollDeadline.value);
                const endsAtDate = new Date(endsAt.value);
                
                console.log('[Events Manager] Checking enrollDeadline < endsAt:', {
                    enrollDeadline: enrollDeadline.value,
                    endsAt: endsAt.value,
                    enrollDeadlineDate: enrollDeadlineDate,
                    endsAtDate: endsAtDate,
                    isValid: enrollDeadlineDate < endsAtDate
                });
                
                if (enrollDeadlineDate >= endsAtDate) {
                    e.preventDefault();
                    e.stopPropagation();
                    alert('Hạn đăng ký sự kiện phải nhỏ hơn ngày giờ kết thúc');
                    enrollDeadline.focus();
                    return false;
                }
            }

            console.log('[Events Manager] ✅ Form validation passed, allowing form to submit...');
            // If validation passes, allow form to submit normally
            return true;
        }

        // Setup form validation immediately
        setupFormValidation();
        
        console.log('[Events Manager] ✅ Events page initialized successfully');
    };
    
    // Log that functions are exposed
    console.log('[Events Manager] Functions exposed to window:', {
        initializeEventsPage: typeof window.initializeEventsPage,
        setupEventSearch: typeof window.setupEventSearch,
        loadEventsFragment: typeof window.loadEventsFragment
    });
})();

