// Face Check-in JavaScript
// Handles camera access, image capture, and API calls for face-based check-in

let stream = null;
let videoElement = null;
let canvasElement = null;

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    videoElement = document.getElementById('videoElement');
    canvasElement = document.getElementById('canvasElement');
    
    if (!videoElement || !canvasElement) {
        console.error('Video or canvas element not found');
        return;
    }
    
    // Set canvas dimensions to match video
    canvasElement.width = 640;
    canvasElement.height = 480;
});

/**
 * Start camera stream
 */
function startCamera() {
    const startBtn = document.getElementById('startCameraBtn');
    const stopBtn = document.getElementById('stopCameraBtn');
    const captureBtn = document.getElementById('captureBtn');
    
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        showMessage('Trình duyệt của bạn không hỗ trợ truy cập camera.', 'error');
        return;
    }
    
    // Request camera access
    navigator.mediaDevices.getUserMedia({ 
        video: { 
            width: { ideal: 640 },
            height: { ideal: 480 },
            facingMode: 'user' // Front camera
        } 
    })
    .then(function(mediaStream) {
        stream = mediaStream;
        videoElement.srcObject = stream;
        videoElement.style.display = 'block';
        
        // Update button states
        startBtn.style.display = 'none';
        stopBtn.style.display = 'inline-block';
        captureBtn.style.display = 'inline-block';
        captureBtn.disabled = false;
        
        showMessage('Camera đã được bật. Vui lòng đảm bảo khuôn mặt của bạn rõ ràng trong khung hình.', 'info');
    })
    .catch(function(error) {
        console.error('Error accessing camera:', error);
        let errorMessage = 'Không thể truy cập camera. ';
        
        if (error.name === 'NotAllowedError') {
            errorMessage += 'Vui lòng cho phép truy cập camera trong cài đặt trình duyệt.';
        } else if (error.name === 'NotFoundError') {
            errorMessage += 'Không tìm thấy camera.';
        } else if (error.name === 'NotReadableError') {
            errorMessage += 'Camera đang được sử dụng bởi ứng dụng khác.';
        } else {
            errorMessage += error.message;
        }
        
        showMessage(errorMessage, 'error');
    });
}

/**
 * Stop camera stream
 */
function stopCamera() {
    if (stream) {
        stream.getTracks().forEach(track => track.stop());
        stream = null;
    }
    
    if (videoElement) {
        videoElement.srcObject = null;
        videoElement.style.display = 'none';
    }
    
    // Update button states
    document.getElementById('startCameraBtn').style.display = 'inline-block';
    document.getElementById('stopCameraBtn').style.display = 'none';
    document.getElementById('captureBtn').style.display = 'none';
    document.getElementById('captureBtn').disabled = true;
}

/**
 * Capture image from video and perform check-in
 */
function captureAndCheckIn() {
    if (!videoElement || !canvasElement || !stream) {
        showMessage('Vui lòng bật camera trước.', 'error');
        return;
    }
    
    const captureBtn = document.getElementById('captureBtn');
    const loadingIndicator = document.getElementById('loadingIndicator');
    
    // Disable button and show loading
    captureBtn.disabled = true;
    loadingIndicator.classList.add('active');
    
    try {
        // Draw current video frame to canvas
        const context = canvasElement.getContext('2d');
        context.drawImage(videoElement, 0, 0, canvasElement.width, canvasElement.height);
        
        // Convert canvas to base64 image
        const imageBase64 = canvasElement.toDataURL('image/jpeg', 0.8);
        
        // Get event ID from global variable or extract from URL
        const eventId = getEventId();
        if (!eventId) {
            throw new Error('Không tìm thấy ID sự kiện');
        }
        
        // Call API
        performFaceCheckIn(eventId, imageBase64)
            .then(function(response) {
                if (response.success) {
                    showMessage(response.message || 'Check-in thành công!', 'success');
                    // Optionally redirect after success
                    setTimeout(function() {
                        window.location.href = '/events/' + eventId;
                    }, 2000);
                } else {
                    showMessage(response.error || 'Check-in thất bại. Vui lòng thử lại.', 'error');
                    captureBtn.disabled = false;
                }
            })
            .catch(function(error) {
                console.error('Error during face check-in:', error);
                showMessage(error.message || 'Có lỗi xảy ra. Vui lòng thử lại.', 'error');
                captureBtn.disabled = false;
            })
            .finally(function() {
                loadingIndicator.classList.remove('active');
            });
            
    } catch (error) {
        console.error('Error capturing image:', error);
        showMessage('Lỗi khi chụp ảnh. Vui lòng thử lại.', 'error');
        captureBtn.disabled = false;
        loadingIndicator.classList.remove('active');
    }
}

/**
 * Perform face check-in API call
 */
function performFaceCheckIn(eventId, imageBase64) {
    return fetch('/events/api/' + eventId + '/face-checkin', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            imageBase64: imageBase64
        })
    })
    .then(function(response) {
        if (!response.ok) {
            return response.json().then(function(data) {
                throw new Error(data.error || 'Lỗi từ server');
            });
        }
        return response.json();
    });
}

/**
 * Get event ID from URL or global variable
 */
function getEventId() {
    // Try global variable first (set in HTML)
    if (typeof EVENT_ID !== 'undefined' && EVENT_ID !== null) {
        return EVENT_ID;
    }
    
    // Extract from URL: /events/{eventId}/face-checkin
    const pathParts = window.location.pathname.split('/');
    const eventIndex = pathParts.indexOf('events');
    if (eventIndex !== -1 && pathParts[eventIndex + 1]) {
        return pathParts[eventIndex + 1];
    }
    
    console.error('Could not extract eventId from URL:', window.location.pathname);
    return null;
}

/**
 * Show message to user
 */
function showMessage(message, type) {
    const messageDiv = document.getElementById('apiMessage');
    if (!messageDiv) {
        console.error('Message div not found');
        return;
    }
    
    // Remove existing classes
    messageDiv.classList.remove('alert-success', 'alert-error', 'alert-info');
    
    // Add appropriate class
    if (type === 'success') {
        messageDiv.classList.add('alert-success');
    } else if (type === 'error') {
        messageDiv.classList.add('alert-error');
    } else {
        messageDiv.classList.add('alert-info');
    }
    
    messageDiv.textContent = message;
    messageDiv.style.display = 'block';
    
    // Auto-hide after 5 seconds for success/info messages
    if (type !== 'error') {
        setTimeout(function() {
            messageDiv.style.display = 'none';
        }, 5000);
    }
}

// Cleanup when page unloads
window.addEventListener('beforeunload', function() {
    stopCamera();
});

