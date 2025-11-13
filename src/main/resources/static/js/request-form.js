console.log('=== Request form script loaded ===');

// Use event delegation to handle form submit since form is loaded dynamically via SPA
let retryCount = 0;
let retryTimeoutId = null; // Track retry timeout to cancel if needed
const MAX_RETRIES = 15; // Maximum number of retries (increased for safety)

function initializeRequestForm() {
    console.log('=== initializeRequestForm CALLED ===');
    
    // Cancel any existing retry timeout (in case function is called multiple times)
    if (retryTimeoutId !== null) {
        clearTimeout(retryTimeoutId);
        retryTimeoutId = null;
    }
    
    const form = document.getElementById('requestForm');
    if (!form) {
        retryCount++;
        if (retryCount >= MAX_RETRIES) {
            console.error('Form not found after maximum retries. Please refresh the page.');
            retryCount = 0; // Reset for next navigation attempt
            return;
        }
        console.warn(`Form not found yet, retrying... (${retryCount}/${MAX_RETRIES})`);
        // Retry after a short delay
        retryTimeoutId = setTimeout(initializeRequestForm, 100);
        return;
    }

    // Form found - reset retry count and proceed with initialization
    console.log('Form found, attaching submit listener');
    retryCount = 0; // Reset for next time this function is called
    retryTimeoutId = null; // Clear timeout reference

    // Remove any existing listener to avoid duplicates
    const newForm = form.cloneNode(true);
    form.parentNode.replaceChild(newForm, form);

    // Attach listener to the new form
    document.getElementById('requestForm').addEventListener('submit', async function(e) {
        console.log('=== Form submit event triggered ===');
        e.preventDefault();

        const formData = new FormData(this);
        const eventId = document.getElementById('eventId').value;

        // Validate required fields
        const receiverId = formData.get('receiverId');
        if (!receiverId || receiverId === '') {
            alert('Please select a department.');
            return;
        }

        if (!eventId) {
            alert('Event ID is missing.');
            return;
        }

        formData.append('eventId', eventId);
        formData.append('type', 'EVENT_APPROVAL'); // Adjust based on your RequestType enum

        // Show loading state
        const submitBtn = this.querySelector('button[type="submit"]');
        const originalBtnText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Submitting...';

        try {
            console.log('Submitting request:', {
                eventId: eventId,
                receiverId: receiverId,
                type: 'EVENT_APPROVAL',
                message: formData.get('message')
            });

            const response = await fetch('/api/requests', {
                method: 'POST',
                body: formData
            });

            console.log('Response status:', response.status);
            console.log('Response headers:', [...response.headers.entries()]);

            if (response.ok) {
                const data = await response.json().catch(() => null);
                console.log('Request created:', data);
                alert('Request submitted successfully!');
                this.reset();
                
                // Reload the fragment via SPA router to show the new request in the list
                if (window.spaRouter && typeof window.spaRouter.navigateTo === 'function') {
                    const currentPath = window.location.pathname;
                    console.log('Reloading fragment for path:', currentPath);
                    window.spaRouter.navigateTo(currentPath);
                } else {
                    // Fallback: reload entire page if SPA router not available
                    console.warn('SPA router not available, reloading entire page');
                    window.location.reload();
                }
            } else {
                let errorMessage = 'Failed to submit request.';
                try {
                    const errorText = await response.text();
                    console.error('Error response:', errorText);
                    
                    // Try to parse error message from response
                    if (errorText) {
                        // Check if response contains validation error message
                        if (errorText.includes('Cannot send request:')) {
                            // Extract the error message
                            const match = errorText.match(/Cannot send request: ([^<]+)/);
                            if (match && match[1]) {
                                errorMessage = match[1].trim();
                            } else {
                                errorMessage = errorText.substring(0, 200); // Use first 200 chars
                            }
                        } else if (response.status === 403) {
                            errorMessage = 'Access denied. You may not have permission to create this request.';
                        } else if (response.status === 400) {
                            // Try to extract meaningful error message
                            if (errorText.includes('already been approved')) {
                                errorMessage = 'Không thể gửi yêu cầu: Sự kiện đã được duyệt.';
                            } else if (errorText.includes('already a pending request')) {
                                errorMessage = 'Không thể gửi yêu cầu: Đã có yêu cầu đang chờ duyệt cho sự kiện này.';
                            } else {
                                errorMessage = 'Yêu cầu không hợp lệ. Vui lòng kiểm tra lại thông tin.';
                            }
                        } else if (response.status === 500) {
                            // Try to extract error message from server response
                            if (errorText.includes('Cannot send request:')) {
                                const match = errorText.match(/Cannot send request: ([^<]+)/);
                                if (match && match[1]) {
                                    errorMessage = match[1].trim();
                                } else {
                                    errorMessage = 'Lỗi server. Vui lòng thử lại sau.';
                                }
                            } else {
                                errorMessage = 'Lỗi server. Vui lòng thử lại sau.';
                            }
                        }
                    }
                } catch (e) {
                    console.error('Error reading response:', e);
                }
                alert(errorMessage);
            }
        } catch (error) {
            console.error('Error submitting request:', error);
            alert('An error occurred while submitting the request: ' + error.message);
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = originalBtnText;
        }
    });
}

// Expose as global function for SPA router to call
// Don't auto-initialize here because SPA router will handle it
window.initializeRequestForm = initializeRequestForm;