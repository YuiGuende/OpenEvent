// ================== Upload ảnh ==================
const uploadBtn = document.getElementById("uploadBtn")
const uploadInput = document.getElementById("uploadInput")
const preview = document.getElementById("preview")

uploadBtn?.addEventListener("click", () => uploadInput?.click())

uploadInput?.addEventListener("change", (e) => {
    preview.innerHTML = ""
    ;[...e.target.files].forEach((file) => {
        if (file.type.startsWith("image/")) {
            const img = document.createElement("img")
            img.src = URL.createObjectURL(file)
            Object.assign(img.style, {
                maxWidth: "120px",
                margin: "8px",
                borderRadius: "8px",
            })
            preview.appendChild(img)
        }
    })
})

// ================== Hiện form khi click Event Title ==================
document.querySelector(".event-title")?.addEventListener("click", () => {
    document.getElementById("eventOverviewForm").style.display = "block"
})

// ================== Đếm ký tự Summary ==================
document.getElementById("eventSummaryInput")?.addEventListener("input", (e) => {
    document.getElementById("summaryCount").textContent = e.target.value.length
})

// ================== Tabs loại sự kiện ==================
const typeTabs = document.querySelectorAll(".event-type-tab")
const toggleField = (id, show) => {
    const el = document.getElementById(id)
    if (el) el.style.display = show ? "block" : "none"
}

typeTabs.forEach((tab) => {
    tab.addEventListener("click", () => {
        typeTabs.forEach((t) => t.classList.remove("active"))
        tab.classList.add("active")

        const type = tab.textContent.trim()
        toggleField("festivalFields", type === "Festival")
        toggleField("competitionFields", type === "Competition")
        toggleField("workshopFields", type === "Workshop")
    })
})

// ================== Lineup ==================
document.getElementById("addLineupBtn")?.addEventListener("click", (e) => {
    e.preventDefault()
    addLineupSection()
})

function addLineupSection() {
    const container = document.getElementById("lineupSections")
    const div = document.createElement("div")
    div.className = "lineup-section"
    div.innerHTML = `
        <div class="lineup-form-container">
            <div class="lineup-form-header">
                <div class="lineup-role-title">
                    <span class="lineup-role-text">Lineup</span>
                    <i class="fas fa-pen lineup-role-edit" style="color:#3d5afe; cursor:pointer;"></i>
                    <select class="event-form-input lineupRole" style="display:none; min-width:120px;">
                        <option value="LINEUP">Lineup</option>
                        <option value="ARTISTS">Artists</option>
                        <option value="SPEAKERS">Speakers</option>
                        <option value="PERFORMERS">Performers</option>
                        <option value="SINGER">Singer</option>
                        <option value="MC">MC</option>
                        <option value="OTHER">Other</option>
                    </select>
                </div>
                <button class="lineup-form-delete" style="color:#e53935;">Delete section</button>
            </div>
            <div class="lineup-form-desc">
                Highlight your lineup of special guests with a section on your event page...
            </div>
            <div class="lineup-form-body">
                <div class="lineup-form-left">
                    <div class="lineup-image-upload">
                        <input type="file" class="lineupImageInput" accept="image/*" style="display:none;">
                        <div class="lineup-image-preview">
                            <img class="lineup-avatar" src="" alt="Preview" style="display:none;">
                            <div class="lineup-upload-overlay"><i class="fas fa-camera"></i></div>
                            <div class="lineup-image-placeholder">
                                <i class="fas fa-image"></i>
                                <div>Upload an image<span style="color:#e53935;">*</span></div>
                                <div class="lineup-image-note" style="color:#e53935;">Image is mandatory</div>
                            </div>
                        </div>
                        <button class="lineup-upload-btn">Upload</button>
                    </div>
                </div>
                <div class="lineup-form-right">
                    <div class="event-form-group">
                        <label><b>Name</b></label>
                        <input type="text" class="event-form-input lineupName" placeholder="Name" required>
                        <div class="lineup-error" style="color:#e53935;display:none;">Name is mandatory</div>
                    </div>
                    <div class="event-form-group">
                        <label><b>Add a tagline</b></label>
                        <textarea class="event-form-input lineupTagline" placeholder="Description" maxlength="5000" rows="4"></textarea>
                        <div class="event-form-count"><span class="lineupTaglineCount">0</span> / 5000</div>
                    </div>
                
                    <div class="lineup-form-options">
                    
                        <button class="lineup-social-btn"><i class="fas fa-link"></i> Add social links</button>
                    </div>
                </div>
            </div>
            <div class="lineup-form-footer">
                <button class="lineup-add-another">+ Add another</button>
            </div>
        </div>
    `
    container.appendChild(div)

    const imageInput = div.querySelector(".lineupImageInput")
    const avatar = div.querySelector(".lineup-avatar")
    const placeholder = div.querySelector(".lineup-image-placeholder")
    const imagePreview = div.querySelector(".lineup-image-preview")

    // Upload ảnh
    const triggerUpload = () => imageInput.click()
    imagePreview.onclick = triggerUpload
    div.querySelector(".lineup-upload-overlay").onclick = (e) => {
        e.stopPropagation()
        triggerUpload()
    }
    div.querySelector(".lineup-upload-btn").onclick = (e) => {
        e.preventDefault()
        triggerUpload()
    }

    imageInput.onchange = (e) => {
        const file = e.target.files[0]
        if (file) {
            avatar.src = URL.createObjectURL(file)
            avatar.style.display = "block"
            placeholder.style.display = "none"
            imagePreview.classList.add("has-image")
            imagePreview.style.borderColor = "#3d5afe"
        }
    }

    // Đếm ký tự tagline
    const tagline = div.querySelector(".lineupTagline")
    const taglineCount = div.querySelector(".lineupTaglineCount")
    tagline.oninput = () => (taglineCount.textContent = tagline.value.length)

    // Xóa section
    div.querySelector(".lineup-form-delete").onclick = () => div.remove()

    // Reset fields khi Add another
    div.querySelector(".lineup-add-another").onclick = () => {
        div.querySelector(".lineupName").value = ""
        tagline.value = ""
        taglineCount.textContent = "0"
        avatar.style.display = "none"
        placeholder.style.display = "flex"
        imageInput.value = ""
        div.querySelector(".lineupConfirm").checked = true
        div.querySelector(".lineupHeadliner").checked = false
    }

    // Role title edit
    const roleText = div.querySelector(".lineup-role-text")
    const roleEdit = div.querySelector(".lineup-role-edit")
    const roleSelect = div.querySelector(".lineupRole")
    roleEdit.onclick = () => {
        roleText.style.display = "none"
        roleEdit.style.display = "none"
        roleSelect.style.display = "inline-block"
        roleSelect.value = roleText.textContent.toUpperCase()
        roleSelect.focus()
    }
    roleSelect.onchange = () => {
        roleText.textContent = roleSelect.options[roleSelect.selectedIndex].text
        roleText.style.display = "inline-block"
        roleEdit.style.display = "inline-block"
        roleSelect.style.display = "none"
    }
    roleSelect.onblur = () => {
        roleText.textContent = roleSelect.options[roleSelect.selectedIndex].text
        roleText.style.display = "inline-block"
        roleEdit.style.display = "inline-block"
        roleSelect.style.display = "none"
    }
}
function renderAgendaForm(container, data = {}) {
    const div = document.createElement("div")
    div.className = "agenda-section"
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
                    <input type="text" class="event-form-input agendaTitle" placeholder="Title" required value="${data.title || ""}">
                </div>
                <div class="agenda-time-row" style="display:flex;gap:16px;margin-bottom:16px;">
                    <div style="flex:1;">
                        <label><i class="far fa-clock"></i> Start time</label>
                        <input type="time" class="event-form-input agendaStartTime" value="${data.start || ""}">
                    </div>
                    <div style="flex:1;">
                        <label><i class="far fa-clock"></i> End time</label>
                        <input type="time" class="event-form-input agendaEndTime" value="${data.end || ""}">
                    </div>
                </div>
                <div class="event-form-group" id="agendaDescGroup">
                    <div style="display:flex;align-items:center;gap:8px;">
                        <label><b>Description</b></label>
                        <label style="font-size:0.98em;">
                            <input type="checkbox" id="toggleDesc" ${data.showDesc !== false ? "checked" : ""}>
                            Show
                        </label>
                    </div>
                    <textarea class="event-form-input agendaDesc" placeholder="Description" maxlength="1000" rows="4">${data.desc || ""}</textarea>
                    <div class="event-form-count"><span class="agendaDescCount">${(data.desc || "").length}</span> / 1000</div>
                </div>
                <div class="event-form-group">
                    <i class="fas fa-user"></i> Host or Artist
                </div>
            </div>
            <div class="agenda-form-footer">
                <button class="agenda-save-btn">${data.title ? "Save" : "Add slot"}</button>
            </div>
        </div>
    `
    container.appendChild(div)

    // Delete form
    div.querySelector(".agenda-form-delete").onclick = () => div.remove()

    // Count chars
    const desc = div.querySelector(".agendaDesc")
    const descCount = div.querySelector(".agendaDescCount")
    desc.oninput = () => (descCount.textContent = desc.value.length)

    // Toggle desc
    const toggleDesc = div.querySelector("#toggleDesc")
    toggleDesc.onchange = () => {
        desc.style.display = toggleDesc.checked ? "block" : "none"
        descCount.parentElement.style.display = toggleDesc.checked ? "block" : "none"
    }
    toggleDesc.onchange()

    // Save slot
    div.querySelector(".agenda-save-btn").onclick = () => {
        const title = div.querySelector(".agendaTitle").value.trim()
        const start = div.querySelector(".agendaStartTime").value
        const end = div.querySelector(".agendaEndTime").value
        if (!title || !start || !end) {
            alert("Vui lòng nhập đầy đủ Title, Start time, End time!")
            return
        }
        const agendaData = {
            title,
            start,
            end,
            desc: desc.value,
            showDesc: toggleDesc.checked,
        }
        renderAgendaView(container, agendaData)
        div.remove()
    }
}

function renderAgendaView(container, agendaData) {
    const agendaView = document.createElement("div")
    agendaView.className = "agenda-view-row"
    const formatTime = (t) => {
        const [h, m] = t.split(":")
        let hour = Number.parseInt(h, 10)
        const ampm = hour >= 12 ? "PM" : "AM"
        hour = hour % 12 || 12
        return `${hour}:${m} ${ampm}`
    }
    let timeStr = `${formatTime(agendaData.start)} - ${formatTime(agendaData.end)}`
    if (agendaData.end < agendaData.start) timeStr += ' <span style="font-size:0.9em;color:#888;">(+1 day)</span>'

    agendaView.innerHTML = `
        <div class="agenda-view-content" style="background:#fff6f5;border-radius:16px;padding:24px 32px;display:flex;align-items:center;justify-content:space-between;box-shadow:0 2px 8px rgba(61,85,254,0.04);cursor:pointer;">
            <div>
                <span style="color:#e57373;border-left:3px solid #e57373;padding-left:8px;margin-right:8px;">${timeStr}</span>
                <b style="font-size:1.15em;">${agendaData.title}</b>
                ${agendaData.showDesc && agendaData.desc ? `<div style="color:#444;margin-top:6px;">${agendaData.desc}</div>` : ""}
            </div>
            <i class="fas fa-pen agenda-edit-btn" style="color:#3d5afe;cursor:pointer;"></i>
        </div>
    `
    agendaView.querySelector(".agenda-view-content").onclick = (e) => {
        if (e.target.classList.contains("agenda-edit-btn") || e.currentTarget === e.target) {
            agendaView.remove()
            renderAgendaForm(container, agendaData)
        }
    }
    container.appendChild(agendaView)
}

// Gắn vào button "Add Agenda"
document.querySelectorAll(".event-add-row").forEach((row) => {
    const btn = row.querySelector(".event-add-btn")
    if (btn && row.querySelector(".event-add-label")?.textContent.includes("Agenda")) {
        btn.addEventListener("click", (e) => {
            e.preventDefault()
            renderAgendaForm(document.getElementById("agendaSections"))
        })
    }
})
