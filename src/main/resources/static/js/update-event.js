class ImageUploadManager {
    constructor() {
        this.posterImages = [];
        this.galleryImages = [];
        this.maxGalleryImages = 5;
        this.maxFileSize = 5 * 1024 * 1024; // 5MB
        this.allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

        this.initEventListeners();
    }

    initEventListeners() {
        console.log('Setting up event listeners...');

        // Poster upload
        const posterBtn = document.getElementById('posterUploadBtn');
        const posterInput = document.getElementById('posterUploadInput');
        const posterArea = document.getElementById('posterUploadArea');

        console.log('Poster elements found:', {
            btn: !!posterBtn,
            input: !!posterInput,
            area: !!posterArea
        });

        if (posterBtn && posterInput && posterArea) {
            // Remove existing listeners to avoid duplicates
            posterBtn.removeEventListener('click', this.posterClickHandler);
            posterInput.removeEventListener('change', this.posterChangeHandler);

            // Define handlers
            this.posterClickHandler = () => {
                console.log('Poster upload button clicked');
                posterInput.click();
            };

            this.posterChangeHandler = (e) => {
                console.log('Poster file selected:', e.target.files.length);
                this.handleFileSelect(e, 'poster');
            };

            // Add listeners
            posterBtn.addEventListener('click', this.posterClickHandler);
            posterInput.addEventListener('change', this.posterChangeHandler);

            // Drag and drop for poster
            this.setupDragAndDrop(posterArea, posterInput, 'poster');
        }

        // Gallery upload
        const galleryBtn = document.getElementById('galleryUploadBtn');
        const galleryInput = document.getElementById('galleryUploadInput');
        const galleryArea = document.getElementById('galleryUploadArea');

        console.log('Gallery elements found:', {
            btn: !!galleryBtn,
            input: !!galleryInput,
            area: !!galleryArea
        });

        if (galleryBtn && galleryInput && galleryArea) {
            // Remove existing listeners to avoid duplicates
            galleryBtn.removeEventListener('click', this.galleryClickHandler);
            galleryInput.removeEventListener('change', this.galleryChangeHandler);

            // Define handlers
            this.galleryClickHandler = () => {
                console.log('Gallery upload button clicked');
                galleryInput.click();
            };

            this.galleryChangeHandler = (e) => {
                console.log('Gallery file selected:', e.target.files.length);
                this.handleFileSelect(e, 'gallery');
            };

            // Add listeners
            galleryBtn.addEventListener('click', this.galleryClickHandler);
            galleryInput.addEventListener('change', this.galleryChangeHandler);

            // Drag and drop for gallery
            this.setupDragAndDrop(galleryArea, galleryInput, 'gallery');
        }
    }

    setupDragAndDrop(area, input, type) {
        area.addEventListener('dragover', (e) => {
            e.preventDefault();
            area.classList.add('dragover');
        });

        area.addEventListener('dragleave', () => {
            area.classList.remove('dragover');
        });

        area.addEventListener('drop', (e) => {
            e.preventDefault();
            area.classList.remove('dragover');
            const files = Array.from(e.dataTransfer.files);
            this.handleFiles(files, type);
        });
    }

    handleFileSelect(event, type) {
        const files = Array.from(event.target.files);
        this.handleFiles(files, type);
    }

    handleFiles(files, type) {
        files.forEach(file => {
            if (this.validateFile(file, type)) {
                this.processFile(file, type);
            }
        });
    }

    validateFile(file, type) {
        // Check file type
        if (!this.allowedTypes.includes(file.type)) {
            this.showError(`File ${file.name} không được hỗ trợ. Chỉ chấp nhận JPG, PNG, WebP.`);
            return false;
        }

        // Check file size
        if (file.size > this.maxFileSize) {
            this.showError(`File ${file.name} quá lớn. Kích thước tối đa là 5MB.`);
            return false;
        }

        // Check gallery limit
        if (type === 'gallery' && this.galleryImages.length >= this.maxGalleryImages) {
            this.showError(`Chỉ được upload tối đa ${this.maxGalleryImages} ảnh gallery.`);
            return false;
        }

        // Check poster limit
        if (type === 'poster' && this.posterImages.length >= 5) {
            this.showError('Chỉ được upload tối đa 5 ảnh poster.');
            return false;
        }

        return true;
    }

    async processFile(file, type) {
        try {
            // Resize image
            const resizedFile = await this.resizeImage(file);

            // Create preview
            const previewUrl = URL.createObjectURL(resizedFile);

            // Add to appropriate array
            const imageData = {
                file: resizedFile,
                previewUrl: previewUrl,
                id: Date.now() + Math.random(),
                orderIndex: type === 'poster' ? this.posterImages.length : 0,
                mainPoster: type === 'poster'
            };

            if (type === 'poster') {
                this.posterImages.push(imageData);
            } else {
                this.galleryImages.push(imageData);
            }

            this.updatePreview(type);
            this.updateUploadButtons();

            // Create image in database
            await this.createImageInDatabase(imageData, type);

        } catch (error) {
            console.error('Error processing file:', error);
            this.showError('Có lỗi xảy ra khi xử lý ảnh.');
        }
    }

    async resizeImage(file) {
        return new Promise((resolve) => {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            const img = new Image();

            img.onload = () => {
                // Calculate new dimensions (max 1920x1080)
                const maxWidth = 1920;
                const maxHeight = 1080;
                let { width, height } = img;

                if (width > maxWidth || height > maxHeight) {
                    const ratio = Math.min(maxWidth / width, maxHeight / height);
                    width *= ratio;
                    height *= ratio;
                }

                canvas.width = width;
                canvas.height = height;

                // Draw and compress
                ctx.drawImage(img, 0, 0, width, height);

                canvas.toBlob((blob) => {
                    resolve(new File([blob], file.name, { type: 'image/jpeg', quality: 0.8 }));
                }, 'image/jpeg', 0.8);
            };

            img.src = URL.createObjectURL(file);
        });
    }

    updatePreview(type) {
        const container = document.getElementById(type === 'poster' ? 'posterPreview' : 'galleryPreview');
        const images = type === 'poster' ? this.posterImages : this.galleryImages;

        container.innerHTML = '';

        images.forEach((imageData, index) => {
            const previewItem = this.createPreviewItem(imageData, index, type);
            container.appendChild(previewItem);
        });
    }

    createPreviewItem(imageData, index, type) {
        const item = document.createElement('div');
        item.className = 'preview-item';
        item.dataset.id = imageData.id;

        const img = document.createElement('img');
        img.src = imageData.previewUrl;
        img.alt = 'Preview';

        const removeBtn = document.createElement('button');
        removeBtn.type='button'
        removeBtn.className = 'remove-btn';
        removeBtn.innerHTML = '×';
        removeBtn.onclick = () => this.removeImage(imageData.id, type);

        item.appendChild(img);
        item.appendChild(removeBtn);

        if (type === 'poster') {
            // Order badge
            const orderBadge = document.createElement('div');
            orderBadge.className = 'order-badge';
            orderBadge.textContent = `#${index + 1}`;
            item.appendChild(orderBadge);

            // Main poster badge - tất cả poster đều có MAIN badge
            const mainBadge = document.createElement('div');
            mainBadge.className = 'main-poster-badge';
            mainBadge.textContent = 'MAIN';
            item.appendChild(mainBadge);
        }

        return item;
    }

    removeImage(id, type) {
        // If it's a database image (has numeric ID), delete from database
        if (typeof id === 'number' || !isNaN(id)) {
            this.removeImageFromDatabase(id, type);
        } else {
            // If it's a temporary image (has string ID), just remove from arrays
            if (type === 'poster') {
                this.posterImages = this.posterImages.filter(img => img.id !== id);
                // Reorder remaining images
                this.posterImages.forEach((img, index) => {
                    img.orderIndex = index;
                });
            } else {
                this.galleryImages = this.galleryImages.filter(img => img.id !== id);
            }

            this.updatePreview(type);
            this.updateUploadButtons();
        }
    }

    // Method to remove image from database
    async removeImageFromDatabase(imageId, type) {
        if (confirm('Bạn có chắc chắn muốn xóa ảnh này?')) {
            try {
                const response = await fetch(`/api/event-images/${imageId}`, {
                    method: 'DELETE'
                });

                if (response.ok) {
                    // Remove from DOM
                    const item = document.querySelector(`[data-id="${imageId}"]`);
                    if (item) {
                        item.remove();
                    }

                    // Nếu xóa poster (mainPoster = true), reload trang để cập nhật UI
                    if (type === 'poster') {
                        window.location.reload();
                    } else {
                        alert('Xóa ảnh thành công!');
                    }
                } else {
                    alert('Lỗi khi xóa ảnh!');
                }
            } catch (error) {
                console.error('Error deleting image:', error);
                alert('Lỗi mạng!');
            }
        }
    }

    setAsMainPoster(id) {
        // If it's a database image (has numeric ID), call API
        if (typeof id === 'number' || !isNaN(id)) {
            this.setAsMainPosterInDatabase(id);
        } else {
            // If it's a temporary image (has string ID), handle locally
            const imageIndex = this.posterImages.findIndex(img => img.id === id);
            if (imageIndex === -1) return;

            // Move image to first position
            const image = this.posterImages.splice(imageIndex, 1)[0];
            this.posterImages.unshift(image);

            // Reorder all images
            this.posterImages.forEach((img, index) => {
                img.orderIndex = index;
            });

            this.updatePreview('poster');
        }
    }

    // Method to set as main poster in database
    async setAsMainPosterInDatabase(imageId) {
        try {
            const response = await fetch(`/api/event-images/${imageId}/set-main`, {
                method: 'PUT'
            });

            if (response.ok) {
                // Reload the page to show updated data
                window.location.reload();
            } else {
                alert('Lỗi khi đặt làm poster chính!');
            }
        } catch (error) {
            console.error('Error setting main poster:', error);
            alert('Lỗi mạng!');
        }
    }

    updateUploadButtons() {
        const posterBtn = document.getElementById('posterUploadBtn');
        const posterArea = document.getElementById('posterUploadArea');
        const galleryBtn = document.getElementById('galleryUploadBtn');
        const galleryArea = document.getElementById('galleryUploadArea');

        // Check poster limit
        if (this.posterImages.length >= 5) {
            posterBtn.disabled = true;
            posterArea.style.opacity = '0.5';
            posterArea.style.cursor = 'not-allowed';
        } else {
            posterBtn.disabled = false;
            posterArea.style.opacity = '1';
            posterArea.style.cursor = 'pointer';
        }

        // Check gallery limit
        if (this.galleryImages.length >= this.maxGalleryImages) {
            galleryBtn.disabled = true;
            galleryArea.style.opacity = '0.5';
            galleryArea.style.cursor = 'not-allowed';
        } else {
            galleryBtn.disabled = false;
            galleryArea.style.opacity = '1';
            galleryArea.style.cursor = 'pointer';
        }
    }

    showError(message) {
        // Create or update error message
        let errorDiv = document.querySelector('.error-message');
        if (!errorDiv) {
            errorDiv = document.createElement('div');
            errorDiv.className = 'error-message';
            document.querySelector('.event-upload-section').appendChild(errorDiv);
        }

        errorDiv.innerHTML = `<i class="fas fa-exclamation-triangle"></i> ${message}`;

        // Auto hide after 5 seconds
        setTimeout(() => {
            if (errorDiv) {
                errorDiv.remove();
            }
        }, 5000);
    }

    // Method to get all images data for form submission
    getAllImagesData() {
        const allImages = [...this.posterImages, ...this.galleryImages];
        return allImages.map(img => ({
            file: img.file,
            orderIndex: img.orderIndex,
            mainPoster: img.mainPoster
        }));
    }

    // Method to upload images to server
    async uploadImagesToServer(eventId) {
        const imagesData = this.getAllImagesData();
        if (imagesData.length === 0) return;

        try {
            const formData = new FormData();
            formData.append('eventId', eventId);

            // Add files
            imagesData.forEach((imgData, index) => {
                formData.append('files', imgData.file);
                formData.append('orderIndexes', imgData.orderIndex);
                formData.append('mainPosters', imgData.mainPoster);
            });

            const response = await fetch('/api/events/upload/multiple-images', {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                console.log('Images uploaded successfully');
                return true;
            } else {
                console.error('Failed to upload images');
                return false;
            }
        } catch (error) {
            console.error('Error uploading images:', error);
            return false;
        }
    }

    // Method to create image in database
    async createImageInDatabase(imageData, type) {
        try {
            const eventId = window.location.pathname.split('/')[3]; // Get eventId from URL

            // Upload image to Cloudinary first
            const imageUrl = await this.uploadImageToCloudinary(imageData.file);

            const imageRequest = {
                url: imageUrl,
                orderIndex: imageData.orderIndex,
                isMainPoster: type === 'poster', // true cho poster, false cho gallery
                eventId: parseInt(eventId)
            };

            console.log('Creating image in database:', imageRequest);

            const response = await fetch('/api/event-images', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(imageRequest)
            });

            if (response.ok) {
                const createdImage = await response.json();
                console.log('Image created successfully:', createdImage);

                // Update the imageData with the database ID
                imageData.id = createdImage.id;
                imageData.url = createdImage.url;
            } else {
                const error = await response.json();
                console.error('Error creating image:', error);
                this.showError('Lỗi khi lưu ảnh vào cơ sở dữ liệu.');
            }
        } catch (error) {
            console.error('Error creating image in database:', error);
            this.showError('Lỗi mạng khi lưu ảnh.');
        }
    }

    // Method to upload image to Cloudinary
    async uploadImageToCloudinary(file) {
        try {
            const formData = new FormData();
            formData.append('file', file);

            const response = await fetch('/api/speakers/upload/image', {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                const result = await response.json();
                return result.imageUrl;
            } else {
                throw new Error('Failed to upload image to Cloudinary');
            }
        } catch (error) {
            console.error('Error uploading to Cloudinary:', error);
            throw error;
        }
    }

    // Clean up event listeners
    cleanup() {
        const posterBtn = document.getElementById('posterUploadBtn');
        const posterInput = document.getElementById('posterUploadInput');
        const galleryBtn = document.getElementById('galleryUploadBtn');
        const galleryInput = document.getElementById('galleryUploadInput');

        if (posterBtn && this.posterClickHandler) {
            posterBtn.removeEventListener('click', this.posterClickHandler);
        }
        if (posterInput && this.posterChangeHandler) {
            posterInput.removeEventListener('change', this.posterChangeHandler);
        }
        if (galleryBtn && this.galleryClickHandler) {
            galleryBtn.removeEventListener('click', this.galleryClickHandler);
        }
        if (galleryInput && this.galleryChangeHandler) {
            galleryInput.removeEventListener('change', this.galleryChangeHandler);
        }
    }
}

// Function to check and retry initialization
function checkAndInitializeUpload() {
    const posterBtn = document.getElementById('posterUploadBtn');
    const galleryBtn = document.getElementById('galleryUploadBtn');

    if (posterBtn && galleryBtn) {
        initializeImageUploadManager();
        return true;
    }
    return false;
}

// Function to initialize image upload manager
function initializeImageUploadManager() {
    console.log('Initializing ImageUploadManager...');

    // Check if required elements exist
    const posterBtn = document.getElementById('posterUploadBtn');
    const galleryBtn = document.getElementById('galleryUploadBtn');

    console.log('Poster button found:', !!posterBtn);
    console.log('Gallery button found:', !!galleryBtn);

    if (window.imageUploadManager) {
        // Clean up existing instance
        window.imageUploadManager.cleanup();
    }

    try {
        window.imageUploadManager = new ImageUploadManager();
        console.log('ImageUploadManager initialized successfully');
    } catch (error) {
        console.error('Error initializing ImageUploadManager:', error);
    }
}

// Initialize when DOM is loaded
// document.addEventListener('DOMContentLoaded', function() {
//     // Wait a bit for dynamic content to load
//     setTimeout(() => {
//         initializeImageUploadManager();
//     }, 100);
// });

// Initialize lineup and agenda buttons
function initializeLineupAndAgendaButtons() {
    console.log('Initializing lineup and agenda buttons...');

    // Lineup functionality
    const addLineupBtn = document.getElementById('addLineupBtn');
    if (addLineupBtn) {
        console.log('Lineup button found, adding event listener');
        addLineupBtn.addEventListener('click', (e) => {
            e.preventDefault();
            console.log('Lineup button clicked');
            if (typeof addLineupSection === 'function') {
                addLineupSection();
            } else {
                console.error('addLineupSection function not found');
            }
        });
    } else {
        console.log('Lineup button not found');
    }

    // Agenda functionality
    const addAgendaBtn = document.getElementById('addAgendaBtn');
    if (addAgendaBtn) {
        console.log('Agenda button found, adding event listener');
        addAgendaBtn.addEventListener('click', (e) => {
            e.preventDefault();
            console.log('Agenda button clicked');
            if (typeof renderAgendaForm === 'function') {
                renderAgendaForm(document.getElementById('agendaSections'));
            } else {
                console.error('renderAgendaForm function not found');
            }
        });
    } else {
        console.log('Agenda button not found');
    }
}

// Initialize event title click functionality
function initializeEventTitleClick() {
    console.log('Initializing event title click...');

    const eventTitle = document.querySelector('.event-title');
    const eventOverviewForm = document.getElementById('eventOverviewForm');

    if (eventTitle && eventOverviewForm) {
        console.log('Event title and form found, adding click listener');

        // Define click handler
        const handleEventTitleClick = () => {
            console.log('Event title clicked, showing form');
            eventOverviewForm.style.display = 'block';
        };

        // Remove existing listeners to avoid duplicates
        eventTitle.removeEventListener('click', handleEventTitleClick);

        // Add click listener
        eventTitle.addEventListener('click', handleEventTitleClick);
    } else {
        console.log('Event title or form not found:', {
            eventTitle: !!eventTitle,
            eventOverviewForm: !!eventOverviewForm
        });
    }
}

// Also initialize when content is dynamically loaded (for SPA)
window.initializeEventFormListeners = function() {
    console.log('Initializing event form listeners...');
    // Wait for elements to be available
    setTimeout(() => {
        console.log('UPDATE EVENT: Starting initialization...');
        initializeImageUploadManager();
        initializeLineupAndAgendaButtons();
        if (typeof window.initializeEventTypeTabs === 'function') {
            window.initializeEventTypeTabs();
        }
        initializeEventTitleClick();

        // Initialize save button

        // Load speakers from database
        console.log('Attempting to load speakers from database...');
        if (typeof window.populateLineupFromEvent === 'function') {
            console.log('Calling populateLineupFromEvent...');
            window.populateLineupFromEvent();
        } else {
            console.error('populateLineupFromEvent function not found!');
        }

        // Load schedules from database
        console.log('Attempting to load schedules from database...');
        if (typeof window.populateSchedulesFromEvent === 'function') {
            console.log('Calling populateSchedulesFromEvent...');
            window.populateSchedulesFromEvent();
        } else {
            console.error('populateSchedulesFromEvent function not found!');
        }

        // Load images from database
        console.log('Attempting to load images from database...');
        if (typeof window.populateImagesFromEvent === 'function') {
            console.log('Calling populateImagesFromEvent...');
            window.populateImagesFromEvent();
        } else {
            console.error('populateImagesFromEvent function not found!');
        }

        // Load places from database - this will initialize PlaceManager
        console.log('Attempting to load places from database...');
        if (typeof window.populatePlacesFromEvent === 'function') {
            console.log('Calling populatePlacesFromEvent...');
            window.populatePlacesFromEvent();
        } else {
            console.error('populatePlacesFromEvent function not found!');
        }

        // Load ticket types from database
        console.log('Attempting to load ticket types from database...');
        if (typeof window.populateTicketTypesFromEvent === 'function') {
            console.log('Calling populateTicketTypesFromEvent...');
            window.populateTicketTypesFromEvent();
        } else {
            console.error('populateTicketTypesFromEvent function not found!');
        }

        // Initialize ticketManager for settings page if not exists
        if (!window.ticketManager && typeof TicketManager !== 'undefined') {
            console.log('Creating ticketManager for settings page...');
            window.ticketManager = new TicketManager();
            // Initialize with empty data since we're on settings page
            window.ticketManager.initializeTicketTypes([]);
        }

        console.log('UPDATE EVENT: Initialization completed');
    }, 200);

    // Add to form submission
    const form = document.querySelector('form');
    if (form) {
        form.addEventListener('submit', async function(e) {
            e.preventDefault();

            // Show loading state
            const submitBtn = form.querySelector('button[type="submit"]');
            const originalText = submitBtn.textContent;
            submitBtn.disabled = true;
            submitBtn.textContent = 'Đang xử lý...';

            try {
                // First, submit the event form
                const formData = new FormData(form);

                // Add places data to form
                console.log('🔍 update-event.js: Checking placeManager...');
                console.log('🔍 update-event.js: placeManager:', placeManager);
                console.log('🔍 update-event.js: typeof placeManager:', typeof placeManager);
                console.log('🔍 update-event.js: window.placeManager:', window.placeManager);

                if (placeManager && typeof placeManager.getPlacesData === 'function') {
                    console.log('🔍 update-event.js: Calling placeManager.getPlacesData()...');
                    const placesData = placeManager.getPlacesData();
                    console.log('🔍 update-event.js: placesData received:', placesData);
                    formData.append('placesJson', JSON.stringify(placesData));
                    console.log('Added places data to form:', placesData);
                } else {
                    console.error('❌ update-event.js: placeManager not available or getPlacesData not a function');
                    console.error('❌ placeManager:', placeManager);
                    console.error('❌ typeof placeManager.getPlacesData:', typeof placeManager?.getPlacesData);
                }

                // Add tickets data to form
                if (window.ticketManager && typeof window.ticketManager.getTicketsData === 'function') {
                    const ticketsData = window.ticketManager.getTicketsData();
                    formData.append('ticketsJson', JSON.stringify(ticketsData));
                    console.log('Added tickets data to form:', ticketsData);
                }

                const eventResponse = await fetch(form.action, {
                    method: 'POST',
                    body: formData
                });

                if (eventResponse.ok) {
                    // Get event ID from response or form
                    const eventId = document.getElementById('eventIdInput')?.value ||
                        new URLSearchParams(new URL(eventResponse.url).search).get('id');

                    if (eventId) {
                        // Upload images
                        const imagesUploaded = await window.imageUploadManager.uploadImagesToServer(eventId);
                        if (imagesUploaded) {
                            alert('Sự kiện và ảnh đã được lưu thành công!');
                            // Redirect or refresh page
                            window.location.reload();
                        } else {
                            alert('Sự kiện đã được lưu nhưng có lỗi khi upload ảnh.');
                        }
                    } else {
                        alert('Sự kiện đã được lưu thành công!');
                    }
                } else {
                    alert('Có lỗi xảy ra khi lưu sự kiện.');
                }
            } catch (error) {
                console.error('Error submitting form:', error);
                alert('Có lỗi xảy ra khi xử lý form.');
            } finally {
                // Reset button state
                submitBtn.disabled = false;
                submitBtn.textContent = originalText;
            }
        });
    }
};

// PlaceManager đã được tách ra file riêng: place-manager.js