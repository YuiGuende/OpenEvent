// Check-in List Manager with Realtime Updates
class CheckInListManager {
    constructor(eventId) {
        this.eventId = eventId;
        this.checkInData = [];
        this.filteredData = [];
        this.updateInterval = null;
        this.searchDebounceTimer = null;
        this.isLoading = false;
        
        this.init();
    }

    init() {
        console.log('🎯 Initializing Check-in List Manager for event:', this.eventId);
        
        // Setup search input
        const searchInput = document.getElementById('checkinSearchInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.handleSearch(e.target.value);
            });
        }

        // Setup refresh button
        const refreshBtn = document.getElementById('refreshBtn');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => {
                this.refreshData();
            });
        }

        // Load initial data
        this.loadData();

        // Start realtime updates (every 5 seconds)
        this.startRealtimeUpdates();
    }

    async loadData() {
        if (this.isLoading) return;
        
        this.isLoading = true;
        this.showLoading();

        try {
            const response = await fetch(`/events/${this.eventId}/attendances`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            this.checkInData = data || [];
            
            console.log('✅ Loaded check-in data:', this.checkInData.length, 'records');
            
            // Apply current search filter
            this.applySearchFilter();
            
            // Update stats
            this.updateStats();
            
            // Render table
            this.renderTable();
            
        } catch (error) {
            console.error('❌ Error loading check-in data:', error);
            this.showError('Không thể tải dữ liệu check-in: ' + error.message);
        } finally {
            this.isLoading = false;
            this.hideLoading();
        }
    }

    handleSearch(searchTerm) {
        // Debounce search
        clearTimeout(this.searchDebounceTimer);
        
        this.searchDebounceTimer = setTimeout(() => {
            this.applySearchFilter(searchTerm);
            this.renderTable();
        }, 300); // 300ms debounce
    }

    applySearchFilter(searchTerm = null) {
        const searchInput = document.getElementById('checkinSearchInput');
        const term = (searchTerm || searchInput?.value || '').toLowerCase().trim();

        if (!term) {
            this.filteredData = [...this.checkInData];
        } else {
            this.filteredData = this.checkInData.filter(attendance => {
                // Search in order ID (mã vé) - từ DTO
                const orderId = (attendance.orderId || '').toString();
                
                // Search in full name
                const fullName = (attendance.fullName || '').toLowerCase();
                
                // Search in email
                const email = (attendance.email || '').toLowerCase();
                
                // Search in phone
                const phone = (attendance.phone || '').toLowerCase();

                return orderId.includes(term) ||
                       fullName.includes(term) ||
                       email.includes(term) ||
                       phone.includes(term);
            });
        }

        // Update search results count
        this.updateSearchResultsCount();
    }

    updateSearchResultsCount() {
        const countEl = document.getElementById('searchResultsCount');
        const searchInput = document.getElementById('checkinSearchInput');
        const term = searchInput?.value?.trim() || '';

        if (countEl) {
            if (term) {
                countEl.textContent = `Tìm thấy ${this.filteredData.length} kết quả cho "${term}"`;
            } else {
                countEl.textContent = `Tổng cộng ${this.filteredData.length} người`;
            }
        }
    }

    updateStats() {
        const total = this.checkInData.length;
        const checkedIn = this.checkInData.filter(a => 
            a.checkInTime != null || a.status === 'CHECKED_IN'
        ).length;
        const checkedOut = this.checkInData.filter(a => 
            a.checkOutTime != null || a.status === 'CHECKED_OUT'
        ).length;
        const present = this.checkInData.filter(a => 
            (a.checkInTime != null || a.status === 'CHECKED_IN') && 
            (a.checkOutTime == null && a.status !== 'CHECKED_OUT')
        ).length;

        // Update stat values with animation
        this.animateValue('statTotal', total);
        this.animateValue('statCheckedIn', checkedIn);
        this.animateValue('statCheckedOut', checkedOut);
        this.animateValue('statPresent', present);
    }

    animateValue(elementId, targetValue) {
        const element = document.getElementById(elementId);
        if (!element) return;

        const currentValue = parseInt(element.textContent) || 0;
        const increment = targetValue > currentValue ? 1 : -1;
        const duration = 500; // 500ms
        const steps = Math.abs(targetValue - currentValue);
        const stepDuration = duration / steps;

        let current = currentValue;
        const timer = setInterval(() => {
            current += increment;
            element.textContent = current;
            
            if ((increment > 0 && current >= targetValue) || 
                (increment < 0 && current <= targetValue)) {
                element.textContent = targetValue;
                clearInterval(timer);
            }
        }, stepDuration);
    }

    renderTable() {
        const tbody = document.getElementById('checkinTableBody');
        const table = document.getElementById('checkinTable');
        const emptyState = document.getElementById('emptyState');

        if (!tbody) return;

        tbody.innerHTML = '';

        if (this.filteredData.length === 0) {
            if (table) table.style.display = 'none';
            if (emptyState) emptyState.style.display = 'block';
            return;
        }

        if (table) table.style.display = 'table';
        if (emptyState) emptyState.style.display = 'none';

        this.filteredData.forEach((attendance, index) => {
            const row = this.createTableRow(attendance, index + 1);
            tbody.appendChild(row);
        });
    }

    createTableRow(attendance, index) {
        const row = document.createElement('tr');

        // Get info from DTO
        const orderId = attendance.orderId || 'N/A';
        const fullName = attendance.fullName || 'N/A';
        const email = attendance.email || 'N/A';
        const phone = attendance.phone || 'N/A';

        // Format check-in time
        const checkInTime = attendance.checkInTime 
            ? this.formatDateTime(attendance.checkInTime)
            : '-';

        // Format check-out time
        const checkOutTime = attendance.checkOutTime 
            ? this.formatDateTime(attendance.checkOutTime)
            : '-';

        // Determine status
        let statusClass = 'status-pending';
        let statusText = 'Chờ check-in';
        let statusIcon = '⏳';

        if (attendance.checkOutTime != null || attendance.status === 'CHECKED_OUT') {
            statusClass = 'status-checked-out';
            statusText = 'Đã check-out';
            statusIcon = '🚪';
        } else if (attendance.checkInTime != null || attendance.status === 'CHECKED_IN') {
            statusClass = 'status-checked-in';
            statusText = 'Đã check-in';
            statusIcon = '✅';
        }

        row.innerHTML = `
            <td>${index}</td>
            <td><strong>#${orderId}</strong></td>
            <td>${this.escapeHtml(fullName)}</td>
            <td>${this.escapeHtml(email)}</td>
            <td>${this.escapeHtml(phone)}</td>
            <td>${checkInTime}</td>
            <td>${checkOutTime}</td>
            <td>
                <span class="checkin-status ${statusClass}">
                    <span>${statusIcon}</span>
                    <span>${statusText}</span>
                </span>
            </td>
        `;

        return row;
    }

    formatDateTime(dateTimeString) {
        if (!dateTimeString) return '-';
        
        try {
            const date = new Date(dateTimeString);
            const day = String(date.getDate()).padStart(2, '0');
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const year = date.getFullYear();
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');
            
            return `${day}/${month}/${year} ${hours}:${minutes}`;
        } catch (e) {
            return dateTimeString;
        }
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    showLoading() {
        const loadingState = document.getElementById('loadingState');
        const table = document.getElementById('checkinTable');
        const emptyState = document.getElementById('emptyState');
        
        if (loadingState) loadingState.style.display = 'block';
        if (table) table.style.display = 'none';
        if (emptyState) emptyState.style.display = 'none';
    }

    hideLoading() {
        const loadingState = document.getElementById('loadingState');
        if (loadingState) loadingState.style.display = 'none';
    }

    showError(message) {
        const emptyState = document.getElementById('emptyState');
        if (emptyState) {
            emptyState.innerHTML = `
                <div class="empty-state-icon">⚠️</div>
                <h3>Lỗi</h3>
                <p>${message}</p>
            `;
            emptyState.style.display = 'block';
        }
    }

    refreshData() {
        const refreshBtn = document.getElementById('refreshBtn');
        if (refreshBtn) {
            refreshBtn.classList.add('spinning');
        }
        
        this.loadData().finally(() => {
            if (refreshBtn) {
                refreshBtn.classList.remove('spinning');
            }
        });
    }

    startRealtimeUpdates() {
        // Update every 5 seconds
        this.updateInterval = setInterval(() => {
            this.loadData();
        }, 5000);
    }

    stopRealtimeUpdates() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.updateInterval = null;
        }
    }

    destroy() {
        this.stopRealtimeUpdates();
        clearTimeout(this.searchDebounceTimer);
    }
}

// Initialize when page loads
let checkInListManager = null;

function initializeCheckInList() {
    const container = document.querySelector('.checkin-list-container');
    if (!container) {
        console.warn('⚠️ Check-in list container not found');
        return;
    }

    const eventId = container.getAttribute('data-event-id');
    if (!eventId) {
        console.error('❌ Event ID not found');
        return;
    }

    // Cleanup previous instance
    if (checkInListManager) {
        checkInListManager.destroy();
    }

    // Create new instance
    checkInListManager = new CheckInListManager(eventId);
    window.checkInListManager = checkInListManager; // For debugging
}

// Auto-initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeCheckInList);
} else {
    initializeCheckInList();
}

// Export for manual initialization
window.initializeCheckInList = initializeCheckInList;
window.refreshCheckInList = function() {
    if (checkInListManager) {
        checkInListManager.refreshData();
    }
};

