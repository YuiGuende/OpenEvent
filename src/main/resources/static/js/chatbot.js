
    // ===== CONFIGURATION =====
    // These will be set dynamically from the page
    let API_BASE_URL = "";
    let USER_ID = null;
    const API_ENDPOINT = "/api/ai/chat/enhanced";
    const WELCOME_FLAG = "oe_welcome_shown";

    // Long validator (string of digits, > 0)
    function isValidUserId(id) {
    if (id == null) return false;
    const s = String(id);
    if (!/^\d+$/.test(s)) return false;
    try { return BigInt(s) > 0n; } catch { return false; }
}

    // ===== BOOT GUARD (PATCH) =====
    let __chatbotBooting = false;
    let __chatbotReady = false;

    // ===== GLOBAL VARIABLES =====
    let chatbotToggler = null;
    let closeBtn = null;
    let chatInput = null;
    let sendBtn = null;
    let chatMessages = null;
    let typingIndicator = null;
    let sessionPopupInitialized = false;
    let __sending = false;

    // ===== EARLY EXPORT FOR INLINE SCRIPTS =====
    if (typeof window !== "undefined" && !window.sendMessage) {
    window.__actualSendMessage = null;
    window.__sendMessagePlaceholderActive = true;

    window.sendMessage = function () {
    if (!window.__sendMessagePlaceholderActive) {
    // Placeholder đã bị thay thế, gọi hàm thực trực tiếp
    if (window.__actualSendMessage && typeof window.__actualSendMessage === "function") {
    try {
    return window.__actualSendMessage.apply(this, arguments);
} catch (error) {
    console.error("❌ Error calling actual sendMessage:", error);
    throw error;
}
}
    return;
}
    if (window.__actualSendMessage && typeof window.__actualSendMessage === "function") {
    console.log("📤 Calling actual sendMessage function from placeholder");
    try {
    return window.__actualSendMessage.apply(this, arguments);
} catch (error) {
    console.error("❌ Error calling actual sendMessage:", error);
    throw error;
}
} else {
    console.warn("⚠️ sendMessage function not ready yet. Retrying in 200ms...");
    const self = this;
    const args = Array.from(arguments);
    setTimeout(function () {
    if (window.__actualSendMessage && typeof window.__actualSendMessage === "function") {
    console.log("📤 Calling actual sendMessage function (after retry)");
    try {
    return window.__actualSendMessage.apply(self, args);
} catch (error) {
    console.error("❌ Error calling sendMessage after retry:", error);
}
} else {
    console.error("❌ sendMessage function still not ready after retry. Please wait for chatbot.js to load completely.");
}
}, 200);
}
};

    window.__setSendMessage = function (fn) {
    if (!fn || typeof fn !== "function") {
    console.error("❌ Invalid function passed to __setSendMessage");
    return;
}
    console.log("🔄 Updating sendMessage from placeholder to actual function");
    window.__actualSendMessage = fn;
    window.__sendMessagePlaceholderActive = false;
    window.sendMessage = fn;
    console.log("✅ sendMessage function updated from placeholder to actual");
};

    console.log("✅ sendMessage placeholder exported to window (early)");
}

    // ===== CHAT HISTORY MANAGEMENT =====
    const chatHistory = new Map();

    function saveChatHistory(sessionId) {
    if (!chatMessages || !sessionId) return;

    const items = Array.from(chatMessages.querySelectorAll(".message"));
    const messages = items.map((msg) => {
    const type = msg.classList.contains("user-message") ? "user" : "bot";
    const bubble = msg.querySelector(".message-bubble, .bot-bubble, .user-bubble, .message-content");
    const text = bubble ? bubble.textContent.trim() : msg.textContent.trim();
    return { type, content: text, timestamp: new Date().toISOString() };
});

    chatHistory.set(sessionId, messages);
    console.log(`Saved chat history for session ${sessionId}:`, messages);
}

    function restoreChatHistory(sessionId) {
    if (!chatMessages) return;

    const history = chatHistory.get(sessionId);

    if (!history || history.length === 0) {
    if (sessionStorage.getItem(WELCOME_FLAG) === "1") {
    hideWelcome();
} else {
    showWelcome();
}
    return;
}

    hideWelcome();
    chatMessages.innerHTML = "";
    history.forEach((msg) => {
    displayMessage(msg.type, msg.content);
});

    console.log(`Restored chat history for session ${sessionId}:`, history);
}

    // ===== INITIALIZATION =====
    async function initChatbot() {
    if (__chatbotReady || __chatbotBooting) return;
    __chatbotBooting = true;

    try {
    console.log("🚀 Starting chatbot initialization...");

    const contextPathMeta = document.querySelector('meta[name="context-path"]');

    let contextPath = contextPathMeta ? contextPathMeta.content : "";

    if (!contextPath || contextPath === "/") {
    const pathname = window.location.pathname;
    const pathParts = pathname.split("/").filter((p) => p);
    if (pathParts.length > 0) {
    contextPath =
    pathParts[0] !== "events" && pathParts[0] !== "index" && pathParts[0] !== ""
    ? "/" + pathParts[0]
    : "";
} else {
    contextPath = "";
}
}

    if (contextPath.endsWith("/")) {
    contextPath = contextPath.slice(0, -1);
}

    API_BASE_URL = contextPath || "";
    USER_ID = await resolveUserId();
    if (!isValidUserId(USER_ID)) {
    // chưa đăng nhập → set trạng thái yêu cầu đăng nhập (không gửi API)
    updateConnectionStatus("auth");
}

    console.log("📋 Chatbot config:", {
    API_BASE_URL: API_BASE_URL || "(root)",
    USER_ID: USER_ID,
    contextPathMeta: contextPathMeta ? contextPathMeta.content : "not found",
    fullApiUrl: `${API_BASE_URL}${API_ENDPOINT}`,
});

    await loadChatbotHTML();

    console.log("⏳ Waiting for fragment to render...");
    await new Promise((resolve) => setTimeout(resolve, 300));

    if (sessionStorage.getItem(WELCOME_FLAG) === "1") {
    hideWelcome();
}

    const toggleBtn = document.getElementById("chatbot-toggle-btn");
    const chatbotContainer = document.querySelector(".chatbot-container");

    console.log("🔍 Element check:", {
    toggleBtn: !!toggleBtn,
    chatbotContainer: !!chatbotContainer,
    toggleBtnId: toggleBtn ? toggleBtn.id : "not found",
    containerClass: chatbotContainer ? chatbotContainer.className : "not found",
});

    console.log("🔧 Initializing toggle...");
    initializeChatbotToggle();

    let retryCount = 0;
    const maxRetries = 5;
    while (!toggleInitialized && retryCount < maxRetries) {
    await new Promise((resolve) => setTimeout(resolve, 500));
    console.log(`🔄 Retrying toggle initialization (${retryCount + 1}/${maxRetries})...`);
    initializeChatbotToggle();
    retryCount++;
}

    if (toggleInitialized) {
    console.log("✅ Toggle initialized successfully");
} else {
    console.error("❌ Failed to initialize toggle after retries");
}

    initializeChatbot();

    setTimeout(() => {
    checkAndRestoreFromChatWeb();
}, 500);

    adjustChatbotPosition();
    window.addEventListener("resize", adjustChatbotPosition);

    checkApiHealth();

    // Attach outside click once (PATCH)
    attachOutsideClickOnce();

    console.log("✅ Chatbot initialization complete!");
    __chatbotReady = true;
} catch (error) {
    console.error("❌ Error initializing chatbot:", error);
    console.error("Error stack:", error.stack);
    showConnectionError("Không thể khởi tạo chatbot. Vui lòng tải lại trang.");
} finally {
    __chatbotBooting = false;
}
}

    if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initChatbot);
} else {
    initChatbot();
}
    setTimeout(() => {
    if (!toggleInitialized) {
    console.log("🔄 Late initialization attempt...");
    initChatbot();
}
}, 1000);

    // ===== HEALTH CHECK =====
    async function checkApiHealth() {
    try {
    // dùng đúng API_ENDPOINT
    const url = `${API_BASE_URL}${API_ENDPOINT}/health`;
    const response = await fetch(url, {
    method: "GET",
    headers: { "Content-Type": "application/json" },
});

    if (response.ok) {
    console.log("API Health: OK");
    updateConnectionStatus("online");
} else if (response.status === 401 || response.status === 403) {
    console.log("API Health: Authentication required");
    updateConnectionStatus("auth");
} else {
    console.log("API Health: Error", response.status);
    updateConnectionStatus("offline");
}
} catch (error) {
    console.warn("API Health check failed:", error);
    updateConnectionStatus("offline");
}
}

    function updateConnectionStatus(status) {
    const statusDot = document.querySelector(".status-dot");
    const statusText = document.querySelector(".status-text");

    if (statusDot && statusText) {
    if (status === "online") {
    statusDot.style.background = "var(--success)";
    statusText.textContent = "Online";
} else if (status === "auth") {
    statusDot.style.background = "var(--error)";
    statusText.textContent = "Đăng nhập";
} else {
    statusDot.style.background = "var(--error)";
    statusText.textContent = "Offline";
}
}
}

    async function resolveUserId() {
    // 1) data-user-id từ body (Thymeleaf render)
    const fromBody = document.body?.getAttribute("data-user-id");
    console.log("🔍 Resolving USER_ID - fromBody:", fromBody);
    if (fromBody && /^\d+$/.test(fromBody)) {
    console.log("✅ Found USER_ID from body:", fromBody);
    // Cache vào localStorage để lần sau không cần đọc lại
    localStorage.setItem("oe_user_id", fromBody);
    return fromBody;
}

    // 2) <meta name="user-id" content="...">
    const metaUser = document.querySelector('meta[name="user-id"]')?.content;
    console.log("🔍 Resolving USER_ID - metaUser:", metaUser);
    if (metaUser && /^\d+$/.test(metaUser)) {
    console.log("✅ Found USER_ID from meta:", metaUser);
    localStorage.setItem("oe_user_id", metaUser);
    return metaUser;
}

    // 3) window.__CURRENT_USER_ID (nếu header script đã set - ưu tiên cao nhất)
    if (typeof window.__CURRENT_USER_ID !== "undefined") {
    const v = String(window.__CURRENT_USER_ID);
    console.log("🔍 Resolving USER_ID - window.__CURRENT_USER_ID:", v);
    if (/^\d+$/.test(v)) {
    console.log("✅ Found USER_ID from window.__CURRENT_USER_ID:", v);
    localStorage.setItem("oe_user_id", v);
    if (document.body) {
    document.body.setAttribute("data-user-id", v);
}
    return v;
}
}

    // 3.5) window.__USER_ID (nếu app set global khi login)
    if (typeof window.__USER_ID !== "undefined") {
    const v = String(window.__USER_ID);
    console.log("🔍 Resolving USER_ID - window.__USER_ID:", v);
    if (/^\d+$/.test(v)) {
    console.log("✅ Found USER_ID from window.__USER_ID:", v);
    localStorage.setItem("oe_user_id", v);
    return v;
}
}

    // 3.6) window.__CURRENT_USER_ACCOUNT_ID (nếu header script đã set - fallback)
    if (typeof window.__CURRENT_USER_ACCOUNT_ID !== "undefined") {
    const v = String(window.__CURRENT_USER_ACCOUNT_ID);
    console.log("🔍 Resolving USER_ID - window.__CURRENT_USER_ACCOUNT_ID:", v);
    if (/^\d+$/.test(v)) {
    console.log("✅ Found USER_ID from window.__CURRENT_USER_ACCOUNT_ID:", v);
    localStorage.setItem("oe_user_id", v);
    if (document.body) {
    document.body.setAttribute("data-user-id", v);
}
    return v;
}
}

    // 4) localStorage (nếu bạn lưu sau login)
    const ls = localStorage.getItem("oe_user_id");
    console.log("🔍 Resolving USER_ID - localStorage:", ls);
    if (ls && /^\d+$/.test(ls)) {
    console.log("✅ Found USER_ID from localStorage:", ls);
    return ls;
}

    // 5) Thử tìm trong các element khác có thể chứa user ID
    const userProfileBtn = document.querySelector('[data-user-id]');
    if (userProfileBtn) {
    const uid = userProfileBtn.getAttribute("data-user-id");
    console.log("🔍 Resolving USER_ID - from user profile button:", uid);
    if (uid && /^\d+$/.test(uid)) {
    console.log("✅ Found USER_ID from user profile button:", uid);
    localStorage.setItem("oe_user_id", uid);
    return uid;
}
}

    // 6) Gọi API /api/current-user để lấy user ID (fallback)
    console.log("🔍 Resolving USER_ID - calling /api/current-user API...");
    console.log("🔍 API_BASE_URL:", API_BASE_URL);
    try {
    // Đảm bảo API_BASE_URL được set, nếu chưa thì dùng giá trị mặc định
    let baseUrl = API_BASE_URL;
    if (!baseUrl || baseUrl === "") {
    // Thử lấy từ context path meta tag
    const contextPathMeta = document.querySelector('meta[name="context-path"]');
    if (contextPathMeta && contextPathMeta.content) {
    baseUrl = contextPathMeta.content;
    } else {
    baseUrl = "";
    }
}
    const apiUrl = baseUrl && baseUrl !== "/" ? `${baseUrl}/api/current-user` : "/api/current-user";
    console.log("🔍 Calling API URL:", apiUrl);
    const response = await fetch(apiUrl, {
    method: "GET",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
});
    console.log("🔍 API Response status:", response.status, response.statusText);
    if (response.ok) {
    const data = await response.json();
    console.log("🔍 API Response data:", data);
    if (data && data.authenticated) {
    // PATCH: Ưu tiên dùng userId (nếu có), nếu không thì dùng accountId
    const userIdToUse = data.userId || data.accountId;
    if (userIdToUse) {
    const userIdStr = String(userIdToUse);
    console.log("🔍 Extracted userId:", userIdStr, "Type:", typeof userIdStr, "(userId:", data.userId, ", accountId:", data.accountId, ")");
    if (/^\d+$/.test(userIdStr)) {
    console.log("✅ Found USER_ID from /api/current-user API:", userIdStr);
    // Cache vào localStorage và set vào body attribute
    localStorage.setItem("oe_user_id", userIdStr);
    if (document.body) {
    document.body.setAttribute("data-user-id", userIdStr);
}
    // Lưu vào window object
    window.__CURRENT_USER_ID = userIdStr;
    window.__CURRENT_USER_ACCOUNT_ID = userIdStr;
    return userIdStr;
} else {
    console.warn("⚠️ userId không phải là số:", userIdStr);
}
} else {
    console.warn("⚠️ API response không có userId hoặc accountId:", data);
}
} else {
    console.warn("⚠️ API response không có authenticated:", data);
}
} else {
    console.warn("⚠️ API response không OK, status:", response.status);
    // Thử đọc error message (chỉ đọc một lần)
    try {
    const errorText = await response.text();
    console.warn("⚠️ API error response:", errorText);
    // Thử parse JSON nếu có thể
    try {
    const errorData = JSON.parse(errorText);
    console.warn("⚠️ API error data (parsed):", errorData);
} catch (parseError) {
    // Không phải JSON, bỏ qua
}
} catch (e) {
    console.warn("⚠️ Không thể đọc error response:", e);
}
}
} catch (error) {
    console.error("❌ Failed to fetch user ID from API:", error);
    console.error("❌ Error details:", error.message, error.stack);
}

    console.warn("⚠️ Could not resolve USER_ID from any source");
    return null; // không có user
}

    function showConnectionError(message) {
    if (chatMessages) {
    displayMessage("bot", `⚠️ ${message}`);
}
}

    // ===== CHATBOT TOGGLE FUNCTIONALITY =====
    let toggleInitialized = false;
    let isToggling = false;

    // Outside click attach/detach (PATCH)
    let outsideClickHandler = null;
    let outsideClickHandlerAttached = false;

    function attachOutsideClickOnce() {
    if (outsideClickHandlerAttached) return;
    outsideClickHandler = function (e) {
    try {
    const toggleBtn = document.getElementById("chatbot-toggle-btn");
    const chatbotContainer = document.querySelector(".chatbot-container");

    if (!isToggling && chatbotContainer && chatbotContainer.classList.contains("active")) {
    if (toggleBtn && !toggleBtn.contains(e.target) && !chatbotContainer.contains(e.target)) {
    chatbotContainer.classList.remove("active");
    chatbotContainer.style.display = "none";
    if (toggleBtn) {
    toggleBtn.style.background = "white";
    toggleBtn.style.color = "#ff9d6b";
    toggleBtn.title = "Mở chatbot";
}
    console.log("Chatbot closed by outside click");
}
}
} catch (error) {
    if (error.message && !error.message.includes("runtime.lastError")) {
    console.debug("Error in outside click handler:", error);
}
}
};
    document.addEventListener("click", outsideClickHandler, true);
    outsideClickHandlerAttached = true;
}
    function detachOutsideClickIfAny() {
    if (outsideClickHandlerAttached && outsideClickHandler) {
    document.removeEventListener("click", outsideClickHandler, true);
    outsideClickHandlerAttached = false;
    outsideClickHandler = null;
}
}

    function attachToggleListener() {
    const toggleBtn = document.getElementById("chatbot-toggle-btn");
    const chatbotContainer = document.querySelector(".chatbot-container");

    console.log("attachToggleListener called", {
    toggleBtn: !!toggleBtn,
    chatbotContainer: !!chatbotContainer,
    toggleBtnElement: toggleBtn,
    chatbotContainerElement: chatbotContainer,
});

    if (!toggleBtn || !chatbotContainer) {
    console.warn("Toggle button or container not found, retrying...", {
    toggleBtn: !!toggleBtn,
    chatbotContainer: !!chatbotContainer,
});
    setTimeout(attachToggleListener, 200);
    return;
}

    if (toggleBtn.hasAttribute("data-listener-attached")) {
    console.log("Listener already attached, skipping...");
    return;
}

    toggleBtn.setAttribute("data-listener-attached", "true");

    toggleBtn.addEventListener(
    "click",
    function (e) {
    try {
    console.log("=== TOGGLE BUTTON CLICKED ===", e);
    e.stopPropagation();
    e.preventDefault();

    const currentToggleBtn = document.getElementById("chatbot-toggle-btn");
    const currentContainer = document.querySelector(".chatbot-container");

    if (!currentContainer) {
    console.error("Chatbot container not found!");
    return;
}

    isToggling = true;

    currentContainer.classList.toggle("active");
    const hasActive = currentContainer.classList.contains("active");

    console.log("Toggle state:", {
    hasActive: hasActive,
    containerClasses: currentContainer.classList.toString(),
    computedDisplay: window.getComputedStyle(currentContainer).display,
    inlineDisplay: currentContainer.style.display,
});

    if (hasActive) {
    currentContainer.style.display = "flex";
    currentContainer.style.zIndex = "999";

    if (currentToggleBtn) {
    currentToggleBtn.style.background = "#ff9d6b";
    currentToggleBtn.style.color = "white";
    currentToggleBtn.title = "Đóng chatbot";
}

    console.log("✅ Chatbot OPENED");

    setTimeout(() => {
    const messageInput = document.querySelector(".message-input");
    if (messageInput) {
    messageInput.focus();
}
}, 100);
} else {
    currentContainer.style.display = "none";

    if (currentToggleBtn) {
    currentToggleBtn.style.background = "white";
    currentToggleBtn.style.color = "#ff9d6b";
    currentToggleBtn.title = "Mở chatbot";
}

    console.log("❌ Chatbot CLOSED");
}

    setTimeout(() => {
    isToggling = false;
}, 300);
} catch (error) {
    console.error("❌ Error in toggle button handler:", error);
    console.error("Error stack:", error.stack);

    try {
    const currentContainer = document.querySelector(".chatbot-container");
    if (currentContainer) {
    currentContainer.classList.add("active");
    currentContainer.style.display = "flex";
    console.log("Recovery: Forced chatbot to open");
}
} catch (recoveryError) {
    console.error("Failed to recover chatbot state:", recoveryError);
}
}
},
    true
    );

    console.log("✅ Toggle listener attached successfully to button");
}

    function initializeChatbotToggle() {
    if (toggleInitialized) {
    console.log("Chatbot toggle already initialized");
    return;
}

    attachToggleListener();

    const toggleBtn = document.getElementById("chatbot-toggle-btn");
    const chatbotContainer = document.querySelector(".chatbot-container");

    console.log("Checking chatbot elements:", {
    toggleBtn: toggleBtn,
    chatbotContainer: chatbotContainer,
    toggleBtnExists: !!toggleBtn,
    containerExists: !!chatbotContainer,
});

    if (toggleBtn && chatbotContainer) {
    console.log("Chatbot toggle initialized successfully");
    toggleInitialized = true;
} else {
    console.warn("Chatbot elements not found yet. Will retry...");
    setTimeout(() => {
    if (!toggleInitialized) {
    console.log("Retrying chatbot toggle initialization...");
    initializeChatbotToggle();
}
}, 500);
}
}

    // ===== LOAD CHATBOT HTML =====
    async function loadChatbotHTML() {
    console.log("Chatbot HTML already loaded via Thymeleaf fragment");
}

    // ===== FALLBACK CHATBOT =====
    function createFallbackChatbot() {
    const container = document.getElementById("openevent-chatbot-container");
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
                            <div style="font-size: 13px; color: #666; margin-bottom: 6px;">AI đang suy nghĩ...</div>
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
    console.log("🔧 Initializing chatbot functionality...");

    chatInput = document.querySelector(".message-input");
    sendBtn = document.querySelector(".send-btn, .send-button, #chatSendBtn");

    console.log("🔍 Elements found:", {
    chatInput: !!chatInput,
    sendBtn: !!sendBtn,
    chatInputElement: chatInput,
    sendBtnElement: sendBtn,
});

    const chatbotBody = document.querySelector(".chatbot-body");
    if (chatbotBody && !document.getElementById("chatMessages")) {
    const messagesContainer = document.createElement("div");
    messagesContainer.id = "chatMessages";
    messagesContainer.className = "messages-container";
    messagesContainer.style.cssText =
    "flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px;";

    const quickActions = chatbotBody.querySelector(".quick-actions");
    if (quickActions) {
    chatbotBody.insertBefore(messagesContainer, quickActions);
} else {
    chatbotBody.appendChild(messagesContainer);
}
}

    chatMessages = document.getElementById("chatMessages");

    if (chatMessages && !document.getElementById("typingIndicator")) {
    const typingIndicatorDiv = document.createElement("div");
    typingIndicatorDiv.id = "typingIndicator";
    typingIndicatorDiv.className = "typing-indicator";
    typingIndicatorDiv.style.cssText = "display: none; padding: 10px 20px;";
    typingIndicatorDiv.innerHTML = `
            <div class="message bot-message">
                <div class="message-avatar">🤖</div>
                <div class="message-bubble bot-bubble">
                    <div style="font-size: 13px; color: #666; margin-bottom: 6px;">AI đang suy nghĩ...</div>
                    <div class="typing-dots">
                        <span></span>
                        <span></span>
                        <span></span>
                    </div>
                </div>
            </div>
        `;
    chatMessages.appendChild(typingIndicatorDiv);
}

    typingIndicator = document.getElementById("typingIndicator");

    if (!chatInput || !sendBtn || !chatMessages) {
    console.warn("Some chatbot elements not found, some features may not work");
    if (!chatInput || !sendBtn) {
    return;
}
}

    // PATCH: Remove existing listener by cloning sendBtn to avoid duplicates
    if (sendBtn) {
    const cloned = sendBtn.cloneNode(true);
    sendBtn.parentNode.replaceChild(cloned, sendBtn);
    sendBtn = cloned;
    sendBtn.addEventListener("click", function (e) {
    e.preventDefault();
    e.stopPropagation();
    console.log("📤 Send button clicked (main)");
    sendMessage();
});
    console.log("✅ Send button listener attached (cloned)");
} else {
    console.warn("⚠️ Send button not found");
}

    if (chatInput) {
    const clonedInput = chatInput.cloneNode(true);
    chatInput.parentNode.replaceChild(clonedInput, chatInput);
    chatInput = clonedInput;

    chatInput.addEventListener("keydown", function (e) {
    if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    console.log("📤 Enter key pressed (main)");
    sendMessage();
}
});

    chatInput.addEventListener("input", function () {
    this.style.height = "auto";
    const newHeight = Math.min(this.scrollHeight, 120);
    this.style.height = newHeight + "px";
});

    chatInput.style.height = "auto";
    chatInput.style.height = Math.min(chatInput.scrollHeight, 120) + "px";

    console.log("✅ Chat input listeners attached");
} else {
    console.warn("⚠️ Chat input not found");
}

    const actionButtons = document.querySelectorAll(".action-btn");
    actionButtons.forEach((btn) => {
    btn.addEventListener("click", function () {
    const actionText = this.textContent.trim();
    let messageToSend = actionText;

    switch (actionText) {
    case "Tạo sự kiện":
    messageToSend = "Tôi muốn tạo sự kiện mới";
    break;
    case "Các sự kiện nổi bật gần đây":
    messageToSend = "Cho tôi xem các sự kiện nổi bật gần đây";
    break;
    case "Mua vé sự kiện":
    messageToSend = "Tôi muốn mua vé sự kiện";
    break;
    default:
    messageToSend = actionText;
}

    if (chatInput) {
    chatInput.value = messageToSend;
    sendMessage();
}
});
});

    const refreshBtn = document.querySelector('.header-btn[title="Làm mới"]');
    console.log("🔍 Refresh button found:", !!refreshBtn);
    if (refreshBtn) {
    const newRefreshBtn = refreshBtn.cloneNode(true);
    refreshBtn.parentNode.replaceChild(newRefreshBtn, refreshBtn);
    const freshRefreshBtn = document.querySelector('.header-btn[title="Làm mới"]');

    freshRefreshBtn.addEventListener("click", function (e) {
    console.log("🔄 Refresh button clicked");
    e.stopPropagation();
    e.preventDefault();
    if (confirm("Bạn có chắc muốn làm mới cuộc trò chuyện? Lịch sử chat hiện tại sẽ được xóa.")) {
    clearChat({ restoreWelcome: true });
    setTimeout(() => {
    createNewSession(null, false);
}, 100);
}
});
    console.log("✅ Refresh button handler attached");
} else {
    console.warn("⚠️ Refresh button not found");
}

    const maximizeBtn = document.querySelector('.header-btn[title="Phóng to"]');
    console.log("🔍 Maximize button found:", !!maximizeBtn);
    if (maximizeBtn) {
    const newMaximizeBtn = maximizeBtn.cloneNode(true);
    maximizeBtn.parentNode.replaceChild(newMaximizeBtn, maximizeBtn);
    const freshMaximizeBtn = document.querySelector('.header-btn[title="Phóng to"]');

    freshMaximizeBtn.addEventListener("click", function (e) {
    console.log("⛶ Maximize button clicked - toggling fullscreen");
    e.stopPropagation();
    e.preventDefault();
    toggleMaximize();
});
    console.log("✅ Maximize button handler attached");
} else {
    console.warn("⚠️ Maximize button not found");
}

    const closeHeaderBtn = document.querySelector('.header-btn[title="Đóng"]');
    console.log("🔍 Close button found:", !!closeHeaderBtn);
    if (closeHeaderBtn) {
    const newCloseBtn = closeHeaderBtn.cloneNode(true);
    closeHeaderBtn.parentNode.replaceChild(newCloseBtn, closeHeaderBtn);
    const freshCloseBtn = document.querySelector('.header-btn[title="Đóng"]');

    freshCloseBtn.addEventListener("click", function (e) {
    console.log("✕ Close button clicked");
    e.stopPropagation();
    e.preventDefault();
    closeChatbot();
});
    console.log("✅ Close button handler attached");
} else {
    console.warn("⚠️ Close button not found");
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
    const inputEl = document.querySelector(".message-input");
    const sendBtnEl = document.querySelector(".send-btn, .send-button, #chatSendBtn");
    if (!inputEl) {
    console.warn("⚠️ Chat input not found");
    return;
}

    const raw = inputEl.value;
    const message = raw ? raw.trim() : "";
    if (!message) {
    console.log("⚠️ Empty message, not sending");
    return;
}

    if (window.__sendingMsg) return;
    window.__sendingMsg = true;
    if (sendBtnEl) sendBtnEl.disabled = true;

    try {
    console.log("📤 Sending message:", message);

    let sessionId = getCurrentSessionId();
    const history = chatHistory.get(sessionId) || [];
    const isFirstUserMsg = history.findIndex((m) => m?.type === "user") === -1;

    if (isFirstUserMsg) {
    hideWelcome();
    sessionStorage.setItem(WELCOME_FLAG, "1");

    const newSessionId = `SESSION_${Date.now()}_${Math.random().toString(36).slice(2, 11)}`;
    sessionStorage.setItem("chatbot_session_id", newSessionId);
    sessionId = newSessionId;
    chatHistory.set(sessionId, []);
}

    // PATCH: cập nhật tiêu đề session theo tin nhắn đầu
    if (isFirstUserMsg) {
    updateSessionTitle(sessionId, message);
}

    displayMessage("user", message);
    inputEl.value = "";
    inputEl.style.height = "auto";
    inputEl.style.height = Math.min(inputEl.scrollHeight, 120) + "px";

    const cur = chatHistory.get(sessionId) || [];
    cur.push({ type: "user", content: message, timestamp: new Date().toISOString() });
    chatHistory.set(sessionId, cur);

    showTyping(true);

    await sendMessageToApi(message);
} finally {
    showTyping(false);
    if (sendBtnEl) sendBtnEl.disabled = false;
    window.__sendingMsg = false;
    const again = document.querySelector(".message-input");
    if (again) again.focus();
}
}

    // ===== API INTEGRATION =====
    async function sendMessageToApi(message, retryCount = 0) {
    // PATCH: lock local button reference
    const localSendBtn = document.querySelector(".send-btn, .send-button, #chatSendBtn");
    if (localSendBtn) localSendBtn.disabled = true;

    if (!message || typeof message !== "string" || message.trim().length === 0) {
    console.error("❌ Invalid message:", message);
    displayMessage("bot", "⚠️ Tin nhắn không hợp lệ. Vui lòng nhập nội dung.");
    if (localSendBtn) localSendBtn.disabled = false;
    showTyping(false);
    return;
}
    // PATCH: Resolve USER_ID mới nhất từ DOM trước khi kiểm tra (để xử lý trường hợp đăng nhập sau khi trang đã load)
    let currentUserId = null;
    try {
    console.log("🔍 Starting to resolve USER_ID...");
    currentUserId = await resolveUserId();
    console.log("🔍 Resolved USER_ID result:", currentUserId);
    // Cập nhật biến global USER_ID để lần sau không cần resolve lại
    USER_ID = currentUserId;
} catch (error) {
    console.error("❌ Error resolving USER_ID:", error);
    currentUserId = null;
}
    if (!isValidUserId(currentUserId)) {
    console.error("❌ Invalid USER_ID:", currentUserId, "- All sources checked, but no valid user ID found");
    displayMessage("bot", "🔒 Bạn chưa đăng nhập. Vui lòng đăng nhập để dùng chatbot.");

    const last = chatMessages?.lastElementChild?.querySelector(".bot-bubble");
    if (last && !last.querySelector(".login-button")) {
    const btn = document.createElement("button");
    btn.textContent = "Đăng nhập";
    btn.className = "login-button";
    btn.style.cssText = `
                background: var(--primary);
                color: white;
                border: none;
                padding: 8px 16px;
                border-radius: 8px;
                cursor: pointer;
                margin-top: 8px;
                font-size: 12px;
                `;
    btn.onclick = () => {
    window.location.href = `${API_BASE_URL}/login`;
};
    last.appendChild(btn);
}
    if (localSendBtn) localSendBtn.disabled = false;
    showTyping(false);
    return;
}

    const sessionId = getCurrentSessionId();
    if (!sessionId || sessionId.trim().length === 0) {
    console.error("❌ Invalid sessionId:", sessionId);
    displayMessage("bot", "⚠️ Lỗi phiên làm việc. Vui lòng tải lại trang.");
    if (localSendBtn) localSendBtn.disabled = false;
    showTyping(false);
    return;
}

    const requestUrl = `${API_BASE_URL}${API_ENDPOINT}`;
    const requestBody = {
    message: message.trim(),
    userId: String(currentUserId), // gửi dạng chuỗi để an toàn Long
    sessionId: sessionId,
};

    console.log("Sending message to API:", {
    url: requestUrl,
    method: "POST",
    body: requestBody,
    messageLength: message.trim().length,
    userIdType: typeof requestBody.userId,
    sessionIdLength: sessionId.length,
});

    const controller = new AbortController();
    const timeoutMs = 20000;
    const tId = setTimeout(() => controller.abort(new Error("TIMEOUT")), timeoutMs);

    try {
    const response = await fetch(requestUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(requestBody),
    signal: controller.signal,
});

    console.log("API Response:", {
    status: response.status,
    statusText: response.statusText,
    ok: response.ok,
});

    if (!response.ok) {
    let errorMessage = `Lỗi HTTP! Trạng thái: ${response.status}`;
    try {
    const errJson = await response.json();
    if (errJson?.message) errorMessage = errJson.message;
    else if (errJson?.error) errorMessage = errJson.error;
    console.log("Error response data:", errJson);
} catch { /* ignore parse error */ }

    switch (response.status) {
    case 400:
    if (errorMessage.startsWith("Lỗi HTTP")) errorMessage = "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại tin nhắn.";
    break;
    case 401:
    errorMessage = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
    break;
    case 403:
    errorMessage = "Không có quyền truy cập. Vui lòng đăng nhập để sử dụng chatbot.";
    break;
    case 429:
    errorMessage = "Bạn đã gửi quá nhiều tin nhắn. Vui lòng chờ một chút rồi thử lại.";
    break;
    case 500:
    errorMessage = "Lỗi máy chủ. Vui lòng thử lại sau.";
    break;
    case 503:
    errorMessage = "Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.";
    break;
}
    throw new Error(errorMessage);
}

    let data = null;
    try {
    data = await response.json();
} catch {
    data = {};
}

    let botMessage = data?.message || "Xin lỗi, tôi chưa hiểu ý bạn.";
    let redirectUrl = null;
    let doReload = false;

    if (typeof botMessage === "string" && botMessage.includes("__REDIRECT:")) {
    const m = botMessage.match(/__REDIRECT:([^]*?)__/);
    if (m && m[1]) {
    redirectUrl = m[1].trim();
    botMessage = botMessage.replace(m[0], "").trim();
}
}
    if (typeof botMessage === "string" && botMessage.includes("__RELOAD__")) {
    doReload = true;
    botMessage = botMessage.replace("__RELOAD__", "").trim();
}

    displayMessage("bot", botMessage);

    try {
    const history = chatHistory.get(sessionId) || [];
    history.push({
    type: "bot",
    content: botMessage,
    timestamp: new Date().toISOString(),
});
    chatHistory.set(sessionId, history);
    saveChatHistory(sessionId);
} catch (e) {
    console.warn("Không thể lưu bot reply vào chatHistory", e);
}

    if (redirectUrl) {
    displayMessage("bot", "🤖 Chuyển hướng trong 1.5 giây...");
    setTimeout(() => {
    window.location.href = API_BASE_URL + redirectUrl;
}, 1500);
} else if (doReload) {
    setTimeout(() => location.reload(), 1500);
}
} catch (error) {
    console.error("Lỗi khi gửi tin nhắn đến API:", error);
    const isAuthErr = /đăng nhập|quyền truy cập/i.test(error.message || "");
    const isTimeout = error?.name === "AbortError" || /TIMEOUT/i.test(error?.message || "");
    const isNetwork = /NetworkError|Failed to fetch|network/i.test(error?.message || "");

    // Retry lỗi mạng/timeout
    if (retryCount < 2 && !isAuthErr && (isTimeout || isNetwork)) {
    const attempt = retryCount + 1;
    displayMessage("bot", `Đang thử lại kết nối... (${attempt}/2)`);
    setTimeout(() => {
    sendMessageToApi(message, attempt);
}, 2000 * attempt);
} else {
    const finalMsg =
    error?.name === "AbortError"
    ? "⏳ Máy chủ phản hồi chậm. Vui lòng thử lại."
    : `❌ ${error.message || "Đã xảy ra lỗi không xác định."}`;
    displayMessage("bot", finalMsg);

    const lastMessage = chatMessages?.lastElementChild;
    const bubble = lastMessage?.querySelector(".bot-bubble");
    if (bubble && !bubble.querySelector(".retry-button")) {
    const retryButton = document.createElement("button");
    retryButton.textContent = "🔄 Thử lại";
    retryButton.className = "retry-button";
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
    bubble.appendChild(retryButton);
}
}
} finally {
    clearTimeout(tId);
    showTyping(false);
    if (localSendBtn) localSendBtn.disabled = false;
}
}

    // ===== SESSION MANAGEMENT =====
    function getCurrentSessionId() {
    let sessionId = sessionStorage.getItem("chatbot_session_id");
    if (!sessionId) {
    sessionId = "SESSION_" + Date.now() + "_" + Math.random().toString(36).substr(2, 9);
    sessionStorage.setItem("chatbot_session_id", sessionId);
}
    return sessionId;
}

    // ===== RESTORE FROM CHATWEB =====
    function checkAndRestoreFromChatWeb() {
    try {
    const returningFromChatWeb = sessionStorage.getItem("returning_from_chatweb");
    if (returningFromChatWeb !== "true") return;

    console.log("🔄 Detected return from chatweb, restoring chat history...");

    if (!chatMessages) {
    chatMessages = document.getElementById("chatMessages");
    if (!chatMessages) {
    console.warn("⚠️ Chat messages container not found, retrying...");
    setTimeout(checkAndRestoreFromChatWeb, 200);
    return;
}
}

    const savedSessionId = sessionStorage.getItem("chatweb_session_id");
    const savedHistory = sessionStorage.getItem("chatweb_history");
    if (!savedSessionId || !savedHistory) {
    console.warn("⚠️ No chat history found from chatweb");
    sessionStorage.removeItem("returning_from_chatweb");
    return;
}

    let historyData;
    try {
    historyData = JSON.parse(savedHistory);
} catch (e) {
    console.error("Failed to parse chat history:", e);
    sessionStorage.removeItem("returning_from_chatweb");
    return;
}

    let restored = Array.isArray(historyData) ? historyData : historyData[savedSessionId] || [];
    if (!Array.isArray(restored)) restored = [];

    chatHistory.set(savedSessionId, restored);
    sessionStorage.setItem("chatbot_session_id", savedSessionId);

    const hasUserMsg =
    restored.some((m) => m && m.type === "user" && m.content && String(m.content).trim().length);
    if (hasUserMsg) {
    sessionStorage.setItem(WELCOME_FLAG, "1");
}

    if (restored.length === 0) {
    showWelcome();
} else {
    hideWelcome();
    chatMessages.innerHTML = "";
    restored.forEach((msg) => {
    const t = msg && (msg.type === "user" || msg.type === "bot") ? msg.type : "bot";
    const c = msg && msg.content ? String(msg.content) : "";
    displayMessage(t, c);
});
}

    console.log("✅ Restored chat history:", restored.length, "messages");

    setTimeout(() => {
    const chatbotContainer = document.querySelector(".chatbot-container");
    const toggleBtn = document.getElementById("chatbot-toggle-btn");
    if (chatbotContainer) {
    chatbotContainer.classList.add("active");
    chatbotContainer.style.display = "flex";
    chatbotContainer.style.zIndex = "999";
}
    if (toggleBtn) {
    toggleBtn.style.background = "#ff9d6b";
    toggleBtn.style.color = "white";
    toggleBtn.title = "Đóng chatbot";
}
    const input = document.querySelector(".message-input");
    if (input) input.focus();
    if (typeof scrollToBottom === "function") scrollToBottom();
}, 300);
} catch (error) {
    console.error("Error restoring from chatweb:", error);
} finally {
    sessionStorage.removeItem("returning_from_chatweb");
}
}

    // ===== UI FUNCTIONS =====
    function clearChat({ restoreWelcome = false } = {}) {
    if (!chatMessages) return;

    chatMessages.querySelectorAll(".message").forEach((msg) => msg.remove());

    const typingIndicator = document.getElementById("typingIndicator");
    if (typingIndicator) typingIndicator.remove();

    if (restoreWelcome) {
    sessionStorage.removeItem(WELCOME_FLAG);
    showWelcome();
} else {
    hideWelcome();
}

    console.log("✅ Chat cleared");
}

    function formatMessage(message) {
    // PATCH: stronger XSS-safe + linkify
    let safe = String(message).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

    safe = safe.replace(/\bhttps?:\/\/[^\s<]+/g, (url) => `<a href="${url}" target="_blank" rel="noopener noreferrer">${url}</a>`);

    safe = safe.replace(/\n/g, "<br>").replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>").replace(/\*(.*?)\*/g, "<em>$1</em>");

    return safe;
}

    function displayMessage(sender, message) {
    const chatbotContainer = document.querySelector(".chatbot-container");
    const isFullscreen = chatbotContainer && chatbotContainer.classList.contains("fullscreen");

    // PATCH: Luôn thêm tin nhắn vào cả popup và fullscreen để đồng bộ
    let targetContainer = chatMessages;
    let fullscreenContainer = null;
    
    if (isFullscreen) {
    fullscreenContainer = document.getElementById("messages-section-fullscreen");
    if (fullscreenContainer) {
    targetContainer = fullscreenContainer;
}
}

    if (!targetContainer) {
    const chatbotBody = document.querySelector(".chatbot-body");
    if (chatbotBody) {
    const messagesContainer = document.createElement("div");
    messagesContainer.id = "chatMessages";
    messagesContainer.className = "messages-container";
    messagesContainer.style.cssText =
    "flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px;";

    if (isFullscreen) {
    const messagesSectionFullscreen = document.getElementById("messages-section-fullscreen");
    if (messagesSectionFullscreen) {
    messagesSectionFullscreen.appendChild(messagesContainer);
} else {
    chatbotBody.appendChild(messagesContainer);
}
} else {
    const quickActions = chatbotBody.querySelector(".quick-actions");
    if (quickActions) {
    chatbotBody.insertBefore(messagesContainer, quickActions);
} else {
    chatbotBody.appendChild(messagesContainer);
}
}
    targetContainer = messagesContainer;
    chatMessages = messagesContainer;
} else {
    console.error("Chatbot body not found");
    return;
}
}

    if (sender === "user") {
    const subtitle = document.querySelector(".chatbot-body > .subtitle");
    const welcomeBubble = document.querySelector(".chatbot-body > .message-bubble");
    const quickActions = document.querySelector(".chatbot-body > .quick-actions");

    if (subtitle) {
    subtitle.style.display = "none";
}
    if (
    welcomeBubble &&
    welcomeBubble.closest(".chatbot-body") &&
    !welcomeBubble.closest(".messages-container") &&
    !welcomeBubble.closest("#messages-section-fullscreen")
    ) {
    welcomeBubble.style.display = "none";
}
    if (quickActions) {
    quickActions.style.display = "none";
}
}

    const messageDiv = document.createElement("div");
    messageDiv.className = `message ${sender}-message`;
    messageDiv.style.cssText =
    sender === "bot"
    ? "display: flex; gap: 0; align-items: flex-start; margin-bottom: 16px; position: relative; padding-left: 0;"
    : "display: flex; gap: 0; align-items: flex-start; margin-bottom: 16px;";

    let avatarDiv = null;
    if (sender === "bot") {
    avatarDiv = document.createElement("div");
    avatarDiv.className = "message-avatar";
    avatarDiv.style.cssText =
    "position: absolute; top: 0; left: 0; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; flex-shrink: 0; background: linear-gradient(135deg, #ff9d6b 0%, #ffb48c 100%); color: white; z-index: 10; transform: translate(-8px, -8px); box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);";
    avatarDiv.textContent = "🤖";
}

    const bubbleDiv = document.createElement("div");
    bubbleDiv.className = `message-bubble ${sender}-bubble`;
    bubbleDiv.style.cssText =
    sender === "bot"
    ? "background: white; padding: 12px 16px; border-radius: 12px; max-width: 80%; box-shadow: 0 2px 4px rgba(0,0,0,0.1); position: relative; margin-left: 0;"
    : "background: #ff9d6b; color: white; padding: 12px 16px; border-radius: 12px; max-width: 80%; margin-left: auto;";

    const contentDiv = document.createElement("div");
    contentDiv.className = "message-content";
    contentDiv.style.cssText = "font-size: 14px; line-height: 1.5; word-wrap: break-word;";
    contentDiv.innerHTML = formatMessage(message);

    bubbleDiv.appendChild(contentDiv);

    if (sender === "bot" && avatarDiv) {
    messageDiv.appendChild(avatarDiv);
    messageDiv.appendChild(bubbleDiv);
} else {
    messageDiv.appendChild(bubbleDiv);
}

    // PATCH: Thêm tin nhắn vào cả popup và fullscreen để đồng bộ
    targetContainer.appendChild(messageDiv);
    
    // Nếu đang ở fullscreen mode, cũng thêm vào popup messages để đồng bộ
    if (isFullscreen && fullscreenContainer && chatMessages && targetContainer === fullscreenContainer) {
    const clonedMsg = messageDiv.cloneNode(true);
    chatMessages.appendChild(clonedMsg);
}
    
    // Nếu đang ở popup mode, cũng thêm vào fullscreen messages để đồng bộ (nếu fullscreen container tồn tại)
    if (!isFullscreen && chatMessages && targetContainer === chatMessages) {
    const fullscreenMsgs = document.getElementById("messages-section-fullscreen");
    if (fullscreenMsgs) {
    const clonedMsg = messageDiv.cloneNode(true);
    fullscreenMsgs.appendChild(clonedMsg);
}
}

    setTimeout(() => {
    if (targetContainer) {
    targetContainer.scrollTo({
    top: targetContainer.scrollHeight,
    behavior: "smooth",
});
}
    // Scroll trong fullscreen messages section nếu đang ở fullscreen
    if (isFullscreen && fullscreenContainer) {
    fullscreenContainer.scrollTo({
    top: fullscreenContainer.scrollHeight,
    behavior: "smooth",
});
}
    // Scroll trong chatbot body nếu đang ở fullscreen
    if (isFullscreen) {
    const chatbotBody = document.querySelector(".chatbot-body");
    if (chatbotBody) {
    chatbotBody.scrollTo({
    top: chatbotBody.scrollHeight,
    behavior: "smooth",
});
}
}
    // Scroll trong popup messages nếu đang ở popup mode
    if (!isFullscreen && chatMessages) {
    chatMessages.scrollTo({
    top: chatMessages.scrollHeight,
    behavior: "smooth",
});
}
}, 100);
}

    function hideWelcome() {
    const wb = document.getElementById("welcome-block");
    if (wb) {
    wb.style.display = "none";
    return;
}

    const subtitle = document.querySelector(".chatbot-body > .subtitle");
    const welcomeBubble = document.querySelector(".chatbot-body > .message-bubble");
    const quickActions = document.querySelector(".chatbot-body > .quick-actions");
    if (subtitle) subtitle.style.display = "none";
    if (welcomeBubble) welcomeBubble.style.display = "none";
    if (quickActions) quickActions.style.display = "none";
}
    function showWelcome() {
    if (sessionStorage.getItem(WELCOME_FLAG) === "1") {
    hideWelcome();
    return;
}

    const wb = document.getElementById("welcome-block");
    if (wb) {
    wb.style.display = "block";
    return;
}

    const subtitle = document.querySelector(".chatbot-body > .subtitle");
    const welcomeBubble = document.querySelector(".chatbot-body > .message-bubble");
    const quickActions = document.querySelector(".chatbot-body > .quick-actions");
    if (subtitle) subtitle.style.display = "block";
    if (welcomeBubble) welcomeBubble.style.display = "block";
    if (quickActions) quickActions.style.display = "grid";
}

    function showTyping(show) {
    if (!chatMessages) return;

    if (!typingIndicator) {
    typingIndicator = document.getElementById("typingIndicator");
    if (!typingIndicator) {
    const typingDiv = document.createElement("div");
    typingDiv.id = "typingIndicator";
    typingDiv.className = "typing-indicator";
    typingDiv.style.cssText = "display: flex; gap: 12px; align-items: flex-start; padding: 10px 0;";
    typingDiv.innerHTML = `
                <div style="width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0;">🤖</div>
                <div style="background: white; padding: 12px 16px; border-radius: 12px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); min-width: 120px;">
                    <div style="font-size: 13px; color: #666; margin-bottom: 6px;">AI đang suy nghĩ...</div>
                    <div class="typing-dots" style="display: flex; gap: 4px;">
                        <span style="width: 8px; height: 8px; background: #ccc; border-radius: 50%; animation: typing 1.4s infinite;"></span>
                        <span style="width: 8px; height: 8px; background: #ccc; border-radius: 50%; animation: typing 1.4s infinite 0.2s;"></span>
                        <span style="width: 8px; height: 8px; background: #ccc; border-radius: 50%; animation: typing 1.4s infinite 0.4s;"></span>
                    </div>
                </div>
            `;
    chatMessages.appendChild(typingDiv);
    typingIndicator = typingDiv;
}
}

    typingIndicator.style.display = show ? "flex" : "none";
    if (show) {
    requestAnimationFrame(() => {
    if (chatMessages) chatMessages.scrollTop = chatMessages.scrollHeight;
});
}
}

    // ===== UTILITY FUNCTIONS =====
    function isChatbotOpen() {
    const chatbotContainer = document.querySelector(".chatbot-container");
    return chatbotContainer && chatbotContainer.classList.contains("active");
}

    function openChatbot() {
    const chatbotContainer = document.querySelector(".chatbot-container");
    const toggleBtn = document.getElementById("chatbot-toggle-btn");

    if (chatbotContainer) {
    chatbotContainer.classList.add("active");
    if (toggleBtn) {
    toggleBtn.style.background = "#ff9d6b";
    toggleBtn.style.color = "white";
    toggleBtn.title = "Đóng chatbot";
}
    if (chatInput) {
    setTimeout(() => chatInput.focus(), 100);
}
}
}

    function closeChatbot() {
    const chatbotContainer = document.querySelector(".chatbot-container");
    const chatbotWrapper = document.querySelector(".chatbot-wrapper");
    const toggleBtn = document.getElementById("chatbot-toggle-btn");

    const headerActions = document.querySelector(".header-actions");
    let maximizeBtn = null;
    if (headerActions) {
    const buttons = headerActions.querySelectorAll(".header-btn");
    if (buttons.length > 1) {
    maximizeBtn = buttons[1];
}
}

    if (chatbotContainer && chatbotContainer.classList.contains("fullscreen")) {
    chatbotContainer.classList.remove("fullscreen");
    if (chatbotWrapper) {
    chatbotWrapper.classList.remove("fullscreen");
}
    document.body.classList.remove("chatbot-fullscreen");
    document.body.style.overflow = "";

    if (maximizeBtn) {
    maximizeBtn.textContent = "⛶";
    maximizeBtn.title = "Phóng to";
}
}

    if (chatbotContainer) {
    chatbotContainer.classList.remove("active");
    chatbotContainer.style.display = "none";
    if (toggleBtn) {
    toggleBtn.style.background = "white";
    toggleBtn.style.color = "#ff9d6b";
    toggleBtn.title = "Mở chatbot";
}
}
}

    // ===== SCROLL TO BOTTOM =====
    function scrollToBottom() {
    if (chatMessages) {
    chatMessages.scrollTop = chatMessages.scrollHeight;
}
}

    // ===== ADJUST CHATBOT POSITION =====
    function adjustChatbotPosition() {
    const container = document.querySelector(".chatbot-container");
    if (container) {
    console.log("Chatbot position is managed by CSS");
}
}

    // ===== SESSION POPUP FUNCTIONS =====
    function openSessionPopup() {
    const sessionPopup = document.getElementById("sessionPopup");
    if (sessionPopup) {
    sessionPopup.style.display = "flex";
    setTimeout(() => {
    sessionPopup.classList.add("show");
}, 10);

    if (!sessionPopupInitialized) {
    setupSessionPopupEvents();
    sessionPopupInitialized = true;
}
}
}

    function closeSessionPopup() {
    const sessionPopup = document.getElementById("sessionPopup");
    if (sessionPopup) {
    sessionPopup.classList.remove("show");
    setTimeout(() => {
    sessionPopup.style.display = "none";
}, 300);
}
}

    function setupSessionPopupEvents() {
    const sessionPopup = document.getElementById("sessionPopup");
    const sessionPopupClose = document.getElementById("sessionPopupClose");
    const newSessionBtn = document.getElementById("oeNewSessionBtn");

    if (sessionPopupClose) {
    sessionPopupClose.addEventListener("click", closeSessionPopup);
}

    document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && sessionPopup && sessionPopup.style.display !== "none") {
    closeSessionPopup();
}
});

    if (newSessionBtn) {
    newSessionBtn.addEventListener("click", () => {
    createNewSession();
    closeSessionPopup();
});
}

    const sessionList = document.querySelector(".session-list");
    if (sessionList) {
    const clonedList = sessionList.cloneNode(true);
    sessionList.parentNode.replaceChild(clonedList, sessionList);

    clonedList.addEventListener("click", (e) => {
    const clickedItem = e.target.closest(".session-item");
    if (!clickedItem) return;

    const deleteBtn = e.target.closest(".session-delete-btn");
    const actionBtn = e.target.closest(".session-action-btn");

    if (deleteBtn) {
    e.stopPropagation();
    deleteSession(clickedItem);
} else if (actionBtn) {
    e.stopPropagation();
    const sessionId = clickedItem.getAttribute("data-session-id");
    selectSession(sessionId);
} else if (e.target.closest(".session-item")) {
    const sessionId = clickedItem.getAttribute("data-session-id");
    selectSession(sessionId);
}
});
}
}

    function selectSession(sessionId) {
    const currentSessionId = getCurrentSessionId();
    saveChatHistory(currentSessionId);

    sessionStorage.setItem("chatbot_session_id", sessionId);

    const history = chatHistory.get(sessionId) || [];
    if (history.length === 0) {
    sessionStorage.removeItem(WELCOME_FLAG);
} else {
    sessionStorage.setItem(WELCOME_FLAG, "1");
}

    restoreChatHistory(sessionId);

    const sessionItems = document.querySelectorAll(".session-item");
    sessionItems.forEach((si) => si.classList.remove("active"));

    const selectedItem = document.querySelector(`[data-session-id="${sessionId}"]`);
    if (selectedItem) selectedItem.classList.add("active");

    console.log("Switched to session:", sessionId);
}

    function createNewSession(firstMessageTitle = null, shouldClearChat = true) {
    if (window.isCreatingSession) {
    console.log("Đang tạo session, vui lòng đợi...");
    return null;
}
    window.isCreatingSession = true;

    try {
    const currentSessionId = getCurrentSessionId();
    saveChatHistory(currentSessionId);

    const newSessionId = `SESSION_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

    sessionStorage.setItem("chatbot_session_id", newSessionId);

    if (!chatHistory.has(newSessionId)) {
    chatHistory.set(newSessionId, []);
}

    sessionStorage.removeItem(WELCOME_FLAG);

    if (shouldClearChat) {
    clearChat({ restoreWelcome: true });
} else {
    showWelcome();
}

    const sessionList = document.querySelector(".session-list");
    if (sessionList) {
    document.querySelectorAll(".session-item").forEach((si) => si.classList.remove("active"));

    let title = "Phiên mới";
    if (firstMessageTitle) {
    title = firstMessageTitle.trim();
    if (title.length > 30) title = title.substring(0, 30) + "...";
}

    const newSessionItem = document.createElement("div");
    newSessionItem.classList.add("session-item", "active");
    newSessionItem.setAttribute("data-session-id", newSessionId);
    newSessionItem.innerHTML = `
                <div class="session-info">
                    <span class="session-title">${title}</span>
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
    sessionList.insertBefore(newSessionItem, sessionList.firstChild);
}

    setTimeout(() => {
    const input = document.querySelector(".message-input");
    if (input) input.focus();
    if (typeof scrollToBottom === "function") scrollToBottom();
}, 50);

    console.log("Created new session:", newSessionId);
    return newSessionId;
} finally {
    setTimeout(() => {
    window.isCreatingSession = false;
}, 300);
}
}

    function deleteSession(sessionItem) {
    const sessionId = sessionItem.getAttribute("data-session-id");
    const sessionTitle = sessionItem.querySelector(".session-title").textContent;

    if (confirm(`Bạn có chắc chắn muốn xóa phiên "${sessionTitle}"?`)) {
    chatHistory.delete(sessionId);
    sessionItem.remove();

    const remainingSessions = document.querySelectorAll(".session-item");
    if (remainingSessions.length > 0) {
    const newActiveSessionId = remainingSessions[0].getAttribute("data-session-id");
    selectSession(newActiveSessionId);
}

    console.log("Deleted session:", sessionTitle);
}
}

    function updateSessionTitle(sessionId, firstMessage) {
    let title = firstMessage.trim();
    if (title.length > 30) {
    title = title.substring(0, 30) + "...";
}

    const sessionItem = document.querySelector(`[data-session-id="${sessionId}"]`);
    if (sessionItem) {
    const titleElement = sessionItem.querySelector(".session-title");
    if (titleElement) {
    titleElement.textContent = title;
}
}
}

    // ===== TOGGLE MAXIMIZE/FULLSCREEN =====
    function toggleMaximize() {
    const chatbotContainer = document.querySelector(".chatbot-container");
    const chatbotWrapper = document.querySelector(".chatbot-wrapper");

    const headerActions = document.querySelector(".header-actions");
    let maximizeBtn = null;
    if (headerActions) {
    const buttons = headerActions.querySelectorAll(".header-btn");
    if (buttons.length > 1) {
    maximizeBtn = buttons[1];
}
}

    if (!chatbotContainer || !chatbotWrapper) {
    console.warn("⚠️ Chatbot container or wrapper not found");
    return;
}

    const isFullscreen = chatbotContainer.classList.contains("fullscreen");

    if (isFullscreen) {
    chatbotContainer.classList.remove("fullscreen");
    chatbotWrapper.classList.remove("fullscreen");
    document.body.classList.remove("chatbot-fullscreen");

    document.body.style.overflow = "";

    if (maximizeBtn) {
    maximizeBtn.textContent = "⛶";
    maximizeBtn.title = "Phóng to";
}

    console.log("✅ Exited fullscreen mode - page elements restored");
} else {
    if (!chatbotContainer.classList.contains("active")) {
    chatbotContainer.classList.add("active");
    chatbotContainer.style.display = "flex";
}

    chatbotContainer.classList.add("fullscreen");
    chatbotWrapper.classList.add("fullscreen");
    document.body.classList.add("chatbot-fullscreen");

    if (maximizeBtn) {
    maximizeBtn.textContent = "⤡";
    maximizeBtn.title = "Thu nhỏ";
}

    document.body.style.overflow = "hidden";

    // PATCH: Copy messages từ popup mode sang fullscreen mode
    const messagesSectionFullscreen = document.getElementById("messages-section-fullscreen");
    const popupMessages = document.getElementById("chatMessages");
    
    if (messagesSectionFullscreen && popupMessages) {
    // Xóa messages cũ trong fullscreen (nếu có)
    messagesSectionFullscreen.innerHTML = "";
    
    // Copy tất cả messages từ popup sang fullscreen
    const messages = popupMessages.querySelectorAll(".message");
    if (messages.length > 0) {
    messages.forEach((msg) => {
    const clonedMsg = msg.cloneNode(true);
    messagesSectionFullscreen.appendChild(clonedMsg);
});
    console.log("✅ Copied", messages.length, "messages to fullscreen mode");
} else {
    console.log("ℹ️ No messages to copy to fullscreen mode");
}
} else {
    console.warn("⚠️ Messages section not found:", {
    messagesSectionFullscreen: !!messagesSectionFullscreen,
    popupMessages: !!popupMessages,
});
}

    console.log("✅ Entered fullscreen mode - header is now main header, body shows messages");
}

    setTimeout(() => {
    const chatbotBody = document.querySelector(".chatbot-body");
    if (chatbotBody) {
    chatbotBody.scrollTo({
    top: chatbotBody.scrollHeight,
    behavior: "smooth",
});
}
    // Scroll trong fullscreen messages section
    const messagesSectionFullscreen = document.getElementById("messages-section-fullscreen");
    if (messagesSectionFullscreen) {
    messagesSectionFullscreen.scrollTo({
    top: messagesSectionFullscreen.scrollHeight,
    behavior: "smooth",
});
}
    // Scroll trong popup messages (nếu đang ở popup mode)
    if (chatMessages && !chatbotContainer.classList.contains("fullscreen")) {
    chatMessages.scrollTo({
    top: chatMessages.scrollHeight,
    behavior: "smooth",
});
}
}, 100);
}

    // (PATCH) Removed moveMessagesToFullscreen/moveMessagesToPopup: dùng 1 container, chỉ đổi layout bằng CSS

    function navigateToChatWeb() {
    const currentSessionId = getCurrentSessionId();
    saveChatHistory(currentSessionId);

    sessionStorage.setItem("chatweb_session_id", currentSessionId);
    if (isValidUserId(USER_ID)) {
    sessionStorage.setItem("chatweb_user_id", String(USER_ID));
} else {
    sessionStorage.removeItem("chatweb_user_id");
}
    sessionStorage.setItem("chatweb_api_base_url", API_BASE_URL);

    const historyData = {};
    chatHistory.forEach((value, key) => {
    historyData[key] = value;
});
    sessionStorage.setItem("chatweb_history", JSON.stringify(historyData));

    // PATCH: chuẩn hóa BASE URL tránh //
    const base = (API_BASE_URL || "").replace(/\/+$/, "");
    const chatwebPath = `${base}/chatweb`;

    const fullUrl = new URL(chatwebPath, window.location.origin);
    fullUrl.searchParams.set("sessionId", currentSessionId);
    if (isValidUserId(USER_ID)) fullUrl.searchParams.set("userId", String(USER_ID));

    console.log("Navigating to chatweb:", {
    url: fullUrl.toString(),
    path: chatwebPath,
    sessionId: currentSessionId,
    userId: USER_ID,
    historySaved: Object.keys(historyData).length,
    API_BASE_URL: API_BASE_URL,
});

    window.location.href = fullUrl.toString();
}

    // ===== SHARE FUNCTIONALITY =====
    function shareChatbot() {
    const chatbotContainer = document.querySelector(".chatbot-container");
    if (!chatbotContainer) return;

    const messages = chatMessages ? Array.from(chatMessages.querySelectorAll(".message")) : [];
    let chatContent = "💬 Cuộc trò chuyện với Eva - Trợ lý AI OpenEvent\n\n";

    messages.forEach((msg) => {
    const isUser = msg.classList.contains("user-message");
    const content = msg.querySelector(".message-content")?.textContent || msg.textContent;
    if (content && content.trim()) {
    chatContent += `${isUser ? "👤 Bạn" : "🤖 Eva"}: ${content.trim()}\n\n`;
}
});

    if (messages.length === 0) {
    chatContent = "💬 Trải nghiệm Eva - Trợ lý AI của OpenEvent!\n\n";
    chatContent += window.location.href;
} else {
    chatContent += `\n🔗 ${window.location.href}`;
}

    if (navigator.share) {
    navigator
    .share({
    title: "Cuộc trò chuyện với Eva - OpenEvent",
    text: chatContent,
    url: window.location.href,
})
    .catch((err) => {
    console.log("Error sharing:", err);
    copyToClipboard(chatContent);
});
} else {
    copyToClipboard(chatContent);
}
}

    function copyToClipboard(text) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard
    .writeText(text)
    .then(() => {
    showNotification("Đã sao chép vào clipboard!", "success");
})
    .catch((err) => {
    console.error("Failed to copy:", err);
    fallbackCopyToClipboard(text);
});
} else {
    fallbackCopyToClipboard(text);
}
}

    function fallbackCopyToClipboard(text) {
    const textArea = document.createElement("textarea");
    textArea.value = text;
    textArea.style.position = "fixed";
    textArea.style.left = "-999999px";
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    try {
    document.execCommand("copy");
    showNotification("Đã sao chép vào clipboard!", "success");
} catch (err) {
    console.error("Fallback copy failed:", err);
    showNotification("Không thể sao chép. Vui lòng thử lại.", "error");
}

    document.body.removeChild(textArea);
}

    function showNotification(message, type = "info") {
    const notification = document.createElement("div");
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${type === "success" ? "#4caf50" : type === "error" ? "#f44336" : "#2196f3"};
        color: white;
        padding: 16px 24px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 10001;
        font-size: 14px;
        font-weight: 500;
        animation: slideInRight 0.3s ease-out;
        max-width: 300px;
    `;
    notification.textContent = message;

    if (!document.getElementById("notification-styles")) {
    const style = document.createElement("style");
    style.id = "notification-styles";
    style.textContent = `
            @keyframes slideInRight {
                from { transform: translateX(100%); opacity: 0; }
                to { transform: translateX(0); opacity: 1; }
            }
            @keyframes slideOutRight {
                from { transform: translateX(0); opacity: 1; }
                to { transform: translateX(100%); opacity: 0; }
            }
        `;
    document.head.appendChild(style);
}

    document.body.appendChild(notification);

    setTimeout(() => {
    notification.style.animation = "slideOutRight 0.3s ease-out";
    setTimeout(() => {
    if (notification.parentNode) {
    notification.parentNode.removeChild(notification);
}
}, 300);
}, 3000);
}

    // ===== EXPORT FOR EXTERNAL USE =====
    if (typeof window !== "undefined") {
    // PATCH: Đảm bảo sendMessage luôn được export, dù có placeholder hay không
    if (window.__setSendMessage) {
    try {
    window.__setSendMessage(sendMessage);
    console.log("✅ sendMessage exported via __setSendMessage");
} catch (error) {
    console.error("❌ Error setting sendMessage via __setSendMessage:", error);
    window.sendMessage = sendMessage;
    console.log("✅ sendMessage exported directly (fallback)");
}
} else {
    window.sendMessage = sendMessage;
    console.log("✅ sendMessage exported to window (direct, no placeholder found)");
}
    
    // Đảm bảo OpenEventAI object cũng có sendMessage
    if (!window.OpenEventAI) {
    window.OpenEventAI = {};
}
}

    window.OpenEventAI = {
    sendQuickAction,
    clearChat,
    openChatbot,
    closeChatbot,
    isChatbotOpen,
    toggleMaximize,
    navigateToChatWeb,
    shareChatbot,
    sendMessage,
};
    
    // PATCH: Đánh dấu chatbot.js đã load xong
    if (typeof window !== "undefined") {
    window.chatbotJsLoaded = true;
    console.log("✅ Chatbot.js fully loaded and initialized");
}
