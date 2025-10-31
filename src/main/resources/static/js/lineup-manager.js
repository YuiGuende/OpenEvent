// Version check
console.log('lineup-manager.js v4 loaded')

// CSRF helpers for Spring Security
function getCsrf() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content')
    const headerName = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content')
    return { token, headerName }
}

function withCsrf(headers = {}) {
    const { token, headerName } = getCsrf()
    if (token && headerName) {
        headers[headerName] = token
    }
    return headers
}

// Function to get role display name
function getRoleDisplayName(role) {
    const roleMap = {
        'SPEAKER': '🎤 Speaker',
        'ARTIST': '🎨 Artist',
        'PERFORMER': '🎪 Performer',
        'SINGER': '🎵 Singer',
        'MC': '🎙️ MC',
        'HOST': '🎭 Host',
        'OTHER': '⭐ Other'
    }
    return roleMap[role] || '🎤 Speaker'
}

// Function to restore Add button
function restoreAddButton() {
    console.log('restoreAddButton called')
    const addButton = document.querySelector('.lineup-delete-section-btn')
    console.log('Found delete button:', addButton)

    if (addButton && addButton.dataset.originalText) {
        console.log('Restoring Add button...')
        console.log('Original text:', addButton.dataset.originalText)
        console.log('Original class:', addButton.dataset.originalClass)

        // Restore original state
        addButton.textContent = addButton.dataset.originalText
        addButton.className = addButton.dataset.originalClass
        addButton.style.cssText = addButton.dataset.originalStyle

        // Remove the onclick handler
        addButton.onclick = null

        console.log('Add button restored successfully')
        return true
    } else {
        console.log('No delete button found or no original data')
        return false
    }
}

window.addLineupSection = function () {
    console.log('addLineupSection called')

    const container = document.getElementById("lineupSections")

    if (!container) {
        console.error('lineupSections container not found')
        return
    }



    const div = document.createElement("div")
    div.className = "lineup-section"
    div.innerHTML = `
        <div class="lineup-form-container">
            <div class="lineup-form-header">
<!--                <div class="lineup-role-title">-->
<!--                    <span class="lineup-role-text">-->
<!--                        <i class="fas fa-users lineup-role-icon"></i>-->
<!--                        Lineup-->
<!--                    </span>-->
<!--                    <i class="fas fa-pen lineup-role-edit" style="color:#e53935; cursor:pointer;"></i>-->
<!--                    <select class="event-form-input lineupRole" style="display:none; min-width:120px;">-->
<!--                        <option value="LINEUP">🎭 Lineup</option>-->
<!--                        <option value="ARTISTS">🎨 Artists</option>-->
<!--                        <option value="SPEAKERS">🎤 Speakers</option>-->
<!--                        <option value="PERFORMERS">🎪 Performers</option>-->
<!--                        <option value="SINGER">🎵 Singer</option>-->
<!--                        <option value="MC">🎙️ MC</option>-->
<!--                        <option value="OTHER">⭐ Other</option>-->
<!--                    </select>-->
<!--                </div>-->
                <button class="lineup-form-delete-individual" title="Delete this form">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
            <div class="lineup-form-desc">
                Highlight your lineup of special guests with a section on your event page. Change the title of this section to fit your event's theme and add info about each person. You can set someone as a headliner to highlight them even more.
            </div>
            
            <!-- Lineup Form Body -->
            <div class="lineup-form-body">
                <div class="lineup-form-left">
                    <!-- Image Upload Area -->
                    <div class="lineup-image-upload">
                        <input type="file" class="lineupImageInput" accept="image/*" style="display:none;">
                        <div class="lineup-image-preview" id="lineupImagePreview">
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
                    <!-- Name Input -->
                    <div class="event-form-group">
                        <label><b>Name</b></label>
                        <input type="text" class="event-form-input lineupName" placeholder="Name" required>
                        <div class="lineup-error lineup-name-error" style="color:#e53935;display:none;">Name is mandatory</div>
                    </div>
                    
                    <!-- Role Selection -->
                    <div class="event-form-group">
                        <label><b>Role</b></label>
                        <select class="event-form-input lineupRole" required>
                            <option value="SPEAKER">🎤 Speaker</option>
                            <option value="ARTIST">🎨 Artist</option>
                            <option value="PERFORMER">🎪 Performer</option>
                            <option value="SINGER">🎵 Singer</option>
                            <option value="MC">🎙️ MC</option>
                            <option value="OTHER">⭐ Other</option>
                        </select>
                    </div>
                    
                    <!-- Tagline Input -->
                    <div class="event-form-group">
                        <label><b>Add a tagline</b></label>
                        <textarea class="event-form-input lineupTagline" placeholder="Description" maxlength="5000" rows="4"></textarea>
                        <div class="event-form-count"><span class="lineupTaglineCount">0</span> / 5000</div>
                    </div>
                    
                    <!-- Social Links Button -->
                    <div class="lineup-form-options">
                        <button class="lineup-social-btn" type="button">
                            <i class="fas fa-link"></i> Add social links
                        </button>
                    </div>
                </div>
            </div>
            
            <!-- Footer -->
            <div class="lineup-form-footer">
                <button type="button" class="lineup-add-another">+ Add another</button>
            </div>
        </div>
        
    `
    container.appendChild(div)

    // Setup event listeners
    setupLineupSectionListeners(div)
    return div;
}

function createCropperModal() {
    const modal = document.createElement('div')
    modal.className = 'cropper-modal'
    modal.id = 'cropperModal'

    // Create modal content directly
    modal.innerHTML = `
        <div class="cropper-modal-content">
            <div class="cropper-modal-header">
                <h3>Crop Image</h3>
                <button class="cropper-close" type="button">&times;</button>
            </div>
            <div class="cropper-modal-body">
                <div class="cropper-container">
                    <img id="cropperImage" src="" alt="Crop me">
                </div>
                <div class="cropper-controls">
                    <div class="cropper-zoom">
                        <i class="fas fa-search-minus"></i>
                        <input type="range" class="cropper-zoom-slider" min="0.1" max="3" step="0.1" value="1">
                        <i class="fas fa-search-plus"></i>
                    </div>
                </div>
            </div>
            <div class="cropper-modal-footer">
                <button class="cropper-cancel" type="button">Cancel</button>
                <button class="cropper-save" type="button">Save</button>
            </div>
        </div>
    `

    return modal
}

function createSocialModal() {
    const modal = document.createElement('div')
    modal.className = 'social-modal'
    modal.id = 'socialModal'
    modal.innerHTML = `
        <div class="social-modal-content">
            <div class="social-modal-header">
                <h3>Add Social Links</h3>
                <button class="social-close" type="button">&times;</button>
            </div>
            <div class="social-modal-body">
                <div class="social-link-item">
                    <div class="social-icon-container">
                        <i class="fas fa-globe social-platform-icon"></i>
                    </div>
                    <div class="social-input-container">
                        <label>Website</label>
                        <input type="url" class="social-link-input" placeholder="Add a social media link">
                    </div>
                    <button class="social-remove" type="button">&times;</button>
                </div>
            </div>
            <div class="social-modal-footer">
                <button class="social-add-more" type="button">+ Add more</button>
                <button class="social-save" type="button">Save</button>
            </div>
        </div>
    `
    return modal
}

function setupLineupSectionListeners(div) {
    // Image upload functionality
    const imageInput = div.querySelector(".lineupImageInput")
    const imagePreview = div.querySelector(".lineup-image-preview")
    const uploadBtn = div.querySelector(".lineup-upload-btn")
    const placeholder = div.querySelector(".lineup-image-placeholder")

    // Upload triggers
    const triggerUpload = () => imageInput.click()
    imagePreview.onclick = triggerUpload
    uploadBtn.onclick = (e) => {
        e.preventDefault()
        triggerUpload()
    }

    // Image input change - show cropper
    imageInput.onchange = (e) => {
        const file = e.target.files[0]
        if (file) {
            showImageCropper(div, file)
        }
    }

    // Tagline character count
    const tagline = div.querySelector(".lineupTagline")
    const taglineCount = div.querySelector(".lineupTaglineCount")
    if (tagline && taglineCount) {
        tagline.oninput = () => (taglineCount.textContent = tagline.value.length)
    }

    // Individual form delete button
    const individualDeleteBtn = div.querySelector(".lineup-form-delete-individual")
    if (individualDeleteBtn) {
        individualDeleteBtn.onclick = function (e) {
            e.preventDefault()
            e.stopPropagation()
            console.log('Individual form delete clicked')

            if (confirm('Are you sure you want to delete this form?')) {
                div.remove()
                console.log('Individual form deleted')
            }
        }
    }

    // Add another button - create summary and new form
    div.querySelector(".lineup-add-another").onclick = () => {
        handleAddAnother(div)
    }

    // Social links button
    div.querySelector(".lineup-social-btn").onclick = () => {
        console.log('Social button clicked')
        showSocialModal(div)
    }

    // Role title edit functionality
    setupRoleEditListeners(div)

    // Modal close handlers are now handled in individual functions
}

function showImageCropper(div, file) {
    console.log('showImageCropper called with file:', file)

    // Create modal dynamically and append to body
    const modal = createCropperModal()
    document.body.appendChild(modal)

    const cropperImage = modal.querySelector("#cropperImage")
    const cropperZoomSlider = modal.querySelector(".cropper-zoom-slider")

    console.log('Modal elements:', {modal, cropperImage, cropperZoomSlider})

    if (!modal || !cropperImage || !cropperZoomSlider) {
        console.error('Missing cropper elements')
        return
    }

    // Set image source
    cropperImage.src = URL.createObjectURL(file)

    // Show modal
    modal.style.display = "block"

    // Attach event listeners immediately
    const saveBtn = modal.querySelector(".cropper-save")
    const cancelBtn = modal.querySelector(".cropper-cancel")
    const closeBtn = modal.querySelector(".cropper-close")

    console.log('Save button element:', saveBtn)
    console.log('Cancel button element:', cancelBtn)
    console.log('Close button element:', closeBtn)

    // Function to close modal and cleanup
    const closeModal = () => {
        console.log('Closing modal and cleaning up')
        try {
            if (cropperImage.cropper) {
                cropperImage.cropper.destroy()
                cropperImage.cropper = null
            }
            if (modal && modal.parentNode) {
                document.body.removeChild(modal)
            }
        } catch (error) {
            console.error('Error closing modal:', error)
        }
    }

    // Save cropped image
    saveBtn.onclick = (e) => {
        e.preventDefault()
        e.stopPropagation()
        console.log('Save button clicked')

        try {
            if (!cropperImage.cropper) {
                console.error('Cropper not initialized')
                alert('Cropper chưa sẵn sàng, vui lòng đợi một chút')
                return
            }

            const canvas = cropperImage.cropper.getCroppedCanvas({
                width: 200,
                height: 200,
                imageSmoothingEnabled: true,
                imageSmoothingQuality: 'high',
            })

            console.log('Canvas created:', canvas)

            if (canvas) {
                // Convert canvas to blob
                canvas.toBlob(async (blob) => {
                    try {
                        // Create FormData for upload
                        const formData = new FormData()
                        formData.append('file', blob, 'speaker-image.jpg')

                        // Upload to server
                        const response = await fetch('/api/speakers/upload/image', {
                            method: 'POST',
                            headers: withCsrf(),
                            body: formData
                        })

                        if (response.ok) {
                            const result = await response.json()
                            const imageUrl = result.imageUrl

                            // Update preview
                            const imagePreview = div.querySelector(".lineup-image-preview")
                            const placeholder = div.querySelector(".lineup-image-placeholder")

                            // Remove existing cropped image
                            const existingImg = imagePreview.querySelector(".lineup-cropped-image")
                            if (existingImg) {
                                existingImg.remove()
                            }

                            // Create new image element
                            const newImg = document.createElement('img')
                            newImg.src = imageUrl
                            newImg.className = 'lineup-cropped-image'
                            newImg.style.cssText = 'width: 100%; height: 100%; object-fit: cover; border-radius: 50%;'

                            // Replace placeholder with cropped image
                            placeholder.style.display = 'none'
                            imagePreview.appendChild(newImg)
                            imagePreview.classList.add('has-image')

                            // Store image URL instead of base64
                            div.dataset.croppedImage = imageUrl

                            console.log('Image uploaded successfully:', imageUrl)
                            alert('Ảnh đã được crop và upload thành công!')
                        } else {
                            console.error('Failed to upload image')
                            alert('Lỗi khi upload ảnh lên server!')
                        }
                    } catch (error) {
                        console.error('Error uploading image:', error)
                        alert('Lỗi mạng khi upload ảnh!')
                    }
                }, 'image/jpeg', 0.8)
            } else {
                console.error('Failed to create canvas')
                alert('Không thể tạo ảnh đã crop, vui lòng thử lại')
            }
        } catch (error) {
            console.error('Error saving cropped image:', error)
            alert('Có lỗi khi lưu ảnh: ' + error.message)
        }

        // Close modal
        closeModal()
    }

    // Cancel cropping
    cancelBtn.onclick = (e) => {
        e.preventDefault()
        e.stopPropagation()
        console.log('Cancel button clicked')

        // Reset file input
        const fileInput = div.querySelector(".lineupImageInput")
        if (fileInput) {
            fileInput.value = ""
        }

        // Close modal
        closeModal()
    }

    // Close button
    closeBtn.onclick = (e) => {
        e.preventDefault()
        e.stopPropagation()
        console.log('Close button clicked')

        // Reset file input
        const fileInput = div.querySelector(".lineupImageInput")
        if (fileInput) {
            fileInput.value = ""
        }

        // Close modal
        closeModal()
    }

    // Close modal when clicking outside
    modal.onclick = (e) => {
        if (e.target === modal) {
            console.log('Clicked outside modal')
            closeModal()
        }
    }

    // Wait for image to load before initializing cropper
    cropperImage.onload = () => {
        console.log('Image loaded, initializing cropper')

        // Destroy existing cropper if any
        if (cropperImage.cropper) {
            cropperImage.cropper.destroy()
        }

        // Initialize cropper
        const cropper = new Cropper(cropperImage, {
            aspectRatio: 1,
            viewMode: 1,
            dragMode: 'move',
            autoCropArea: 0.8,
            restore: false,
            guides: false,
            center: false,
            highlight: false,
            cropBoxMovable: true,
            cropBoxResizable: true,
            toggleDragModeOnDblclick: false,
            ready: function () {
                console.log('Cropper ready')
            }
        })

        // Store cropper instance
        cropperImage.cropper = cropper

        // Zoom slider
        cropperZoomSlider.oninput = () => {
            cropper.zoomTo(parseFloat(cropperZoomSlider.value))
        }
    }

    // Handle image load error
    cropperImage.onerror = () => {
        console.error('Failed to load image')
        document.body.removeChild(modal)
    }
}

function showSocialModal(div) {
    console.log('showSocialModal called')
    // Create modal dynamically and append to body
    const modal = createSocialModal()
    document.body.appendChild(modal)
    console.log('Modal created and appended')

    // Add more social links
    modal.querySelector(".social-add-more").onclick = () => {
        addSocialLinkItem(modal)
    }

    // Save social links
    modal.querySelector(".social-save").onclick = () => {
        saveSocialLinks(div, modal)
    }

    // Close modal handlers
    modal.querySelector(".social-close").onclick = () => {
        document.body.removeChild(modal)
    }

    // Close modal when clicking outside
    modal.onclick = (e) => {
        if (e.target.id === "socialModal") {
            document.body.removeChild(modal)
        }
    }
}

function addSocialLinkItem(modal) {
    const socialBody = modal.querySelector(".social-modal-body")
    const newItem = document.createElement("div")
    newItem.className = "social-link-item"
    newItem.innerHTML = `
        <div class="social-icon-container">
            <i class="fas fa-globe social-platform-icon"></i>
        </div>
        <div class="social-input-container">
            <label>Website</label>
            <input type="url" class="social-link-input" placeholder="Add a social media link">
        </div>
        <button class="social-remove" type="button">&times;</button>
    `

    // Add remove functionality
    newItem.querySelector(".social-remove").onclick = () => {
        newItem.remove()
    }

    // Add input change functionality
    const input = newItem.querySelector(".social-link-input")
    const icon = newItem.querySelector(".social-platform-icon")
    const label = newItem.querySelector("label")

    input.oninput = () => {
        updateSocialIcon(input.value, icon, label)
    }

    socialBody.appendChild(newItem)
}

function updateSocialIcon(url, icon, label) {
    const platformMap = {
        'facebook.com': {icon: 'fab fa-facebook', label: 'Facebook'},
        'instagram.com': {icon: 'fab fa-instagram', label: 'Instagram'},
        'twitter.com': {icon: 'fab fa-twitter', label: 'Twitter'},
        'youtube.com': {icon: 'fab fa-youtube', label: 'YouTube'},
        'linkedin.com': {icon: 'fab fa-linkedin', label: 'LinkedIn'},
        'tiktok.com': {icon: 'fab fa-tiktok', label: 'TikTok'},
        'snapchat.com': {icon: 'fab fa-snapchat', label: 'Snapchat'},
    }

    let platform = 'Website'
    let iconClass = 'fas fa-globe'

    for (const [domain, data] of Object.entries(platformMap)) {
        if (url.includes(domain)) {
            platform = data.label
            iconClass = data.icon
            break
        }
    }

    icon.className = `social-platform-icon ${iconClass}`
    label.textContent = platform
}

// Function to update social links preview
function updateSocialLinksPreview(div, socialLinks) {
    console.log('updateSocialLinksPreview called with:', socialLinks)

    // Find or create social links preview container
    let previewContainer = div.querySelector('.social-links-preview')
    if (!previewContainer) {
        previewContainer = document.createElement('div')
        previewContainer.className = 'social-links-preview'
        previewContainer.style.cssText = `
            margin-top: 1rem;
            padding: 0.5rem;
            background: #f8f9fa;
            border-radius: 4px;
            border: 1px solid #e0e0e0;
        `

        // Insert after social button
        const socialBtn = div.querySelector('.lineup-social-btn')
        if (socialBtn) {
            socialBtn.parentNode.insertBefore(previewContainer, socialBtn.nextSibling)
        }
    }

    // Clear existing content
    previewContainer.innerHTML = ''

    // Add social links
    socialLinks.forEach(link => {
        if (link.url.trim()) {
            const linkElement = document.createElement('div')
            linkElement.style.cssText = `
                display: inline-block;
                margin: 0.25rem;
                padding: 0.25rem 0.5rem;
                background: white;
                border: 1px solid #ddd;
                border-radius: 4px;
                font-size: 0.8rem;
            `
            linkElement.innerHTML = `
                <i class="fas fa-link"></i> ${link.platform}: ${link.url}
            `
            previewContainer.appendChild(linkElement)
        }
    })
}

function saveSocialLinks(div, modal) {
    const socialItems = modal.querySelectorAll(".social-link-item")
    const socialLinks = []

    socialItems.forEach(item => {
        const input = item.querySelector(".social-link-input")
        const icon = item.querySelector(".social-platform-icon")
        const label = item.querySelector("label")

        if (input.value.trim()) {
            socialLinks.push({
                url: input.value.trim(),
                platform: label.textContent,
                icon: icon.className
            })
        }
    })

    // Store social links
    div.dataset.socialLinks = JSON.stringify(socialLinks)

    // Update preview if any links exist
    if (socialLinks.length > 0) {
        updateSocialLinksPreview(div, socialLinks)
    }

    // Close modal
    document.body.removeChild(modal)
}


async function handleAddAnother(div) {
    console.log('handleAddAnother called')

    const nameInput = div.querySelector(".lineupName")
    const imagePreview = div.querySelector(".lineup-image-preview")
    const hasImage = imagePreview.querySelector(".lineup-cropped-image") || div.dataset.croppedImage

    console.log('Name:', nameInput.value)
    console.log('Has image:', !!hasImage)

    // Simple validation
    if (!nameInput.value.trim()) {
        alert('Name is required')
        return
    }

    if (!hasImage) {
        alert('Please upload an image first')
        return
    }

    console.log('Validation passed, creating speaker...')

    // Prepare data for API
    const name = nameInput.value.trim()
    const tagline = div.querySelector(".lineupTagline").value.trim()
    const role = div.querySelector(".lineupRole").value
    const croppedImage = div.dataset.croppedImage
    const eventId = window.location.pathname.split('/')[3] // Get eventId from URL

    try {
        // Create speaker data
        const speakerData = {
            name: name,
            profile: tagline,
            imageUrl: croppedImage,
            defaultRole: role
        }

        console.log('Sending speaker data:', speakerData)

        // Call API to create speaker
        const response = await fetch('/api/speakers', {
            method: 'POST',
            headers: withCsrf({
                'Content-Type': 'application/json'
            }),
            body: JSON.stringify(speakerData)
        })

        if (response.ok) {
            const createdSpeaker = await response.json()
            console.log('Speaker created successfully:', createdSpeaker)

            // Add speaker to event
            const addToEventResponse = await fetch(`/api/speakers/${createdSpeaker.id}/events/${eventId}`, {
                method: 'POST',
                headers: withCsrf()
            })

            if (addToEventResponse.ok) {
                console.log('Speaker added to event successfully')

                // Create summary card with created speaker data
                const summaryCard = document.createElement("div")
                summaryCard.className = "lineup-member-summary"
                summaryCard.innerHTML = `
        <div class="summary-content">
            <div class="summary-drag-handle">
                <i class="fas fa-grip-lines"></i>
            </div>
            <div class="summary-avatar">
                            <img src="${createdSpeaker.imageUrl}" alt="${createdSpeaker.name}" class="summary-image">
            </div>
            <div class="summary-details">
                            <div class="summary-name">${createdSpeaker.name}</div>
                            <div class="summary-role">${getRoleDisplayName(createdSpeaker.defaultRole)}</div>
                            <div class="summary-tagline">${createdSpeaker.profile}</div>
            </div>
            <div class="summary-actions">
                <button class="summary-edit" type="button" title="Edit member">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="summary-delete" type="button" title="Delete member">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        </div>
    `

                // Add edit functionality
                summaryCard.querySelector(".summary-edit").onclick = () => {
                    editSpeakerFromSummary(summaryCard, createdSpeaker)
                }

                // Add delete functionality
                summaryCard.querySelector(".summary-delete").onclick = async () => {
                    if (confirm(`Bạn có chắc chắn muốn xóa "${createdSpeaker.name}"?`)) {
                        try {
                        const response = await fetch(`/api/speakers/${createdSpeaker.id}`, {
                            method: 'DELETE',
                            headers: withCsrf()
                        });
                            if (response.ok) {
                                summaryCard.remove()
                            } else {
                                alert('Lỗi khi xóa!');
                            }
                        } catch (error) {
                            alert('Lỗi mạng!');
                        }
                    }
                }

                // Insert before the form
                div.querySelector(".lineup-form-container").insertAdjacentElement("beforebegin", summaryCard)

                alert('Speaker created successfully!')
            } else {
                alert('Error adding speaker to event!')
            }
        } else {
            alert('Error creating speaker!')
        }
    } catch (error) {
        console.error('Error creating speaker:', error)
        alert('Network error!')
    }

    // Reset form
    nameInput.value = ""
    div.querySelector(".lineupTagline").value = ""
    div.querySelector(".lineupTaglineCount").textContent = "0"
    div.querySelector(".lineupRole").value = "SPEAKER"

    // Reset image
    const placeholder = imagePreview.querySelector(".lineup-image-placeholder")
    const croppedImg = imagePreview.querySelector(".lineup-cropped-image")
    if (croppedImg) {
        croppedImg.remove()
    }
    placeholder.style.display = "flex"
    imagePreview.classList.remove('has-image')
    div.dataset.croppedImage = ""

    console.log('Form reset successfully')
}

function populateLineupFromEvent() {
    console.log('--- BƯỚC B: populateLineupFromEvent ĐANG CHẠY ---');
    console.log('initialSpeakersData:', initialSpeakersData);

    // 1. Kiểm tra dữ liệu
    if (!initialSpeakersData || initialSpeakersData.length === 0) {
        console.log('No initial speakers to populate.');
        addLineupSection();
        return;
    }
    console.log('LOG: Tìm thấy', initialSpeakersData.length, 'speakers để load.');

    const container = document.getElementById("lineupSections");
    if (!container) {
        console.error('lineupSections container not found during population.');
        return;
    }

    console.log(`Populating ${initialSpeakersData.length} speakers...`);
    container.innerHTML = ''; // Xóa sạch nội dung cũ để bắt đầu

    // 2. Lặp qua dữ liệu và CHỈ TẠO SUMMARY CARD
    initialSpeakersData.forEach(speaker => {
        const name = speaker.name || 'N/A';
        const tagline = speaker.profile || '';
        const croppedImage = speaker.imageUrl || '/default-avatar.png';

        const summaryCard = document.createElement("div");
        summaryCard.className = "lineup-member-summary";
        summaryCard.innerHTML = `
        <div class="summary-content">
                <div class="summary-drag-handle"><i class="fas fa-grip-lines"></i></div>
                <div class="summary-avatar"><img src="${croppedImage}" alt="${name}" class="summary-image"></div>
            <div class="summary-details">
                <div class="summary-name">${name}</div>
                <div class="summary-role">${getRoleDisplayName(speaker.defaultRole)}</div>
                <div class="summary-tagline">${tagline}</div>
            </div>
            <div class="summary-actions">
                    <button class="summary-edit" type="button" title="Edit member"><i class="fas fa-pen"></i></button>
                    <button class="summary-delete" type="button" title="Delete member"><i class="fas fa-trash"></i></button>
            </div>
        </div>
        `;

        // 3. Gắn sự kiện cho các nút trên summary card
        summaryCard.querySelector(".summary-delete").onclick = async () => {
            if (confirm(`Bạn có chắc chắn muốn xóa "${name}"?`)) {
                try {
                    const response = await fetch(`/api/speakers/${speaker.id}`, {
                        method: 'DELETE',
                        headers: withCsrf()
                    });
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
            editSpeakerFromSummary(summaryCard, speaker);
        };

        container.appendChild(summaryCard);
    });

    // 4. Sau khi đã thêm tất cả các summary, hãy thêm MỘT form trống ở cuối
    addLineupSection();
}

// Gọi khi DOM load hoặc khi fragment được load
document.addEventListener('DOMContentLoaded', populateLineupFromEvent);
//
// Function to edit speaker from summary card
function editSpeakerFromSummary(summaryCard, speaker) {
    console.log('Edit clicked for:', speaker);
    // Tạo form để chỉnh sửa
    const formContainer = addLineupSection();
    if (!formContainer) return;

    // Điền dữ liệu vào form
    formContainer.querySelector(".lineupName").value = speaker.name || "";
    formContainer.querySelector(".lineupTagline").value = speaker.profile || "";
    formContainer.querySelector(".lineupRole").value = speaker.defaultRole || "SPEAKER";

    // Điền ảnh nếu có
    const imagePreview = formContainer.querySelector(".lineup-image-preview");
    const placeholder = imagePreview.querySelector(".lineup-image-placeholder");
    if (speaker.imageUrl) {
        const img = document.createElement('img');
        img.src = speaker.imageUrl;
        img.className = 'lineup-cropped-image';
        img.style.cssText = 'width:100%; height:100%; object-fit:cover; border-radius:50%;';
        placeholder.style.display = 'none';
        imagePreview.appendChild(img);
        formContainer.dataset.croppedImage = speaker.imageUrl;
    }

    // Thay đổi nút "Add another" thành "Save Update" và thêm nút Cancel
    const addAnotherBtn = formContainer.querySelector(".lineup-add-another");
    if (addAnotherBtn) {
        addAnotherBtn.textContent = "Save Update";
        addAnotherBtn.className = "lineup-save-update";
        addAnotherBtn.style.cssText = `
                background: #28a745;
                color: white;
                border: none;
                padding: 0.5rem 1rem;
                border-radius: 4px;
                cursor: pointer;
                font-size: 0.9rem;
                margin-right: 0.5rem;
            `;

        // Thêm nút Cancel
        const cancelBtn = document.createElement("button");
        cancelBtn.textContent = "Cancel";
        cancelBtn.className = "lineup-cancel-update";
        cancelBtn.type = "button";
        cancelBtn.style.cssText = `
                background: #6c757d;
                color: white;
                border: none;
                padding: 0.5rem 1rem;
                border-radius: 4px;
                cursor: pointer;
                font-size: 0.9rem;
            `;

        // Insert cancel button after save button
        addAnotherBtn.parentNode.insertBefore(cancelBtn, addAnotherBtn.nextSibling);

        // Event handler cho nút Cancel
        cancelBtn.onclick = (e) => {
            e.preventDefault();
            e.stopPropagation();

            // Create original summary card with original data
            const originalSummaryCard = document.createElement("div");
            originalSummaryCard.className = "lineup-member-summary";
            originalSummaryCard.innerHTML = `
                    <div class="summary-content">
                        <div class="summary-drag-handle"><i class="fas fa-grip-lines"></i></div>
                        <div class="summary-avatar"><img src="${speaker.imageUrl}" alt="${speaker.name}" class="summary-image"></div>
                        <div class="summary-details">
                            <div class="summary-name">${speaker.name}</div>
                            <div class="summary-role">${getRoleDisplayName(speaker.defaultRole)}</div>
                            <div class="summary-tagline">${speaker.profile}</div>
                        </div>
                        <div class="summary-actions">
                            <button class="summary-edit" type="button" title="Edit member"><i class="fas fa-pen"></i></button>
                            <button class="summary-delete" type="button" title="Delete member"><i class="fas fa-trash"></i></button>
                        </div>
                    </div>
                `;

            // Add event handlers to original summary card
            originalSummaryCard.querySelector(".summary-delete").onclick = async () => {
                if (confirm(`Bạn có chắc chắn muốn xóa "${speaker.name}"?`)) {
                    try {
                        const response = await fetch(`/api/speakers/${speaker.id}`, {method: 'DELETE'});
                        if (response.ok) {
                            originalSummaryCard.remove();
                        } else {
                            alert('Lỗi khi xóa!');
                        }
                    } catch (error) {
                        alert('Lỗi mạng!');
                    }
                }
            };

            originalSummaryCard.querySelector(".summary-edit").onclick = () => {
                editSpeakerFromSummary(originalSummaryCard, speaker);
            };

            // Replace form with original summary card
            formContainer.parentNode.replaceChild(originalSummaryCard, formContainer);
        };

        // Thay đổi event handler
        addAnotherBtn.onclick = async (e) => {
            e.preventDefault();
            e.stopPropagation();

            // Validate form
            const nameInput = formContainer.querySelector(".lineupName");
            const imagePreview = formContainer.querySelector(".lineup-image-preview");
            const hasImage = imagePreview.querySelector(".lineup-cropped-image") || formContainer.dataset.croppedImage;

            if (!nameInput.value.trim()) {
                alert('Name is required');
                return;
            }

            if (!hasImage) {
                alert('Please upload an image first');
                return;
            }

            // Show loading state
            addAnotherBtn.disabled = true;
            addAnotherBtn.textContent = "Saving...";

            try {
                // Prepare data for API
                const updateData = {
                    id: speaker.id,
                    name: nameInput.value.trim(),
                    profile: formContainer.querySelector(".lineupTagline").value.trim(),
                    imageUrl: formContainer.dataset.croppedImage || speaker.imageUrl,
                    defaultRole: formContainer.querySelector(".lineupRole").value,
                    // eventId: window.location.pathname.split('/')[3] // Get eventId from URL
                };

                // Call API to update speaker
                const response = await fetch(`/api/speakers/${speaker.id}`, {
                    method: 'PUT',
                    headers: withCsrf({
                        'Content-Type': 'application/json'
                    }),
                    body: JSON.stringify(updateData)
                });

                if (response.ok) {
                    // Update speaker data
                    speaker.name = updateData.name;
                    speaker.profile = updateData.profile;
                    speaker.imageUrl = updateData.imageUrl;
                    speaker.defaultRole = updateData.defaultRole;

                    // Create new summary card with updated data
                    const updatedSummaryCard = document.createElement("div");
                    updatedSummaryCard.className = "lineup-member-summary";
                    updatedSummaryCard.innerHTML = `
                            <div class="summary-content">
                                <div class="summary-drag-handle"><i class="fas fa-grip-lines"></i></div>
                                <div class="summary-avatar"><img src="${speaker.imageUrl}" alt="${speaker.name}" class="summary-image"></div>
                                <div class="summary-details">
                                    <div class="summary-name">${speaker.name}</div>
                                    <div class="summary-role">${getRoleDisplayName(speaker.defaultRole)}</div>
                                    <div class="summary-tagline">${speaker.profile}</div>
                                </div>
                                <div class="summary-actions">
                                    <button class="summary-edit" type="button" title="Edit member"><i class="fas fa-pen"></i></button>
                                    <button class="summary-delete" type="button" title="Delete member"><i class="fas fa-trash"></i></button>
                                </div>
                            </div>
                        `;

                    // Add event handlers to updated summary card
                    updatedSummaryCard.querySelector(".summary-delete").onclick = async () => {
                        if (confirm(`Bạn có chắc chắn muốn xóa "${speaker.name}"?`)) {
                            try {
                                const response = await fetch(`/api/speakers/${speaker.id}`, {method: 'DELETE'});
                                if (response.ok) {
                                    updatedSummaryCard.remove();
                                } else {
                                    alert('Lỗi khi xóa!');
                                }
                            } catch (error) {
                                alert('Lỗi mạng!');
                            }
                        }
                    };

                    updatedSummaryCard.querySelector(".summary-edit").onclick = () => {
                        editSpeakerFromSummary(updatedSummaryCard, speaker);
                    };

                    // Replace form with updated summary card
                    formContainer.parentNode.replaceChild(updatedSummaryCard, formContainer);

                    alert('Speaker updated successfully!');
                } else {
                    alert('Error updating speaker!');
                }
            } catch (error) {
                console.error('Error updating speaker:', error);
                alert('Network error!');
            } finally {
                addAnotherBtn.disabled = false;
                addAnotherBtn.textContent = "Save Update";
            }
        };
    }

    // Thay thế summary bằng form
    summaryCard.parentNode.replaceChild(formContainer, summaryCard);
}

// Export function để có thể gọi từ bên ngoài
window.populateLineupFromEvent = populateLineupFromEvent;
window.addLineupSection = addLineupSection;
window.editSpeakerFromSummary = editSpeakerFromSummary;








function setupRoleEditListeners(div) {
    const roleText = div.querySelector(".lineup-role-text")
    const roleEdit = div.querySelector(".lineup-role-edit")
    const roleSelect = div.querySelector(".lineupRole")
    const roleIcon = div.querySelector(".lineup-role-icon")

    // Function to update role icon based on selected value
    const updateRoleIcon = (value) => {
        const iconMap = {
            'LINEUP': 'fas fa-users',
            'ARTISTS': 'fas fa-palette',
            'SPEAKERS': 'fas fa-microphone',
            'PERFORMERS': 'fas fa-theater-masks',
            'SINGER': 'fas fa-music',
            'MC': 'fas fa-microphone-alt',
            'OTHER': 'fas fa-star'
        }

        if (roleIcon && iconMap[value]) {
            roleIcon.className = `lineup-role-icon ${iconMap[value]}`
        }
    }

    if (roleText && roleEdit && roleSelect) {
        roleEdit.onclick = (e) => {
            e.preventDefault()
            roleText.style.display = "none"
            roleEdit.style.display = "none"
            roleSelect.style.display = "inline-block"
            roleSelect.value = roleText.textContent.trim().split(' ')[1]?.toUpperCase() || 'LINEUP'
            roleSelect.focus()
        }

        roleSelect.onchange = () => {
            const selectedText = roleSelect.options[roleSelect.selectedIndex].text
            const selectedValue = roleSelect.value

            // Update icon
            updateRoleIcon(selectedValue)

            // Update text (remove emoji and keep only text)
            const textOnly = selectedText.replace(/[🎭🎨🎤🎪🎵🎙️⭐]/g, '').trim()
            roleText.innerHTML = `<i class="lineup-role-icon"></i> ${textOnly}`

            // Update the icon in the new HTML
            const newRoleIcon = roleText.querySelector(".lineup-role-icon")
            if (newRoleIcon) {
                updateRoleIcon(selectedValue)
            }

            roleText.style.display = "inline-block"
            roleEdit.style.display = "inline-block"
            roleSelect.style.display = "none"
        }

        roleSelect.onblur = () => {
            const selectedText = roleSelect.options[roleSelect.selectedIndex].text
            const selectedValue = roleSelect.value

            // Update icon
            updateRoleIcon(selectedValue)

            // Update text (remove emoji and keep only text)
            const textOnly = selectedText.replace(/[🎭🎨🎤🎪🎵🎙️⭐]/g, '').trim()
            roleText.innerHTML = `<i class="lineup-role-icon"></i> ${textOnly}`

            // Update the icon in the new HTML
            const newRoleIcon = roleText.querySelector(".lineup-role-icon")
            if (newRoleIcon) {
                updateRoleIcon(selectedValue)
            }

            roleText.style.display = "inline-block"
            roleEdit.style.display = "inline-block"
            roleSelect.style.display = "none"
        }
    }
}