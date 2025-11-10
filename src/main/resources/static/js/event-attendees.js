// Event Attendees JavaScript Functions

function getEventId() {
    const element = document.querySelector("[data-event-id]");
    if (element?.dataset.eventId) return element.dataset.eventId;

    const pathParts = window.location.pathname.split("/");
    const eventIndex = pathParts.indexOf("event");
    if (eventIndex !== -1 && pathParts[eventIndex + 1]) {
        return pathParts[eventIndex + 1];
    }

    if (pathParts.length > 3 && pathParts[1] === "manage" && pathParts[2] === "event") {
        return pathParts[3];
    }

    console.error("Could not extract eventId from URL:", window.location.pathname);
    return null;
}

// Modal Management - Simplified (modals are now in layout, not in fragment)
let addModal, editModal;

function initializeModals() {
    if (!window.bootstrap) {
        setTimeout(initializeModals, 100);
        return;
    }

    const addModalElement = document.getElementById("addModal");
    const editModalElement = document.getElementById("editModal");

    if (addModalElement && !addModal) {
        try {
            addModal = new window.bootstrap.Modal(addModalElement, { backdrop: true, keyboard: true });
        } catch (e) {
            console.error("Error initializing addModal:", e);
        }
    }

    if (editModalElement && !editModal) {
        try {
            editModal = new window.bootstrap.Modal(editModalElement, { backdrop: true, keyboard: true });
        } catch (e) {
            console.error("Error initializing editModal:", e);
        }
    }
}

// Load ticket types for Add Modal
function loadTicketTypes() {
    const eventId = getEventId();
    if (!eventId) return;

    const select = document.getElementById("addTicketType");
    if (!select) return;

    fetch(`/api/ticket-types/event/${eventId}`)
        .then(response => response.json())
        .then(ticketTypes => {
            select.innerHTML = '<option value="">-- Chọn loại vé --</option>';
            ticketTypes.forEach(ticket => {
                const option = document.createElement('option');
                option.value = ticket.ticketTypeId;
                option.textContent = ticket.name;
                select.appendChild(option);
            });
        })
        .catch(error => {
            console.error("Error loading ticket types:", error);
            select.innerHTML = '<option value="">Lỗi khi tải loại vé</option>';
        });
}

// Modal Functions
function openAddModal() {
    if (!addModal) initializeModals();
    
    const addModalElement = document.getElementById("addModal");
    if (!addModalElement) {
        showAlert("Không tìm thấy form thêm người tham dự. Vui lòng tải lại trang.", "error");
        return;
    }

    document.getElementById("addForm")?.reset();
    loadTicketTypes();
    
    if (addModal) {
        addModal.show();
    }
}

function closeAddModal() {
    if (addModal) {
        addModal.hide();
    }
}

function openEditModal(buttonElement, attendanceId) {
    if (!editModal) initializeModals();

    const editModalElement = document.getElementById("editModal");
    if (!editModalElement) {
        showAlert("Không tìm thấy form chỉnh sửa. Vui lòng tải lại trang.", "error");
        return;
    }

    // Populate form fields
    document.getElementById("editAttendanceId").value = attendanceId || "";
    document.getElementById("editName").value = buttonElement.dataset.name || "";
    document.getElementById("editEmail").value = buttonElement.dataset.email || "";
    document.getElementById("editPhone").value = buttonElement.dataset.phone || "";
    document.getElementById("editOrganization").value = buttonElement.dataset.organization || "";

    if (editModal) {
        editModal.show();
    }
}

function closeEditModal() {
    if (editModal) {
        editModal.hide();
    }
}

// Alert Function
function showAlert(message, type = "info") {
    const alertType = type === "error" ? "danger" : type === "success" ? "success" : "info";
    const title = type === "error" ? "Lỗi!" : type === "success" ? "Thành công!" : "Thông báo";
    const alertHtml = `
        <div class="alert alert-${alertType} alert-dismissible fade show position-fixed" 
             role="alert" style="top: 20px; right: 20px; z-index: 9999; min-width: 300px;">
            <strong>${title}</strong> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
    document.body.insertAdjacentHTML("beforeend", alertHtml);
    setTimeout(() => {
        document.querySelectorAll(".position-fixed").forEach(alert => alert.remove());
    }, 4000);
}

// Form Validation
function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function isValidPhone(phone) {
    return /^0\d{9}$|^\+84\d{9}$/.test(phone);
}

// Submit Functions
function submitAddAttendee() {
    const eventId = getEventId();
    if (!eventId) {
        showAlert("Không tìm thấy ID sự kiện. Vui lòng tải lại trang.", "error");
        return;
    }

    const name = document.getElementById("addName").value.trim();
    const email = document.getElementById("addEmail").value.trim();
    const phone = document.getElementById("addPhone").value.trim();
    const organization = document.getElementById("addOrganization").value.trim();
    const ticketTypeId = document.getElementById("addTicketType").value;

    if (!name) { showAlert("Vui lòng nhập tên", "error"); return; }
    if (!email || !isValidEmail(email)) { showAlert("Vui lòng nhập email hợp lệ", "error"); return; }
    if (!phone || !isValidPhone(phone)) { showAlert("Vui lòng nhập số điện thoại hợp lệ (09...)", "error"); return; }
    if (!ticketTypeId) { showAlert("Vui lòng chọn loại vé", "error"); return; }

    const params = new URLSearchParams({ name, email, phone, organization, ticketTypeId });

    fetch(`/event/${eventId}/attendees/add`, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params.toString(),
    })
        .then(response => {
            if (response.ok) {
                closeAddModal();
                showAlert("Thêm người tham dự thành công", "success");
                setTimeout(() => reloadAttendeesFragment(), 500);
            } else {
                return response.text().then(text => {
                    try {
                        throw new Error(JSON.parse(text).error || "Lỗi khi thêm người tham dự");
                    } catch {
                        throw new Error(text || "Lỗi khi thêm người tham dự");
                    }
                });
            }
        })
        .catch(error => {
            console.error("Error:", error);
            showAlert(error.message || "Lỗi khi thêm người tham dự", "error");
        });
}

function submitEditAttendee() {
    const eventId = getEventId();
    if (!eventId) {
        showAlert("Không tìm thấy ID sự kiện. Vui lòng tải lại trang.", "error");
        return;
    }

    const attendanceId = document.getElementById("editAttendanceId").value;
    const name = document.getElementById("editName").value.trim();
    const email = document.getElementById("editEmail").value.trim();
    const phone = document.getElementById("editPhone").value.trim();
    const organization = document.getElementById("editOrganization").value.trim();

    if (!name) { showAlert("Vui lòng nhập tên", "error"); return; }
    if (!email || !isValidEmail(email)) { showAlert("Vui lòng nhập email hợp lệ", "error"); return; }
    if (!phone || !isValidPhone(phone)) { showAlert("Vui lòng nhập số điện thoại hợp lệ (09...)", "error"); return; }

    const params = new URLSearchParams({ name, email, phone, organization });

    fetch(`/event/${eventId}/attendees/${attendanceId}/edit`, {
        method: "PUT",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params.toString(),
    })
        .then(response => {
            if (response.ok) {
                closeEditModal();
                showAlert("Cập nhật thông tin thành công", "success");
                setTimeout(() => reloadAttendeesFragment(), 500);
            } else {
                return response.text().then(text => {
                    try {
                        throw new Error(JSON.parse(text).error || "Lỗi khi cập nhật thông tin");
                    } catch {
                        throw new Error(text || "Lỗi khi cập nhật thông tin");
                    }
                });
            }
        })
        .catch(error => {
            console.error("Error:", error);
            showAlert(error.message || "Lỗi khi cập nhật thông tin", "error");
        });
}

// Action Functions
function checkIn(attendanceId) {
    if (!confirm("Xác nhận check-in người tham dự này?")) return;
    const eventId = getEventId();
    if (!eventId) {
        showAlert("Không tìm thấy ID sự kiện. Vui lòng tải lại trang.", "error");
        return;
    }

    fetch(`/event/${eventId}/attendees/${attendanceId}/check-in`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
    })
        .then(response => {
            if (response.ok) {
                showAlert("Check-in thành công", "success");
                setTimeout(() => reloadAttendeesFragment(), 500);
            } else {
                return response.json().then(data => {
                    throw new Error(data.error || "Lỗi khi check-in");
                });
            }
        })
        .catch(error => {
            console.error("Error:", error);
            showAlert(error.message || "Lỗi khi check-in", "error");
        });
}

function checkOut(attendanceId) {
    if (!confirm("Xác nhận check-out người tham dự này?")) return;
    const eventId = getEventId();
    if (!eventId) {
        showAlert("Không tìm thấy ID sự kiện. Vui lòng tải lại trang.", "error");
        return;
    }

    fetch(`/event/${eventId}/attendees/${attendanceId}/check-out`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
    })
        .then(response => {
            if (response.ok) {
                showAlert("Check-out thành công", "success");
                setTimeout(() => reloadAttendeesFragment(), 500);
            } else {
                return response.json().then(data => {
                    throw new Error(data.error || "Lỗi khi check-out");
                });
            }
        })
        .catch(error => {
            console.error("Error:", error);
            showAlert(error.message || "Lỗi khi check-out", "error");
        });
}

function deleteAttendee(attendanceId) {
    if (!confirm("Bạn chắc chắn muốn xóa người tham dự này?")) return;
    const eventId = getEventId();
    if (!eventId) {
        showAlert("Không tìm thấy ID sự kiện. Vui lòng tải lại trang.", "error");
        return;
    }

    fetch(`/event/${eventId}/attendees/${attendanceId}`, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
    })
        .then(response => {
            if (response.ok) {
                showAlert("Xóa người tham dự thành công", "success");
                setTimeout(() => reloadAttendeesFragment(), 500);
            } else {
                return response.json().then(data => {
                    throw new Error(data.error || "Lỗi khi xóa người tham dự");
                });
            }
        })
        .catch(error => {
            console.error("Error:", error);
            showAlert(error.message || "Lỗi khi xóa người tham dự", "error");
        });
}

// Search and Filter
let searchTimeout = null;
let searchAbortController = null;

function setupAutoSearch() {
    if (searchAbortController) searchAbortController.abort();
    searchAbortController = new AbortController();

    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                if (e.target.value.trim().length >= 2 || e.target.value.trim().length === 0) {
                    performSearch();
                }
            }, 500);
        }, { signal: searchAbortController.signal });
    }

    ['ticketTypeFilter', 'paymentStatusFilter', 'checkinStatusFilter'].forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener('change', performFilter, { signal: searchAbortController.signal });
        }
    });
}

function performSearch() {
    loadPage(0);
}

function performFilter() {
    performSearch();
}

function loadPage(page) {
    const eventId = getEventId();
    if (!eventId) {
        console.error('[EventAttendees] Cannot load page - eventId not found');
        return;
    }

    const params = new URLSearchParams({
        id: eventId,
        page: page,
        size: '10'
    });

    const searchValue = document.getElementById('searchInput')?.value.trim() || '';
    const ticketTypeFilter = document.getElementById('ticketTypeFilter')?.value || '';
    const paymentStatusFilter = document.getElementById('paymentStatusFilter')?.value || '';
    const checkinStatusFilter = document.getElementById('checkinStatusFilter')?.value || '';

    if (searchValue) params.set('search', searchValue);
    if (ticketTypeFilter) params.set('ticketTypeFilter', ticketTypeFilter);
    if (paymentStatusFilter) params.set('paymentStatusFilter', paymentStatusFilter);
    if (checkinStatusFilter) params.set('checkinStatusFilter', checkinStatusFilter);

    fetch(`/fragments/attendees?${params.toString()}`)
        .then(response => response.text())
        .then(html => {
            const mainContent = document.querySelector('#main-content');
            if (mainContent) {
                mainContent.innerHTML = html;
                setTimeout(() => {
                    if (typeof window.setupAutoSearch === 'function') window.setupAutoSearch();
                }, 100);
            }
        })
        .catch(error => {
            console.error('[EventAttendees] Error loading fragment:', error);
            showAlert("Lỗi khi tải dữ liệu. Vui lòng thử lại.", "error");
        });
}

function reloadAttendeesFragment() {
    const activePageLink = document.querySelector('.pagination .page-item.active .page-link');
    let currentPage = 0;
    if (activePageLink) {
        const pageNum = parseInt(activePageLink.textContent.trim());
        if (!isNaN(pageNum)) currentPage = pageNum - 1;
    }
    loadPage(currentPage);
}

function resetSearch() {
    ['searchInput', 'ticketTypeFilter', 'paymentStatusFilter', 'checkinStatusFilter'].forEach(id => {
        const element = document.getElementById(id);
        if (element) element.value = '';
    });
    loadPage(0);
}

// Export functions
window.performSearch = performSearch;
window.performFilter = performFilter;
window.resetSearch = resetSearch;
window.setupAutoSearch = setupAutoSearch;
window.loadPage = loadPage;
window.openAddModal = openAddModal;
window.closeAddModal = closeAddModal;
window.openEditModal = openEditModal;
window.closeEditModal = closeEditModal;
window.submitAddAttendee = submitAddAttendee;
window.submitEditAttendee = submitEditAttendee;
window.checkIn = checkIn;
window.checkOut = checkOut;
window.deleteAttendee = deleteAttendee;
window.initializeModals = initializeModals;

// Initialize modals when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeModals);
} else {
    initializeModals();
}

// Initialize auto-search when fragment loads
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => setTimeout(setupAutoSearch, 500));
} else {
    setTimeout(setupAutoSearch, 500);
}
