// ===== CONFIGURATION =====
let API_BASE_URL = "";
let USER_ID = 2;
let SESSION_ID = null;
const API_ENDPOINT = "/api/ai/chat/enhanced";

// ===== GLOBAL VARIABLES =====
let chatInput = null;
let sendBtn = null;
let messagesSection = null;
let avatarSection = null;
let refreshBtn = null;
let minimizeBtn = null;
let closeBtn = null;

// ===== CHAT HISTORY MANAGEMENT =====
const chatHistory = [];

// ===== INITIALIZATION =====
async function initChatWeb() {
    try {
        console.log('🚀 Starting chatweb initialization...');
        
        // Get configuration from page
        const contextPathMeta = document.querySelector('meta[name="context-path"]');
        const userIdFromBody = document.body.getAttribute('data-user-id');
        const sessionIdFromBody = document.body.getAttribute('data-session-id');
        
        // Extract context path
        let contextPath = contextPathMeta ? contextPathMeta.content : '';
        
        // Fallback: Auto-detect from window.location
        if (!contextPath || contextPath === '/') {
            const pathname = window.location.pathname;
            const pathParts = pathname.split('/').filter(p => p);
            if (pathParts.length > 0 && pathParts[0] !== 'chatweb') {
                contextPath = '/' + pathParts[0];
            } else {
                contextPath = '';
            }
        }
        
        if (contextPath.endsWith('/')) {
            contextPath = contextPath.slice(0, -1);
        }
        
        API_BASE_URL = contextPath || '';
        USER_ID = userIdFromBody ? parseInt(userIdFromBody) : 2;
        SESSION_ID = sessionIdFromBody || getSessionIdFromUrl() || generateSessionId();
        
        // Save referrer URL (page before chatweb) for minimize button
        const referrer = document.referrer || (API_BASE_URL ? API_BASE_URL + '/' : '/');
        sessionStorage.setItem('chatweb_referrer_url', referrer);
        console.log('💾 Saved referrer URL:', referrer);
        
        // Try to load from sessionStorage
        loadFromSessionStorage();
        
        console.log('📋 Chatweb config:', {
            API_BASE_URL: API_BASE_URL || '(root)',
            USER_ID: USER_ID,
            SESSION_ID: SESSION_ID,
            contextPathMeta: contextPathMeta ? contextPathMeta.content : 'not found',
            fullApiUrl: `${API_BASE_URL}${API_ENDPOINT}`
        });
        
        // Get DOM elements
        chatInput = document.getElementById('chat-input');
        sendBtn = document.getElementById('send-btn');
        messagesSection = document.getElementById('messages-section');
        avatarSection = document.getElementById('avatar-section');
        refreshBtn = document.getElementById('refresh-btn');
        minimizeBtn = document.getElementById('minimize-btn');
        closeBtn = document.getElementById('close-btn');
        
        console.log('🔍 DOM Elements check:', {
            chatInput: !!chatInput,
            sendBtn: !!sendBtn,
            messagesSection: !!messagesSection,
            avatarSection: !!avatarSection,
            refreshBtn: !!refreshBtn,
            minimizeBtn: !!minimizeBtn,
            closeBtn: !!closeBtn
        });
        
        if (!chatInput || !sendBtn || !messagesSection) {
            console.error('❌ Essential elements not found');
            return;
        }
        
        // Initialize event listeners
        initializeEventListeners();
        
        // Load chat history
        if (chatHistory.length > 0) {
            restoreChatHistory();
        } else {
            // Show welcome message
            showWelcomeMessage();
        }
        
        // Check API health
        checkApiHealth();
        
        // Focus input
        setTimeout(() => {
            chatInput.focus();
        }, 100);
        
        console.log('✅ Chatweb initialization complete!');
        
    } catch (error) {
        console.error('❌ Error initializing chatweb:', error);
        console.error('Error stack:', error.stack);
        showConnectionError('Không thể khởi tạo chatbot. Vui lòng tải lại trang.');
    }
}

// ===== SESSION MANAGEMENT =====
function getSessionIdFromUrl() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('sessionId');
}

function generateSessionId() {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

function loadFromSessionStorage() {
    try {
        const savedSessionId = sessionStorage.getItem('chatweb_session_id');
        const savedUserId = sessionStorage.getItem('chatweb_user_id');
        const savedHistory = sessionStorage.getItem('chatweb_history');
        const savedApiBaseUrl = sessionStorage.getItem('chatweb_api_base_url');
        
        if (savedSessionId && !SESSION_ID) {
            SESSION_ID = savedSessionId;
        }
        
        if (savedUserId && !USER_ID) {
            USER_ID = parseInt(savedUserId);
        }
        
        if (savedApiBaseUrl && !API_BASE_URL) {
            API_BASE_URL = savedApiBaseUrl;
        }
        
        if (savedHistory) {
            try {
                const historyData = JSON.parse(savedHistory);
                chatHistory.length = 0;
                Object.values(historyData).forEach(msg => {
                    chatHistory.push(msg);
                });
                console.log('✅ Loaded chat history from sessionStorage:', chatHistory.length, 'messages');
            } catch (e) {
                console.warn('Failed to parse chat history:', e);
            }
        }
    } catch (e) {
        console.warn('Failed to load from sessionStorage:', e);
    }
}

function saveToSessionStorage() {
    try {
        sessionStorage.setItem('chatweb_session_id', SESSION_ID);
        sessionStorage.setItem('chatweb_user_id', USER_ID.toString());
        sessionStorage.setItem('chatweb_api_base_url', API_BASE_URL);
        
        const historyData = {};
        chatHistory.forEach((msg, index) => {
            historyData[index] = msg;
        });
        sessionStorage.setItem('chatweb_history', JSON.stringify(historyData));
    } catch (e) {
        console.warn('Failed to save to sessionStorage:', e);
    }
}

// ===== EVENT LISTENERS =====
function initializeEventListeners() {
    console.log('🔧 Initializing event listeners...');
    
    // Send button
    if (sendBtn) {
        sendBtn.addEventListener('click', sendMessage);
        console.log('✅ Send button listener attached');
    } else {
        console.warn('⚠️ Send button not found');
    }
    
    // Input enter key
    if (chatInput) {
        chatInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
        console.log('✅ Input keypress listener attached');
    } else {
        console.warn('⚠️ Chat input not found');
    }
    
    // Header buttons - re-query to ensure fresh references
    refreshBtn = document.getElementById('refresh-btn');
    minimizeBtn = document.getElementById('minimize-btn');
    closeBtn = document.getElementById('close-btn');
    
    if (refreshBtn) {
        // Check if listener already attached
        if (refreshBtn.hasAttribute('data-listener-attached')) {
            console.log('⚠️ Refresh button listener already attached, skipping...');
        } else {
            refreshBtn.setAttribute('data-listener-attached', 'true');
            refreshBtn.addEventListener('click', function(e) {
                console.log('🔄 Refresh button clicked');
                e.stopPropagation();
                e.preventDefault();
                handleRefresh();
            });
            console.log('✅ Refresh button listener attached');
        }
    } else {
        console.warn('⚠️ Refresh button not found');
    }
    
    if (minimizeBtn) {
        // Check if listener already attached
        if (minimizeBtn.hasAttribute('data-listener-attached')) {
            console.log('⚠️ Minimize button listener already attached, skipping...');
        } else {
            minimizeBtn.setAttribute('data-listener-attached', 'true');
            minimizeBtn.addEventListener('click', function(e) {
                console.log('⤡ Minimize button clicked');
                e.stopPropagation();
                e.preventDefault();
                handleMinimize();
            });
            console.log('✅ Minimize button listener attached');
        }
    } else {
        console.warn('⚠️ Minimize button not found');
    }
    
    if (closeBtn) {
        // Check if listener already attached
        if (closeBtn.hasAttribute('data-listener-attached')) {
            console.log('⚠️ Close button listener already attached, skipping...');
        } else {
            closeBtn.setAttribute('data-listener-attached', 'true');
            closeBtn.addEventListener('click', function(e) {
                console.log('⇲ Close button clicked');
                e.stopPropagation();
                e.preventDefault();
                handleClose();
            });
            console.log('✅ Close button listener attached');
        }
    } else {
        console.warn('⚠️ Close button not found');
    }
}

// ===== HEADER BUTTON HANDLERS =====
function handleRefresh() {
    console.log('🔄 handleRefresh called');
    if (confirm('Bạn có chắc muốn làm mới cuộc trò chuyện? Lịch sử chat hiện tại sẽ được xóa.')) {
        clearChat();
        createNewSession();
    }
}

function handleMinimize() {
    console.log('⤡ handleMinimize called');
    
    // Save current chat state before navigating back
    const historyData = {};
    chatHistory.forEach((msg, index) => {
        historyData[index] = msg;
    });
    sessionStorage.setItem('chatweb_history', JSON.stringify(historyData));
    sessionStorage.setItem('chatweb_session_id', SESSION_ID);
    sessionStorage.setItem('chatweb_user_id', USER_ID.toString());
    sessionStorage.setItem('chatweb_api_base_url', API_BASE_URL);
    
    // Mark that we're returning from chatweb (so chatbot.js can restore history)
    sessionStorage.setItem('returning_from_chatweb', 'true');
    
    // Get referrer URL or fallback to home
    const referrerUrl = sessionStorage.getItem('chatweb_referrer_url');
    const targetUrl = referrerUrl || (API_BASE_URL ? API_BASE_URL + '/' : '/');
    
    console.log('Navigating back to:', targetUrl);
    console.log('Chat history saved:', chatHistory.length, 'messages');
    
    window.location.href = targetUrl;
}

function handleClose() {
    console.log('⇲ handleClose called');
    // Navigate back to previous page or home
    const baseUrl = API_BASE_URL || '';
    const targetUrl = baseUrl + (baseUrl.endsWith('/') ? '' : '/');
    console.log('Navigating to:', targetUrl);
    window.location.href = targetUrl;
}

// ===== CHAT FUNCTIONS =====
function showWelcomeMessage() {
    const welcomeText = `Chào bạn, mình là trợ lý AI của OpenEvent!<br><br>
    Bạn cần hỗ trợ gì hoặc có thể chọn một trong các chủ đề dưới đây nhé.
    Trong quá trình tư vấn, nếu chưa hài lòng với câu trả lời của mình, bạn vui lòng chat "Tôi muốn gặp tư vấn viên" để được hỗ trợ.`;
    
    const quickActions = [
        { text: 'Tạo sự kiện', message: 'Tôi muốn tạo sự kiện mới' },
        { text: 'Các sự kiện nổi bật gần đây', message: 'Cho tôi xem các sự kiện nổi bật gần đây' },
        { text: 'Mua vé sự kiện', message: 'Tôi muốn mua vé sự kiện' },
        { text: 'Quản lý sự kiện', message: 'Tôi muốn quản lý sự kiện của tôi' }
    ];
    
    displayMessage('bot', welcomeText, quickActions);
}

function clearChat() {
    if (messagesSection) {
        messagesSection.innerHTML = '';
    }
    if (avatarSection) {
        avatarSection.style.display = '';
    }
    chatHistory.length = 0;
    saveToSessionStorage();
}

function createNewSession() {
    SESSION_ID = generateSessionId();
    saveToSessionStorage();
    showWelcomeMessage();
}

function restoreChatHistory() {
    if (!messagesSection) return;
    
    messagesSection.innerHTML = '';
    
    // Hide avatar section if there are messages
    if (avatarSection && chatHistory.length > 0) {
        avatarSection.style.display = 'none';
    }
    
    chatHistory.forEach(msg => {
        if (msg.type === 'bot' && msg.quickActions) {
            displayMessage('bot', msg.content, msg.quickActions);
        } else {
            displayMessage(msg.type, msg.content);
        }
    });
    
    // Scroll to bottom
    setTimeout(() => {
        window.scrollTo(0, document.body.scrollHeight);
    }, 100);
}

function displayMessage(type, content, quickActions = null) {
    if (!messagesSection) return;
    
    // Hide avatar section when first message is displayed
    if (avatarSection && chatHistory.length === 0) {
        avatarSection.style.display = 'none';
    }
    
    const messageGroup = document.createElement('div');
    messageGroup.className = `message-group ${type}-message`;
    
    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.textContent = type === 'user' ? '👤' : '🤖';
    
    const messageContent = document.createElement('div');
    messageContent.className = 'message-content';
    
    const messageText = document.createElement('div');
    messageText.className = 'message-text';
    messageText.innerHTML = content;
    
    messageContent.appendChild(messageText);
    
    // Add quick actions if provided
    if (quickActions && Array.isArray(quickActions) && quickActions.length > 0) {
        const actionsDiv = document.createElement('div');
        actionsDiv.className = 'quick-actions';
        
        quickActions.forEach(action => {
            const btn = document.createElement('button');
            btn.className = 'action-btn';
            btn.textContent = action.text || action;
            btn.addEventListener('click', () => {
                const messageToSend = action.message || action.text || action;
                if (chatInput) {
                    chatInput.value = messageToSend;
                    sendMessage();
                }
            });
            actionsDiv.appendChild(btn);
        });
        
        messageContent.appendChild(actionsDiv);
    }
    
    messageGroup.appendChild(avatar);
    messageGroup.appendChild(messageContent);
    messagesSection.appendChild(messageGroup);
    
    // Save to history
    const historyItem = {
        type: type,
        content: content,
        timestamp: new Date().toISOString()
    };
    if (quickActions) {
        historyItem.quickActions = quickActions;
    }
    chatHistory.push(historyItem);
    saveToSessionStorage();
    
    // Scroll to bottom
    setTimeout(() => {
        window.scrollTo(0, document.body.scrollHeight);
    }, 100);
}

function showTyping(show) {
    if (!messagesSection) return;
    
    let typingIndicator = document.getElementById('typing-indicator');
    
    if (show) {
        if (!typingIndicator) {
            typingIndicator = document.createElement('div');
            typingIndicator.id = 'typing-indicator';
            typingIndicator.className = 'typing-indicator';
            
            const avatar = document.createElement('div');
            avatar.className = 'message-avatar';
            avatar.textContent = '🤖';
            
            const messageContent = document.createElement('div');
            messageContent.className = 'message-content';
            
            const typingDots = document.createElement('div');
            typingDots.className = 'typing-dots';
            for (let i = 0; i < 3; i++) {
                const span = document.createElement('span');
                typingDots.appendChild(span);
            }
            
            messageContent.appendChild(typingDots);
            typingIndicator.appendChild(avatar);
            typingIndicator.appendChild(messageContent);
            messagesSection.appendChild(typingIndicator);
        }
        typingIndicator.style.display = 'flex';
    } else {
        if (typingIndicator) {
            typingIndicator.style.display = 'none';
        }
    }
    
    // Scroll to bottom when showing typing
    if (show) {
        setTimeout(() => {
            window.scrollTo(0, document.body.scrollHeight);
        }, 100);
    }
}

async function sendMessage() {
    if (!chatInput) return;
    
    const message = chatInput.value.trim();
    if (!message) return;
    
    // Clear input
    chatInput.value = '';
    
    // Display user message
    displayMessage('user', message);
    
    // Show typing indicator
    showTyping(true);
    
    // Send to API
    await sendMessageToApi(message);
}

// ===== API INTEGRATION =====
async function sendMessageToApi(message, retryCount = 0) {
    if (!sendBtn) return;
    
    sendBtn.disabled = true;
    
    const requestUrl = `${API_BASE_URL}${API_ENDPOINT}`;
    const requestBody = {
        message: message,
        userId: USER_ID,
        sessionId: SESSION_ID
    };
    
    console.log('Sending message to API:', {
        url: requestUrl,
        method: 'POST',
        body: requestBody
    });
    
    try {
        const response = await fetch(requestUrl, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(requestBody)
        });
        
        console.log('API Response:', {
            status: response.status,
            statusText: response.statusText,
            ok: response.ok
        });
        
        if (!response.ok) {
            let errorMessage = `Lỗi HTTP! Trạng thái: ${response.status}`;
            
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
        
        // Parse bot message
        let botMessage = data.message || 'Xin lỗi, tôi chưa hiểu ý bạn.';
        let redirectUrl = null;
        let doReload = false;
        
        // Check for REDIRECT signal
        if (botMessage.includes("__REDIRECT:")) {
            const match = botMessage.match(/__REDIRECT:([^]*)__/);
            if (match && match[1]) {
                redirectUrl = match[1];
                botMessage = botMessage.replace(match[0], "").trim();
            }
        } else if (botMessage.includes("__RELOAD__")) {
            doReload = true;
            botMessage = botMessage.replace("__RELOAD__", "").trim();
        }
        
        // Display bot message
        displayMessage('bot', botMessage);
        
        // Handle redirect or reload
        if (redirectUrl) {
            displayMessage('bot', '🤖 Chuyển hướng trong 1.5 giây...');
            setTimeout(() => {
                window.location.href = API_BASE_URL + redirectUrl;
            }, 1500);
        } else if (doReload) {
            setTimeout(() => {
                location.reload();
            }, 1500);
        }
        
        sendBtn.disabled = false;
        
    } catch (error) {
        console.error('Error sending message:', error);
        showTyping(false);
        sendBtn.disabled = false;
        
        displayMessage('bot', `⚠️ ${error.message || 'Có lỗi xảy ra khi gửi tin nhắn. Vui lòng thử lại.'}`);
        
        // Retry logic (optional)
        if (retryCount < 2) {
            console.log(`Retrying... (${retryCount + 1}/2)`);
            setTimeout(() => {
                sendMessageToApi(message, retryCount + 1);
            }, 1000);
        }
    }
}

// ===== HEALTH CHECK =====
async function checkApiHealth() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/ai/chat/enhanced/health`, {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        });
        
        if (response.ok) {
            console.log('API Health: OK');
        } else {
            console.log('API Health: Error', response.status);
        }
    } catch (error) {
        console.warn('API Health check failed:', error);
    }
}

function showConnectionError(message) {
    if (messagesSection) {
        displayMessage('bot', `⚠️ ${message}`);
    }
}

// ===== INITIALIZE ON LOAD =====
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initChatWeb);
} else {
    initChatWeb();
}

// Fallback: Try to initialize again after a delay if buttons weren't found
setTimeout(() => {
    const refreshBtn = document.getElementById('refresh-btn');
    const minimizeBtn = document.getElementById('minimize-btn');
    const closeBtn = document.getElementById('close-btn');
    
    if (refreshBtn && !refreshBtn.hasAttribute('data-listener-attached')) {
        console.log('🔄 Late initialization: Attaching refresh button listener...');
        refreshBtn.setAttribute('data-listener-attached', 'true');
        refreshBtn.addEventListener('click', function(e) {
            console.log('🔄 Refresh button clicked (late init)');
            e.stopPropagation();
            e.preventDefault();
            handleRefresh();
        });
    }
    
    if (minimizeBtn && !minimizeBtn.hasAttribute('data-listener-attached')) {
        console.log('🔄 Late initialization: Attaching minimize button listener...');
        minimizeBtn.setAttribute('data-listener-attached', 'true');
        minimizeBtn.addEventListener('click', function(e) {
            console.log('⤡ Minimize button clicked (late init)');
            e.stopPropagation();
            e.preventDefault();
            handleMinimize();
        });
    }
    
    if (closeBtn && !closeBtn.hasAttribute('data-listener-attached')) {
        console.log('🔄 Late initialization: Attaching close button listener...');
        closeBtn.setAttribute('data-listener-attached', 'true');
        closeBtn.addEventListener('click', function(e) {
            console.log('⇲ Close button clicked (late init)');
            e.stopPropagation();
            e.preventDefault();
            handleClose();
        });
    }
}, 500);

