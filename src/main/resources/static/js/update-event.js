class ImageUploadManager {
    constructor() {
        this.posterImages = [];
        this.galleryImages = [];
        this.maxGalleryImages = 5;
        this.maxFileSize = 5 * 1024 * 1024; // 5MB
        this.allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

        this.initEventListeners();
        
        // Load existing images from database if available
        this.loadExistingImages();
    }
    
    // Load existing images from database into manager
    loadExistingImages() {
        console.log('=== ImageUploadManager.loadExistingImages CALLED ===');
        
        // Try to get initialImagesData from window (set by Thymeleaf or fragment)
        const existingImages = window.initialImagesData || [];
        console.log('Found existing images:', existingImages.length);
        
        if (existingImages && existingImages.length > 0) {
            // Clear arrays first to avoid duplicates
            this.posterImages = [];
            this.galleryImages = [];
            
            existingImages.forEach(imageData => {
                // Convert database image to ImageUploadManager format
                const imageEntry = {
                    id: imageData.id, // Database ID
                    url: imageData.url,
                    previewUrl: imageData.url, // Use URL as preview
                    file: null, // No file since it's from database
                    orderIndex: imageData.orderIndex || 0,
                    mainPoster: imageData.mainPoster || false
                };
                
                if (imageData.mainPoster) {
                    this.posterImages.push(imageEntry);
                    console.log('Added poster image:', imageEntry);
                } else {
                    this.galleryImages.push(imageEntry);
                    console.log('Added gallery image:', imageEntry);
                }
            });
            
            // Sort posters by orderIndex after loading
            this.posterImages.sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0));
            console.log('Poster images after sorting:', this.posterImages.map(img => ({ id: img.id, orderIndex: img.orderIndex })));
            
            // Update preview to show existing images
            this.updatePreview('poster');
            this.updatePreview('gallery');
            this.updateUploadButtons();
            
            console.log('Loaded existing images - Posters:', this.posterImages.length, 'Gallery:', this.galleryImages.length);
        }
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

            // Create temporary ID that won't conflict with database IDs (use negative number or string)
            const tempId = 'temp_' + Date.now() + '_' + Math.random();

            // For poster: if this is the first poster or we want it to be the main (orderIndex = 0),
            // we need to shift existing posters' orderIndex
            let orderIndex = 0;
            if (type === 'poster') {
                // New poster should have orderIndex = 0 to be the main poster
                // Shift all existing posters' orderIndex by 1
                this.posterImages.forEach(img => {
                    img.orderIndex = (img.orderIndex || 0) + 1;
                });
                orderIndex = 0;
            }

            // Add to appropriate array
            const imageData = {
                file: resizedFile,
                previewUrl: previewUrl,
                id: tempId, // Temporary ID (will be replaced with database ID)
                orderIndex: orderIndex,
                mainPoster: type === 'poster'
            };

            console.log('=== processFile: Adding new image ===');
            console.log('Type:', type);
            console.log('Current posterImages length:', this.posterImages.length);
            console.log('Current galleryImages length:', this.galleryImages.length);
            console.log('New imageData:', imageData);

            if (type === 'poster') {
                this.posterImages.unshift(imageData); // Add to beginning to maintain order
                console.log('After adding - posterImages length:', this.posterImages.length);
                console.log('All poster images:', this.posterImages.map(img => ({ id: img.id, orderIndex: img.orderIndex })));
            } else {
                this.galleryImages.push(imageData);
            }

            // Update preview immediately to show new image
            console.log('Updating preview for type:', type);
            console.log('Current state before updatePreview:');
            console.log('  posterImages:', this.posterImages.map(img => ({ id: img.id, orderIndex: img.orderIndex })));
            console.log('  galleryImages:', this.galleryImages.map(img => ({ id: img.id, orderIndex: img.orderIndex })));
            
            this.updatePreview(type);
            this.updateUploadButtons();

            // Create image in database (this will update the ID and URL)
            await this.createImageInDatabase(imageData, type);

        } catch (error) {
            console.error('Error processing file:', error);
            this.showError('Có lỗi xảy ra khi xử lý ảnh.');
            
            // Remove failed image from arrays
            if (type === 'poster') {
                this.posterImages = this.posterImages.filter(img => img.id !== imageData?.id);
            } else {
                this.galleryImages = this.galleryImages.filter(img => img.id !== imageData?.id);
            }
            this.updatePreview(type);
            this.updateUploadButtons();
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
        if (!container) {
            console.error(`Container not found for type: ${type}`);
            return;
        }
        
        const images = type === 'poster' ? this.posterImages : this.galleryImages;

        console.log(`=== updatePreview(${type}) CALLED ===`);
        console.log(`Images array length: ${images.length}`);
        console.log(`Images before sort:`, images.map((img, idx) => ({ 
            index: idx, 
            id: img.id, 
            orderIndex: img.orderIndex, 
            url: img.url || img.previewUrl,
            previewUrl: img.previewUrl
        })));

        // Clear container first
        container.innerHTML = '';

        // Sort by orderIndex to ensure correct order (poster with orderIndex 0 should be first)
        // Make a deep copy to avoid mutating the original array
        const sortedImages = images.slice().sort((a, b) => {
            const orderA = a.orderIndex || 0;
            const orderB = b.orderIndex || 0;
            return orderA - orderB;
        });
        
        console.log(`Sorted images:`, sortedImages.map((img, idx) => ({ 
            index: idx, 
            id: img.id, 
            orderIndex: img.orderIndex,
            url: img.url || img.previewUrl,
            previewUrl: img.previewUrl
        })));

        // Render all images
        sortedImages.forEach((imageData, index) => {
            try {
                const previewItem = this.createPreviewItem(imageData, index, type);
                if (previewItem) {
                    container.appendChild(previewItem);
                    console.log(`✓ Added preview item for image ID: ${imageData.id}, orderIndex: ${imageData.orderIndex}, displayIndex: ${index}`);
                } else {
                    console.error(`Failed to create preview item for image ID: ${imageData.id}`);
                }
            } catch (error) {
                console.error(`Error creating preview item for image ID: ${imageData.id}:`, error);
            }
        });
        
        console.log(`Preview updated. Container now has ${container.children.length} items (expected: ${sortedImages.length})`);
        
        // Verify all images are displayed
        if (container.children.length !== sortedImages.length) {
            console.warn(`⚠️ Mismatch: Expected ${sortedImages.length} items, but container has ${container.children.length} items`);
        }
    }

    createPreviewItem(imageData, index, type) {
        const item = document.createElement('div');
        item.className = 'preview-item';
        // Store both original ID and a unique identifier for DOM element
        item.dataset.id = String(imageData.id);
        item.dataset.imageIndex = index; // Add index for easier tracking

        const img = document.createElement('img');
        // Use previewUrl if available (for new uploads), otherwise use url (for database images)
        img.src = imageData.previewUrl || imageData.url;
        img.alt = 'Preview';
        
        // Ensure image loads correctly
        img.onerror = () => {
            console.warn('Failed to load image:', imageData.previewUrl || imageData.url);
            img.src = '/img/placeholder.svg'; // Fallback image
        };

        const removeBtn = document.createElement('button');
        removeBtn.type='button'
        removeBtn.className = 'remove-btn';
        removeBtn.innerHTML = '×';
        // Use closure to capture current imageData
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
        console.log('=== removeImage CALLED ===');
        console.log('id:', id, 'type:', type, 'typeof id:', typeof id);
        
        // Check if it's a temporary ID (starts with 'temp_') or a database ID (number)
        const isTemporaryId = typeof id === 'string' && id.startsWith('temp_');
        const isDatabaseId = typeof id === 'number' || (typeof id === 'string' && !isNaN(parseFloat(id)) && !id.startsWith('temp_'));
        
        console.log('isTemporaryId:', isTemporaryId, 'isDatabaseId:', isDatabaseId);
        
        // If it's a database image (has numeric ID), delete from database
        if (isDatabaseId) {
            const numericId = typeof id === 'string' ? parseFloat(id) : id;
            this.removeImageFromDatabase(numericId, type);
        } else {
            console.log('Removing temporary image from arrays');
            // If it's a temporary image (has string ID starting with 'temp_'), just remove from arrays
            const imageArray = type === 'poster' ? this.posterImages : this.galleryImages;
            const originalLength = imageArray.length;
            
            if (type === 'poster') {
                this.posterImages = this.posterImages.filter(img => {
                    const imgIdStr = String(img.id);
                    const targetIdStr = String(id);
                    return imgIdStr !== targetIdStr;
                });
                // Reorder remaining images
                this.posterImages.forEach((img, index) => {
                    img.orderIndex = index;
                });
            } else {
                this.galleryImages = this.galleryImages.filter(img => {
                    const imgIdStr = String(img.id);
                    const targetIdStr = String(id);
                    return imgIdStr !== targetIdStr;
                });
            }
            
            console.log(`Removed temporary image. Array length: ${originalLength} -> ${imageArray.length}`);
            this.updatePreview(type);
            this.updateUploadButtons();
        }
    }

    // Method to remove image from database
    async removeImageFromDatabase(imageId, type) {
        if (confirm('Bạn có chắc chắn muốn xóa ảnh này?')) {
            try {
                console.log('Deleting image from database:', imageId, 'type:', type);
                
                const response = await fetch(`/api/event-images/${imageId}`, {
                    method: 'DELETE'
                });

                console.log('Delete response status:', response.status);

                if (response.ok) {
                    // Remove from manager arrays
                    if (type === 'poster') {
                        this.posterImages = this.posterImages.filter(img => img.id !== imageId);
                        // Reorder remaining images
                        this.posterImages.forEach((img, index) => {
                            img.orderIndex = index;
                        });
                    } else {
                        this.galleryImages = this.galleryImages.filter(img => img.id !== imageId);
                    }
                    
                    // Remove from DOM
                    const item = document.querySelector(`[data-id="${imageId}"]`);
                    if (item) {
                        item.remove();
                    }
                    
                    // Update upload buttons
                    this.updateUploadButtons();
                    
                    console.log('Image deleted successfully. Posters:', this.posterImages.length, 'Gallery:', this.galleryImages.length);
                    alert('Xóa ảnh thành công!');
                } else {
                    const errorText = await response.text();
                    console.error('Error deleting image - status:', response.status);
                    console.error('Error response:', errorText);
                    alert('Lỗi khi xóa ảnh!');
                }
            } catch (error) {
                console.error('Error deleting image:', error);
                console.error('Error stack:', error.stack);
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
        console.log('=== createImageInDatabase CALLED ===');
        console.log('imageData:', imageData);
        console.log('type:', type);
        
        try {
            const eventId = window.location.pathname.split('/')[3]; // Get eventId from URL
            console.log('Extracted eventId from URL:', eventId);
            
            if (!eventId || isNaN(eventId)) {
                console.error('Invalid eventId:', eventId);
                this.showError('Không tìm thấy Event ID. Vui lòng reload trang.');
                return;
            }

            console.log('Uploading image to Cloudinary...');
            // Upload image to Cloudinary first
            const imageUrl = await this.uploadImageToCloudinary(imageData.file);
            console.log('Image uploaded to Cloudinary, URL:', imageUrl);

            const imageRequest = {
                url: imageUrl,
                orderIndex: imageData.orderIndex || 0,
                isMainPoster: type === 'poster', // true cho poster, false cho gallery
                eventId: parseInt(eventId)
            };

            console.log('Creating image in database with request:', imageRequest);

            const response = await fetch('/api/event-images', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(imageRequest)
            });

            console.log('Response status:', response.status);
            console.log('Response ok:', response.ok);

            if (response.ok) {
                const createdImage = await response.json();
                console.log('Image created successfully:', createdImage);
                console.log('Current posterImages before update:', this.posterImages.map(img => ({ id: img.id, orderIndex: img.orderIndex })));

                // Find the image in the array by matching the temporary ID
                const imageArray = type === 'poster' ? this.posterImages : this.galleryImages;
                
                console.log('Searching for image with temp ID:', imageData.id);
                console.log('Available IDs in array:', imageArray.map(img => ({ id: img.id, type: typeof img.id })));
                
                const imageIndex = imageArray.findIndex(img => {
                    // Match by temporary ID (string comparison)
                    const match = String(img.id) === String(imageData.id);
                    return match;
                });
                
                console.log('Found imageIndex:', imageIndex);
                if (imageIndex !== -1) {
                    console.log('Found matching image at index:', imageIndex, 'with temp ID:', imageArray[imageIndex].id);
                }
                
                if (imageIndex !== -1) {
                    // Update the imageData with the database ID and URL
                    const updatedImage = imageArray[imageIndex];
                    console.log('Updating image at index:', imageIndex, 'from temp ID:', updatedImage.id, 'to DB ID:', createdImage.id);
                    console.log('Array length before update:', imageArray.length);
                    
                    // Store the orderIndex before update
                    const savedOrderIndex = updatedImage.orderIndex;
                    
                    updatedImage.id = createdImage.id;
                    updatedImage.url = createdImage.url;
                    updatedImage.orderIndex = savedOrderIndex; // Preserve orderIndex
                    
                    // Keep previewUrl if it's a blob URL, otherwise use database URL
                    if (updatedImage.previewUrl && updatedImage.previewUrl.startsWith('blob:')) {
                        // Keep blob URL for now, will use database URL on reload
                        console.log('Keeping blob URL for preview:', updatedImage.previewUrl);
                    } else {
                        updatedImage.previewUrl = createdImage.url;
                    }
                    
                    console.log('Updated imageData:', updatedImage);
                    console.log('Array length after update:', imageArray.length);
                    console.log('Current array state:', imageArray.map(img => ({ 
                        id: img.id, 
                        orderIndex: img.orderIndex, 
                        url: img.url || img.previewUrl 
                    })));
                } else {
                    console.error('Could not find image in array to update!');
                    console.error('Searched for temp ID:', imageData.id);
                    console.error('Array length:', imageArray.length);
                    console.error('Array contents:', imageArray.map(img => ({ 
                        id: img.id, 
                        tempId: imageData.id, 
                        match: String(img.id) === String(imageData.id) 
                    })));
                    // Fallback: create new entry if not found (shouldn't happen, but just in case)
                    const newImageEntry = {
                        id: createdImage.id,
                        url: createdImage.url,
                        previewUrl: createdImage.url,
                        file: null,
                        orderIndex: imageData.orderIndex || 0,
                        mainPoster: type === 'poster'
                    };
                    if (type === 'poster') {
                        this.posterImages.unshift(newImageEntry); // Add to beginning to match the order
                    } else {
                        this.galleryImages.push(newImageEntry);
                    }
                    console.log('Added fallback entry. New array length:', imageArray.length);
                }
                
                // Verify array integrity before updating preview
                console.log('Before final updatePreview - Array state:');
                console.log('  posterImages:', this.posterImages.length, 'items');
                console.log('  galleryImages:', this.galleryImages.length, 'items');
                console.log('  All poster IDs:', this.posterImages.map(img => img.id));
                console.log('  All gallery IDs:', this.galleryImages.map(img => img.id));
                
                // Update preview to show updated images with correct IDs
                console.log('Updating preview after DB save - Current array length:', imageArray.length);
                this.updatePreview(type);
                this.updateUploadButtons();
                
                console.log('Image added to manager:', type === 'poster' ? 
                    `Posters: ${this.posterImages.length}` : `Gallery: ${this.galleryImages.length}`);
            } else {
                const errorText = await response.text();
                console.error('Error creating image - status:', response.status);
                console.error('Error response:', errorText);
                
                let errorMessage = 'Lỗi khi lưu ảnh vào cơ sở dữ liệu.';
                try {
                    const errorJson = JSON.parse(errorText);
                    if (errorJson.error) {
                        errorMessage = errorJson.error;
                    }
                } catch (e) {
                    // Not JSON, use default message
                }
                this.showError(errorMessage);
                
                // Remove failed image from arrays
                const imageArray = type === 'poster' ? this.posterImages : this.galleryImages;
                const imageIndex = imageArray.findIndex(img => img.id === imageData.id);
                if (imageIndex !== -1) {
                    imageArray.splice(imageIndex, 1);
                    this.updatePreview(type);
                    this.updateUploadButtons();
                }
            }
        } catch (error) {
            console.error('Error creating image in database:', error);
            console.error('Error stack:', error.stack);
            this.showError('Lỗi mạng khi lưu ảnh: ' + error.message);
            
            // Remove failed image from arrays
            const imageArray = type === 'poster' ? this.posterImages : this.galleryImages;
            const imageIndex = imageArray.findIndex(img => img.id === imageData.id);
            if (imageIndex !== -1) {
                imageArray.splice(imageIndex, 1);
                this.updatePreview(type);
                this.updateUploadButtons();
            }
        }
    }

    // Method to upload image to Cloudinary
    async uploadImageToCloudinary(file) {
        console.log('=== uploadImageToCloudinary CALLED ===');
        console.log('File:', file.name, 'Size:', file.size, 'Type:', file.type);
        
        try {
            const formData = new FormData();
            formData.append('file', file);

            console.log('Sending request to /api/speakers/upload/image');
            const response = await fetch('/api/speakers/upload/image', {
                method: 'POST',
                body: formData
            });

            console.log('Cloudinary upload response status:', response.status);

            if (response.ok) {
                const result = await response.json();
                console.log('Cloudinary upload result:', result);
                
                const imageUrl = result.imageUrl;
                if (!imageUrl) {
                    console.error('No imageUrl in response:', result);
                    throw new Error('Không nhận được URL ảnh từ server');
                }
                
                return imageUrl;
            } else {
                const errorText = await response.text();
                console.error('Failed to upload image to Cloudinary - status:', response.status);
                console.error('Error response:', errorText);
                throw new Error('Không thể upload ảnh lên Cloudinary: ' + (errorText || 'Unknown error'));
            }
        } catch (error) {
            console.error('Error uploading to Cloudinary:', error);
            console.error('Error stack:', error.stack);
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

    const eventOverviewForm = document.getElementById('eventOverviewForm');

    // Check if form is hidden (display: none) - if form is already visible, no need to add click handler
    if (eventOverviewForm) {
        const formStyle = window.getComputedStyle(eventOverviewForm);
        const isFormHidden = formStyle.display === 'none';
        
        if (isFormHidden) {
            // Only add click handler if form is hidden
            const eventTitle = document.querySelector('.event-title');
            
            if (eventTitle) {
                console.log('Event title and form found, form is hidden - adding click listener');

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
                console.log('Event title not found');
            }
        } else {
            console.log('Event overview form is already visible, no need to add click handler');
        }
    } else {
        console.log('Event overview form not found');
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
        
        // Auto-show event type fields if function exists (for when loading existing event data)
        if (typeof window.autoShowEventTypeFields === 'function') {
            console.log('Auto-showing event type fields...');
            window.autoShowEventTypeFields();
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