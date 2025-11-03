// ================== Getting Started Page Management ==================

// Function to show default content

// Function to attach event listeners
function attachEventListeners() {
    const setupEventBtn = document.getElementById('setupEventBtn');
    if (setupEventBtn) {
        setupEventBtn.addEventListener('click', showUpdateEventForm);
    }
    
    const settingsNavLink = document.getElementById('settingsNavLink');
    if (settingsNavLink) {
        settingsNavLink.addEventListener('click', showUpdateEventForm);
    }
}

// Load fragments function (kept for compatibility)
async function loadFragment(url, containerId) {
    try {
        const response = await fetch(url);
        const html = await response.text();
        document.getElementById(containerId).innerHTML = html;
    } catch (error) {
        console.error('Error loading fragment:', error);
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    // Show default content
    showDefaultContent();
    
    // Initialize sidebar
    if (typeof initializeSidebar === 'function') {
        initializeSidebar();
    }
});
