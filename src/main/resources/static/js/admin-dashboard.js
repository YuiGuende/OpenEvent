// Admin Dashboard JavaScript
let allAuditLogs = [];
let chartInstances = {};

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    console.log('[DEBUG] DOMContentLoaded - Initializing dashboard...');
    console.log('[DEBUG] Checking if showPage exists:', typeof window.showPage);
    loadDashboardData();
});

// Load all dashboard data
function loadDashboardData() {
    fetch('/admin/api/stats?period=month')
        .then(response => response.json())
        .then(data => {
            console.log("[v0] Dashboard data loaded:", data);
            updateKPIs(data);
            initCharts(data);
            loadAuditLogs();
            loadPayoutData();
            loadTopEvents();
        })
        .catch(error => console.error('Error loading dashboard data:', error));
}

// Update KPI Cards
function updateKPIs(data) {
    document.getElementById('totalRevenue').textContent = formatCurrency(data.totalSystemRevenue);
    document.getElementById('activeEvents').textContent = data.totalActiveEvents;
    document.getElementById('newUsers').textContent = data.totalNewUsers;
    document.getElementById('pendingRequests').textContent = data.pendingApprovalRequests;

    const trendElement = document.getElementById('revenueTrend');
    if (data.revenueChangePercent >= 0) {
        trendElement.className = 'trend up';
        trendElement.innerHTML = `<i class="fas fa-arrow-up"></i> ${data.revenueChangePercent.toFixed(1)}%`;
    } else {
        trendElement.className = 'trend down';
        trendElement.innerHTML = `<i class="fas fa-arrow-down"></i> ${data.revenueChangePercent.toFixed(1)}%`;
    }
}

// Initialize Charts
function initCharts(data) {
    // Revenue Overview Chart
    if (chartInstances.revenueOverview) chartInstances.revenueOverview.destroy();
    const revenueCtx = document.getElementById('revenueOverviewChart');
    if (revenueCtx) {
        chartInstances.revenueOverview = new Chart(revenueCtx.getContext('2d'), {
            type: 'bar',
            data: {
                labels: data.revenueByMonth.map(item => item.month || item.date),
                datasets: [
                    {
                        type: 'bar',
                        label: 'Revenue (Bar)',
                        data: data.revenueByMonth.map(item => item.revenue),
                        backgroundColor: 'rgba(44, 123, 229, 0.6)',
                        borderColor: 'rgba(44, 123, 229, 1)',
                        borderWidth: 1
                    },
                    {
                        type: 'line',
                        label: 'Revenue (Line)',
                        data: data.revenueByMonth.map(item => item.revenue),
                        borderColor: '#ff6b35',
                        backgroundColor: 'rgba(255, 107, 53, 0.1)',
                        borderWidth: 3,
                        fill: false,
                        tension: 0.4
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: true } },
                scales: { y: { beginAtZero: true } }
            }
        });
    }

    // Event Type Chart
    if (chartInstances.eventType) chartInstances.eventType.destroy();
    const eventTypeCtx = document.getElementById('eventTypeChart');
    if (eventTypeCtx && data.eventsByType && data.eventsByType[0]) {
        const eventTypeData = data.eventsByType[0];
        chartInstances.eventType = new Chart(eventTypeCtx.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: eventTypeData.labels,
                datasets: [{
                    data: eventTypeData.data,
                    backgroundColor: ['#2c7be5', '#00d4ff', '#0095ff', '#0066cc', '#004999', '#003366']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom' } }
            }
        });
    }

    // Revenue Trend Chart
    if (chartInstances.revenueTrend) chartInstances.revenueTrend.destroy();
    const revenueTrendCtx = document.getElementById('revenueTrendChart');
    if (revenueTrendCtx) {
        chartInstances.revenueTrend = new Chart(revenueTrendCtx.getContext('2d'), {
            type: 'line',
            data: {
                labels: data.revenueByMonth.map(item => item.month || item.date),
                datasets: [{
                    label: 'Revenue',
                    data: data.revenueByMonth.map(item => item.revenue),
                    borderColor: '#2c7be5',
                    backgroundColor: 'rgba(44, 123, 229, 0.1)',
                    borderWidth: 2,
                    fill: true,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: true } },
                scales: { y: { beginAtZero: true } }
            }
        });
    }

    // Event Distribution Chart
    if (chartInstances.eventDist) chartInstances.eventDist.destroy();
    const eventDistCtx = document.getElementById('eventDistributionChart');
    if (eventDistCtx && data.eventsByType && data.eventsByType[0]) {
        const eventTypeData = data.eventsByType[0];
        chartInstances.eventDist = new Chart(eventDistCtx.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: eventTypeData.labels,
                datasets: [{
                    data: eventTypeData.data,
                    backgroundColor: ['#2c7be5', '#00d4ff', '#0095ff', '#0066cc', '#004999', '#003366']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom' } }
            }
        });
    }

    // Approval Rate Chart
    if (chartInstances.approvalRate) chartInstances.approvalRate.destroy();
    const approvalRateCtx = document.getElementById('approvalRateChart');
    if (approvalRateCtx) {
        chartInstances.approvalRate = new Chart(approvalRateCtx.getContext('2d'), {
            type: 'bar',
            data: {
                labels: ['Approved', 'Rejected', 'Pending'],
                datasets: [{
                    label: 'Count',
                    data: [data.approvedEvents || 0, data.rejectedEvents || 0, data.pendingEvents || 0],
                    backgroundColor: ['#2c7be5', '#dc3545', '#ffc107']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true } }
            }
        });
    }

    // Registration Trend Chart
    if (chartInstances.registration) chartInstances.registration.destroy();
    const registrationCtx = document.getElementById('registrationTrendChart');
    if (registrationCtx && data.userRegistrationTrend) {
        const labels = data.userRegistrationTrend.map(item => {
            if (item.month) {
                const parts = item.month.split('-');
                return parts.length === 2 ? `${parts[1]}/${parts[0]}` : item.month;
            } else if (item.day) {
                const parts = item.day.split('-');
                return parts.length === 3 ? `${parts[2]}/${parts[1]}` : item.day;
            }
            return '';
        });
        
        chartInstances.registration = new Chart(registrationCtx.getContext('2d'), {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'New Users',
                    data: data.userRegistrationTrend.map(item => item.users || item.count || 0),
                    backgroundColor: '#2c7be5'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: true } },
                scales: { 
                    y: { beginAtZero: true },
                    x: {
                        ticks: {
                            maxRotation: 45,
                            minRotation: 45
                        }
                    }
                }
            }
        });
    }
}

// Load Audit Logs
function loadAuditLogs() {
    fetch('/admin/api/audit-logs')
        .then(response => response.json())
        .then(data => {
            allAuditLogs = data;
            displayAuditLogs(data.slice(0, 10));
            displayRecentActivity(data.slice(0, 4));
        })
        .catch(error => console.error('Error loading audit logs:', error));
}

// Display Audit Logs
function displayAuditLogs(logs) {
    const tbody = document.getElementById('auditLogBody');
    if (!tbody) return;
    if (logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No data available</td></tr>';
        return;
    }

    tbody.innerHTML = logs.map(log => `
            <tr data-action="${log.actionType}" data-user="${log.actorId}" data-date="${log.createdAt}">
                <td>${new Date(log.createdAt).toLocaleString('vi-VN')}</td>
                <td>${log.actorId || 'N/A'}</td>
                <td><span class="badge badge-warning">${log.actionType}</span></td>
                <td>${log.description || 'N/A'}</td>
            </tr>
        `).join('');
}

// Display Recent Activity
function displayRecentActivity(logs) {
    const tbody = document.getElementById('recentActivityBody');
    if (!tbody) return;
    if (logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No data available</td></tr>';
        return;
    }

    tbody.innerHTML = logs.map(log => `
            <tr>
                <td>${new Date(log.createdAt).toLocaleString('vi-VN')}</td>
                <td>${log.actorId || 'N/A'}</td>
                <td>${log.description || 'N/A'}</td>
                <td><span class="badge badge-success">Completed</span></td>
            </tr>
        `).join('');
}

// Load Payout Data
function loadPayoutData() {
    const payoutData = [
        { hostId: '#H2845', hostName: 'Host A', amount: '145,000,000 VND', date: '25/10/2025', status: 'Completed' },
        { hostId: '#H1923', hostName: 'Host B', amount: '89,500,000 VND', date: '24/10/2025', status: 'Completed' },
        { hostId: '#H3467', hostName: 'Host C', amount: '234,000,000 VND', date: '23/10/2025', status: 'Completed' },
        { hostId: '#H5612', hostName: 'Host D', amount: '67,800,000 VND', date: '22/10/2025', status: 'Processing' },
        { hostId: '#H7834', hostName: 'Host E', amount: '156,200,000 VND', date: '21/10/2025', status: 'Completed' }
    ];

    const tbody = document.getElementById('payoutBody');
    if (tbody) {
        tbody.innerHTML = payoutData.map(payout => `
                <tr>
                    <td>${payout.hostId}</td>
                    <td>${payout.hostName}</td>
                    <td>${payout.amount}</td>
                    <td>${payout.date}</td>
                    <td><span class="badge ${payout.status === 'Completed' ? 'badge-success' : 'badge-warning'}">${payout.status}</span></td>
                </tr>
            `).join('');
    }
}

// Load Top Events
function loadTopEvents() {
    const topEvents = [
        { rank: 1, name: 'Vietnam Tech Summit 2025', host: 'Tech Events Vietnam', type: 'Technology', tickets: 2847, revenue: '856,000,000 VND' },
        { rank: 2, name: 'HCM Music Festival', host: 'Sound Wave Productions', type: 'Music', tickets: 5234, revenue: '784,000,000 VND' },
        { rank: 3, name: 'Startup Networking Night', host: 'Innovation Hub', type: 'Business', tickets: 1456, revenue: '437,000,000 VND' }
    ];

    const tbody = document.getElementById('topEventsBody');
    if (tbody) {
        tbody.innerHTML = topEvents.map(event => `
                <tr>
                    <td>${event.rank}</td>
                    <td>${event.name}</td>
                    <td>${event.host}</td>
                    <td>${event.type}</td>
                    <td>${event.tickets}</td>
                    <td>${event.revenue}</td>
                </tr>
            `).join('');
    }
}

// Page Navigation - Make sure it's in global scope
window.showPage = function(pageId) {
    console.log('[DEBUG] ========== showPage CALLED ==========');
    console.log('[DEBUG] showPage called with pageId:', pageId);
    document.querySelectorAll('.page-section').forEach(section => {
        section.classList.remove('active');
    });
    const targetSection = document.getElementById(pageId);
    if (targetSection) {
        targetSection.classList.add('active');
        console.log('[DEBUG] Activated page section:', pageId);
    } else {
        console.error('[DEBUG] Page section not found:', pageId);
    }

    document.querySelectorAll('.menu-item, .submenu-item').forEach(item => {
        item.classList.remove('active');
    });
    
    // Activate the clicked menu item
    const activeItem = document.querySelector(`[onclick*="showPage('${pageId}')"]`);
    if (activeItem) {
        activeItem.classList.add('active');
    }

    if (window.innerWidth <= 768) {
        const sidebar = document.getElementById('sidebar');
        if (sidebar) sidebar.classList.remove('show');
    }
    
    // Load data when switching to specific pages
    if (pageId === 'users') {
        console.log('[DEBUG] ========== Loading User Activity page ==========');
        console.log('[DEBUG] Checking if functions exist:', {
            loadUserStatistics: typeof window.loadUserStatistics,
            loadUsers: typeof window.loadUsers,
            loadEnhancedAuditLogs: typeof window.loadEnhancedAuditLogs
        });
        console.log('[DEBUG] Checking if page section exists:', !!document.getElementById('users'));
        console.log('[DEBUG] Checking if elements exist:', {
            userStatsTotalUsers: !!document.getElementById('userStatsTotalUsers'),
            userListBody: !!document.getElementById('userListBody'),
            enhancedAuditLogBody: !!document.getElementById('enhancedAuditLogBody')
        });
        
        // Use setTimeout to ensure DOM is ready
        setTimeout(() => {
            try {
                console.log('[DEBUG] Calling loadUserStatistics...');
                if (typeof window.loadUserStatistics === 'function') {
                    window.loadUserStatistics();
                } else {
                    console.error('[DEBUG] loadUserStatistics is not a function!');
                }
                
                console.log('[DEBUG] Calling loadUsers...');
                if (typeof window.loadUsers === 'function') {
                    window.loadUsers(0);
                } else {
                    console.error('[DEBUG] loadUsers is not a function!');
                }
                
                console.log('[DEBUG] Calling loadEnhancedAuditLogs...');
                if (typeof window.loadEnhancedAuditLogs === 'function') {
                    window.loadEnhancedAuditLogs(0);
                } else {
                    console.error('[DEBUG] loadEnhancedAuditLogs is not a function!');
                }
            } catch (error) {
                console.error('[DEBUG] Error calling load functions:', error);
                console.error('[DEBUG] Error stack:', error.stack);
            }
        }, 100);
    }
    
    // Load financial data when financial page is shown
    if (pageId === 'financial') {
        loadFinancialSummary();
        loadOrders(0);
        loadPayments(0);
    }
    
    // Load event operations data when events page is shown
    if (pageId === 'events') {
        console.log('[DEBUG] Loading Event Operations page, calling all load functions...');
        loadPendingApprovals(0);
        loadEventsByStatus(0);
        loadUpcomingEvents(0);
        loadDepartmentStats();
        loadAttendanceStats(0);
        loadVenueConflicts();
        loadPointsTracking(0);
        loadSpeakerStats(0);
        loadEventPerformanceMetrics();
    }
}

// Toggle Submenu
function toggleSubmenu(submenuId) {
    console.log('[DEBUG] toggleSubmenu called with:', submenuId);
    const submenu = document.getElementById(submenuId + '-submenu');
    if (submenu) {
        submenu.classList.toggle('show');
        console.log('[DEBUG] Submenu toggled, has show class:', submenu.classList.contains('show'));
    } else {
        console.error('[DEBUG] Submenu not found:', submenuId + '-submenu');
    }
}

// Toggle Mobile Sidebar
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    if (sidebar) sidebar.classList.toggle('show');
}

// Filter Chart
function filterChart(chartName, period, buttonElement) {
    if (!buttonElement) return;
    const parentContainer = buttonElement.closest('.chart-container');
    if (parentContainer) {
        parentContainer.querySelectorAll('.chart-filter-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        buttonElement.classList.add('active');
    }

    fetch(`/admin/api/stats?period=${period}`)
        .then(response => response.json())
        .then(data => {
            initCharts(data);
        })
        .catch(error => console.error('Error filtering chart:', error));
}

// Filter Audit Log
function filterAuditLog() {
    const actionType = document.getElementById('actionTypeFilter')?.value || '';
    const userId = document.getElementById('userIdFilter')?.value || '';
    const fromDate = document.getElementById('fromDateFilter')?.value || '';
    const toDate = document.getElementById('toDateFilter')?.value || '';

    let filtered = allAuditLogs;

    if (actionType) {
        filtered = filtered.filter(log => log.actionType === actionType);
    }
    if (userId) {
        const userIdStr = userId.toString().toLowerCase();
        filtered = filtered.filter(log => {
            if (log.actorId == null) return false;
            return log.actorId.toString().toLowerCase().includes(userIdStr);
        });
    }
    if (fromDate) {
        filtered = filtered.filter(log => new Date(log.createdAt) >= new Date(fromDate));
    }
    if (toDate) {
        filtered = filtered.filter(log => new Date(log.createdAt) <= new Date(toDate));
    }

    displayAuditLogs(filtered);
}

// Reset Filters
function resetFilters() {
    const actionTypeFilter = document.getElementById('actionTypeFilter');
    const userIdFilter = document.getElementById('userIdFilter');
    const fromDateFilter = document.getElementById('fromDateFilter');
    const toDateFilter = document.getElementById('toDateFilter');
    
    if (actionTypeFilter) actionTypeFilter.value = '';
    if (userIdFilter) userIdFilter.value = '';
    if (fromDateFilter) fromDateFilter.value = '';
    if (toDateFilter) toDateFilter.value = '';
    
    displayAuditLogs(allAuditLogs.slice(0, 10));
}

// Export to CSV
function exportToCSV(tableId) {
    const table = document.getElementById(tableId);
    if (!table) {
        alert('Xuất dữ liệu thành công! (Demo)');
        return;
    }

    let csv = [];
    const rows = table.querySelectorAll('tr');

    rows.forEach(row => {
        const cols = row.querySelectorAll('td, th');
        const csvRow = Array.from(cols).map(col => col.innerText).join(',');
        csv.push(csvRow);
    });

    const csvContent = csv.join('\n');
    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);

    link.setAttribute('href', url);
    link.setAttribute('download', tableId + '-export.csv');
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// ========== Event Operations Functions ==========

// Pending Approval Requests
let pendingApprovalsPage = 0;
let selectedPendingIds = new Set();

function loadPendingApprovals(page = 0) {
    console.log('[DEBUG] loadPendingApprovals called with page:', page);
    pendingApprovalsPage = page;
    const url = `/admin/api/pending-approvals?page=${page}&size=20`;
    console.log('[DEBUG] Fetching from:', url);
    fetch(url)
        .then(response => {
            console.log('[DEBUG] Response status:', response.status, response.statusText);
            if (!response.ok) {
                console.error('[DEBUG] Response not OK!');
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('[DEBUG] Received data:', data);
            const tbody = document.getElementById('pendingApprovalsBody');
            if (!tbody) {
                console.error('[DEBUG] pendingApprovalsBody element not found!');
                return;
            }
            if (data.content && data.content.length > 0) {
                tbody.innerHTML = data.content.map(item => `
                    <tr>
                        <td><input type="checkbox" class="pending-checkbox" value="${item.eventId}" onchange="togglePendingSelection(${item.eventId})"></td>
                        <td><strong>${item.eventTitle}</strong></td>
                        <td><span class="badge badge-info">${item.eventType}</span></td>
                        <td>${item.hostName}</td>
                        <td>${item.departmentName}</td>
                        <td><span class="badge ${item.daysPending > 7 ? 'badge-danger' : 'badge-warning'}">${item.daysPending} days</span></td>
                        <td>
                            <button class="btn btn-sm btn-success" onclick="approveEvent(${item.requestId})">
                                <i class="fas fa-check"></i> Approve
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="rejectEvent(${item.requestId})">
                                <i class="fas fa-times"></i> Reject
                            </button>
                        </td>
                    </tr>
                `).join('');
                renderPagination('pendingApprovalsPagination', data, loadPendingApprovals);
            } else {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No pending approvals</td></tr>';
            }
        })
        .catch(error => {
            console.error('[DEBUG] Error loading pending approvals:', error);
            const tbody = document.getElementById('pendingApprovalsBody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Error loading data</td></tr>';
        });
}

function toggleSelectAll(type) {
    const checkboxes = document.querySelectorAll(`.${type}-checkbox`);
    const selectAll = document.getElementById(`selectAll${type.charAt(0).toUpperCase() + type.slice(1)}`);
    if (selectAll) {
        checkboxes.forEach(cb => {
            cb.checked = selectAll.checked;
            if (type === 'pending') {
                if (cb.checked) selectedPendingIds.add(parseInt(cb.value));
                else selectedPendingIds.delete(parseInt(cb.value));
            }
        });
    }
}

function togglePendingSelection(eventId) {
    const checkbox = document.querySelector(`.pending-checkbox[value="${eventId}"]`);
    if (checkbox && checkbox.checked) selectedPendingIds.add(eventId);
    else selectedPendingIds.delete(eventId);
}

function bulkApproveSelected() {
    if (selectedPendingIds.size === 0) {
        alert('Please select at least one event');
        return;
    }
    if (confirm(`Approve ${selectedPendingIds.size} event(s)?`)) {
        fetch('/admin/api/events/bulk-approve', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(Array.from(selectedPendingIds))
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('Events approved successfully');
                selectedPendingIds.clear();
                loadPendingApprovals(pendingApprovalsPage);
            } else {
                alert('Error approving events');
            }
        });
    }
}

function bulkRejectSelected() {
    if (selectedPendingIds.size === 0) {
        alert('Please select at least one event');
        return;
    }
    const reason = prompt('Please provide a reason for rejection:');
    if (reason) {
        fetch('/admin/api/events/bulk-reject', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ eventIds: Array.from(selectedPendingIds), reason: reason })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('Events rejected successfully');
                selectedPendingIds.clear();
                loadPendingApprovals(pendingApprovalsPage);
            } else {
                alert('Error rejecting events');
            }
        });
    }
}

function approveEvent(requestId) {
    if (confirm('Approve this event?')) {
        fetch(`/admin/api/requests/${requestId}/approve`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ responseMessage: 'Approved by admin' })
        })
        .then(response => {
            if (response.ok) {
                loadPendingApprovals(pendingApprovalsPage);
            }
        });
    }
}

function rejectEvent(requestId) {
    const reason = prompt('Please provide a reason for rejection:');
    if (reason) {
        fetch(`/admin/api/requests/${requestId}/reject`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ responseMessage: reason })
        })
        .then(response => {
            if (response.ok) {
                loadPendingApprovals(pendingApprovalsPage);
            }
        });
    }
}

// Event Status Management
let eventsByStatusPage = 0;

function loadEventsByStatus(page = 0) {
    console.log('[DEBUG] loadEventsByStatus called with page:', page);
    eventsByStatusPage = page;
    const status = document.getElementById('eventStatusFilter')?.value || '';
    const eventType = document.getElementById('eventTypeFilter')?.value || '';
    const search = document.getElementById('eventSearchFilter')?.value || '';
    
    let url = `/admin/api/events/status?page=${page}&size=20`;
    if (status) url += `&status=${status}`;
    if (eventType) url += `&eventType=${eventType}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    
    console.log('[DEBUG] Fetching from:', url);
    fetch(url)
        .then(response => {
            console.log('[DEBUG] Response status:', response.status);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            console.log('[DEBUG] Received data:', data);
            const tbody = document.getElementById('eventsByStatusBody');
            if (!tbody) {
                console.error('[DEBUG] eventsByStatusBody element not found!');
                return;
            }
            if (data.content && data.content.length > 0) {
                tbody.innerHTML = data.content.map(item => `
                    <tr>
                        <td><input type="checkbox" class="events-checkbox" value="${item.eventId}"></td>
                        <td><strong>${item.title}</strong></td>
                        <td><span class="badge badge-info">${item.eventType}</span></td>
                        <td>${getEventStatusBadge(item.status)}</td>
                        <td>${item.departmentName}</td>
                        <td>${formatDateTime(item.startsAt)}</td>
                        <td>${item.registeredCount || 0}</td>
                        <td>${formatCurrency(item.revenue || 0)}</td>
                        <td>
                            <button class="btn btn-sm btn-primary" onclick="viewEventDetails(${item.eventId})">
                                <i class="fas fa-eye"></i>
                            </button>
                        </td>
                    </tr>
                `).join('');
                renderPagination('eventsByStatusPagination', data, loadEventsByStatus);
            } else {
                tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted">No events found</td></tr>';
            }
        })
        .catch(error => {
            console.error('[DEBUG] Error loading events:', error);
            const tbody = document.getElementById('eventsByStatusBody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="9" class="text-center text-danger">Error loading data</td></tr>';
        });
}

function getEventStatusBadge(status) {
    const badges = {
        'DRAFT': '<span class="badge badge-secondary">Draft</span>',
        'PUBLIC': '<span class="badge badge-success">Public</span>',
        'ONGOING': '<span class="badge badge-primary">Ongoing</span>',
        'FINISH': '<span class="badge badge-info">Finished</span>',
        'CANCEL': '<span class="badge badge-danger">Cancelled</span>'
    };
    return badges[status] || `<span class="badge badge-secondary">${status}</span>`;
}

// Upcoming Events
let upcomingEventsPage = 0;

function loadUpcomingEvents(page = 0) {
    console.log('[DEBUG] loadUpcomingEvents called with page:', page);
    upcomingEventsPage = page;
    const days = document.getElementById('upcomingDaysFilter')?.value || 30;
    const url = `/admin/api/events/upcoming?days=${days}&page=${page}&size=20`;
    console.log('[DEBUG] Fetching from:', url);
    fetch(url)
        .then(response => {
            console.log('[DEBUG] Response status:', response.status);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            console.log('[DEBUG] Received data:', data);
            const tbody = document.getElementById('upcomingEventsBody');
            if (!tbody) {
                console.error('[DEBUG] upcomingEventsBody element not found!');
                return;
            }
            if (data.content && data.content.length > 0) {
                tbody.innerHTML = data.content.map(item => `
                    <tr>
                        <td><strong>${item.title}</strong></td>
                        <td><span class="badge badge-info">${item.eventType}</span></td>
                        <td>${formatDateTime(item.startsAt)}</td>
                        <td>${item.venue || 'TBA'}</td>
                        <td>${item.registeredCount || 0} / ${item.capacity || 'N/A'}</td>
                        <td>
                            <div class="progress" style="height: 20px;">
                                <div class="progress-bar ${item.fillRate > 80 ? 'bg-success' : item.fillRate > 50 ? 'bg-warning' : 'bg-danger'}" 
                                     style="width: ${item.fillRate}%">${item.fillRate.toFixed(1)}%</div>
                            </div>
                        </td>
                        <td>${item.departmentName}</td>
                    </tr>
                `).join('');
                renderPagination('upcomingEventsPagination', data, loadUpcomingEvents);
            } else {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No upcoming events</td></tr>';
            }
        })
        .catch(error => {
            console.error('[DEBUG] Error loading upcoming events:', error);
            const tbody = document.getElementById('upcomingEventsBody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Error loading data</td></tr>';
        });
}

// Department Statistics
function loadDepartmentStats() {
    console.log('[DEBUG] loadDepartmentStats called');
    const url = '/admin/api/departments/stats';
    console.log('[DEBUG] Fetching from:', url);
    fetch(url)
        .then(response => {
            console.log('[DEBUG] Response status:', response.status);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            console.log('[DEBUG] Received data:', data);
            const tbody = document.getElementById('departmentStatsBody');
            if (!tbody) {
                console.error('[DEBUG] departmentStatsBody element not found!');
                return;
            }
            if (data && data.length > 0) {
                tbody.innerHTML = data.map(item => `
                    <tr>
                        <td><strong>${item.departmentName}</strong></td>
                        <td>${item.totalEvents}</td>
                        <td><span class="badge badge-success">${item.activeEvents}</span></td>
                        <td><span class="badge badge-warning">${item.pendingApproval}</span></td>
                        <td>${item.completedEvents}</td>
                        <td>${formatCurrency(item.totalRevenue || 0)}</td>
                        <td>${item.totalParticipants || 0}</td>
                    </tr>
                `).join('');
                updateDepartmentCharts(data);
            } else {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No department data</td></tr>';
            }
        })
        .catch(error => {
            console.error('[DEBUG] Error loading department stats:', error);
            const tbody = document.getElementById('departmentStatsBody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Error loading data</td></tr>';
        });
}

function updateDepartmentCharts(data) {
    const labels = data.map(d => d.departmentName);
    const eventsData = data.map(d => d.totalEvents);
    const revenueData = data.map(d => d.totalRevenue || 0);
    
    if (chartInstances.departmentEvents) chartInstances.departmentEvents.destroy();
    const eventsCtx = document.getElementById('departmentEventsChart');
    if (eventsCtx) {
        chartInstances.departmentEvents = new Chart(eventsCtx.getContext('2d'), {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Total Events',
                    data: eventsData,
                    backgroundColor: 'rgba(44, 123, 229, 0.6)'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: { y: { beginAtZero: true } }
            }
        });
    }
    
    if (chartInstances.departmentRevenue) chartInstances.departmentRevenue.destroy();
    const revenueCtx = document.getElementById('departmentRevenueChart');
    if (revenueCtx) {
        chartInstances.departmentRevenue = new Chart(revenueCtx.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: revenueData,
                    backgroundColor: ['#2c7be5', '#00d4ff', '#0095ff', '#0066cc', '#004999']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false
            }
        });
    }
}

// Attendance Statistics
let attendanceStatsPage = 0;

function loadAttendanceStats(page = 0) {
    console.log('[DEBUG] loadAttendanceStats called with page:', page);
    attendanceStatsPage = page;
    const eventType = document.getElementById('attendanceEventTypeFilter')?.value || '';
    
    let url = `/admin/api/attendance/stats?page=${page}&size=20`;
    if (eventType) url += `&eventType=${eventType}`;
    
    console.log('[DEBUG] Fetching from:', url);
    fetch(url)
        .then(response => {
            console.log('[DEBUG] Response status:', response.status);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            console.log('[DEBUG] Received data:', data);
            const tbody = document.getElementById('attendanceStatsBody');
            if (!tbody) {
                console.error('[DEBUG] attendanceStatsBody element not found!');
                return;
            }
            if (data.content && data.content.length > 0) {
                tbody.innerHTML = data.content.map(item => `
                    <tr>
                        <td><strong>${item.eventTitle}</strong></td>
                        <td><span class="badge badge-info">${item.eventType}</span></td>
                        <td>${formatDateTime(item.startsAt)}</td>
                        <td>${item.totalRegistered || 0}</td>
                        <td><span class="badge badge-success">${item.checkedInCount || 0}</span></td>
                        <td><span class="badge badge-danger">${item.noShowCount || 0}</span></td>
                        <td>
                            <div class="progress" style="height: 20px;">
                                <div class="progress-bar bg-success" style="width: ${item.attendanceRate || 0}%">
                                    ${(item.attendanceRate || 0).toFixed(1)}%
                                </div>
                            </div>
                        </td>
                    </tr>
                `).join('');
                renderPagination('attendanceStatsPagination', data, loadAttendanceStats);
                updateAttendanceCharts(data.content);
            } else {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No attendance data</td></tr>';
            }
        })
        .catch(error => {
            console.error('[DEBUG] Error loading attendance stats:', error);
            const tbody = document.getElementById('attendanceStatsBody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Error loading data</td></tr>';
        });
}

function updateAttendanceCharts(data) {
    const eventTypes = ['MUSIC', 'WORKSHOP', 'COMPETITION', 'FESTIVAL'];
    const attendanceRates = eventTypes.map(type => {
        const events = data.filter(e => e.eventType === type);
        if (events.length === 0) return 0;
        const avg = events.reduce((sum, e) => sum + (e.attendanceRate || 0), 0) / events.length;
        return avg;
    });
    
    const totalCheckedIn = data.reduce((sum, e) => sum + (e.checkedInCount || 0), 0);
    const totalNoShow = data.reduce((sum, e) => sum + (e.noShowCount || 0), 0);
    
    if (chartInstances.attendanceRate) chartInstances.attendanceRate.destroy();
    const rateCtx = document.getElementById('attendanceRateChart');
    if (rateCtx) {
        chartInstances.attendanceRate = new Chart(rateCtx.getContext('2d'), {
            type: 'bar',
            data: {
                labels: eventTypes,
                datasets: [{
                    label: 'Attendance Rate (%)',
                    data: attendanceRates,
                    backgroundColor: 'rgba(44, 123, 229, 0.6)'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: { y: { beginAtZero: true, max: 100 } }
            }
        });
    }
    
    if (chartInstances.checkin) chartInstances.checkin.destroy();
    const checkinCtx = document.getElementById('checkinChart');
    if (checkinCtx) {
        chartInstances.checkin = new Chart(checkinCtx.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: ['Checked In', 'No Show'],
                datasets: [{
                    data: [totalCheckedIn, totalNoShow],
                    backgroundColor: ['#28a745', '#dc3545']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false
            }
        });
    }
}

// Venue Conflicts
function loadVenueConflicts() {
    console.log('[DEBUG] loadVenueConflicts called');
    const url = '/admin/api/venue/conflicts';
    console.log('[DEBUG] Fetching from:', url);
    fetch(url)
        .then(response => {
            console.log('[DEBUG] Response status:', response.status);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            console.log('[DEBUG] Received data:', data);
            const tbody = document.getElementById('venueConflictsBody');
            const alertDiv = document.getElementById('venueConflictsAlert');
            const conflictsCount = document.getElementById('conflictsCount');
            
            if (!tbody) {
                console.error('[DEBUG] venueConflictsBody element not found!');
                return;
            }
            if (data && data.length > 0) {
                if (alertDiv) alertDiv.style.display = 'block';
                if (conflictsCount) conflictsCount.textContent = data.length;
                
                tbody.innerHTML = data.map(item => `
                    <tr>
                        <td><strong>${item.venueName}</strong></td>
                        <td>${item.event1Title}</td>
                        <td>${item.event2Title}</td>
                        <td>${item.overlapMinutes} minutes</td>
                        <td>
                            <span class="badge ${item.conflictSeverity === 'HIGH' ? 'badge-danger' : item.conflictSeverity === 'MEDIUM' ? 'badge-warning' : 'badge-info'}">
                                ${item.conflictSeverity}
                            </span>
                        </td>
                        <td>
                            <button class="btn btn-sm btn-primary" onclick="viewEventDetails(${item.event1Id})">
                                <i class="fas fa-eye"></i> Event 1
                            </button>
                            <button class="btn btn-sm btn-primary" onclick="viewEventDetails(${item.event2Id})">
                                <i class="fas fa-eye"></i> Event 2
                            </button>
                        </td>
                    </tr>
                `).join('');
            } else {
                if (alertDiv) alertDiv.style.display = 'none';
                tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No conflicts detected</td></tr>';
            }
        })
        .catch(error => {
            console.error('[DEBUG] Error loading venue conflicts:', error);
            const tbody = document.getElementById('venueConflictsBody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Error loading data</td></tr>';
        });
}

// Points Tracking
let pointsTrackingPage = 0;

function loadPointsTracking(page = 0) {
    console.log('[DEBUG] loadPointsTracking called with page:', page);
    pointsTrackingPage = page;
    const eventType = document.getElementById('pointsEventTypeFilter')?.value || '';
    
    let url = `/admin/api/points/tracking?page=${page}&size=20`;
    if (eventType) url += `&eventType=${eventType}`;
    
    console.log('[DEBUG] Fetching from:', url);
    fetch(url)
        .then(response => {
            console.log('[DEBUG] Response status:', response.status);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            console.log('[DEBUG] Received data:', data);
            const tbody = document.getElementById('pointsTrackingBody');
            if (!tbody) {
                console.error('[DEBUG] pointsTrackingBody element not found!');
                return;
            }
            if (data.content && data.content.length > 0) {
                tbody.innerHTML = data.content.map(item => `
                    <tr>
                        <td><strong>${item.eventTitle}</strong></td>
                        <td><span class="badge badge-info">${item.eventType}</span></td>
                        <td><span class="badge badge-success">${item.pointsAwarded || 0} points</span></td>
                        <td>${item.studentsEarnedPoints || 0}</td>
                        <td>${item.totalParticipants || 0}</td>
                        <td>
                            <div class="progress" style="height: 20px;">
                                <div class="progress-bar bg-success" style="width: ${item.pointsDistributionRate || 0}%">
                                    ${(item.pointsDistributionRate || 0).toFixed(1)}%
                                </div>
                            </div>
                        </td>
                        <td>${item.learningObjectives || 'N/A'}</td>
                    </tr>
                `).join('');
                renderPagination('pointsTrackingPagination', data, loadPointsTracking);
            } else {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No points data</td></tr>';
            }
        })
        .catch(error => {
            console.error('[DEBUG] Error loading points tracking:', error);
            const tbody = document.getElementById('pointsTrackingBody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Error loading data</td></tr>';
        });
}

// Speaker Statistics
let speakerStatsPage = 0;

function loadSpeakerStats(page = 0) {
    console.log('[DEBUG] loadSpeakerStats called with page:', page);
    speakerStatsPage = page;
    const url = `/admin/api/speakers/stats?page=${page}&size=20`;
    console.log('[DEBUG] Fetching from:', url);
    fetch(url)
        .then(response => {
            console.log('[DEBUG] Response status:', response.status);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            console.log('[DEBUG] Received data:', data);
            const tbody = document.getElementById('speakerStatsBody');
            if (!tbody) {
                console.error('[DEBUG] speakerStatsBody element not found!');
                return;
            }
            if (data.content && data.content.length > 0) {
                tbody.innerHTML = data.content.map(item => `
                    <tr>
                        <td><strong>${item.speakerName}</strong></td>
                        <td><span class="badge badge-info">${item.role}</span></td>
                        <td>${item.eventsCount}</td>
                        <td>${item.totalParticipants || 0}</td>
                        <td>${formatCurrency(item.totalRevenue || 0)}</td>
                    </tr>
                `).join('');
                renderPagination('speakerStatsPagination', data, loadSpeakerStats);
                updateSpeakerCharts(data.content);
            } else {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No speaker data</td></tr>';
            }
        })
        .catch(error => {
            console.error('[DEBUG] Error loading speaker stats:', error);
            const tbody = document.getElementById('speakerStatsBody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">Error loading data</td></tr>';
        });
}

function updateSpeakerCharts(data) {
    const topSpeakers = data.slice(0, 5);
    const labels = topSpeakers.map(s => s.speakerName);
    const eventsData = topSpeakers.map(s => s.eventsCount);
    const participantsData = topSpeakers.map(s => s.totalParticipants || 0);
    
    if (chartInstances.speakerEvents) chartInstances.speakerEvents.destroy();
    const eventsCtx = document.getElementById('speakerEventsChart');
    if (eventsCtx) {
        chartInstances.speakerEvents = new Chart(eventsCtx.getContext('2d'), {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Events Count',
                    data: eventsData,
                    backgroundColor: 'rgba(44, 123, 229, 0.6)'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: { y: { beginAtZero: true } }
            }
        });
    }
    
    if (chartInstances.speakerPerformance) chartInstances.speakerPerformance.destroy();
    const perfCtx = document.getElementById('speakerPerformanceChart');
    if (perfCtx) {
        chartInstances.speakerPerformance = new Chart(perfCtx.getContext('2d'), {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Total Participants',
                    data: participantsData,
                    borderColor: '#ff6b35',
                    backgroundColor: 'rgba(255, 107, 53, 0.1)',
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: { y: { beginAtZero: true } }
            }
        });
    }
}

// Event Performance Metrics
function loadEventPerformanceMetrics() {
    console.log('[DEBUG] loadEventPerformanceMetrics called');
    const url = '/admin/api/events/performance-metrics';
    console.log('[DEBUG] Fetching from:', url);
    fetch(url)
        .then(response => {
            console.log('[DEBUG] Response status:', response.status);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            console.log('[DEBUG] Received data:', data);
            const regRate = document.getElementById('registrationRate');
            const attRate = document.getElementById('attendanceRateKPI');
            const revPerEvent = document.getElementById('revenuePerEvent');
            const costPerPart = document.getElementById('costPerParticipant');
            
            if (regRate) regRate.textContent = (data.registrationConversionRate || 0).toFixed(1) + '%';
            if (attRate) attRate.textContent = (data.attendanceRate || 0).toFixed(1) + '%';
            if (revPerEvent) revPerEvent.textContent = formatCurrency(data.revenuePerEvent || 0);
            if (costPerPart) costPerPart.textContent = formatCurrency(data.costPerParticipant || 0);
        })
        .catch(error => {
            console.error('[DEBUG] Error loading performance metrics:', error);
        });
}

// Helper Functions
function formatDateTime(dateString) {
    if (!dateString) return 'N/A';
    try {
        const date = new Date(dateString);
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch (e) {
        return dateString;
    }
}

function viewEventDetails(eventId) {
    window.open(`/events/${eventId}`, '_blank');
}

function renderPagination(containerId, pageData, loadFunction) {
    const container = document.getElementById(containerId);
    if (!container || !pageData || pageData.totalPages <= 1) {
        if (container) container.innerHTML = '';
        return;
    }
    
    let html = '<ul class="pagination pagination-sm">';
    
    html += `<li class="page-item ${pageData.first ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="event.preventDefault(); ${loadFunction.name}(${pageData.number - 1})">Previous</a>
    </li>`;
    
    for (let i = 0; i < pageData.totalPages; i++) {
        html += `<li class="page-item ${i === pageData.number ? 'active' : ''}">
            <a class="page-link" href="#" onclick="event.preventDefault(); ${loadFunction.name}(${i})">${i + 1}</a>
        </li>`;
    }
    
    html += `<li class="page-item ${pageData.last ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="event.preventDefault(); ${loadFunction.name}(${pageData.number + 1})">Next</a>
    </li>`;
    
    html += '</ul>';
    container.innerHTML = html;
}

// Format Currency
function formatCurrency(value) {
    if (!value) return '0₫';
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
        minimumFractionDigits: 0
    }).format(value);
}

// Financial Reports JavaScript
let currentOrdersPage = 0;
let currentPaymentsPage = 0;
let ordersPageSize = 20;
let paymentsPageSize = 20;
let searchTimeout = null;

// Load Financial Summary
function loadFinancialSummary() {
    fetch('/admin/api/financial-summary')
        .then(response => response.json())
        .then(data => {
            const totalRevenueEl = document.getElementById('financialTotalRevenue');
            const totalOrdersEl = document.getElementById('financialTotalOrders');
            const avgOrderValueEl = document.getElementById('financialAvgOrderValue');
            const totalServiceFeeEl = document.getElementById('totalServiceFee');
            const totalHostPaymentEl = document.getElementById('totalHostPayment');
            const netProfitEl = document.getElementById('financialNetProfit');
            const pendingPaymentsEl = document.getElementById('financialPendingPayments');
            const refundedAmountEl = document.getElementById('financialRefundedAmount');
            
            if (totalRevenueEl) totalRevenueEl.textContent = formatCurrency(data.totalRevenue);
            if (totalOrdersEl) totalOrdersEl.textContent = data.totalOrders || 0;
            if (avgOrderValueEl) {
                const avgValue = data.averageOrderValue ? (typeof data.averageOrderValue === 'object' ? data.averageOrderValue : parseFloat(data.averageOrderValue)) : 0;
                avgOrderValueEl.textContent = formatCurrency(avgValue);
            }
            if (totalServiceFeeEl) totalServiceFeeEl.textContent = formatCurrency(data.totalServiceFees);
            if (totalHostPaymentEl) totalHostPaymentEl.textContent = formatCurrency(data.totalHostPayouts);
            if (netProfitEl) netProfitEl.textContent = formatCurrency(data.netProfit);
            if (pendingPaymentsEl) pendingPaymentsEl.textContent = formatCurrency(data.pendingPaymentsAmount);
            if (refundedAmountEl) refundedAmountEl.textContent = formatCurrency(data.refundedAmount);
            
            // Update revenue streams charts
            updateRevenueSourceChart(data.revenueBySource);
            updateRevenueByEventTypeChart(data.revenueByEventType);
        })
        .catch(error => console.error('Error loading financial summary:', error));
}

// Update Revenue Source Chart
function updateRevenueSourceChart(revenueBySource) {
    if (chartInstances.revenueSource) chartInstances.revenueSource.destroy();
    const ctx = document.getElementById('revenueSourceChart');
    if (!ctx) return;
    
    const labels = Object.keys(revenueBySource || {});
    const data = Object.values(revenueBySource || {});
    
    chartInstances.revenueSource = new Chart(ctx.getContext('2d'), {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: ['#2c7be5', '#00d4ff', '#ff6b35', '#10b981']
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { position: 'bottom' } }
        }
    });
}

// Update Revenue By Event Type Chart
function updateRevenueByEventTypeChart(revenueByEventType) {
    if (chartInstances.revenueByEventType) chartInstances.revenueByEventType.destroy();
    const ctx = document.getElementById('revenueByEventTypeChart');
    if (!ctx) return;
    
    const labels = Object.keys(revenueByEventType || {});
    const data = Object.values(revenueByEventType || {});
    
    chartInstances.revenueByEventType = new Chart(ctx.getContext('2d'), {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Doanh thu',
                data: data,
                backgroundColor: '#2c7be5'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true } }
        }
    });
}

// Load Orders
function loadOrders(page = 0) {
    currentOrdersPage = page;
    const statusEl = document.getElementById('orderStatusFilter');
    const searchEl = document.getElementById('orderSearch');
    const fromDateEl = document.getElementById('orderFromDate');
    const toDateEl = document.getElementById('orderToDate');
    
    const status = statusEl ? statusEl.value : '';
    const search = searchEl ? searchEl.value : '';
    const fromDate = fromDateEl ? fromDateEl.value : '';
    const toDate = toDateEl ? toDateEl.value : '';
    
    let url = `/admin/api/orders?page=${page}&size=${ordersPageSize}`;
    if (status) url += `&status=${status}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    if (fromDate) url += `&fromDate=${fromDate}`;
    if (toDate) url += `&toDate=${toDate}`;
    
    fetch(url)
        .then(response => response.json())
        .then(data => {
            displayOrders(data.content);
            updateOrdersPagination(data);
        })
        .catch(error => {
            console.error('Error loading orders:', error);
            const ordersBody = document.getElementById('ordersBody');
            if (ordersBody) {
                ordersBody.innerHTML = '<tr><td colspan="9" class="text-center text-danger">Error loading data</td></tr>';
            }
        });
}

// Display Orders
function displayOrders(orders) {
    const tbody = document.getElementById('ordersBody');
    if (!tbody) return;
    if (!orders || orders.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted">No data available</td></tr>';
        return;
    }
    
    tbody.innerHTML = orders.map(order => {
        const statusBadge = getOrderStatusBadge(order.status);
        const paymentBadge = getPaymentStatusBadge(order.paymentStatus);
        const createdAt = order.createdAt ? new Date(order.createdAt).toLocaleString('vi-VN') : 'N/A';
        const totalAmount = order.totalAmount ? (typeof order.totalAmount === 'object' ? parseFloat(order.totalAmount) : order.totalAmount) : 0;
        
        return `
            <tr>
                <td>#${order.orderId}</td>
                <td>${order.customerName || 'N/A'}<br><small class="text-muted">${order.customerEmail || ''}</small></td>
                <td>${order.eventTitle || 'N/A'}</td>
                <td>${order.ticketTypeName || 'N/A'}</td>
                <td>${order.quantity || 0}</td>
                <td>${formatCurrency(totalAmount)}</td>
                <td>${statusBadge}</td>
                <td>${paymentBadge}</td>
                <td>${createdAt}</td>
            </tr>
        `;
    }).join('');
}

// Update Orders Pagination
function updateOrdersPagination(pageData) {
    const pagination = document.getElementById('ordersPagination');
    if (!pagination) return;
    
    const totalPages = pageData.totalPages;
    const currentPage = pageData.number;
    
    let html = '';
    if (totalPages > 1) {
        html += `<li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadOrders(${currentPage - 1}); return false;">Previous</a>
        </li>`;
        
        for (let i = 0; i < totalPages; i++) {
            if (i === 0 || i === totalPages - 1 || (i >= currentPage - 2 && i <= currentPage + 2)) {
                html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="loadOrders(${i}); return false;">${i + 1}</a>
                </li>`;
            } else if (i === currentPage - 3 || i === currentPage + 3) {
                html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
        }
        
        html += `<li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadOrders(${currentPage + 1}); return false;">Next</a>
        </li>`;
    }
    
    pagination.innerHTML = html;
}

// Load Payments
function loadPayments(page = 0) {
    currentPaymentsPage = page;
    const statusEl = document.getElementById('paymentStatusFilter');
    const searchEl = document.getElementById('paymentSearch');
    const fromDateEl = document.getElementById('paymentFromDate');
    const toDateEl = document.getElementById('paymentToDate');
    
    const status = statusEl ? statusEl.value : '';
    const search = searchEl ? searchEl.value : '';
    const fromDate = fromDateEl ? fromDateEl.value : '';
    const toDate = toDateEl ? toDateEl.value : '';
    
    let url = `/admin/api/payments?page=${page}&size=${paymentsPageSize}`;
    if (status) url += `&status=${status}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    if (fromDate) url += `&fromDate=${fromDate}`;
    if (toDate) url += `&toDate=${toDate}`;
    
    fetch(url)
        .then(response => response.json())
        .then(data => {
            displayPayments(data.content);
            updatePaymentsPagination(data);
        })
        .catch(error => {
            console.error('Error loading payments:', error);
            const paymentsBody = document.getElementById('paymentsBody');
            if (paymentsBody) {
                paymentsBody.innerHTML = '<tr><td colspan="9" class="text-center text-danger">Error loading data</td></tr>';
            }
        });
}

// Display Payments
function displayPayments(payments) {
    const tbody = document.getElementById('paymentsBody');
    if (!tbody) return;
    if (!payments || payments.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted">No data available</td></tr>';
        return;
    }
    
    tbody.innerHTML = payments.map(payment => {
        const statusBadge = getPaymentStatusBadge(payment.status);
        const createdAt = payment.createdAt ? new Date(payment.createdAt).toLocaleString('vi-VN') : 'N/A';
        const paidAt = payment.paidAt ? new Date(payment.paidAt).toLocaleString('vi-VN') : '-';
        const amount = payment.amount ? (typeof payment.amount === 'object' ? parseFloat(payment.amount) : payment.amount) : 0;
        
        return `
            <tr>
                <td>#${payment.paymentId}</td>
                <td>#${payment.orderId || 'N/A'}</td>
                <td>${payment.customerName || 'N/A'}<br><small class="text-muted">${payment.customerEmail || ''}</small></td>
                <td>${formatCurrency(amount)}</td>
                <td>${payment.paymentMethod || 'PayOS'}</td>
                <td>${statusBadge}</td>
                <td>${payment.transactionId || '-'}</td>
                <td>${createdAt}</td>
                <td>${paidAt}</td>
            </tr>
        `;
    }).join('');
}

// Update Payments Pagination
function updatePaymentsPagination(pageData) {
    const pagination = document.getElementById('paymentsPagination');
    if (!pagination) return;
    
    const totalPages = pageData.totalPages;
    const currentPage = pageData.number;
    
    let html = '';
    if (totalPages > 1) {
        html += `<li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadPayments(${currentPage - 1}); return false;">Previous</a>
        </li>`;
        
        for (let i = 0; i < totalPages; i++) {
            if (i === 0 || i === totalPages - 1 || (i >= currentPage - 2 && i <= currentPage + 2)) {
                html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="loadPayments(${i}); return false;">${i + 1}</a>
                </li>`;
            } else if (i === currentPage - 3 || i === currentPage + 3) {
                html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
        }
        
        html += `<li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="loadPayments(${currentPage + 1}); return false;">Next</a>
        </li>`;
    }
    
    pagination.innerHTML = html;
}

// Helper functions
function getOrderStatusBadge(status) {
    const badges = {
        'PENDING': '<span class="badge badge-warning">Pending</span>',
        'PAID': '<span class="badge badge-success">Paid</span>',
        'CANCELLED': '<span class="badge badge-danger">Cancelled</span>',
        'EXPIRED': '<span class="badge badge-danger">Expired</span>',
        'REFUNDED': '<span class="badge badge-danger">Refunded</span>'
    };
    return badges[status] || '<span class="badge badge-warning">' + status + '</span>';
}

function getPaymentStatusBadge(status) {
    const badges = {
        'PENDING': '<span class="badge badge-warning">Pending</span>',
        'PAID': '<span class="badge badge-success">Paid</span>',
        'CANCELLED': '<span class="badge badge-danger">Cancelled</span>',
        'EXPIRED': '<span class="badge badge-danger">Expired</span>'
    };
    return badges[status] || '<span class="badge badge-warning">' + (status || 'N/A') + '</span>';
}

// Debounce search
function debounceSearch(type) {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
        if (type === 'order') {
            loadOrders(0);
        } else if (type === 'payment') {
            loadPayments(0);
        }
    }, 500);
}

// Export functions
function exportOrdersToCSV() {
    const statusEl = document.getElementById('orderStatusFilter');
    const searchEl = document.getElementById('orderSearch');
    const fromDateEl = document.getElementById('orderFromDate');
    const toDateEl = document.getElementById('orderToDate');
    
    const status = statusEl ? statusEl.value : '';
    const search = searchEl ? searchEl.value : '';
    const fromDate = fromDateEl ? fromDateEl.value : '';
    const toDate = toDateEl ? toDateEl.value : '';
    
    let url = `/admin/api/orders/export`;
    const params = [];
    if (status) params.push(`status=${status}`);
    if (search) params.push(`search=${encodeURIComponent(search)}`);
    if (fromDate) params.push(`fromDate=${fromDate}`);
    if (toDate) params.push(`toDate=${toDate}`);
    if (params.length > 0) url += '?' + params.join('&');
    
    fetch(url)
        .then(response => response.json())
        .then(data => {
            exportToCSVFromData(data, 'orders-export.csv', [
                'orderId', 'customerName', 'customerEmail', 'eventTitle', 
                'ticketTypeName', 'quantity', 'totalAmount', 'status', 'paymentStatus', 'createdAt'
            ]);
        })
        .catch(error => console.error('Error exporting orders:', error));
}

function exportPaymentsToCSV() {
    const statusEl = document.getElementById('paymentStatusFilter');
    const searchEl = document.getElementById('paymentSearch');
    const fromDateEl = document.getElementById('paymentFromDate');
    const toDateEl = document.getElementById('paymentToDate');
    
    const status = statusEl ? statusEl.value : '';
    const search = searchEl ? searchEl.value : '';
    const fromDate = fromDateEl ? fromDateEl.value : '';
    const toDate = toDateEl ? toDateEl.value : '';
    
    let url = `/admin/api/payments/export`;
    const params = [];
    if (status) params.push(`status=${status}`);
    if (search) params.push(`search=${encodeURIComponent(search)}`);
    if (fromDate) params.push(`fromDate=${fromDate}`);
    if (toDate) params.push(`toDate=${toDate}`);
    if (params.length > 0) url += '?' + params.join('&');
    
    fetch(url)
        .then(response => response.json())
        .then(data => {
            exportToCSVFromData(data, 'payments-export.csv', [
                'paymentId', 'orderId', 'customerName', 'customerEmail', 
                'amount', 'paymentMethod', 'status', 'transactionId', 'createdAt', 'paidAt'
            ]);
        })
        .catch(error => console.error('Error exporting payments:', error));
}

function exportToCSVFromData(data, filename, fields) {
    if (!data || data.length === 0) {
        alert('No data to export');
        return;
    }
    
    const headers = fields.map(f => f.charAt(0).toUpperCase() + f.slice(1).replace(/([A-Z])/g, ' $1'));
    let csv = headers.join(',') + '\n';
    
    data.forEach(item => {
        const row = fields.map(field => {
            let value = item[field];
            if (value === null || value === undefined) return '';
            if (typeof value === 'object' && value.longValue) value = value.longValue();
            if (typeof value === 'string' && value.includes(',')) value = '"' + value + '"';
            return value;
        });
        csv += row.join(',') + '\n';
    });
    
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// ========== User Activity Functions ==========

let userListPage = 0;
let enhancedAuditLogPage = 0;

// Load User Statistics - Make sure it's in global scope
window.loadUserStatistics = function() {
    console.log('[DEBUG] ========== loadUserStatistics START ==========');
    console.log('[DEBUG] Calling API: /admin/api/users/statistics');
    
    // Check if elements exist
    const totalUsersEl = document.getElementById('userStatsTotalUsers');
    const activeUsersEl = document.getElementById('userStatsActiveUsers');
    const newUsersEl = document.getElementById('userStatsNewUsers');
    const retentionEl = document.getElementById('userStatsRetentionRate');
    const dauEl = document.getElementById('userStatsDAU');
    const wauEl = document.getElementById('userStatsWAU');
    const mauEl = document.getElementById('userStatsMAU');
    const feedbackEl = document.getElementById('userStatsFeedbackCount');
    
    console.log('[DEBUG] Elements check:', {
        totalUsersEl: !!totalUsersEl,
        activeUsersEl: !!activeUsersEl,
        newUsersEl: !!newUsersEl,
        retentionEl: !!retentionEl,
        dauEl: !!dauEl,
        wauEl: !!wauEl,
        mauEl: !!mauEl,
        feedbackEl: !!feedbackEl
    });
    
    fetch('/admin/api/users/statistics')
        .then(response => {
            console.log('[DEBUG] User statistics response status:', response.status);
            console.log('[DEBUG] Response headers:', response.headers);
            if (!response.ok) {
                console.error('[DEBUG] Response not OK:', response.status, response.statusText);
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('[DEBUG] User statistics data received:', data);
            console.log('[DEBUG] Data type:', typeof data);
            console.log('[DEBUG] Data keys:', Object.keys(data));
            
            if (totalUsersEl) totalUsersEl.textContent = data.totalUsers || 0;
            if (activeUsersEl) activeUsersEl.textContent = data.activeUsers || 0;
            if (newUsersEl) newUsersEl.textContent = data.newUsersThisMonth || 0;
            if (retentionEl) retentionEl.textContent = (data.retentionRate || 0).toFixed(1) + '%';
            if (dauEl) dauEl.textContent = data.dailyActiveUsers || 0;
            if (wauEl) wauEl.textContent = data.weeklyActiveUsers || 0;
            if (mauEl) mauEl.textContent = data.monthlyActiveUsers || 0;
            if (feedbackEl) feedbackEl.textContent = data.totalFeedbackCount || 0;
            
            console.log('[DEBUG] User statistics updated successfully');
        })
        .catch(error => {
            console.error('[DEBUG] Error loading user statistics:', error);
            console.error('[DEBUG] Error stack:', error.stack);
        });
};

// Load Users List - Make sure it's in global scope
window.loadUsers = function(page = 0) {
    console.log('[DEBUG] ========== loadUsers START ==========');
    console.log(`[DEBUG] loadUsers called with page: ${page}`);
    userListPage = page;
    
    const role = document.getElementById('userRoleFilter')?.value || '';
    const search = document.getElementById('userSearchFilter')?.value || '';
    const fromDate = document.getElementById('userFromDateFilter')?.value || '';
    const toDate = document.getElementById('userToDateFilter')?.value || '';
    
    const params = new URLSearchParams();
    if (role) params.append('role', role);
    if (search) params.append('search', search);
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);
    params.append('page', page);
    params.append('size', '20');
    
    const url = `/admin/api/users?${params.toString()}`;
    console.log(`[DEBUG] Fetching users from: ${url}`);
    
    fetch(url)
        .then(response => {
            console.log(`[DEBUG] Users response status: ${response.status}`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            console.log('[DEBUG] Received users data:', data);
            console.log('[DEBUG] Data type:', typeof data);
            console.log('[DEBUG] Has content?', !!data.content);
            console.log('[DEBUG] Content length:', data.content ? data.content.length : 0);
            
            const tbody = document.getElementById('userListBody');
            if (!tbody) {
                console.error('[DEBUG] userListBody element not found!');
                console.error('[DEBUG] Available elements with "user" in id:');
                document.querySelectorAll('[id*="user"]').forEach(el => {
                    console.log('[DEBUG] -', el.id);
                });
                return;
            }
            
            console.log('[DEBUG] userListBody found, updating table...');
            
            if (!data.content || data.content.length === 0) {
                console.log('[DEBUG] No users in data, showing empty message');
                tbody.innerHTML = '<tr><td colspan="11" class="text-center text-muted">No users found</td></tr>';
                return;
            }
            
            console.log('[DEBUG] Rendering', data.content.length, 'users');
            tbody.innerHTML = data.content.map(user => `
                <tr>
                    <td>${user.userId || 'N/A'}</td>
                    <td>${user.userName || 'N/A'}</td>
                    <td>${user.email || 'N/A'}</td>
                    <td><span class="badge badge-info">${user.role || 'N/A'}</span></td>
                    <td>${user.registrationDate ? new Date(user.registrationDate).toLocaleDateString() : 'N/A'}</td>
                    <td>${user.lastActivityDate ? new Date(user.lastActivityDate).toLocaleDateString() : 'N/A'}</td>
                    <td>${user.totalEvents || 0}</td>
                    <td>${user.totalOrders || 0}</td>
                    <td>${formatCurrency(user.totalSpent || 0)}</td>
                    <td>${user.totalPoints || 0}</td>
                    <td>${user.feedbackCount || 0}</td>
                </tr>
            `).join('');
            
            console.log('[DEBUG] Table updated, rendering pagination...');
            // Render pagination
            renderPagination('userListPagination', data, window.loadUsers);
            console.log('[DEBUG] Users list loaded successfully');
        })
        .catch(error => {
            console.error('Error loading users:', error);
            const tbody = document.getElementById('userListBody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="11" class="text-center text-danger">Error loading data</td></tr>';
        });
};

// Load Enhanced Audit Logs - Make sure it's in global scope
window.loadEnhancedAuditLogs = function(page = 0) {
    console.log('[DEBUG] ========== loadEnhancedAuditLogs START ==========');
    console.log(`[DEBUG] loadEnhancedAuditLogs called with page: ${page}`);
    enhancedAuditLogPage = page;
    
    const actionType = document.getElementById('actionTypeFilter')?.value || '';
    const entityType = document.getElementById('entityTypeFilter')?.value || '';
    const userId = document.getElementById('userIdFilter')?.value || '';
    const search = document.getElementById('auditLogSearchFilter')?.value || '';
    const fromDate = document.getElementById('fromDateFilter')?.value || '';
    const toDate = document.getElementById('toDateFilter')?.value || '';
    
    const params = new URLSearchParams();
    if (actionType) params.append('actionType', actionType);
    if (entityType) params.append('entityType', entityType);
    if (userId) params.append('userId', userId);
    if (search) params.append('search', search);
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);
    params.append('page', page);
    params.append('size', '20');
    
    const url = `/admin/api/audit-logs/enhanced?${params.toString()}`;
    console.log(`[DEBUG] Fetching enhanced audit logs from: ${url}`);
    
    fetch(url)
        .then(response => {
            console.log(`[DEBUG] Enhanced audit logs response status: ${response.status}`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            console.log('[DEBUG] Received enhanced audit logs data:', data);
            console.log('[DEBUG] Data type:', typeof data);
            console.log('[DEBUG] Has content?', !!data.content);
            console.log('[DEBUG] Content length:', data.content ? data.content.length : 0);
            
            const tbody = document.getElementById('enhancedAuditLogBody');
            if (!tbody) {
                console.error('[DEBUG] enhancedAuditLogBody element not found!');
                console.error('[DEBUG] Available elements with "audit" in id:');
                document.querySelectorAll('[id*="audit"]').forEach(el => {
                    console.log('[DEBUG] -', el.id);
                });
                return;
            }
            
            console.log('[DEBUG] enhancedAuditLogBody found, updating table...');
            
            if (!data.content || data.content.length === 0) {
                console.log('[DEBUG] No audit logs in data, showing empty message');
                tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No audit logs found</td></tr>';
                return;
            }
            
            console.log('[DEBUG] Rendering', data.content.length, 'audit logs');
            tbody.innerHTML = data.content.map(log => `
                <tr>
                    <td>${log.timestamp ? new Date(log.timestamp).toLocaleString() : 'N/A'}</td>
                    <td>${log.userName || 'System'}<br><small class="text-muted">${log.userEmail || 'N/A'}</small></td>
                    <td><span class="badge badge-secondary">${log.userRole || 'SYSTEM'}</span></td>
                    <td><span class="badge badge-warning">${log.actionType || 'N/A'}</span></td>
                    <td><span class="badge badge-info">${log.entityType || 'N/A'}</span></td>
                    <td>${log.entityDetails || 'N/A'}</td>
                    <td>${log.description || 'N/A'}</td>
                </tr>
            `).join('');
            
            console.log('[DEBUG] Table updated, rendering pagination...');
            // Render pagination
            renderPagination('enhancedAuditLogPagination', data, window.loadEnhancedAuditLogs);
            console.log('[DEBUG] Enhanced audit logs loaded successfully');
        })
        .catch(error => {
            console.error('Error loading enhanced audit logs:', error);
            const tbody = document.getElementById('enhancedAuditLogBody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Error loading data</td></tr>';
        });
};

// Export Users to CSV
function exportUsersToCSV() {
    const role = document.getElementById('userRoleFilter')?.value || '';
    const search = document.getElementById('userSearchFilter')?.value || '';
    const fromDate = document.getElementById('userFromDateFilter')?.value || '';
    const toDate = document.getElementById('userToDateFilter')?.value || '';
    
    const params = new URLSearchParams();
    if (role) params.append('role', role);
    if (search) params.append('search', search);
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);
    
    const url = `/admin/api/users?${params.toString()}&page=0&size=10000`;
    
    fetch(url)
        .then(response => response.json())
        .then(data => {
            if (data.content && data.content.length > 0) {
                exportToCSVFromData(data.content, 'users-export.csv', [
                    'userId', 'userName', 'email', 'role', 'status', 
                    'registrationDate', 'lastActivityDate', 'totalEvents', 
                    'totalOrders', 'totalTickets', 'totalSpent', 'totalPoints', 'feedbackCount'
                ]);
            } else {
                alert('No data to export');
            }
        })
        .catch(error => console.error('Error exporting users:', error));
}

// Export Enhanced Audit Logs to CSV
function exportAuditLogsToCSV() {
    const actionType = document.getElementById('actionTypeFilter')?.value || '';
    const entityType = document.getElementById('entityTypeFilter')?.value || '';
    const userId = document.getElementById('userIdFilter')?.value || '';
    const search = document.getElementById('auditLogSearchFilter')?.value || '';
    const fromDate = document.getElementById('fromDateFilter')?.value || '';
    const toDate = document.getElementById('toDateFilter')?.value || '';
    
    const params = new URLSearchParams();
    if (actionType) params.append('actionType', actionType);
    if (entityType) params.append('entityType', entityType);
    if (userId) params.append('userId', userId);
    if (search) params.append('search', search);
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);
    
    const url = `/admin/api/audit-logs/export?${params.toString()}`;
    
    fetch(url)
        .then(response => response.json())
        .then(data => {
            if (data && data.length > 0) {
                exportToCSVFromData(data, 'audit-logs-export.csv', [
                    'timestamp', 'userName', 'userEmail', 'userRole', 
                    'actionType', 'entityType', 'entityId', 'description', 'entityDetails'
                ]);
            } else {
                alert('No data to export');
            }
        })
        .catch(error => console.error('Error exporting audit logs:', error));
}

// Debounce search function for callbacks
let userSearchTimeout;
function debounceSearchCallback(callback, delay = 500) {
    clearTimeout(userSearchTimeout);
    userSearchTimeout = setTimeout(callback, delay);
}
