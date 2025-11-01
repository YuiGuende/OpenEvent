// orders.js - Complete Orders management functionality
console.log('🔥 orders.js loaded!');

// Global variables
let ordersData = [];
let currentOrdersData = [];
let detailModal = null;

// Immediate function assignment to prevent ReferenceError
window.selectStatusFilter = function(element) {
    console.log('🔍 selectStatusFilter called');
    event.preventDefault();
    const status = element.getAttribute('data-status');
    console.log('Selected status:', status);
    
    // Update dropdown text
    const textElement = document.getElementById('statusFilterText');
    if (textElement) {
        const statusTexts = {
            '': 'Tất cả trạng thái',
            'PENDING': 'Chờ xử lý',
            'PAID': 'Đã thanh toán',
            'CANCELLED': 'Đã hủy',
            'EXPIRED': 'Hết hạn',
            'REFUNDED': 'Hoàn tiền'
        };
        textElement.textContent = statusTexts[status] || 'Tất cả trạng thái';
    }
    
    // Apply filter
    applyFilter(status);
};

window.viewOrderDetail = function(button) {
    console.log('🔍 viewOrderDetail called');
    const orderId = button.getAttribute('data-order-id');
    console.log('Order ID:', orderId);
    
    // Find order data
    const order = findOrderById(orderId);
    if (!order) {
        console.warn('Order not found:', orderId);
        alert('Không tìm thấy thông tin đơn hàng #' + orderId);
        return;
    }
    
    // Populate modal
    populateModal(order);
    
    // Show modal
    showModal();
};

// Helper functions
function findOrderById(orderId) {
    // First try to find in current data
    let order = currentOrdersData.find(o => o.orderId == orderId);
    
    if (!order) {
        // Try to build from DOM
        buildOrdersFromDom();
        order = currentOrdersData.find(o => o.orderId == orderId);
    }
    
    return order;
}

function buildOrdersFromDom() {
    const tbody = document.querySelector('.orders-table tbody');
    if (!tbody) {
        console.warn('No orders table found');
        return;
    }
    
    const rows = Array.from(tbody.querySelectorAll('tr'));
    ordersData = rows.map((tr) => {
        const cells = tr.querySelectorAll('td');
        if (cells.length < 7) return null;
        
        const orderId = parseInt(cells[0]?.querySelector('.fw-semibold')?.textContent?.replace('#', '') || '0');
        const eventTitle = cells[1]?.querySelector('.fw-medium')?.textContent?.trim() || '';
        const ticketTypeName = cells[2]?.textContent?.trim() || '';
        const participantName = cells[3]?.textContent?.trim() || '';
        const statusText = cells[4]?.querySelector('.status-badge')?.textContent?.trim() || '';
        const amountText = cells[5]?.textContent?.trim() || '';
        const createdAtText = cells[6]?.textContent?.trim() || '';
        
        return {
            orderId,
            eventTitle,
            ticketTypeName,
            participantName,
            status: statusText,
            formattedAmount: amountText,
            totalAmount: parseAmount(amountText),
            createdAt: createdAtText,
            customerEmail: ''
        };
    }).filter(o => o && o.orderId > 0);
    
    currentOrdersData = [...ordersData];
    console.log('📊 Built orders data from DOM:', ordersData);
}

function parseAmount(text) {
    if (!text) return 0;
    const cleaned = text.replace(/[^\d]/g, '');
    return parseInt(cleaned || '0', 10);
}

function populateModal(order) {
    document.getElementById('detailOrderId').textContent = order.orderId;
    document.getElementById('detailEventTitle').textContent = order.eventTitle;
    document.getElementById('detailTicketType').textContent = order.ticketTypeName;
    document.getElementById('detailParticipantName').textContent = order.participantName;
    document.getElementById('detailCustomerEmail').textContent = order.customerEmail;
    document.getElementById('detailStatus').innerHTML = `<span class="status-badge status-${order.status.toLowerCase()}">${order.status}</span>`;
    document.getElementById('detailCreatedAt').textContent = formatDate(order.createdAt);
    document.getElementById('detailTotalAmount').textContent = order.formattedAmount;
}

function showModal() {
    const modal = document.getElementById('detailModal');
    if (modal) {
        console.log('🔍 Showing modal...');
        
        // Try Bootstrap modal first
        if (window.bootstrap && bootstrap.Modal) {
            console.log('Using Bootstrap modal');
            if (!detailModal) {
                detailModal = new bootstrap.Modal(modal, {
                    backdrop: true,
                    keyboard: true
                });
            }
            detailModal.show();
        } else {
            console.log('Using fallback modal');
            // Fallback: manual modal show
            modal.style.display = 'block';
            modal.classList.add('show');
            
            // Add backdrop
            const backdrop = document.createElement('div');
            backdrop.className = 'modal-backdrop fade show';
            backdrop.id = 'modalBackdrop';
            document.body.appendChild(backdrop);
            // Lock body scroll similar to Bootstrap behavior
            document.body.classList.add('modal-open');
            document.body.style.overflow = 'hidden';
            
            // Add click handler to close
            backdrop.addEventListener('click', closeModal);
            
            // Add escape key handler
            document.addEventListener('keydown', function(e) {
                if (e.key === 'Escape') {
                    closeModal();
                }
            });
        }
    }
}

function closeModal() {
    console.log('🔍 Closing modal...');
    const modal = document.getElementById('detailModal');
    if (modal) {
        if (detailModal && typeof detailModal.hide === 'function') {
            detailModal.hide();
        } else {
            // Manual close
            modal.style.display = 'none';
            modal.classList.remove('show');
            
            // Remove any backdrops and unlock body scroll
            const backdrops = document.querySelectorAll('.modal-backdrop');
            backdrops.forEach(el => el.remove());
            document.body.classList.remove('modal-open');
            document.body.style.removeProperty('overflow');
            document.body.style.removeProperty('paddingRight');
        }
        // Safety: also remove leftover backdrops when using Bootstrap
        const strayBackdrops = document.querySelectorAll('.modal-backdrop');
        if (strayBackdrops.length) {
            strayBackdrops.forEach(el => el.remove());
            document.body.classList.remove('modal-open');
            document.body.style.removeProperty('overflow');
            document.body.style.removeProperty('paddingRight');
        }
    }
}

function applyFilter(status) {
    console.log('🔍 Applying filter:', status);
    
    let filtered = [...ordersData];
    
    if (status) {
        filtered = filtered.filter(o => o.status === status);
    }
    
    console.log('📊 Filtered results:', filtered.length);
    renderOrders(filtered);
}

function renderOrders(list) {
    const tbody = document.querySelector('.orders-table tbody');
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted py-4">Không có đơn hàng phù hợp</td></tr>`;
        return;
    }

    const rows = list.map(order => `
        <tr>
            <td><span class="fw-semibold text-secondary">#${order.orderId}</span></td>
            <td>
                <div class="d-flex align-items-center gap-3">
                    <img src="${order.eventImageUrl || '/images/default-event.jpg'}" alt="Event" class="rounded" style="width: 50px; height: 50px; object-fit: cover;"/>
                    <span class="fw-medium text-truncate" style="max-width: 200px;">${order.eventTitle || ''}</span>
                </div>
            </td>
            <td>${order.ticketTypeName || ''}</td>
            <td>${order.participantName || ''}</td>
            <td><span class="status-badge status-${(order.status||'').toLowerCase()}">${order.status || ''}</span></td>
            <td><span class="fw-bold fs-6" style="color: var(--orange-primary);">${order.formattedAmount || formatCurrency(order.totalAmount || 0)}</span></td>
            <td>${formatDate(order.createdAt)}</td>
            <td>
                <button class="btn btn-sm btn-outline-primary" style="border-color: var(--orange-primary); color: var(--orange-primary);" data-order-id="${order.orderId}" onclick="viewOrderDetail(this)">Xem chi tiết</button>
            </td>
        </tr>
    `).join('');

    tbody.innerHTML = rows;
}

function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;
    return date.toLocaleString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

// Initialize when DOM is ready
function initializeOrders() {
    console.log('🚀 Initializing orders page...');
    
    // Build orders data from DOM
    buildOrdersFromDom();
    
    // Bind event listeners
    bindEventListeners();
    
    console.log('✅ Orders page initialized');
}

function bindEventListeners() {
    // Search input
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        let searchTimeout;
        searchInput.addEventListener('input', () => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => applySearch(), 250);
        });
    }

    // Sort dropdown
    const sortDropdown = document.getElementById('sortDropdown');
    if (sortDropdown) {
        sortDropdown.addEventListener('change', () => applySort());
    }

    // Export button
    const exportBtn = document.getElementById('exportBtn');
    if (exportBtn) {
        exportBtn.addEventListener('click', () => {
            console.log('📤 Exporting orders...');
            alert('Chức năng xuất dữ liệu đang được phát triển');
        });
    }

    // Modal close button
    const modalCloseBtn = document.querySelector('#detailModal .btn-close');
    if (modalCloseBtn) {
        modalCloseBtn.addEventListener('click', closeModal);
        console.log('✅ Modal close button bound');
    }

    // Modal backdrop click
    const modal = document.getElementById('detailModal');
    if (modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                closeModal();
            }
        });
    }

    console.log('✅ Event listeners bound');
}

function applySearch() {
    const term = (document.getElementById('searchInput')?.value || '').toLowerCase().trim();
    console.log('🔍 Searching for:', term);
    
    let filtered = [...ordersData];
    
    if (term) {
        filtered = filtered.filter(o => {
            const id = ('#' + o.orderId).toLowerCase();
            const eventTitle = (o.eventTitle || '').toLowerCase();
            const name = (o.participantName || '').toLowerCase();
            return id.includes(term) || eventTitle.includes(term) || name.includes(term);
        });
    }
    
    console.log('📊 Search results:', filtered.length);
    renderOrders(filtered);
}

function applySort() {
    const sort = document.getElementById('sortDropdown')?.value || 'newest';
    console.log('🔍 Sorting by:', sort);
    
    let sorted = [...currentOrdersData];
    
    sorted.sort((a, b) => {
        if (sort === 'newest') return new Date(b.createdAt) - new Date(a.createdAt);
        if (sort === 'oldest') return new Date(a.createdAt) - new Date(b.createdAt);
        if (sort === 'amount-high') return (b.totalAmount || 0) - (a.totalAmount || 0);
        if (sort === 'amount-low') return (a.totalAmount || 0) - (b.totalAmount || 0);
        return 0;
    });
    
    console.log('📊 Sorted results:', sorted.length);
    renderOrders(sorted);
}

// Auto-initialize
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeOrders);
} else {
    setTimeout(initializeOrders, 100);
}

// Export for manual initialization
window.initializeOrders = initializeOrders;
window.closeModal = closeModal;
console.log('✅ orders.js setup complete');