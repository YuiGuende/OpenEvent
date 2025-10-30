// order.js - JavaScript cho Order management với TicketType
class OrderManager {
    constructor() {
        this.apiBase = '/api';
        // State for filtering/sorting
        this.allOrders = [];
        this.filters = {
            status: 'ALL', // ALL | PENDING | PAID | CANCELLED
            sort: 'NEWEST' // NEWEST | OLDEST | PRICE_HIGH | PRICE_LOW
        };
        this.init();
    }

    init() {
        this.bindEvents();
        this.loadUserOrders();
    }

    bindEvents() {
        // Bind order creation form
        const orderForm = document.getElementById('orderForm');
        if (orderForm) {
            orderForm.addEventListener('submit', (e) => this.handleCreateOrder(e));
        }

        // Note: Ticket type and quantity handling is now done in HTML directly

        // Filters & Sorting
        const statusFilter = document.getElementById('statusFilter');
        if (statusFilter) {
            statusFilter.addEventListener('change', (e) => {
                this.filters.status = e.target.value || 'ALL';
                this.applyFiltersAndRender();
            });
        }

        const sortSelect = document.getElementById('sortSelect');
        if (sortSelect) {
            sortSelect.addEventListener('change', (e) => {
                this.filters.sort = e.target.value || 'NEWEST';
                this.applyFiltersAndRender();
            });
        }

        // Bind payment buttons
        const paymentButtons = document.querySelectorAll('.btn-payment');
        paymentButtons.forEach(button => {
            button.addEventListener('click', (e) => this.handlePayment(e));
        });

        // Bind cancel buttons
        const cancelButtons = document.querySelectorAll('.btn-cancel-order');
        cancelButtons.forEach(button => {
            button.addEventListener('click', (e) => this.handleCancelOrder(e));
        });
    }

    async loadUserOrders() {
        try {
            const response = await fetch(`${this.apiBase}/orders/my-orders`, {
                method: 'GET',
                credentials: 'include'
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();
            // API có thể trả về: Array<Order> hoặc { success, orders }
            let orders = [];
            if (Array.isArray(data)) {
                orders = data;
            } else if (data && Array.isArray(data.orders)) {
                orders = data.orders;
            } else if (data && data.success && data.data && Array.isArray(data.data)) {
                orders = data.data;
            }

            this.allOrders = orders;
            this.applyFiltersAndRender();
        } catch (error) {
            console.error('Error loading orders:', error);
            this.showNotification('Lỗi khi tải danh sách đơn hàng', 'error');
        }
    }

    applyFiltersAndRender() {
        // Filter by status
        let filtered = this.allOrders.filter(order => {
            if (!this.filters.status || this.filters.status === 'ALL') return true;
            return order.status === this.filters.status;
        });

        // Sort
        filtered.sort((a, b) => {
            switch (this.filters.sort) {
                case 'OLDEST':
                    return new Date(a.createdAt) - new Date(b.createdAt);
                case 'PRICE_HIGH':
                    return (b.totalAmount || 0) - (a.totalAmount || 0);
                case 'PRICE_LOW':
                    return (a.totalAmount || 0) - (b.totalAmount || 0);
                case 'NEWEST':
                default:
                    return new Date(b.createdAt) - new Date(a.createdAt);
            }
        });

        this.displayOrders(filtered);
    }

    displayOrders(orders) {
        const ordersContainer = document.getElementById('ordersContainer');
        if (!ordersContainer) return;

        if (orders.length === 0) {
            ordersContainer.innerHTML = '<p class="text-center text-muted">Bạn chưa có đơn hàng nào.</p>';
            return;
        }

        const ordersHTML = orders.map(order => this.createOrderCard(order)).join('');
        ordersContainer.innerHTML = ordersHTML;

        // Re-bind events for new elements
        this.bindOrderEvents();

        // Update total count text if available
        const totalCountEl = document.getElementById('ordersCount');
        if (totalCountEl) {
            totalCountEl.textContent = `${orders.length}`;
        }
    }

    createOrderCard(order) {
        const statusClass = this.getStatusClass(order.status);
        const statusText = this.getStatusText(order.status);

        return `
            <div class="order-card" data-order-id="${order.orderId}">
                <div class="order-header">
                    <h3>Đơn hàng #${order.orderId}</h3>
                    <span class="order-status ${statusClass}">${statusText}</span>
                </div>
                <div class="order-details">
                    <p><strong>Sự kiện:</strong> ${order.event?.title || 'N/A'}</p>
                    <p><strong>Tổng tiền:</strong> ${this.formatCurrency(order.totalAmount)}</p>
                    <p><strong>Ngày tạo:</strong> ${this.formatDate(order.createdAt)}</p>
                </div>
                <div class="order-items">
                    <h4>Chi tiết vé:</h4>
                    <div class="order-item">
                        <span>${order.ticketType?.name || 'N/A'}</span>
                        <span>x${order.quantity || 1}</span>
                        <span>${this.formatCurrency(order.totalAmount)}</span>
                    </div>
                </div>
                <div class="order-actions">
                    ${this.getOrderActions(order)}
                </div>
            </div>
        `;
    }

    getOrderActions(order) {
        const actions = [];

        if (order.status === 'PENDING') {
            actions.push(`
                <button class="btn btn-primary btn-payment" data-order-id="${order.orderId}">
                    Thanh toán
                </button>
            `);
            actions.push(`
                <button class="btn btn-secondary btn-cancel-order" data-order-id="${order.orderId}">
                    Hủy đơn
                </button>
            `);
        }

        if (order.status === 'PAID') {
            actions.push(`
                <button class="btn btn-success" disabled>
                    Đã thanh toán
                </button>
            `);
        }

        if (order.status === 'CANCELLED') {
            actions.push(`
                <button class="btn btn-danger" disabled>
                    Đã hủy
                </button>
            `);
        }

        // Detail button
        actions.push(`
            <button class="btn btn-outline" data-action="view-details" data-order-id="${order.orderId}">
                Xem chi tiết
            </button>
        `);

        return actions.join('');
    }

    getStatusClass(status) {
        const statusClasses = {
            'PENDING': 'status-pending',
            'PAID': 'status-paid',
            'CANCELLED': 'status-cancelled'
        };
        return statusClasses[status] || 'status-unknown';
    }

    getStatusText(status) {
        const statusTexts = {
            'PENDING': 'Chờ thanh toán',
            'PAID': 'Đã thanh toán',
            'CANCELLED': 'Đã hủy'
        };
        return statusTexts[status] || 'Không xác định';
    }

    async handleCreateOrder(event) {
        event.preventDefault();

        const formData = new FormData(event.target);
        const orderData = this.collectOrderData(formData);

        try {
            const response = await fetch(`${this.apiBase}/orders/with-ticket-types`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(orderData)
            });

            const result = await response.json();

            if (result.success) {
                this.showNotification('Tạo đơn hàng thành công!', 'success');
                this.loadUserOrders();
                event.target.reset();
            } else {
                this.showNotification(result.message || 'Lỗi khi tạo đơn hàng', 'error');
            }
        } catch (error) {
            console.error('Error creating order:', error);
            this.showNotification('Lỗi khi tạo đơn hàng', 'error');
        }
    }

    collectOrderData(formData) {
        const orderData = {
            eventId: parseInt(formData.get('eventId')),
            participantName: formData.get('participantName'),
            participantEmail: formData.get('participantEmail'),
            participantPhone: formData.get('participantPhone'),
            participantOrganization: formData.get('participantOrganization'),
            notes: formData.get('notes'),
            order: null
        };

        // Collect the selected ticket type and quantity
        const ticketTypeId = formData.get('ticketTypeId');
        const quantity = parseInt(formData.get('quantity')) || 0;

        if (ticketTypeId && quantity > 0) {
            orderData.order = {
                ticketTypeId: parseInt(ticketTypeId),
                quantity: quantity
            };
        }

        return orderData;
    }

    async handlePayment(event) {
        const orderId = event.target.dataset.orderId;

        try {
            const response = await fetch(`${this.apiBase}/payments/create-for-order/${orderId}`, {
                method: 'POST',
                credentials: 'include'
            });

            const result = await response.json();

            if (result.success) {
                // Redirect to payment URL
                window.location.href = result.checkoutUrl;
            } else {
                this.showNotification(result.message || 'Lỗi khi tạo thanh toán', 'error');
            }
        } catch (error) {
            console.error('Error creating payment:', error);
            this.showNotification('Lỗi khi tạo thanh toán', 'error');
        }
    }

    async handleCancelOrder(event) {
        const orderId = event.target.dataset.orderId;

        if (!confirm('Bạn có chắc chắn muốn hủy đơn hàng này?')) {
            return;
        }

        try {
            const response = await fetch(`${this.apiBase}/orders/${orderId}/cancel`, {
                method: 'POST',
                credentials: 'include'
            });

            const result = await response.json();

            if (result.success) {
                this.showNotification('Hủy đơn hàng thành công!', 'success');
                this.loadUserOrders();
            } else {
                this.showNotification(result.message || 'Lỗi khi hủy đơn hàng', 'error');
            }
        } catch (error) {
            console.error('Error cancelling order:', error);
            this.showNotification('Lỗi khi hủy đơn hàng', 'error');
        }
    }

    // Note: Ticket type change and total calculation are now handled in HTML directly

    bindOrderEvents() {
        // Re-bind events for dynamically created elements
        const paymentButtons = document.querySelectorAll('.btn-payment');
        paymentButtons.forEach(button => {
            button.addEventListener('click', (e) => this.handlePayment(e));
        });

        const cancelButtons = document.querySelectorAll('.btn-cancel-order');
        cancelButtons.forEach(button => {
            button.addEventListener('click', (e) => this.handleCancelOrder(e));
        });

        const detailButtons = document.querySelectorAll('[data-action="view-details"]');
        detailButtons.forEach(button => {
            button.addEventListener('click', (e) => this.handleViewDetails(e));
        });
    }

    handleViewDetails(event) {
        const orderId = parseInt(event.currentTarget.dataset.orderId);
        const order = this.allOrders.find(o => o.orderId === orderId);
        if (!order) return;

        const modal = document.getElementById('orderDetailModal');
        const body = document.getElementById('orderDetailBody');
        if (!modal || !body) return;

        const items = order.orderItems || [];
        const itemsHtml = items.map(item => `
            <tr>
                <td>${item.ticketType?.name || 'N/A'}</td>
                <td class="text-center">${item.quantity}</td>
                <td class="text-right">${this.formatCurrency(item.subtotal)}</td>
            </tr>
        `).join('');

        body.innerHTML = `
            <p><strong>Đơn hàng #${order.orderId}</strong></p>
            <p><strong>Sự kiện:</strong> ${order.event?.title || 'N/A'}</p>
            <p><strong>Trạng thái:</strong> ${this.getStatusText(order.status)}</p>
            <p><strong>Tổng tiền:</strong> ${this.formatCurrency(order.totalAmount)}</p>
            <p><strong>Ngày tạo:</strong> ${this.formatDate(order.createdAt)}</p>
            <div class="table-responsive">
                <table class="details-table">
                    <thead>
                        <tr>
                            <th>Loại vé</th>
                            <th class="text-center">Số lượng</th>
                            <th class="text-right">Thành tiền</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${itemsHtml}
                    </tbody>
                </table>
            </div>
        `;

        modal.style.display = 'block';

        const closeBtn = modal.querySelector('[data-close-modal]');
        if (closeBtn) {
            closeBtn.onclick = () => modal.style.display = 'none';
        }
        modal.onclick = (e) => {
            if (e.target === modal) modal.style.display = 'none';
        };
    }

    formatCurrency(amount) {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(amount);
    }

    formatDate(dateString) {
        if (!dateString) return '';
        const d = new Date(dateString);
        if (Number.isNaN(d.getTime())) return '';
        return d.toLocaleDateString('vi-VN');
    }

    showNotification(message, type = 'info') {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;

        // Add to page
        document.body.appendChild(notification);

        // Remove after 3 seconds
        setTimeout(() => {
            notification.remove();
        }, 3000);
    }
}

// Initialize OrderManager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new OrderManager();
});

// Export for use in other modules
window.OrderManager = OrderManager;