// ================== Agenda Management ==================

// Function to populate schedules from database
function populateSchedulesFromEvent() {
    console.log('--- BƯỚC B: populateSchedulesFromEvent ĐANG CHẠY ---');
    console.log('initialSchedulesData:', initialSchedulesData);
    
    // 1. Kiểm tra dữ liệu
    if (!initialSchedulesData || initialSchedulesData.length === 0) {
        console.log('No initial schedules to populate.');
        return;
    }
    console.log('LOG: Tìm thấy', initialSchedulesData.length, 'schedules để load.');

    const container = document.getElementById("agendaSections");
    if (!container) {
        console.error('agendaSections container not found during population.');
        return;
    }

    console.log(`Populating ${initialSchedulesData.length} schedules...`);
    container.innerHTML = ''; // Xóa sạch nội dung cũ để bắt đầu

    // 2. Lặp qua dữ liệu và TẠO SUMMARY CARDS
    initialSchedulesData.forEach(schedule => {
        const activity = schedule.activity || 'N/A';
        const description = schedule.description || '';
        const startTime = schedule.startTime || '';
        const endTime = schedule.endTime || '';

        const summaryCard = document.createElement("div");
        summaryCard.className = "schedule-member-summary";
        summaryCard.innerHTML = `
            <div class="summary-content">
                <div class="summary-drag-handle"><i class="fas fa-grip-lines"></i></div>
                <div class="summary-details">
                    <div class="summary-time">${formatTimeRange(startTime, endTime)}</div>
                    <div class="summary-activity">${activity}</div>
                    <div class="summary-description">${description}</div>
                </div>
                <div class="summary-actions">
                    <button class="summary-edit" type="button" title="Edit schedule"><i class="fas fa-pen"></i></button>
                    <button class="summary-delete" type="button" title="Delete schedule"><i class="fas fa-trash"></i></button>
                </div>
            </div>
        `;

        // 3. Gắn sự kiện cho các nút trên summary card
        summaryCard.querySelector(".summary-delete").onclick = async () => {
            if (confirm(`Bạn có chắc chắn muốn xóa "${activity}"?`)) {
                try {
                    const response = await fetch(`/api/schedules/${schedule.scheduleId}`, {method: 'DELETE'});
                    if (response.ok) {
                        summaryCard.remove();
                    } else {
                        alert('Lỗi khi xóa!');
                    }
                } catch (error) {
                    alert('Lỗi mạng!');
                }
            }
        };

        summaryCard.querySelector(".summary-edit").onclick = () => {
            editScheduleFromSummary(summaryCard, schedule);
        };

        container.appendChild(summaryCard);
    });
}

// Function to format time range
function formatTimeRange(startTime, endTime) {
    if (!startTime || !endTime) return '';
    
    const formatTime = (timeStr) => {
        const time = new Date(timeStr);
        return time.toLocaleTimeString('en-US', { 
            hour: 'numeric', 
            minute: '2-digit',
            hour12: true 
        });
    };
    
    return `${formatTime(startTime)} - ${formatTime(endTime)}`;
}

// Function to edit schedule from summary card
function editScheduleFromSummary(summaryCard, schedule) {
    console.log('Edit clicked for schedule:', schedule);
    
    // Tạo form để chỉnh sửa
    const formContainer = renderAgendaForm(document.getElementById("agendaSections"), {
        scheduleId: schedule.scheduleId,
        title: schedule.activity,
        start: formatTimeForInput(schedule.startTime),
        end: formatTimeForInput(schedule.endTime),
        desc: schedule.description || '',
        showDesc: !!schedule.description
    });
    
    // Thay thế summary bằng form
    summaryCard.parentNode.replaceChild(formContainer, summaryCard);
}

// Function to format time for input
function formatTimeForInput(timeStr) {
    if (!timeStr) return '';
    const time = new Date(timeStr);
    return time.toTimeString().slice(0, 5); // HH:MM format
}

// Function to create new schedule
async function createSchedule(agendaData) {
    try {
        const eventId = window.location.pathname.split('/')[3]; // Get eventId from URL
        
        // Convert time strings to LocalDateTime format
        const startDateTime = convertToDateTime(agendaData.start);
        const endDateTime = convertToDateTime(agendaData.end);
        
        const scheduleData = {
            activity: agendaData.title,
            startTime: startDateTime,
            endTime: endDateTime,
            description: agendaData.desc || null,
            eventId: parseInt(eventId)
        };
        
        console.log('Creating schedule:', scheduleData);
        
        const response = await fetch('/api/schedules', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(scheduleData)
        });
        
        if (response.ok) {
            const createdSchedule = await response.json();
            console.log('Schedule created successfully:', createdSchedule);
            
            // Create summary card with created schedule data
            const summaryCard = document.createElement("div");
            summaryCard.className = "schedule-member-summary";
            summaryCard.innerHTML = `
                <div class="summary-content">
                    <div class="summary-drag-handle"><i class="fas fa-grip-lines"></i></div>
                    <div class="summary-details">
                        <div class="summary-time">${formatTimeRange(createdSchedule.startTime, createdSchedule.endTime)}</div>
                        <div class="summary-activity">${createdSchedule.activity}</div>
                        <div class="summary-description">${createdSchedule.description || ''}</div>
                    </div>
                    <div class="summary-actions">
                        <button class="summary-edit" type="button" title="Edit schedule"><i class="fas fa-pen"></i></button>
                        <button class="summary-delete" type="button" title="Delete schedule"><i class="fas fa-trash"></i></button>
                    </div>
                </div>
            `;
            
            // Add event handlers
            summaryCard.querySelector(".summary-edit").onclick = () => {
                editScheduleFromSummary(summaryCard, createdSchedule);
            };
            
            summaryCard.querySelector(".summary-delete").onclick = async () => {
                if (confirm(`Bạn có chắc chắn muốn xóa "${createdSchedule.activity}"?`)) {
                    try {
                        const response = await fetch(`/api/schedules/${createdSchedule.scheduleId}`, {method: 'DELETE'});
                        if (response.ok) {
                            summaryCard.remove();
                        } else {
                            alert('Lỗi khi xóa!');
                        }
                    } catch (error) {
                        alert('Lỗi mạng!');
                    }
                }
            };
            
            // Insert into container
            const container = document.getElementById("agendaSections");
            container.appendChild(summaryCard);
            
            alert('Schedule created successfully!');
        } else {
            const error = await response.json();
            alert('Error creating schedule: ' + error.error);
        }
    } catch (error) {
        console.error('Error creating schedule:', error);
        alert('Network error!');
    }
}

// Function to update existing schedule
async function updateSchedule(scheduleId, agendaData) {
    try {
        // Convert time strings to LocalDateTime format
        const startDateTime = convertToDateTime(agendaData.start);
        const endDateTime = convertToDateTime(agendaData.end);
        
        const scheduleData = {
            activity: agendaData.title,
            startTime: startDateTime,
            endTime: endDateTime,
            description: agendaData.desc || null
        };
        
        console.log('Updating schedule:', scheduleData);
        
        const response = await fetch(`/api/schedules/${scheduleId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(scheduleData)
        });
        
        if (response.ok) {
            const updatedSchedule = await response.json();
            console.log('Schedule updated successfully:', updatedSchedule);
            
            // Reload the page to show updated data
            window.location.reload();
        } else {
            const error = await response.json();
            alert('Error updating schedule: ' + error.error);
        }
    } catch (error) {
        console.error('Error updating schedule:', error);
        alert('Network error!');
    }
}

// Function to convert time string to LocalDateTime format
function convertToDateTime(timeStr) {
    if (!timeStr) return null;
    
    // Get current date
    const today = new Date();
    const [hours, minutes] = timeStr.split(':');
    
    // Create new date with today's date and specified time
    const dateTime = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 
                             parseInt(hours), parseInt(minutes));
    
    // Return in LocalDateTime format (without timezone)
    const year = dateTime.getFullYear();
    const month = String(dateTime.getMonth() + 1).padStart(2, '0');
    const day = String(dateTime.getDate()).padStart(2, '0');
    const hour = String(dateTime.getHours()).padStart(2, '0');
    const minute = String(dateTime.getMinutes()).padStart(2, '0');
    
    return `${year}-${month}-${day}T${hour}:${minute}:00`;
}

// Export function để có thể gọi từ bên ngoài
window.populateSchedulesFromEvent = populateSchedulesFromEvent;

// Agenda functionality
window.renderAgendaForm = function(container, data = {}) {
    const div = document.createElement('div');
    div.className = 'agenda-section';
    
    // Store data in a variable accessible to event handlers
    const formData = data;
    div.innerHTML = `
        <div class="agenda-form-container">
            <div class="agenda-form-header">
                <span class="agenda-form-title"><b>Agenda</b></span>
                <button class="agenda-form-delete" style="color:#e53935; float:right;">Delete section</button>
            </div>
            <div class="agenda-form-desc">
                Add an itinerary, schedule, or lineup to your event...
            </div>
            <div class="agenda-tabs">
                <button class="agenda-tab agenda-tab-active">Agenda</button>
            </div>
            <div class="agenda-form-body">
                <div class="event-form-group">
                    <label><b>Title<span style="color:#e53935;">*</span></b></label>
                    <input type="text" class="event-form-input agendaTitle" placeholder="Title" required value="${formData.title || ''}">
                </div>
                <div class="agenda-time-row" style="display:flex;gap:16px;margin-bottom:16px;">
                    <div style="flex:1;">
                        <label><i class="far fa-clock"></i> Start time</label>
                        <input type="time" class="event-form-input agendaStartTime" value="${formData.start || ''}">
                    </div>
                    <div style="flex:1;">
                        <label><i class="far fa-clock"></i> End time</label>
                        <input type="time" class="event-form-input agendaEndTime" value="${formData.end || ''}">
                    </div>
                </div>
                <div class="event-form-group" id="agendaDescGroup">
                    <div style="display:flex;align-items:center;gap:8px;">
                        <label><b>Description</b></label>
                        <label style="font-size:0.98em;">
                            <input type="checkbox" id="toggleDesc" ${formData.showDesc !== false ? 'checked' : ''}>
                            Show
                        </label>
                    </div>
                    <textarea class="event-form-input agendaDesc" placeholder="Description" maxlength="1000" rows="4">${formData.desc || ''}</textarea>
                    <div class="event-form-count"><span class="agendaDescCount">${(formData.desc || '').length}</span> / 1000</div>
                </div>
            </div>
            <div class="agenda-form-footer">
                <button type="button" class="agenda-save-btn">${formData.title ? 'Save' : 'Add slot'}</button>
            </div>
        </div>
    `;
    container.appendChild(div);

    // Setup event listeners for this agenda section
    setupAgendaFormListeners(div, container, formData);
}

function setupAgendaFormListeners(div, container, formData) {
    // Delete form
    div.querySelector('.agenda-form-delete').onclick = () => div.remove();

    // Count chars
    const desc = div.querySelector('.agendaDesc');
    const descCount = div.querySelector('.agendaDescCount');
    desc.oninput = () => descCount.textContent = desc.value.length;

    // Toggle desc
    const toggleDesc = div.querySelector('#toggleDesc');
    toggleDesc.onchange = function () {
        desc.style.display = toggleDesc.checked ? 'block' : 'none';
        descCount.parentElement.style.display = toggleDesc.checked ? 'block' : 'none';
    };
    toggleDesc.onchange();

    // Save slot
    div.querySelector('.agenda-save-btn').onclick = async function () {
        const title = div.querySelector('.agendaTitle').value.trim();
        const start = div.querySelector('.agendaStartTime').value;
        const end = div.querySelector('.agendaEndTime').value;
        if (!title || !start || !end) {
            alert('Vui lòng nhập đầy đủ Title, Start time, End time!');
            return;
        }
        
        const agendaData = {
            title, start, end,
            desc: desc.value,
            showDesc: toggleDesc.checked
        };
        
        // Check if this is an edit (has existing schedule data)
        if (formData.scheduleId) {
            // Update existing schedule
            await updateSchedule(formData.scheduleId, agendaData);
        } else {
            // Create new schedule
            await createSchedule(agendaData);
        }
        
        div.remove();
    };
}

function renderAgendaView(container, agendaData) {
    const agendaView = document.createElement('div');
    agendaView.className = 'agenda-view-row';
    const formatTime = t => {
        const [h, m] = t.split(':');
        let hour = parseInt(h, 10);
        const ampm = hour >= 12 ? 'PM' : 'AM';
        hour = hour % 12 || 12;
        return `${hour}:${m} ${ampm}`;
    };
    let timeStr = `${formatTime(agendaData.start)} - ${formatTime(agendaData.end)}`;
    if (agendaData.end < agendaData.start)
        timeStr += ' <span style="font-size:0.9em;color:#888;">(+1 day)</span>';

    agendaView.innerHTML = `
        <div class="agenda-view-content" style="background:#fff6f5;border-radius:16px;padding:24px 32px;display:flex;align-items:center;justify-content:space-between;box-shadow:0 2px 8px rgba(61,85,254,0.04);cursor:pointer;">
            <div>
                <span style="color:#e57373;border-left:3px solid #e57373;padding-left:8px;margin-right:8px;">${timeStr}</span>
                <b style="font-size:1.15em;">${agendaData.title}</b>
                ${agendaData.showDesc && agendaData.desc ? `<div style="color:#444;margin-top:6px;">${agendaData.desc}</div>` : ''}
            </div>
            <i class="fas fa-pen agenda-edit-btn" style="color:#3d5afe;cursor:pointer;"></i>
        </div>
    `;
    agendaView.querySelector('.agenda-view-content').onclick = function (e) {
        if (e.target.classList.contains('agenda-edit-btn') || e.currentTarget === e.target) {
            agendaView.remove();
            renderAgendaForm(container, agendaData);
        }
    };
    container.appendChild(agendaView);
}
