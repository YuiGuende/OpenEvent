// ================== Event Form Management ==================

// Function to handle event type selection (called from HTML onclick)
window.selectEventType = function(eventType) {
    console.log('selectEventType called with:', eventType);
    
    // Remove active class from all tabs
    const typeTabs = document.querySelectorAll('.event-type-tab');
    typeTabs.forEach(tab => tab.classList.remove('active'));
    
    // Add active class to clicked tab
    const clickedTab = Array.from(typeTabs).find(tab => {
        const tabText = tab.textContent.trim();
        switch(eventType) {
            case 'MUSIC': return tabText === 'Music';
            case 'WORKSHOP': return tabText === 'Workshop';
            case 'FESTIVAL': return tabText === 'Festival';
            case 'COMPETITION': return tabText === 'Competition';
            case 'OTHER': return tabText === 'Other';
            default: return false;
        }
    });
    
    if (clickedTab) {
        clickedTab.classList.add('active');
    }
    
    // Toggle fields based on event type
    const toggleField = (id, show) => {
        const el = document.getElementById(id);
        if (el) el.style.display = show ? 'block' : 'none';
    };
    
    // Hide all fields first
    toggleField('musicFields', false);
    toggleField('festivalFields', false);
    toggleField('competitionFields', false);
    toggleField('workshopFields', false);
    
    // Show relevant fields based on event type
    switch(eventType) {
        case 'MUSIC':
            toggleField('musicFields', true);
            break;
        case 'FESTIVAL':
            toggleField('festivalFields', true);
            break;
        case 'COMPETITION':
            toggleField('competitionFields', true);
            break;
        case 'WORKSHOP':
            toggleField('workshopFields', true);
            break;
        case 'OTHER':
            // No specific fields for OTHER type
            break;
    }
    
    console.log('Tab clicked:', eventType);
};

// Function to initialize event type tabs (for both create and update pages)
window.initializeEventTypeTabs = function() {
    console.log('Initializing event type tabs...');
    
    const typeTabs = document.querySelectorAll('.event-type-tab');
    console.log('Event type tabs found:', typeTabs.length);
    
    if (typeTabs.length === 0) {
        console.log('No event type tabs found');
        return;
    }
    
    // Set initial state based on active tab
    const setInitialTabState = () => {
        const activeTab = document.querySelector('.event-type-tab.active');
        if (activeTab) {
            const type = activeTab.textContent.trim();
            console.log('Setting initial state for tab:', type);
            
            // Convert tab text to event type and call selectEventType
            let eventType = '';
            switch(type) {
                case 'Music': eventType = 'MUSIC'; break;
                case 'Workshop': eventType = 'WORKSHOP'; break;
                case 'Festival': eventType = 'FESTIVAL'; break;
                case 'Competition': eventType = 'COMPETITION'; break;
                case 'Other': eventType = 'OTHER'; break;
            }
            
            if (eventType) {
                // Call selectEventType to set up the fields
                window.selectEventType(eventType);
            }
        }
    };

    // Set initial state immediately
    setInitialTabState();

    // Add click listeners that use selectEventType
    typeTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const type = tab.textContent.trim();
            console.log('Tab clicked:', type);
            
            // Convert tab text to event type and call selectEventType
            let eventType = '';
            switch(type) {
                case 'Music': eventType = 'MUSIC'; break;
                case 'Workshop': eventType = 'WORKSHOP'; break;
                case 'Festival': eventType = 'FESTIVAL'; break;
                case 'Competition': eventType = 'COMPETITION'; break;
                case 'Other': eventType = 'OTHER'; break;
            }
            
            if (eventType) {
                window.selectEventType(eventType);
            }
        });
    });
};

// Function to initialize all event form listeners (for create event page)
function initializeCreateEventFormListeners() {
    // Only run on create event page, not update event page
    if (document.getElementById('posterUploadBtn') || document.getElementById('galleryUploadBtn')) {
        console.log('Skipping create event form listeners - update event page detected');
        return;
    }
    
    // Upload functionality
    const uploadBtn = document.getElementById('uploadBtn');
    const uploadInput = document.getElementById('uploadInput');
    const preview = document.getElementById('preview');

    if (uploadBtn && uploadInput) {
        uploadBtn.addEventListener('click', () => uploadInput.click());
        
        uploadInput.addEventListener('change', (e) => {
            preview.innerHTML = '';
            [...e.target.files].forEach(file => {
                if (file.type.startsWith('image/')) {
                    const img = document.createElement('img');
                    img.src = URL.createObjectURL(file);
                    Object.assign(img.style, {
                        maxWidth: '120px',
                        margin: '8px',
                        borderRadius: '8px'
                    });
                    preview.appendChild(img);
                }
            });
        });
    }

    // Event title click to show form (only for create event page)
    const eventTitle = document.querySelector('.event-title');
    if (eventTitle && !document.getElementById('posterUploadBtn')) {
        eventTitle.addEventListener('click', () => {
            const form = document.getElementById('eventOverviewForm');
            if (form) form.style.display = 'block';
        });
    }

    // Summary character count
    const summaryInput = document.getElementById('eventSummaryInput');
    const summaryCount = document.getElementById('summaryCount');
    if (summaryInput && summaryCount) {
        summaryInput.addEventListener('input', (e) => {
            summaryCount.textContent = e.target.value.length;
        });
    }

    // Benefits character count
    const benefitsInput = document.getElementById('benefitsInput');
    const benefitsCount = document.getElementById('benefitsCount');
    if (benefitsInput && benefitsCount) {
        benefitsInput.addEventListener('input', (e) => {
            benefitsCount.textContent = e.target.value.length;
        });
    }

    // Learning Objects character count
    const learningObjectsInput = document.getElementById('learningObjectsInput');
    const learningObjectsCount = document.getElementById('learningObjectsCount');
    if (learningObjectsInput && learningObjectsCount) {
        learningObjectsInput.addEventListener('input', (e) => {
            learningObjectsCount.textContent = e.target.value.length;
        });
    }

    // Event type tabs - use the centralized function
    if (typeof window.initializeEventTypeTabs === 'function') {
        window.initializeEventTypeTabs();
    }

    // Lineup functionality
    const addLineupBtn = document.getElementById('addLineupBtn');
    if (addLineupBtn) {
        addLineupBtn.addEventListener('click', (e) => {
            e.preventDefault();
            if (typeof addLineupSection === 'function') {
                addLineupSection();
            }
        });
    }

    // Agenda functionality
    const addAgendaBtn = document.getElementById('addAgendaBtn');
    if (addAgendaBtn) {
        addAgendaBtn.addEventListener('click', (e) => {
            e.preventDefault();
            if (typeof renderAgendaForm === 'function') {
                renderAgendaForm(document.getElementById('agendaSections'));
            }
        });
    }
}

// Initialize create event form when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Only initialize if we're on create event page
    if (document.getElementById('uploadBtn') && document.getElementById('uploadInput')) {
        initializeCreateEventFormListeners();
    }
});

// Function to show update event form inline
// function showUpdateEventInline() {
//     const contentContainer = document.getElementById('content-container');
//
//     // Show loading state
//     contentContainer.innerHTML = '<div class="text-center"><i class="fas fa-spinner fa-spin"></i> Đang tải...</div>';
//
//     // Load fragment via AJAX
//     fetch('/fragments/update-event')
//         .then(response => {
//             if (!response.ok) {
//                 throw new Error('Failed to load fragment');
//             }
//             return response.text();
//         })
//         .then(html => {
//             contentContainer.innerHTML = html;
//
//             // Highlight the settings nav item
//             document.getElementById('settingsNavItem').classList.add('active');
//
//             // Remove active from other nav items
//             document.querySelectorAll('.nav-item').forEach(item => {
//                 if (item.id !== 'settingsNavItem') {
//                     item.classList.remove('active');
//                 }
//             });
//
//             // Update breadcrumb
//             updateBreadcrumb('Cài đặt sự kiện');
//
//             // Initialize event listeners for the new content
//             initializeEventFormListeners();
//
//             // Initialize image upload manager
//             if (typeof checkAndInitializeUpload === 'function') {
//                 // Retry initialization if elements not found
//                 let retryCount = 0;
//                 const maxRetries = 5;
//                 const retryInterval = setInterval(() => {
//                     if (checkAndInitializeUpload() || retryCount >= maxRetries) {
//                         clearInterval(retryInterval);
//                     }
//                     retryCount++;
//                 }, 200);
//             }
//         })
//         .catch(error => {
//             console.error('Error loading event form:', error);
//             console.log('Falling back to inline HTML...');
//
//             // Fallback to inline HTML
//             contentContainer.innerHTML = getInlineEventFormHTML();
//
//             // Highlight the settings nav item
//             document.getElementById('settingsNavItem').classList.add('active');
//
//             // Remove active from other nav items
//             document.querySelectorAll('.nav-item').forEach(item => {
//                 if (item.id !== 'settingsNavItem') {
//                     item.classList.remove('active');
//                 }
//             });
//
//             // Update breadcrumb
//             updateBreadcrumb('Cài đặt sự kiện');
//
//             // Initialize event listeners for the new content
//             initializeEventFormListeners();
//
//             // Initialize image upload manager
//             if (typeof checkAndInitializeUpload === 'function') {
//                 // Retry initialization if elements not found
//                 let retryCount = 0;
//                 const maxRetries = 5;
//                 const retryInterval = setInterval(() => {
//                     if (checkAndInitializeUpload() || retryCount >= maxRetries) {
//                         clearInterval(retryInterval);
//                     }
//                     retryCount++;
//                 }, 200);
//             }
//         });
// }
//
// // Function to show update event form
// function showUpdateEventForm() {
//     // Directly show inline content without trying to load fragment
//     showUpdateEventInline();
// }

// Function to update breadcrumb
function updateBreadcrumb(currentPage) {
    const breadcrumb = document.querySelector('.breadcrumb');
    breadcrumb.innerHTML = `
        <li class="breadcrumb-item">
            <a href="#"><i class="bi bi-house-door"></i> Trang chủ</a>
        </li>
        <li class="breadcrumb-item">
            <a href="#">duc le</a>
        </li>
        <li class="breadcrumb-item active">${currentPage}</li>
    `;
}
