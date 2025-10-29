// Ticket Management System
class TicketManager {
    constructor() {
        this.tickets = []
        this.editingId = null
        this.deleteId = null
        this.init()
    }

    /**
     * Khởi tạo danh sách ticket types từ server
     * @param {Array} serverTicketTypes - Danh sách ticket types từ server
     */
    initializeTicketTypes(serverTicketTypes) {
        console.log('🔍 TicketManager: Initializing ticket types with server data:', serverTicketTypes);
        console.log('🔍 TicketManager: serverTicketTypes type:', typeof serverTicketTypes);
        console.log('🔍 TicketManager: serverTicketTypes length:', serverTicketTypes ? serverTicketTypes.length : 'null/undefined');
        
        // Convert server data to local format
        this.tickets = serverTicketTypes ? serverTicketTypes.map(ticket => ({
            id: ticket.ticketTypeId.toString(),
            name: ticket.name,
            description: ticket.description || '',
            price: ticket.price || 0,
            totalQuantity: ticket.totalQuantity || 0,
            soldQuantity: ticket.soldQuantity || 0,
            sale: ticket.sale || 0,
            startSaleDate: ticket.startSaleDate ? new Date(ticket.startSaleDate).toISOString().slice(0, 16) : '',
            endSaleDate: ticket.endSaleDate ? new Date(ticket.endSaleDate).toISOString().slice(0, 16) : '',
            isNew: false,
            isDeleted: false
        })) : [];
        
        console.log('🔍 TicketManager: After initialization - tickets:', this.tickets);
        console.log('🔍 TicketManager: About to render tickets...');
        
        this.renderTickets();
        
        console.log('🔍 TicketManager: Tickets rendered successfully');
    }

    init() {
        this.setupEventListeners()
        this.setupRichTextEditor()
        this.setupFormValidation()
        this.showCreateForm() // Ensure form starts in create mode
        this.renderTickets()
    }

    // Local Storage
    loadTickets() {
        const stored = localStorage.getItem("tickets")
        return stored ? JSON.parse(stored) : []
    }

    saveTickets() {
        localStorage.setItem("tickets", JSON.stringify(this.tickets))
    }

    // Event Listeners
    setupEventListeners() {
        console.log("Setting up event listeners...")
        
        // Form submit - only setup if form exists (ticket page)
        const form = document.getElementById("ticket-form")
        if (form) {
            form.addEventListener("submit", (e) => {
                e.preventDefault()
                console.log("Form submit event triggered")
                this.handleSubmit()
            })
            console.log("Form submit listener attached")
        } else {
            console.log("Ticket form not found - skipping form setup (likely on settings page)")
        }

        // Cancel edit
        const cancelBtn = document.getElementById("cancel-edit-btn")
        if (cancelBtn) {
            cancelBtn.addEventListener("click", () => {
                this.cancelEdit()
            })
        }

        // Modal buttons
        const cancelDeleteBtn = document.getElementById("cancel-delete-btn")
        if (cancelDeleteBtn) {
            cancelDeleteBtn.addEventListener("click", () => {
                this.closeDeleteModal()
            })
        }

        const confirmDeleteBtn = document.getElementById("confirm-delete-btn")
        if (confirmDeleteBtn) {
            confirmDeleteBtn.addEventListener("click", () => {
                this.confirmDelete()
            })
        }

        // Close modal on outside click
        const deleteModal = document.getElementById("delete-modal")
        if (deleteModal) {
            deleteModal.addEventListener("click", (e) => {
                if (e.target.id === "delete-modal") {
                    this.closeDeleteModal()
                }
            })
        }
        
        console.log("Event listeners setup complete")
    }

    // Rich Text Editor
    setupRichTextEditor() {
        const editor = document.getElementById("ticket-description")
        const toolbar = document.querySelectorAll(".toolbar-btn")

        toolbar.forEach((btn) => {
            btn.addEventListener("click", (e) => {
                e.preventDefault()
                const command = btn.dataset.command
                document.execCommand(command, false, null)
                editor.focus()

                // Toggle active state for some commands
                if (["bold", "italic", "underline"].includes(command)) {
                    btn.classList.toggle("active")
                }
            })
        })

        // Update toolbar state on selection change
        editor.addEventListener("mouseup", () => this.updateToolbarState())
        editor.addEventListener("keyup", () => this.updateToolbarState())
    }

    updateToolbarState() {
        const commands = ["bold", "italic", "underline"]
        commands.forEach((cmd) => {
            const btn = document.querySelector(`[data-command="${cmd}"]`)
            if (btn) {
                if (document.queryCommandState(cmd)) {
                    btn.classList.add("active")
                } else {
                    btn.classList.remove("active")
                }
            }
        })
    }

    // Form Validation
    setupFormValidation() {
        const form = document.getElementById("ticket-form")
        const inputs = form.querySelectorAll("input[required], input[type='number']")
        
        inputs.forEach(input => {
            // Real-time validation on input
            input.addEventListener("input", () => {
                this.validateField(input)
            })
            
            // Validation on blur
            input.addEventListener("blur", () => {
                this.validateField(input)
            })
        })
        
        // Special validation for date range
        const startDate = document.getElementById("ticket-start-date")
        const endDate = document.getElementById("ticket-end-date")
        
        if (startDate && endDate) {
            startDate.addEventListener("change", () => {
                this.validateDateRange()
            })
            
            endDate.addEventListener("change", () => {
                this.validateDateRange()
            })
        }
    }

    validateField(field) {
        const value = field.value.trim()
        const isValid = this.isFieldValid(field, value)
        
        // Remove existing validation classes
        field.classList.remove("is-valid", "is-invalid")
        
        if (value.length > 0) {
            if (isValid) {
                field.classList.add("is-valid")
            } else {
                field.classList.add("is-invalid")
            }
        }
        
        return isValid
    }

    isFieldValid(field, value) {
        const fieldId = field.id
        
        switch (fieldId) {
            case "ticket-name":
                return value.length >= 2
                
            case "ticket-price":
                const price = parseFloat(value)
                return !isNaN(price) && price >= 0
                
            case "ticket-sale":
                const sale = parseFloat(value)
                return !isNaN(sale) && sale >= 0 && sale <= 100
                
            case "ticket-quantity":
                const quantity = parseInt(value)
                return !isNaN(quantity) && quantity >= 1
                
            case "ticket-start-date":
            case "ticket-end-date":
                return value.length > 0
                
            default:
                return true
        }
    }

    validateDateRange() {
        const startDate = document.getElementById("ticket-start-date")
        const endDate = document.getElementById("ticket-end-date")
        
        if (startDate.value && endDate.value) {
            const start = new Date(startDate.value)
            const end = new Date(endDate.value)
            
            if (start >= end) {
                endDate.classList.remove("is-valid")
                endDate.classList.add("is-invalid")
            } else {
                endDate.classList.remove("is-invalid")
                if (endDate.value) {
                    endDate.classList.add("is-valid")
                }
            }
        }
    }

    // Form Handling
    handleSubmit() {
        console.log("Form submitted!")
        const formData = this.getFormData()
        console.log("Form data:", formData)

        if (!this.validateForm(formData)) {
            console.log("Form validation failed")
            return
        }

        console.log("Form validation passed, processing...")
        if (this.editingId) {
            this.updateTicket(this.editingId, formData)
        } else {
            this.createTicket(formData)
        }

        // After successful submission, show create form (no cancel button)
        this.showCreateForm()
        this.renderTickets()

        // Persist immediately to server when clicking save
        this.saveTicketsToServer()
    }

    getFormData() {
        return {
            id: this.editingId || Date.now().toString(),
            name: document.getElementById("ticket-name").value.trim(),
            price: Number.parseFloat(document.getElementById("ticket-price").value) || 0,
            totalQuantity: Number.parseInt(document.getElementById("ticket-quantity").value) || 0,
            soldQuantity: this.editingId ? this.tickets.find((t) => t.id === this.editingId)?.soldQuantity || 0 : 0,
            sale: Number.parseFloat(document.getElementById("ticket-sale").value) || 0,
            startSaleDate: document.getElementById("ticket-start-date").value,
            endSaleDate: document.getElementById("ticket-end-date").value,
            description: document.getElementById("ticket-description").innerHTML,
        }
    }

    validateForm(data) {
        if (!data.name) {
            alert("Vui lòng nhập tên vé!")
            return false
        }

        if (data.price < 0) {
            alert("Giá vé không hợp lệ!")
            return false
        }

        if (data.totalQuantity < 1) {
            alert("Số lượng vé phải lớn hơn 0!")
            return false
        }

        if (data.sale < 0 || data.sale > 100) {
            alert("Giảm giá phải từ 0-100%!")
            return false
        }

        if (new Date(data.startSaleDate) >= new Date(data.endSaleDate)) {
            alert("Ngày kết thúc phải sau ngày bắt đầu!")
            return false
        }

        return true
    }

    createTicket(data) {
        data.isNew = true;
        data.isDeleted = false;
        this.tickets.unshift(data);
        this.renderTickets();
        if (typeof this.showNotification === 'function') {
            this.showNotification("Tạo vé thành công!", "success")
        }
    }

    updateTicket(id, data) {
        const index = this.tickets.findIndex((t) => t.id === id)
        if (index !== -1) {
            this.tickets[index] = { ...this.tickets[index], ...data }
            this.tickets[index].isNew = false; // Mark as not new (existing ticket)
            this.tickets[index].isDeleted = false; // Ensure not marked as deleted
            this.renderTickets();
            if (typeof this.showNotification === 'function') {
                this.showNotification("Cập nhật vé thành công!", "success")
            }
        }
    }

    deleteTicket(id) {
        const index = this.tickets.findIndex((t) => t.id === id);
        if (index !== -1) {
            // If it's a new ticket, remove it completely
            if (this.tickets[index].isNew) {
                this.tickets.splice(index, 1);
            } else {
                // If it's an existing ticket, mark as deleted
                this.tickets[index].isDeleted = true;
            }
            this.renderTickets();
            if (typeof this.showNotification === 'function') {
                this.showNotification("Xóa vé thành công!", "success")
            }
        }
    }

    // Edit & Delete
    editTicket(id) {
        console.log("Editing ticket with id:", id)
        const ticket = this.tickets.find((t) => t.id === id)
        if (!ticket) {
            console.error("Ticket not found:", id)
            return
        }

        this.editingId = id

        // Fill form
        document.getElementById("ticket-name").value = ticket.name
        document.getElementById("ticket-price").value = ticket.price
        document.getElementById("ticket-quantity").value = ticket.totalQuantity
        document.getElementById("ticket-sale").value = ticket.sale
        document.getElementById("ticket-start-date").value = ticket.startSaleDate
        document.getElementById("ticket-end-date").value = ticket.endSaleDate
        // Removed payment-method line since it doesn't exist
        document.getElementById("ticket-description").innerHTML = ticket.description

        // Update UI
        document.getElementById("form-title").innerHTML = '<i class="fas fa-edit"></i> Chỉnh Sửa Vé'
        document.getElementById("submit-btn").innerHTML = '<i class="fas fa-save"></i> Cập Nhật'
        
        const cancelBtn = document.getElementById("cancel-edit-btn")
        if (cancelBtn) {
            console.log("Cancel button found, current display:", cancelBtn.style.display)
            // Force show the button for edit mode
            cancelBtn.style.setProperty("display", "block", "important")
            console.log("Cancel button should be visible now, new display:", cancelBtn.style.display)
        } else {
            console.error("Cancel button not found!")
        }

        // Scroll to form
        const formSection = document.querySelector(".form-section")
        if (formSection) {
            formSection.scrollIntoView({ behavior: "smooth" })
        }
    }

    cancelEdit() {
        console.log("Canceling edit...")
        this.editingId = null
        this.showCreateForm()
    }

    // Show form in create mode (no cancel button)
    showCreateForm() {
        console.log("Showing create form...")
        this.editingId = null
        
        // Reset form fields
        document.getElementById("ticket-form").reset()
        document.getElementById("ticket-description").innerHTML = ""
        
        // Update UI for create mode
        document.getElementById("form-title").innerHTML = '<i class="fas fa-plus-circle"></i> Tạo Vé Mới'
        document.getElementById("submit-btn").innerHTML = '<i class="fas fa-save"></i> Lưu Vé'
        
        // Hide cancel button
        const cancelBtn = document.getElementById("cancel-edit-btn")
        if (cancelBtn) {
            cancelBtn.style.setProperty("display", "none", "important")
            console.log("Cancel button hidden for create mode")
        }
        
        // Clear validation states
        const inputs = document.querySelectorAll("input")
        inputs.forEach(input => {
            input.classList.remove("is-valid", "is-invalid")
        })
    }

    openDeleteModal(id) {
        this.deleteId = id
        document.getElementById("delete-modal").classList.add("active")
    }

    closeDeleteModal() {
        this.deleteId = null
        document.getElementById("delete-modal").classList.remove("active")
    }

    confirmDelete() {
        if (this.deleteId) {
            const id = this.deleteId
            this.closeDeleteModal()
            const t = this.tickets.find(x => x.id === id)

            if (t && t.id && !isNaN(parseInt(t.id))) {
                fetch(`/api/events/ticket/${parseInt(t.id)}`, { method: 'DELETE' })
                    .then(async (res) => {
                        if (!res.ok) {
                            // Try to parse backend error
                            try {
                                const data = await res.json()
                                const msg = data?.error || data?.message || 'Không thể xóa vé'
                                throw new Error(msg)
                            } catch (_) {
                                const text = await res.text()
                                throw new Error(text || 'Không thể xóa vé')
                            }
                        }
                        // Server confirmed deletion → remove locally
                        this.tickets = this.tickets.filter(x => x.id !== id)
                        this.renderTickets()
                        if (typeof this.showNotification === 'function') {
                            this.showNotification('Đã xóa vé', 'success')
                        }
                    })
                    .catch(err => {
                        console.error('Delete ticket error:', err)
                        if (typeof this.showNotification === 'function') {
                            this.showNotification(err?.message || 'Xóa vé thất bại', 'error')
                        }
                    })
            } else {
                // Vé mới (chưa lưu DB) → xóa local
                this.deleteTicket(id)
            }
        }
    }

    // UI Rendering
    renderTickets() {
        const container = document.getElementById("ticket-list")
        const emptyState = document.getElementById("empty-state")
        const ticketCount = document.getElementById("ticket-count")

        // Filter out deleted tickets for display
        const visibleTickets = this.tickets.filter(ticket => !ticket.isDeleted);

        if (visibleTickets.length === 0) {
            container.style.display = "none"
            emptyState.style.display = "block"
            ticketCount.textContent = "0 vé"
            return
        }

        container.style.display = "flex"
        emptyState.style.display = "none"
        ticketCount.textContent = `${visibleTickets.length} vé`

        container.innerHTML = visibleTickets.map((ticket) => this.renderTicketCard(ticket)).join("")

        // Attach event listeners
        this.attachTicketListeners()
    }

    renderTicketCard(ticket) {
        const finalPrice = ticket.price * (1 - ticket.sale / 100)
        const available = ticket.totalQuantity - ticket.soldQuantity
        const soldPercent = ticket.totalQuantity > 0 ? Math.round((ticket.soldQuantity / ticket.totalQuantity) * 100) : 0
        
        // Format dates like in the image: "23:24 09/10/2025"
        const formatDateTime = (dateString) => {
            const date = new Date(dateString)
            const hours = date.getHours().toString().padStart(2, '0')
            const minutes = date.getMinutes().toString().padStart(2, '0')
            const day = date.getDate().toString().padStart(2, '0')
            const month = (date.getMonth() + 1).toString().padStart(2, '0')
            const year = date.getFullYear()
            return `${hours}:${minutes} ${day}/${month}/${year}`
        }
        
        const startDate = formatDateTime(ticket.startSaleDate)
        const endDate = formatDateTime(ticket.endSaleDate)

        return `
            <div class="ticket-card" data-id="${ticket.id}">
                <div class="ticket-header">
                    <div>
                        <h3 class="ticket-title">${ticket.name}</h3>
                        <p class="ticket-subtitle">${ticket.description ? ticket.description.replace(/<[^>]*>/g, '').substring(0, 50) + '...' : 'Mô tả vé'}</p>
                    </div>
                    <div class="ticket-actions">
                        <button class="icon-btn edit" data-action="edit" data-id="${ticket.id}" title="Chỉnh sửa">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="icon-btn delete" data-action="delete" data-id="${ticket.id}" title="Xóa">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>

                <div class="ticket-info">
                    <div class="info-item">
                        <div class="info-icon price-icon">
                            <i class="fas fa-dollar-sign"></i>
                        </div>
                        <div class="info-content">
                            <span class="info-label">Giá vé</span>
                            <div>
                                <span class="info-value price-current">${this.formatCurrency(finalPrice)}</span>
                                <span class="price-original">${this.formatCurrency(ticket.price)}</span>
                            </div>
                        </div>
                    </div>

                    <div class="info-item">
                        <div class="info-icon quantity-icon">
                            <i class="fas fa-users"></i>
                        </div>
                        <div class="info-content">
                            <span class="info-label">Số lượng</span>
                            <span class="info-value quantity-info">${ticket.soldQuantity}/${ticket.totalQuantity} (${soldPercent}%)</span>
                        </div>
                    </div>

                    <div class="info-item" style="grid-column: 1 / -1;">
                        <div class="info-icon calendar-icon">
                            <i class="fas fa-calendar"></i>
                        </div>
                        <div class="info-content">
                            <span class="info-label">Thời gian bán:</span>
                            <div class="sale-period">
                                <div><strong>Từ:</strong> ${startDate}</div>
                                <div><strong>Đến:</strong> ${endDate}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `
    }

    attachTicketListeners() {
        console.log("Attaching ticket listeners...")
        
        const editButtons = document.querySelectorAll('[data-action="edit"]')
        console.log("Found edit buttons:", editButtons.length)
        
        editButtons.forEach((btn) => {
            btn.addEventListener("click", (e) => {
                const id = e.currentTarget.dataset.id
                console.log("Edit button clicked, ticket id:", id)
                this.editTicket(id)
            })
        })

        const deleteButtons = document.querySelectorAll('[data-action="delete"]')
        console.log("Found delete buttons:", deleteButtons.length)
        
        deleteButtons.forEach((btn) => {
            btn.addEventListener("click", (e) => {
                const id = e.currentTarget.dataset.id
                console.log("Delete button clicked, ticket id:", id)
                this.openDeleteModal(id)
            })
        })
        
        console.log("Ticket listeners attached successfully")
    }

    // Utilities
    formatCurrency(amount) {
        return new Intl.NumberFormat("vi-VN", {
            style: "currency",
            currency: "VND",
        }).format(amount)
    }

    resetForm() {
        document.getElementById("ticket-form").reset()
        document.getElementById("ticket-description").innerHTML = ""
        this.editingId = null

        // Reset to create mode
        document.getElementById("form-title").innerHTML = '<i class="fas fa-plus-circle"></i> Tạo Vé Mới'
        document.getElementById("submit-btn").innerHTML = '<i class="fas fa-save"></i> Lưu Vé'
        
        // Hide cancel button when creating new ticket
        const cancelBtn = document.getElementById("cancel-edit-btn")
        if (cancelBtn) {
            cancelBtn.style.display = "none"
        }

        // Clear validation states
        const inputs = document.querySelectorAll("input")
        inputs.forEach(input => {
            input.classList.remove("is-valid", "is-invalid")
        })
    }

    // Get tickets data for form submission
    getTicketsData() {
        return this.tickets.map(ticket => ({
            ticketTypeId: ticket.id ? parseInt(ticket.id) : null,
            name: ticket.name,
            description: ticket.description,
            price: ticket.price,
            totalQuantity: ticket.totalQuantity,
            soldQuantity: ticket.soldQuantity,
            startSaleDate: ticket.startSaleDate,
            endSaleDate: ticket.endSaleDate,
            sale: ticket.sale,
            isNew: ticket.isNew || false,
            isDeleted: ticket.isDeleted || false
        }));
    }
}

/**
 * Populate ticket types from database (similar to populateLineupFromEvent and populateSchedulesFromEvent)
 */
function populateTicketTypesFromEvent() {
    console.log('--- BƯỚC B: populateTicketTypesFromEvent ĐANG CHẠY ---');
    console.log('initialTicketTypesData:', initialTicketTypesData);
    

    if (!window.ticketManager) {
        console.log('Creating new TicketManager instance...');
        window.ticketManager = new TicketManager();
    }
    // 1. Kiểm tra dữ liệu
    if (!initialTicketTypesData || initialTicketTypesData.length === 0) {
        console.log('No initial ticket types to populate.');
        return;
    }
    console.log('LOG: Tìm thấy', initialTicketTypesData.length, 'ticket types để load.');

    // 2. Khởi tạo TicketManager với dữ liệu từ database
    
    
    console.log('Initializing TicketManager with database data...');
    window.ticketManager.initializeTicketTypes(initialTicketTypesData);
    
    console.log('✅ Ticket types populated successfully from database');
}
function initializeTicketForm() {
    console.log('🎫 Initializing ticket form...');
    
    // Always create TicketManager
    if (!window.ticketManager) {
        console.log('Creating new TicketManager instance...');
        window.ticketManager = new TicketManager();
    }
    
    // Initialize with data from Thymeleaf
    populateTicketTypesFromEvent();
    
    console.log('✅ Ticket form initialized successfully');
}

// Make functions globally accessible
window.populateTicketTypesFromEvent = populateTicketTypesFromEvent;
    window.initializeTicketForm = initializeTicketForm;

// Simple notification helper to avoid runtime errors
TicketManager.prototype.showNotification = function(message, type) {
    try {
        const color = type === 'success' ? '#16a34a' : type === 'error' ? '#dc2626' : '#2563eb'
        const toast = document.createElement('div')
        toast.textContent = message
        toast.style.position = 'fixed'
        toast.style.right = '16px'
        toast.style.bottom = '16px'
        toast.style.padding = '12px 16px'
        toast.style.background = color
        toast.style.color = 'white'
        toast.style.borderRadius = '8px'
        toast.style.zIndex = '9999'
        document.body.appendChild(toast)
        setTimeout(() => toast.remove(), 2000)
    } catch (e) {
        console.log(message)
    }
}

// Save tickets to backend immediately (used on ticket page)
TicketManager.prototype.saveTicketsToServer = async function() {
    try {
        const pathParts = window.location.pathname.split('/')
        const eventId = pathParts[3]
        if (!eventId || isNaN(eventId)) {
            console.error('Cannot determine eventId from URL, skip saving tickets')
            return
        }

        const ticketsData = this.getTicketsData()
        const formData = new FormData()
        formData.append('ticketsJson', JSON.stringify(ticketsData))

        const res = await fetch(`/api/events/update-tickets/${eventId}`, {
            method: 'POST',
            body: formData
        })

        if (!res.ok) throw new Error('Failed to save tickets')

        if (typeof this.showNotification === 'function') {
            this.showNotification('Đã lưu vé vào hệ thống', 'success')
        }
    } catch (err) {
        console.error('Error saving tickets:', err)
        if (typeof this.showNotification === 'function') {
            this.showNotification('Lưu vé thất bại', 'error')
        }
    }
}
