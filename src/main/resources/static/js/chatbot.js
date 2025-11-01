// ===== CONFIGURATION =====
// These will be set dynamically from the page
let API_BASE_URL = "";
let USER_ID = 2;
const API_ENDPOINT = "/api/ai/chat/enhanced";

// ===== GLOBAL VARIABLES =====
let chatbotToggler = null;
let closeBtn = null;
let chatInput = null;
let sendBtn = null;
let chatMessages = null;
let typingIndicator = null;
let sessionPopupInitialized = false;

// ===== CHAT HISTORY MANAGEMENT =====
const chatHistory = new Map(); // Lưu trữ chat history cho mỗi session

// Hàm lưu chat history
function saveChatHistory(sessionId) {
    if (!chatMessages) return;
    
    const messages = Array.from(chatMessages.children).map(msg => ({
        type: msg.classList.contains('user-message') ? 'user' : 'bot',
        content: msg.querySelector('.message-bubble').textContent.trim(),
        timestamp: new Date().toISOString()
    }));
    
    chatHistory.set(sessionId, messages);
    console.log(`Saved chat history for session ${sessionId}:`, messages);
}

// Hàm khôi phục chat history
function restoreChatHistory(sessionId) {
    if (!chatMessages) return;
    
    const history = chatHistory.get(sessionId);
    if (!history || history.length === 0) {
        // Nếu không có history, hiển thị welcome message
        displayMessage('bot', 'Chào bạn, chúng ta bắt đầu lại nhé! Bạn cần tôi giúp gì?');
        return;
    }
    
    // Xóa chat hiện tại
    chatMessages.innerHTML = '';
    
    // Khôi phục từng tin nhắn
    history.forEach(msg => {
        displayMessage(msg.type, msg.content);
    });
    
    console.log(`Restored chat history for session ${sessionId}:`, history);
}

// ===== INITIALIZATION =====
document.addEventListener('DOMContentLoaded', async () => {
    try {
        // Get configuration from page
        const contextPathMeta = document.querySelector('meta[name="context-path"]');
        const userIdFromBody = document.body.getAttribute('data-user-id');
        
        // Extract context path from Thymeleaf URL (e.g., "/openevent/" -> "/openevent")
        let contextPath = contextPathMeta ? contextPathMeta.content : '';
        if (contextPath.endsWith('/')) {
            contextPath = contextPath.slice(0, -1); // Remove trailing slash
        }
        API_BASE_URL = contextPath;
        USER_ID = userIdFromBody ? parseInt(userIdFromBody) : 2;
        
        console.log('Chatbot config:', { API_BASE_URL, USER_ID });
        
        // Debug: Check if user is authenticated
        console.log('Checking authentication...');
        try {
            const authCheck = await fetch(`${API_BASE_URL}/api/ai/chat/enhanced/health`, {
                method: 'GET',
                headers: {'Content-Type': 'application/json'}
            });
            console.log('Auth check result:', authCheck.status, authCheck.statusText);
        } catch (error) {
            console.error('Auth check failed:', error);
        }
        
        // Load chatbot HTML dynamically
        await loadChatbotHTML();
        
        // Initialize chatbot functionality
        initializeChatbot();
        
        // Adjust chatbot position for viewport
        adjustChatbotPosition();
        
        // Listen for window resize
        window.addEventListener('resize', adjustChatbotPosition);
        
        // Check API health on initialization
        checkApiHealth();
        
    } catch (error) {
        console.error('Error initializing chatbot:', error);
        showConnectionError('Không thể khởi tạo chatbot. Vui lòng tải lại trang.');
    }
});

// ===== HEALTH CHECK =====
async function checkApiHealth() {
    try {
        // Try to check health endpoint
        const response = await fetch(`${API_BASE_URL}/api/ai/chat/enhanced/health`, {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        });
        
        if (response.ok) {
            console.log('API Health: OK');
            updateConnectionStatus('online');
        } else if (response.status === 401 || response.status === 403) {
            console.log('API Health: Authentication required');
            updateConnectionStatus('offline');
        } else {
            console.log('API Health: Error', response.status);
            updateConnectionStatus('offline');
        }
    } catch (error) {
        console.warn('API Health check failed:', error);
        updateConnectionStatus('offline');
    }
}

function updateConnectionStatus(status) {
    const statusDot = document.querySelector('.status-dot');
    const statusText = document.querySelector('.status-text');
    
    if (statusDot && statusText) {
        if (status === 'online') {
            statusDot.style.background = 'var(--success)';
            statusText.textContent = 'Online';
        } else {
            statusDot.style.background = 'var(--error)';
            statusText.textContent = 'Offline';
        }
    }
}

function showConnectionError(message) {
    if (chatMessages) {
        displayMessage('bot', `⚠️ ${message}`);
    }
}

// ===== LOAD CHATBOT HTML =====
async function loadChatbotHTML() {
    // HTML is already loaded via Thymeleaf fragment, no need to fetch
    console.log('Chatbot HTML already loaded via Thymeleaf fragment');
}

// ===== FALLBACK CHATBOT =====
function createFallbackChatbot() {
    const container = document.getElementById('openevent-chatbot-container');
    if (!container) return;
    
    container.innerHTML = `
        <button class="chatbot-toggler" id="chatbotToggler">
            <span class="material-symbols-outlined chat-icon">smart_toy</span>
            <span class="material-symbols-outlined close-icon">close</span>
        </button>
        <div id="chatbotContainer" class="chatbot-container">
            <header class="chatbot-header">
                <div class="header-left">
                    <div class="avatar-container">
                        <span class="material-symbols-outlined">smart_toy</span>
                    </div>
                    <div class="header-info">
                        <h2 class="chatbot-title">OpenEventAI</h2>
                        <div class="status-indicator">
                            <span class="status-dot"></span>
                            <span class="status-text">Online</span>
                        </div>
                    </div>
                </div>
                <button class="close-btn" id="closeBtn">
                    <span class="material-symbols-outlined">close</span>
                </button>
            </header>
            <div class="chatbot-body">
                <div class="messages-container" id="chatMessages">
                    <div class="message bot-message">
                        <div class="message-avatar">
                            <span class="material-symbols-outlined">smart_toy</span>
                        </div>
                        <div class="message-bubble bot-bubble">
                            <div class="message-content">
                                Xin chào! 👋 Tôi là OpenEventAI, trợ lý thông minh của bạn. Tôi có thể giúp bạn tìm kiếm sự kiện, đặt vé, và nhiều hơn nữa!
                            </div>
                        </div>
                    </div>
                </div>
                <div class="typing-indicator" id="typingIndicator">
                    <div class="message bot-message">
                        <div class="message-avatar">
                            <span class="material-symbols-outlined">smart_toy</span>
                        </div>
                        <div class="message-bubble bot-bubble typing-bubble">
                            <div class="typing-dots">
                                <span></span>
                                <span></span>
                                <span></span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="chatbot-input">
                <div class="input-container">
                    <input type="text" id="chatInput" class="message-input" placeholder="Nhập tin nhắn của bạn..." maxlength="500">
                    <button id="chatSendBtn" class="send-button" type="button">
                        <span class="material-symbols-outlined">send</span>
                    </button>
                </div>
            </div>
        </div>
    `;
}

// ===== INITIALIZE CHATBOT =====
function initializeChatbot() {
    // Get DOM elements
    chatbotToggler = document.querySelector(".chatbot-toggler");
    closeBtn = document.querySelector(".close-btn");
    chatInput = document.getElementById('chatInput');
    sendBtn = document.getElementById('chatSendBtn');
    chatMessages = document.getElementById('chatMessages');
    typingIndicator = document.getElementById('typingIndicator');

    // Check if elements exist
    if (!chatbotToggler || !closeBtn || !chatInput || !sendBtn || !chatMessages || !typingIndicator) {
        console.error('Chatbot elements not found');
        return;
    }


    // Add event listeners
    sendBtn.addEventListener('click', sendMessage);
    chatInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
    
    // Toggle chatbot popup
    chatbotToggler.addEventListener("click", () => {
        document.body.classList.toggle("show-chatbot");
        if (document.body.classList.contains("show-chatbot")) {
            chatInput.focus();
        } else {
            // Close session popup when chatbot is closed
            closeSessionPopup();
        }
    });
    
    // Close chatbot
    closeBtn.addEventListener("click", () => {
        document.body.classList.remove("show-chatbot");
        // Close session popup when chatbot is closed
        closeSessionPopup();
    });
    
    // Avatar click to open session popup
    const avatarContainer = document.querySelector('.avatar-container');
    if (avatarContainer) {
        avatarContainer.addEventListener('click', openSessionPopup);
    }
}

// ===== MESSAGE FUNCTIONS =====
function sendQuickAction(message) {
    if (chatInput) {
        chatInput.value = message;
        sendMessage();
    }
}

async function sendMessage() {
    if (!chatInput) return;
    
    const message = chatInput.value.trim();
    if (!message) return;

    // Lưu chat history trước khi gửi tin nhắn mới
    const currentSessionId = getCurrentSessionId();
    saveChatHistory(currentSessionId);
    
    // Check if this is the first user message in current session
    const history = chatHistory.get(currentSessionId) || [];
    const userMessages = history.filter(msg => msg.type === 'user');
    const isFirstMessage = userMessages.length === 0;
    
    displayMessage('user', message);
    chatInput.value = '';
    showTyping(true);
    
    // If this is the first user message, update session title
    if (isFirstMessage) {
        updateSessionTitle(currentSessionId, message);
    }
    
    await sendMessageToApi(message);
}

// ===== API INTEGRATION =====
async function sendMessageToApi(message, retryCount = 0) {
    if (!sendBtn) return;
    
    sendBtn.disabled = true;
    try {
        const response = await fetch(`${API_BASE_URL}${API_ENDPOINT}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ 
                message: message, 
                userId: USER_ID,
                sessionId: getCurrentSessionId()
            })
        });

        if (!response.ok) {
            let errorMessage = `Lỗi HTTP! Trạng thái: ${response.status}`;
            
            // Xử lý các mã lỗi cụ thể
            switch (response.status) {
                case 400:
                    errorMessage = 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại tin nhắn.';
                    break;
                case 401:
                    errorMessage = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
                    break;
                case 403:
                    errorMessage = 'Không có quyền truy cập. Vui lòng đăng nhập để sử dụng chatbot.';
                    break;
                case 429:
                    errorMessage = 'Bạn đã gửi quá nhiều tin nhắn. Vui lòng chờ một chút rồi thử lại.';
                    break;
                case 500:
                    errorMessage = 'Lỗi máy chủ. Vui lòng thử lại sau.';
                    break;
                case 503:
                    errorMessage = 'Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.';
                    break;
            }
            
            throw new Error(errorMessage);
        }

        const data = await response.json();
        showTyping(false);

        // 1. Tách tin nhắn và tín hiệu
        let botMessage = data.message || 'Xin lỗi, tôi chưa hiểu ý bạn.';
        let redirectUrl = null;
        let doReload = false;

        // 2. Kiểm tra tín hiệu REDIRECT trước
        if (botMessage.includes("__REDIRECT:")) {
            const match = botMessage.match(/__REDIRECT:([^]*)__/);
            if (match && match[1]) {
                redirectUrl = match[1];
                botMessage = botMessage.replace(match[0], "").trim(); // Xóa tín hiệu
            }
        } else if (botMessage.includes("__RELOAD__")) { // Kiểm tra RELOAD
            doReload = true;
            botMessage = botMessage.replace("__RELOAD__", "").trim(); // Xóa tín hiệu
        }

        // 3. Hiển thị tin nhắn sạch cho người dùng
        displayMessage('bot', botMessage);

        // 4. (Rất quan trọng) Lưu lịch sử chat
        // (Bạn nên có logic lưu history vào sessionStorage/localStorage ở đây
        // để không bị mất chat khi chuyển trang)
        try {
            const currentSessionId = getCurrentSessionId();
            saveChatHistory(currentSessionId);
        } catch (e) { console.warn('Không thể lưu chat history', e); }


        // 5. Thực hiện hành động (Redirect hoặc Reload)
        if (redirectUrl) {
            displayMessage('bot', '🤖 Chuyển hướng trong 1.5 giây...');
            setTimeout(() => {
                // Đảm bảo URL là đầy đủ nếu cần
                // Nếu API_BASE_URL là "/openevent" và redirectUrl là "/events"
                // thì nó sẽ thành "/openevent/events"
                window.location.href = API_BASE_URL + redirectUrl;
            }, 1500);
        } else if (doReload) {
            setTimeout(() => {
                location.reload();
            }, 1500);
        }

    } catch (error) {
        console.error("Lỗi khi gửi tin nhắn đến API:", error);
        showTyping(false);
        
        // Thử lại nếu chưa quá số lần cho phép
        if (retryCount < 2 && !error.message.includes('Phiên đăng nhập') && !error.message.includes('quyền truy cập')) {
            displayMessage('bot', `Đang thử lại kết nối... (${retryCount + 1}/2)`);
            setTimeout(() => {
                sendMessageToApi(message, retryCount + 1);
            }, 2000 * (retryCount + 1)); // Tăng thời gian chờ theo số lần thử
        } else {
            displayMessage('bot', `❌ ${error.message}`);
            
            // Hiển thị nút thử lại
            const retryButton = document.createElement('button');
            retryButton.textContent = '🔄 Thử lại';
            retryButton.className = 'retry-button';
            retryButton.style.cssText = `
                background: var(--primary);
                color: white;
                border: none;
                padding: 8px 16px;
                border-radius: 8px;
                cursor: pointer;
                margin-top: 8px;
                font-size: 12px;
            `;
            retryButton.onclick = () => {
                retryButton.remove();
                sendMessageToApi(message);
            };
            
            // Thêm nút vào tin nhắn cuối cùng
            const lastMessage = chatMessages.lastElementChild;
            if (lastMessage && lastMessage.querySelector('.bot-bubble')) {
                lastMessage.querySelector('.bot-bubble').appendChild(retryButton);
            }
        }
    } finally {
        if (sendBtn) {
            sendBtn.disabled = false;
        }
    }
}

// ===== SESSION MANAGEMENT =====
function getCurrentSessionId() {
    let sessionId = sessionStorage.getItem('chatbot_session_id');
    if (!sessionId) {
        sessionId = 'SESSION_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
        sessionStorage.setItem('chatbot_session_id', sessionId);
    }
    return sessionId;
}

// ===== UI FUNCTIONS =====
function clearChat() {
    if (!chatMessages) return;
    
    chatMessages.innerHTML = '';
    // Tùy chọn: Gửi yêu cầu xóa lịch sử chat trên server nếu cần
    // fetch(`${API_BASE_URL}/api/ai/clear_history`, { method: 'POST', ... });
    displayMessage('bot', 'Chào bạn, chúng ta bắt đầu lại nhé! Bạn cần tôi giúp gì?');
}

function displayMessage(sender, message) {
    if (!chatMessages) return;
    
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}-message`;

    // Tạo avatar container
    const avatarDiv = document.createElement('div');
    avatarDiv.className = 'message-avatar';
    
    const avatarIcon = document.createElement('span');
    avatarIcon.className = 'material-symbols-outlined';
    avatarIcon.textContent = sender === 'bot' ? 'smart_toy' : 'person';
    avatarDiv.appendChild(avatarIcon);

    // Tạo message bubble
    const bubbleDiv = document.createElement('div');
    bubbleDiv.className = `message-bubble ${sender}-bubble`;

    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';
    contentDiv.innerHTML = formatMessage(message);
    
    bubbleDiv.appendChild(contentDiv);

    // Thêm các phần tử vào message
    if (sender === 'user') {
        messageDiv.appendChild(bubbleDiv);
        messageDiv.appendChild(avatarDiv);
    } else {
        messageDiv.appendChild(avatarDiv);
        messageDiv.appendChild(bubbleDiv);
    }

    chatMessages.appendChild(messageDiv);
    
    // Force scroll to bottom with animation
    setTimeout(() => {
        chatMessages.scrollTo({
            top: chatMessages.scrollHeight,
            behavior: 'smooth'
        });
    }, 100);
}

function formatMessage(message) {
    // Chuyển đổi an toàn để tránh lỗi XSS
    const safeMessage = message.replace(/</g, "&lt;").replace(/>/g, "&gt;");

    // Chuyển đổi Markdown: newlines, bold, italic
    return safeMessage
        .replace(/\n/g, '<br>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>');
}

function showTyping(show) {
    if (!typingIndicator || !chatMessages) return;

    // Ensure typing indicator is inside the messages container so it's visible
    // if (!chatMessages.contains(typingIndicator)) {
    //     chatMessages.appendChild(typingIndicator);
    // }

    if (show) {
        typingIndicator.classList.add('show');
        // Scroll to bottom to reveal the typing indicator
        chatMessages.scrollTo({
            top: chatMessages.scrollHeight,
            behavior: 'smooth'
        });
    } else {
        typingIndicator.classList.remove('show');
    }
}

// ===== UTILITY FUNCTIONS =====
function isChatbotOpen() {
    return document.body.classList.contains("show-chatbot");
}

function openChatbot() {
    document.body.classList.add("show-chatbot");
    if (chatInput) {
        chatInput.focus();
    }
}

function closeChatbot() {
    document.body.classList.remove("show-chatbot");
}

// ===== SCROLL TO BOTTOM =====
function scrollToBottom() {
    if (chatMessages) {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
}

// ===== ADJUST CHATBOT POSITION =====
function adjustChatbotPosition() {
    const container = document.getElementById('chatbotContainer');
    if (!container) return;
    
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    
    // Reset styles first
    container.style.right = '';
    container.style.width = '';
    container.style.maxWidth = '';
    container.style.height = '';
    container.style.maxHeight = '';
    container.style.minHeight = '';
    
    // Responsive adjustments based on viewport size
    if (viewportWidth <= 480) {
        // Full screen for very small devices
        container.style.right = '0';
        container.style.bottom = '0';
        container.style.width = '100%';
        container.style.height = '100%';
        container.style.minHeight = '100%';
        container.style.maxHeight = '100%';
        container.style.borderRadius = '0';
        container.style.border = 'none';
        container.style.boxShadow = 'none';
    } else if (viewportWidth <= 600) {
        // Almost full width for small devices
        container.style.right = '4px';
        container.style.width = 'calc(100vw - 8px)';
        container.style.height = `min(90vh, ${Math.min(450, viewportHeight - 100)}px)`;
        container.style.minHeight = '300px';
    } else if (viewportWidth <= 768) {
        // Medium screens
        container.style.right = '8px';
        container.style.width = 'calc(100vw - 16px)';
        container.style.height = `min(85vh, ${Math.min(500, viewportHeight - 120)}px)`;
        container.style.minHeight = '350px';
    } else {
        // Large screens - default behavior
        container.style.right = '16px';
        container.style.width = 'min(400px, calc(100vw - 32px))';
        container.style.height = `min(75vh, ${Math.min(600, viewportHeight - 120)}px)`;
        container.style.minHeight = '400px';
    }
    
    // Ensure chatbot doesn't exceed viewport height
    const maxHeight = viewportHeight - 120;
    if (container.offsetHeight > maxHeight) {
        container.style.height = `${maxHeight}px`;
    }
    
    console.log('Chatbot position adjusted for viewport:', {
        viewportWidth,
        viewportHeight,
        containerWidth: container.offsetWidth,
        containerHeight: container.offsetHeight,
        containerStyle: {
            right: container.style.right,
            width: container.style.width,
            height: container.style.height
        }
    });
}

// ===== SESSION POPUP FUNCTIONS =====
function openSessionPopup() {
    const sessionPopup = document.getElementById('sessionPopup');
    if (sessionPopup) {
        sessionPopup.style.display = 'flex';
        setTimeout(() => {
            sessionPopup.classList.add('show');
        }, 10);
        
        // Add event listeners for popup - chỉ setup một lần
        if (!sessionPopupInitialized) {
            setupSessionPopupEvents();
            sessionPopupInitialized = true;
        }
    }
}

function closeSessionPopup() {
    const sessionPopup = document.getElementById('sessionPopup');
    if (sessionPopup) {
        sessionPopup.classList.remove('show');
        setTimeout(() => {
            sessionPopup.style.display = 'none';
        }, 300);
    }
}

function setupSessionPopupEvents() {
    const sessionPopup = document.getElementById('sessionPopup');
    const sessionPopupClose = document.getElementById('sessionPopupClose');
    const newSessionBtn = document.getElementById('oeNewSessionBtn');
    
    if (sessionPopupClose) {
        sessionPopupClose.addEventListener('click', closeSessionPopup);
    }
    
    // Close popup with Escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && sessionPopup && sessionPopup.style.display !== 'none') {
            closeSessionPopup();
        }
    });
    
    // Handle new session button - sử dụng event delegation
    if (newSessionBtn) {
        newSessionBtn.addEventListener('click', () => {
            createNewSession();
            closeSessionPopup();
        });
    }
    
    // Sử dụng event delegation cho session items
    const sessionList = document.querySelector('.session-list');
    if (sessionList) {
        // Xóa listener cũ nếu có
        const clonedList = sessionList.cloneNode(true);
        sessionList.parentNode.replaceChild(clonedList, sessionList);
        
        // Thêm listener mới cho toàn bộ list
        clonedList.addEventListener('click', (e) => {
            const clickedItem = e.target.closest('.session-item');
            if (!clickedItem) return;
            
            const deleteBtn = e.target.closest('.session-delete-btn');
            const actionBtn = e.target.closest('.session-action-btn');
            
            if (deleteBtn) {
                e.stopPropagation();
                deleteSession(clickedItem);
            } else if (actionBtn) {
                e.stopPropagation();
                // Switch to this session
                const sessionId = clickedItem.getAttribute('data-session-id');
                selectSession(sessionId);
            } else if (e.target.closest('.session-item')) {
                // Click vào session item để select
                const sessionId = clickedItem.getAttribute('data-session-id');
                selectSession(sessionId);
            }
        });
    }
}

// Hàm chuyển đổi phiên làm việc
function selectSession(sessionId) {
    // Lưu chat history của phiên hiện tại
    const currentSessionId = getCurrentSessionId();
    saveChatHistory(currentSessionId);
    
    // Chuyển sang phiên mới
    sessionStorage.setItem('chatbot_session_id', sessionId);
    
    // Khôi phục chat history của phiên được chọn
    restoreChatHistory(sessionId);
    
    // Cập nhật UI
    const sessionItems = document.querySelectorAll('.session-item');
    sessionItems.forEach(si => si.classList.remove('active'));
    
    const selectedItem = document.querySelector(`[data-session-id="${sessionId}"]`);
    if (selectedItem) {
        selectedItem.classList.add('active');
    }
    
    console.log('Switched to session:', sessionId);
}

function createNewSession() {
    // Kiểm tra nếu đang trong quá trình tạo session để tránh tạo nhiều lần
    if (window.isCreatingSession) {
        console.log('Đang tạo session, vui lòng đợi...');
        return;
    }
    
    window.isCreatingSession = true;
    
    try {
        // Lưu chat history của phiên hiện tại
        const currentSessionId = getCurrentSessionId();
        saveChatHistory(currentSessionId);
        
        // Generate a new session ID
        const newSessionId = `SESSION_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        
        // Update session storage
        sessionStorage.setItem('chatbot_session_id', newSessionId);
        
        // Khôi phục chat history cho phiên mới (sẽ là welcome message)
        restoreChatHistory(newSessionId);
        
        // Update UI session list
        const sessionList = document.querySelector('.session-list');
        if (sessionList) {
            // Remove active class from all sessions
            const sessionItems = document.querySelectorAll('.session-item');
            sessionItems.forEach(si => si.classList.remove('active'));
            
            // Create new session item
            const newSessionItem = document.createElement('div');
            newSessionItem.classList.add('session-item', 'active');
            newSessionItem.setAttribute('data-session-id', newSessionId);
            newSessionItem.innerHTML = `
                <div class="session-info">
                    <span class="session-title">Phiên mới</span>
                    <span class="session-time">Vừa tạo</span>
                </div>
                <div class="session-actions">
                    <button class="session-action-btn" title="Sử dụng phiên này">
                        <span class="material-symbols-outlined">play_arrow</span>
                    </button>
                    <button class="session-delete-btn" title="Xóa phiên này">
                        <span class="material-symbols-outlined">delete</span>
                    </button>
                </div>
            `;
            
            // Add new session at the top
            sessionList.insertBefore(newSessionItem, sessionList.firstChild);
            
            console.log('Created new session:', newSessionId);
        }
    } finally {
        // Reset flag sau một khoảng thời gian ngắn
        setTimeout(() => {
            window.isCreatingSession = false;
        }, 300);
    }
}

function deleteSession(sessionItem) {
    const sessionId = sessionItem.getAttribute('data-session-id');
    const sessionTitle = sessionItem.querySelector('.session-title').textContent;
    
    // Show confirmation dialog
    if (confirm(`Bạn có chắc chắn muốn xóa phiên "${sessionTitle}"?`)) {
        // Xóa chat history
        chatHistory.delete(sessionId);
        
        // Remove from DOM
        sessionItem.remove();
        
        // If this was the active session, activate another one
        const remainingSessions = document.querySelectorAll('.session-item');
        if (remainingSessions.length > 0) {
            const newActiveSessionId = remainingSessions[0].getAttribute('data-session-id');
            selectSession(newActiveSessionId);
        }
        
        console.log('Deleted session:', sessionTitle);
    }
}

function updateSessionTitle(sessionId, firstMessage) {
    // Extract title from first message (limit to 30 characters)
    let title = firstMessage.trim();
    if (title.length > 30) {
        title = title.substring(0, 30) + '...';
    }
    
    // Find the session item and update its title
    const sessionItem = document.querySelector(`[data-session-id="${sessionId}"]`);
    if (sessionItem) {
        const titleElement = sessionItem.querySelector('.session-title');
        if (titleElement) {
            titleElement.textContent = title;
        }
    }
}

// ===== EXPORT FOR EXTERNAL USE =====
window.OpenEventAI = {
    sendQuickAction,
    clearChat,
    openChatbot,
    closeChatbot,
    isChatbotOpen
};