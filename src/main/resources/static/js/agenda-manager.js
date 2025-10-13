// ================== Agenda Management ==================

// Agenda functionality
window.renderAgendaForm = function(container, data = {}) {
    const div = document.createElement('div');
    div.className = 'agenda-section';
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
                <button class="agenda-tab">+ Add new agenda</button>
            </div>
            <div class="agenda-form-body">
                <div class="event-form-group">
                    <label><b>Title<span style="color:#e53935;">*</span></b></label>
                    <input type="text" class="event-form-input agendaTitle" placeholder="Title" required value="${data.title || ''}">
                </div>
                <div class="agenda-time-row" style="display:flex;gap:16px;margin-bottom:16px;">
                    <div style="flex:1;">
                        <label><i class="far fa-clock"></i> Start time</label>
                        <input type="time" class="event-form-input agendaStartTime" value="${data.start || ''}">
                    </div>
                    <div style="flex:1;">
                        <label><i class="far fa-clock"></i> End time</label>
                        <input type="time" class="event-form-input agendaEndTime" value="${data.end || ''}">
                    </div>
                </div>
                <div class="event-form-group" id="agendaDescGroup">
                    <div style="display:flex;align-items:center;gap:8px;">
                        <label><b>Description</b></label>
                        <label style="font-size:0.98em;">
                            <input type="checkbox" id="toggleDesc" ${data.showDesc !== false ? 'checked' : ''}>
                            Show
                        </label>
                    </div>
                    <textarea class="event-form-input agendaDesc" placeholder="Description" maxlength="1000" rows="4">${data.desc || ''}</textarea>
                    <div class="event-form-count"><span class="agendaDescCount">${(data.desc || '').length}</span> / 1000</div>
                </div>
            </div>
            <div class="agenda-form-footer">
                <button type="button" class="agenda-save-btn">${data.title ? 'Save' : 'Add slot'}</button>
            </div>
        </div>
    `;
    container.appendChild(div);

    // Setup event listeners for this agenda section
    setupAgendaFormListeners(div, container);
}

function setupAgendaFormListeners(div, container) {
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
    div.querySelector('.agenda-save-btn').onclick = function () {
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
        renderAgendaView(container, agendaData);
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
