// ================== Image Management ==================

// Function to populate images from database
function populateImagesFromEvent() {
    console.log('--- BƯỚC C: populateImagesFromEvent ĐANG CHẠY ---');
    console.log('initialImagesData:', initialImagesData);
    
    // 1. Kiểm tra dữ liệu
    if (!initialImagesData || initialImagesData.length === 0) {
        console.log('No initial images to populate.');
        return;
    }
    console.log('LOG: Tìm thấy', initialImagesData.length, 'images để load.');

    // 2. Phân loại images thành poster và gallery
    const posterImages = initialImagesData.filter(img => img.mainPoster === true);
    const galleryImages = initialImagesData.filter(img => img.mainPoster === false);

    console.log('Poster images:', posterImages.length);
    console.log('Gallery images:', galleryImages.length);

    // 3. Load poster images
    if (posterImages.length > 0) {
        loadPosterImages(posterImages);
    }

    // 4. Load gallery images
    if (galleryImages.length > 0) {
        loadGalleryImages(galleryImages);
    }
}

// Function to load poster images
function loadPosterImages(posterImages) {
    const container = document.getElementById("posterPreview");
    if (!container) {
        console.error('posterPreview container not found');
        return;
    }

    container.innerHTML = ''; // Clear existing content

    posterImages.forEach((image, index) => {
        const previewItem = createImagePreviewItem(image, index, 'poster');
        container.appendChild(previewItem);
    });
}

// Function to load gallery images
function loadGalleryImages(galleryImages) {
    const container = document.getElementById("galleryPreview");
    if (!container) {
        console.error('galleryPreview container not found');
        return;
    }

    container.innerHTML = ''; // Clear existing content

    galleryImages.forEach((image, index) => {
        const previewItem = createImagePreviewItem(image, index, 'gallery');
        container.appendChild(previewItem);
    });
}

// Function to create image preview item
function createImagePreviewItem(imageData, index, type) {
    const item = document.createElement('div');
    item.className = 'preview-item';
    item.dataset.id = imageData.id;

    const img = document.createElement('img');
    img.src = imageData.url;
    img.alt = 'Preview';

    const removeBtn = document.createElement('button');
    removeBtn.type='button'
    removeBtn.className = 'remove-btn';
    removeBtn.innerHTML = '×';
    removeBtn.onclick = () => removeImage(imageData.id, type);

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

// Function to remove image
async function removeImage(imageId, type) {
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
                alert('Xóa ảnh thành công!');
            } else {
                alert('Lỗi khi xóa ảnh!');
            }
        } catch (error) {
            console.error('Error deleting image:', error);
            alert('Lỗi mạng!');
        }
    }
}

// Function to set as main poster
async function setAsMainPoster(imageId) {
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

// Function to create new image
async function createImage(imageData, type) {
    try {
        const eventId = window.location.pathname.split('/')[3]; // Get eventId from URL
        
        const imageRequest = {
            url: imageData.url,
            orderIndex: imageData.orderIndex || 0,
            isMainPoster: type === 'poster',
            eventId: parseInt(eventId)
        };
        
        console.log('Creating image:', imageRequest);
        
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
            
            // Create preview item with created image data
            const container = document.getElementById(type === 'poster' ? 'posterPreview' : 'galleryPreview');
            const previewItem = createImagePreviewItem(createdImage, container.children.length, type);
            container.appendChild(previewItem);
            
            alert('Thêm ảnh thành công!');
        } else {
            const error = await response.json();
            alert('Lỗi khi thêm ảnh: ' + error.error);
        }
    } catch (error) {
        console.error('Error creating image:', error);
        alert('Lỗi mạng!');
    }
}

// Export function để có thể gọi từ bên ngoài
window.populateImagesFromEvent = populateImagesFromEvent;
window.createImage = createImage;
