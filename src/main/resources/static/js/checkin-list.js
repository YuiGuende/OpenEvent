// Check-in List Manager with Realtime Updates
class CheckInListManager {
    constructor(eventId) {
        this.eventId = eventId;
        this.checkInData = [];
        this.filteredData = [];
        this.previousDataHash = null; // Store hash of previous data to detect changes
        this.updateInterval = null;
        this.searchDebounceTimer = null;
        this.isLoading = false;
        this.realtimeEnabled = true; // Default to enabled
        this.updateIntervalMs = 10000; // 10 seconds instead of 5
        this.isFirstRender = true; // Flag to track first render after initialization
        
        this.init();
    }

    init() {
        console.log('🎯 Initializing Check-in List Manager for event:', this.eventId);
        
        // Verify required elements exist
        const container = document.querySelector('.checkin-list-container');
        if (!container) {
            console.error('❌ Check-in list container not found!');
            return;
        }
        
        // Setup search input
        const searchInput = document.getElementById('checkinSearchInput');
        if (searchInput) {
            console.log('✅ Search input found');
            searchInput.addEventListener('input', (e) => {
                this.handleSearch(e.target.value);
            });
        } else {
            console.warn('⚠️ Search input not found');
        }

        // Setup refresh button
        const refreshBtn = document.getElementById('refreshBtn');
        if (refreshBtn) {
            console.log('✅ Refresh button found');
            refreshBtn.addEventListener('click', () => {
                this.refreshData();
            });
        } else {
            console.warn('⚠️ Refresh button not found');
        }

        // Setup realtime toggle button
        const realtimeToggleBtn = document.getElementById('realtimeToggleBtn');
        if (realtimeToggleBtn) {
            console.log('✅ Realtime toggle button found');
            realtimeToggleBtn.addEventListener('click', () => {
                this.toggleRealtime();
            });
            this.updateRealtimeToggleUI();
        } else {
            console.warn('⚠️ Realtime toggle button not found');
        }

        // Load initial data
        console.log('📥 Loading initial data...');
        this.loadData();

        // Start realtime updates (if enabled)
        if (this.realtimeEnabled) {
            this.startRealtimeUpdates();
            console.log(`🔄 Realtime updates started (every ${this.updateIntervalMs/1000} seconds)`);
        }
    }

    async loadData() {
        if (this.isLoading) {
            console.log('⏳ Already loading, skipping...');
            return;
        }
        
        this.isLoading = true;
        this.showLoading();

        try {
            console.log(`📡 Fetching attendances for event: ${this.eventId}`);
            const response = await fetch(`/events/${this.eventId}/attendances`);
            
            if (!response.ok) {
                const errorText = await response.text();
                console.error(`❌ HTTP error! status: ${response.status}, body: ${errorText}`);
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            console.log('📦 Raw data received:', data);
            
            const newData = Array.isArray(data) ? data : [];
            
            // Check if data has actually changed (smart update)
            const dataHash = this.getDataHash(newData);
            const hasChanged = dataHash !== this.previousDataHash;
            
            // Always render on first render (isFirstRender = true)
            // Only skip if data hasn't changed AND we've already rendered before AND it's not first render
            if (!hasChanged && this.previousDataHash !== null && !this.isFirstRender) {
                console.log('No changes, skip UI update');

                // FIX: table phải được hiển thị lại nếu filteredData vẫn có dữ liệu
                const table = document.getElementById('checkinTable');
                const emptyState = document.getElementById('emptyState');
                if (this.filteredData.length > 0) {
                    table.style.display = 'table';
                    emptyState.style.display = 'none';
                }

                this.isLoading = false;
                this.hideLoading();
                return;
            }
            
            console.log('✅ Loaded check-in data:', newData.length, 'records', 
                hasChanged ? '(changed)' : (this.isFirstRender ? '(first render)' : '(first load)'));
            
            if (newData.length > 0) {
                console.log('📋 Sample record:', newData[0]);
            }
            
            // Update data first
            this.checkInData = newData;
            
            // Apply current search filter (this will update filteredData)
            this.applySearchFilter();
            
            // Update stats (with smooth animation)
            this.updateStats();
            
            // Render table (with smooth update)
            console.log('🎨 Rendering table with', this.filteredData.length, 'filtered records', 
                this.isFirstRender ? '(first render - will clear tbody)' : '');
            this.renderTableSmooth();
            
            // Store hash AFTER successful render
            // Mark first render as complete
            if (this.isFirstRender) {
                this.isFirstRender = false;
                console.log('✅ First render completed');
            }
            this.previousDataHash = dataHash; // Update hash after render
            
        } catch (error) {
            console.error('❌ Error loading check-in data:', error);
            console.error('Error stack:', error.stack);
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
        this.renderTableSmooth();
    }

    renderTableSmooth() {
        const tbody = document.getElementById('checkinTableBody');
        const table = document.getElementById('checkinTable');
        const emptyState = document.getElementById('emptyState');

        if (!tbody) {
            console.warn('⚠️ checkinTableBody not found, cannot render');
            return;
        }
        
        console.log('🎨 renderTableSmooth called with', this.filteredData.length, 'filtered records');

        if (this.filteredData.length === 0) {
            if (table) {
                table.style.opacity = '0';
                setTimeout(() => {
                    table.style.display = 'none';
                }, 200);
            }
            if (emptyState) {
                emptyState.style.display = 'block';
                emptyState.style.opacity = '0';
                setTimeout(() => {
                    emptyState.style.opacity = '1';
                }, 10);
            }
            return;
        }

        // Show table with fade-in
        if (table) {
            table.style.display = 'table';
            table.style.opacity = '0';
            setTimeout(() => {
                table.style.opacity = '1';
            }, 10);
            console.log('✅ Table displayed');
        } else {
            console.warn('⚠️ Table element not found');
        }
        if (emptyState) {
            emptyState.style.display = 'none';
        }

        // On first render after initialization, clear tbody completely
        if (this.isFirstRender) {
            console.log('🆕 First render - clearing tbody and rendering all rows');
            tbody.innerHTML = ''; // Clear all existing rows
            this.isFirstRender = false;
        }
        
        // Smart update: only update changed rows
        const existingRows = Array.from(tbody.children);
        const existingIds = new Set(existingRows.map(row => row.dataset.attendanceId));
        const newIds = new Set(this.filteredData.map(a => a.attendanceId?.toString()));

        // Remove rows that no longer exist
        existingRows.forEach(row => {
            const id = row.dataset.attendanceId;
            if (!newIds.has(id)) {
                row.style.opacity = '0';
                row.style.transform = 'translateX(-20px)';
                setTimeout(() => row.remove(), 300);
            }
        });

        // Update or add rows
        this.filteredData.forEach((attendance, index) => {
            const rowId = attendance.attendanceId?.toString();
            const existingRow = existingRows.find(row => row.dataset.attendanceId === rowId);
            
            if (existingRow && existingRow.parentNode) {
                // Check if row content actually changed
                const existingCheckIn = existingRow.cells[5]?.textContent?.trim() || '';
                const existingCheckOut = existingRow.cells[6]?.textContent?.trim() || '';
                const existingStatus = existingRow.cells[7]?.querySelector('.checkin-status')?.textContent?.trim() || '';
                
                const newCheckIn = attendance.checkInTime ? this.formatDateTime(attendance.checkInTime) : '-';
                const newCheckOut = attendance.checkOutTime ? this.formatDateTime(attendance.checkOutTime) : '-';
                let newStatusText = 'Chờ check-in';
                if (attendance.checkOutTime != null || attendance.status === 'CHECKED_OUT') {
                    newStatusText = 'Đã check-out';
                } else if (attendance.checkInTime != null || attendance.status === 'CHECKED_IN') {
                    newStatusText = 'Đã check-in';
                }
                
                // Only update if content changed
                if (existingCheckIn !== newCheckIn || existingCheckOut !== newCheckOut || existingStatus !== newStatusText) {
                    // Update only changed cells, not entire row
                    if (existingCheckIn !== newCheckIn) {
                        existingRow.cells[5].textContent = newCheckIn;
                        existingRow.cells[5].style.animation = 'highlight 0.5s ease';
                    }
                    if (existingCheckOut !== newCheckOut) {
                        existingRow.cells[6].textContent = newCheckOut;
                        existingRow.cells[6].style.animation = 'highlight 0.5s ease';
                    }
                    if (existingStatus !== newStatusText) {
                        const statusCell = existingRow.cells[7];
                        const statusSpan = statusCell.querySelector('.checkin-status');
                        if (statusSpan) {
                            // Update status smoothly
                            statusSpan.style.opacity = '0.5';
                            setTimeout(() => {
                                let statusClass = 'status-pending';
                                let statusIcon = '⏳';
                                if (attendance.checkOutTime != null || attendance.status === 'CHECKED_OUT') {
                                    statusClass = 'status-checked-out';
                                    statusIcon = '🚪';
                                } else if (attendance.checkInTime != null || attendance.status === 'CHECKED_IN') {
                                    statusClass = 'status-checked-in';
                                    statusIcon = '✅';
                                }
                                statusSpan.className = `checkin-status ${statusClass}`;
                                statusSpan.innerHTML = `<span>${statusIcon}</span><span>${newStatusText}</span>`;
                                statusSpan.style.opacity = '1';
                            }, 150);
                        }
                    }
                }
                // Update row number if order changed
                if (existingRow.cells[0].textContent !== (index + 1).toString()) {
                    existingRow.cells[0].textContent = index + 1;
                }
            } else {
                // Add new row with animation
                const row = this.createTableRow(attendance, index + 1);
                row.style.opacity = '0';
                row.style.transform = 'translateY(-10px)';
                tbody.appendChild(row);
                console.log('➕ Added new row for attendance:', attendance.attendanceId);
                setTimeout(() => {
                    row.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
                    row.style.opacity = '1';
                    row.style.transform = 'translateY(0)';
                }, 10);
            }
        });
        
        console.log('✅ Finished rendering table, total rows:', tbody.children.length);
    }

    createTableRow(attendance, index) {
        const row = document.createElement('tr');
        row.dataset.attendanceId = attendance.attendanceId?.toString() || '';
        row.style.transition = 'opacity 0.3s ease, transform 0.3s ease';

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

    getDataHash(data) {
        // Create a simple hash of the data to detect changes
        // Only hash essential fields that would indicate a change
        const hashString = data.map(a => 
            `${a.attendanceId}-${a.checkInTime || ''}-${a.checkOutTime || ''}-${a.status || ''}`
        ).sort().join('|');
        
        // Simple hash function
        let hash = 0;
        for (let i = 0; i < hashString.length; i++) {
            const char = hashString.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash; // Convert to 32bit integer
        }
        return hash.toString();
    }

    toggleRealtime() {
        this.realtimeEnabled = !this.realtimeEnabled;
        this.updateRealtimeToggleUI();
        
        if (this.realtimeEnabled) {
            this.startRealtimeUpdates();
            console.log('✅ Realtime updates enabled');
        } else {
            this.stopRealtimeUpdates();
            console.log('⏸️ Realtime updates disabled');
        }
    }

    updateRealtimeToggleUI() {
        const toggleBtn = document.getElementById('realtimeToggleBtn');
        const toggleText = document.getElementById('realtimeToggleText');
        const indicator = document.getElementById('realtimeIndicator');
        
        if (toggleBtn && toggleText) {
            if (this.realtimeEnabled) {
                toggleBtn.classList.add('active');
                toggleText.textContent = 'Auto-update: ON';
                if (indicator) indicator.style.display = 'inline-flex';
            } else {
                toggleBtn.classList.remove('active');
                toggleText.textContent = 'Auto-update: OFF';
                if (indicator) indicator.style.display = 'none';
            }
        }
    }

    startRealtimeUpdates() {
        // Stop existing interval if any
        this.stopRealtimeUpdates();
        
        // Update every 10 seconds (less frequent, more professional)
        this.updateInterval = setInterval(() => {
            if (!this.isLoading) {
                this.loadData();
            }
        }, this.updateIntervalMs);
    }

    stopRealtimeUpdates() {
        if (this.updateInterval) {
            console.log('🛑 Stopping realtime updates...');
            clearInterval(this.updateInterval);
            this.updateInterval = null;
            console.log('✅ Realtime updates stopped');
        }
    }

    destroy() {
        console.log('🧹 Destroying check-in list manager...');
        this.stopRealtimeUpdates();
        clearTimeout(this.searchDebounceTimer);
    }
}

// Initialize when page loads
let checkInListManager = null;
let isInitializing = false;

function initializeCheckInList() {
    // Prevent multiple simultaneous initializations
    if (isInitializing) {
        console.log('⏳ Check-in list initialization already in progress, skipping...');
        return;
    }

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

    // Always re-initialize when fragment is loaded (SPA routing)
    // The container is always new when fragment loads, so we need fresh instance
    // Note: Router.js should have already destroyed the instance, but we check again to be safe
    if (checkInListManager) {
        console.log('🧹 Cleaning up previous instance before re-initializing...');
        try {
            checkInListManager.destroy();
        } catch (e) {
            console.warn('⚠️ Error destroying previous instance:', e);
        }
        checkInListManager = null;
        window.checkInListManager = null; // Also clear from window
    }

    isInitializing = true;

    try {

        // Create new instance
        console.log('🆕 Creating new check-in list instance for event:', eventId);
        // Verify container still exists before creating instance
        const verifyContainer = document.querySelector('.checkin-list-container');
        if (!verifyContainer) {
            console.error('❌ Container not found after delay, cannot initialize');
            return;
        }
        checkInListManager = new CheckInListManager(eventId);
        window.checkInListManager = checkInListManager; // For debugging
        console.log('✅ Check-in list initialized successfully');
    } catch (error) {
        console.error('❌ Error initializing check-in list:', error);
        console.error('Error stack:', error.stack);
    } finally {
        isInitializing = false;
    }
}

// Auto-initialize when DOM is ready (only once)
// NOTE: This will be called automatically, but host.js will also call it when fragment loads
// The initializeCheckInList function has guards to prevent multiple initializations
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        // Only auto-initialize if we're on a page with check-in list
        const container = document.querySelector('.checkin-list-container');
        if (container) {
            console.log('🔄 Auto-initializing check-in list from DOMContentLoaded');
            initializeCheckInList();
        }
    }, { once: true });
} else {
    // Small delay to ensure DOM is fully ready
    setTimeout(() => {
        const container = document.querySelector('.checkin-list-container');
        if (container) {
            console.log('🔄 Auto-initializing check-in list (DOM already ready)');
            initializeCheckInList();
        }
    }, 100);
}

// Export for manual initialization
window.initializeCheckInList = initializeCheckInList;
window.refreshCheckInList = function() {
    if (checkInListManager) {
        checkInListManager.refreshData();
    }
};

