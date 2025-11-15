let stompClient = null;
let currentRoomId = null;
let currentRecipientId = null;
let currentEventId = null;
let currentUserId = null;
let currentRoomType = null; // 'HOST_DEPARTMENT' or 'HOST_VOLUNTEERS'
let currentRoomSubscription = null; // Track current room subscription

let roomListEl;
let messageListEl;
let roomTitleEl;
let messageInputEl;
let sendBtnEl;
let emptyStateEl;
let createRoomBtnEl;

// Cache để lưu messages trong memory và sessionStorage
let messagesCache = new Map(); // roomId -> messages array

// Functions để save/load messages từ sessionStorage
function saveMessagesToStorage(roomId) {
    try {
        const messages = messagesCache.get(roomId) || [];
        const key = `event-chat-messages-${roomId}`;
        sessionStorage.setItem(key, JSON.stringify(messages));
    } catch (e) {
        console.warn('Failed to save messages to sessionStorage:', e);
    }
}

function loadMessagesFromStorage(roomId) {
    try {
        const key = `event-chat-messages-${roomId}`;
        const stored = sessionStorage.getItem(key);
        if (stored) {
            return JSON.parse(stored);
        }
    } catch (e) {
        console.warn('Failed to load messages from sessionStorage:', e);
    }
    return [];
}

document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('event-chat');
    if (!container) {
        return;
    }

    currentEventId = parseInt(container.dataset.eventId, 10);
    currentUserId = parseInt(container.dataset.currentUserId, 10);
    const isHost = container.dataset.isHost === 'true';

    roomListEl = document.getElementById('chat-room-list');
    messageListEl = document.getElementById('chat-message-list');
    roomTitleEl = document.getElementById('chat-room-title');
    messageInputEl = document.getElementById('chat-input');
    sendBtnEl = document.getElementById('chat-send-btn');
    emptyStateEl = document.getElementById('chat-empty-state');
    createRoomBtnEl = document.getElementById('create-volunteer-room-btn');

    // Allow eventId = 0 for department chat (HOST_DEPARTMENT rooms)
    if (!Number.isFinite(currentUserId) || (currentEventId !== 0 && !Number.isFinite(currentEventId))) {
        console.warn('Event chat requires valid currentUserId and eventId (can be 0 for department chat).');
        return;
    }
    
    // Hide create room button if not host
    if (createRoomBtnEl && !isHost) {
        createRoomBtnEl.style.display = 'none';
    }

    console.log('Event chat initializing:', {
        eventId: currentEventId,
        userId: currentUserId,
        container: !!container
    });
    
    connectToChat();
    loadRooms();

    if (sendBtnEl) {
        sendBtnEl.addEventListener('click', sendMessage);
    }
    if (messageInputEl) {
        messageInputEl.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
    }
    if (createRoomBtnEl) {
        createRoomBtnEl.addEventListener('click', createVolunteerRoom);
    }
});

function connectToChat() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, () => {
        console.log('Connected to event chat WebSocket');
        // Room subscriptions will be set up when a room is selected
    }, (error) => {
        console.error('STOMP connection error', error);
    });
}

function subscribeToRoom(roomId) {
    if (!roomId) {
        console.warn('subscribeToRoom called with invalid roomId:', roomId);
        return;
    }

    if (!stompClient) {
        console.warn('STOMP client not initialized, cannot subscribe to room');
        // Retry after a short delay
        setTimeout(() => subscribeToRoom(roomId), 500);
        return;
    }

    if (!stompClient.connected) {
        console.warn('STOMP client not connected, will retry subscription to room:', roomId);
        // Wait for connection, then retry
        const checkConnection = setInterval(() => {
            if (stompClient && stompClient.connected) {
                clearInterval(checkConnection);
                subscribeToRoom(roomId);
            }
        }, 200);
        // Stop retrying after 5 seconds
        setTimeout(() => clearInterval(checkConnection), 5000);
        return;
    }

    // Unsubscribe from previous room if exists
    if (currentRoomSubscription) {
        console.log('Unsubscribing from previous room');
        currentRoomSubscription.unsubscribe();
        currentRoomSubscription = null;
    }

    // Subscribe to room-specific destination
    const destination = `/queue/event-chat/rooms/${roomId}`;
    console.log('Subscribing to room:', destination, 'roomId:', roomId);
    
    try {
        currentRoomSubscription = stompClient.subscribe(destination, (frame) => {
        try {
            if (!frame || !frame.body) {
                console.warn('Received empty frame from WebSocket');
                return;
            }
            
            // Validate that this is a chat message (should have roomId, body, etc.)
            const bodyStr = frame.body.toString().trim();
            
            // Skip if body doesn't look like JSON
            if (!bodyStr.startsWith('{') || !bodyStr.endsWith('}')) {
                console.warn('Received non-JSON message, skipping:', bodyStr.substring(0, 100));
                return;
            }
            
            console.log('Received WebSocket message:', bodyStr);
            const payload = JSON.parse(bodyStr);
            
            // Validate payload structure
            if (!payload.roomId || !payload.body) {
                console.warn('Invalid chat message format:', payload);
                return;
            }
            
            handleIncomingMessage(payload);
        } catch (error) {
            console.error('Error parsing WebSocket message:', error);
            console.error('Raw message body:', frame.body);
            // Don't crash, just log the error
        }
    });
        console.log('Successfully subscribed to room:', destination);
    } catch (error) {
        console.error('Error subscribing to room:', error);
    }
}

async function loadRooms() {
    const url = `/api/event-chat/rooms/${currentEventId}`;
    try {
        const res = await fetch(url, { credentials: 'include' });
        const text = await res.text();

        if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText} @ ${url}\nBody: ${text.slice(0,200)}`);

        let data;
        try {
            data = JSON.parse(text);
        } catch (e) {
            throw new Error(`Invalid JSON from ${url}\nBody: ${text.slice(0,200)}`);
        }

        const rooms = Array.isArray(data) ? data : (data && data.content ? data.content : []);
        console.log('rooms length =', rooms.length, rooms);
        if (rooms.length > 0) {
            console.log('First room data:', rooms[0]);
        }

        try {
            renderRoomList(rooms);
        } catch (e) {
            console.error('renderRoomList failed:', e, rooms);
        }
    } catch (err) {
        console.error('loadRooms failed:', err);
    }
}

function renderRoomList(rooms) {
    if (!roomListEl) return;
    roomListEl.innerHTML = '';

    if (!Array.isArray(rooms) || rooms.length === 0) {
        const li = document.createElement('li');
        li.className = 'chat-room-empty';
        li.textContent = 'Chưa có cuộc trò chuyện nào.';
        roomListEl.appendChild(li);
        return;
    }

    rooms.forEach((room, index) => {
        if (!room || room.id == null) return; // guard

        const li = document.createElement('li');
        li.className = 'chat-room-item';
        li.dataset.roomId = room.id;
        li.dataset.roomType = room.roomType || 'HOST_VOLUNTEERS';

        let roomName = '';
        let roomSubtext = '';
        
        if (room.roomType === 'HOST_DEPARTMENT') {
            // Room với Department
            // Backend trả về: host trong field host, department trong field volunteer
            const host = room.host;
            const department = room.volunteer; // Backend trả về department trong field volunteer
            
            // Xác định đối tác: nếu current user là host thì hiển thị department, ngược lại hiển thị host
            let counterpart = null;
            if (host && host.userId === currentUserId) {
                // Current user là host → hiển thị department
                counterpart = department;
                roomName = department ? (department.name || department.email || 'Department') : 'Department';
                roomSubtext = 'Chat với Department';
                // Fallback về userId = 2 nếu department null (vì chỉ có 1 department)
                li.dataset.recipientId = (department && department.userId) ? department.userId.toString() : '2';
            } else {
                // Current user là department → hiển thị host
                counterpart = host;
                roomName = host ? (host.name || host.email || 'Host') : 'Host';
                roomSubtext = 'Chat với Host';
                li.dataset.recipientId = (host && host.userId) ? host.userId.toString() : '';
            }
        } else if (room.roomType === 'HOST_VOLUNTEERS') {
            // Group chat với Volunteers
            roomName = room.eventTitle || `Sự kiện #${room.eventId || ''}`;
            roomSubtext = 'Group chat với Volunteers';
            if (room.participantCount != null && room.participantCount > 0) {
                roomSubtext += ` (${room.participantCount} người)`;
            }
            li.dataset.recipientId = ''; // Group chat không có single recipient
        }

        li.dataset.recipientName = roomName;

        li.innerHTML = `
            <div class="chat-room-name">${roomName}</div>
            <div class="chat-room-subtext">${roomSubtext}</div>
            ${room.roomType === 'HOST_VOLUNTEERS' ? '<span class="badge badge-group">Group</span>' : ''}
        `;

        li.addEventListener('click', () => selectRoom(li));
        roomListEl.appendChild(li);

        if (index === 0) selectRoom(li);
    });
}

function selectRoom(li) {
    if (!li) {
        console.warn('selectRoom called with null/undefined li');
        return;
    }

    const roomId = parseInt(li.dataset.roomId, 10);
    const recipientId = li.dataset.recipientId ? parseInt(li.dataset.recipientId, 10) : null;
    const roomType = li.dataset.roomType || 'HOST_VOLUNTEERS';
    
    console.log('selectRoom called:', {
        roomId: roomId,
        recipientId: recipientId,
        roomType: roomType,
        recipientName: li.dataset.recipientName
    });

    if (!Number.isFinite(roomId)) {
        console.error('Invalid roomId:', li.dataset.roomId);
        return;
    }

    Array.from(roomListEl.children).forEach((child) => child.classList.remove('active'));
    li.classList.add('active');

    currentRoomId = roomId;
    currentRecipientId = recipientId;
    currentRoomType = roomType;

    if (roomTitleEl) {
        roomTitleEl.textContent = li.dataset.recipientName || 'Chat';
    }

    if (emptyStateEl) {
        emptyStateEl.style.display = 'none';
    }
    
    // KHÔNG clear ngay, để loadHistory() xử lý việc hiển thị từ cache trước
    // if (messageListEl) {
    //     messageListEl.innerHTML = '';
    // }

    // Subscribe to room-specific WebSocket destination
    subscribeToRoom(currentRoomId);
    
    // Load history (sẽ hiển thị cached messages ngay, sau đó update từ API)
    loadHistory(currentRoomId);
}

function loadHistory(roomId) {
    if (!roomId || !messageListEl) return;

    // Load từ cache/storage trước (hiển thị ngay để tránh mất tin nhắn khi quay lại trang)
    const cachedMessages = messagesCache.get(roomId) || loadMessagesFromStorage(roomId);
    if (cachedMessages.length > 0) {
        messageListEl.innerHTML = '';
        cachedMessages.forEach((message) => {
            // Render message mà không lưu vào cache (đã có trong cache rồi)
            renderMessageToDOM(message);
        });
        scrollMessagesToBottom();
        console.log('Loaded', cachedMessages.length, 'cached messages for room', roomId);
    } else {
        // Nếu không có cache, clear để tránh hiển thị tin nhắn cũ
        messageListEl.innerHTML = '';
    }

    // Sau đó fetch từ API để cập nhật (có thể có tin nhắn mới)
    fetch(`/api/event-chat/rooms/${roomId}/messages?page=0&size=50`, {credentials: 'include'})
        .then((res) => {
            if (!res.ok) {
                throw new Error(`Failed to load messages: ${res.status}`);
            }
            return res.json();
        })
        .then((page) => {
            const messages = page && page.content ? page.content : [];
            
            // Cập nhật cache với messages từ API
            const apiMessages = messages.map((message) => ({
                roomId: roomId,
                senderUserId: message.sender?.userId,
                senderName: message.sender?.name || message.sender?.email || 'Người dùng',
                body: message.body,
                timestamp: message.timestamp
            }));
            
            messagesCache.set(roomId, apiMessages);
            saveMessagesToStorage(roomId);
            
            // Chỉ update UI nếu đang ở room này
            if (currentRoomId === roomId) {
                messageListEl.innerHTML = '';
                apiMessages.forEach((message) => {
                    renderMessageToDOM(message);
                });
                scrollMessagesToBottom();
                console.log('Updated with', apiMessages.length, 'messages from API for room', roomId);
            }
        })
        .catch((err) => {
            console.error('Error loading history from API:', err);
            // Nếu API fail, giữ lại cached messages (đã hiển thị ở trên)
        });
}

function sendMessage() {
    if (!stompClient || !messageInputEl || !currentRoomId) {
        return;
    }

    const content = messageInputEl.value.trim();
    if (!content) {
        return;
    }

    // For HOST_VOLUNTEERS (group chat), recipientUserId is not required
    // For HOST_DEPARTMENT, we need recipientUserId (the counterpart: host or department)
    if (currentRoomType === 'HOST_DEPARTMENT' && !Number.isFinite(currentRecipientId)) {
        alert('Không xác định được đối tượng nhận tin.');
        return;
    }

    // For group chat, we still send recipientUserId (can be any participant or null)
    // Backend will handle group chat logic
    // For department chat (eventId = 0), recipientUserId is required (host or department)
    const payload = {
        eventId: currentEventId || 0, // Use 0 for department chat
        recipientUserId: currentRecipientId || (currentRoomType === 'HOST_VOLUNTEERS' ? currentUserId : null),
        content: content
    };

    stompClient.send('/app/event-chat.send', {}, JSON.stringify(payload));
    messageInputEl.value = '';
}

function handleIncomingMessage(message) {
    if (!message) return;

    // If message is for another room, indicate unread
    if (currentRoomId !== message.roomId) {
        markRoomUnread(message.roomId);
        return;
    }

    // Đảm bảo có senderName từ WebSocket message
    appendMessage({
        roomId: message.roomId,
        senderUserId: message.senderUserId,
        senderName: message.senderName || 'Người dùng',
        body: message.body,
        timestamp: message.timestamp
    });
    scrollMessagesToBottom();
}

// Render message vào DOM (không lưu vào cache)
function renderMessageToDOM(message) {
    if (!messageListEl) return;

    const wrapper = document.createElement('div');
    wrapper.className = 'chat-message-item';

    const outgoing = message.senderUserId === currentUserId;
    wrapper.classList.add(outgoing ? 'from-me' : 'from-them');

    // Thêm tên người gửi cho tin nhắn của người khác
    if (!outgoing && message.senderName) {
        const sender = document.createElement('div');
        sender.className = 'chat-message-sender';
        sender.textContent = message.senderName;
        wrapper.appendChild(sender);
    }

    const body = document.createElement('div');
    body.className = 'chat-message-body';
    body.textContent = message.body || '';

    const meta = document.createElement('div');
    meta.className = 'chat-message-meta';
    meta.textContent = message.timestamp ? formatDate(message.timestamp) : '';

    wrapper.appendChild(body);
    wrapper.appendChild(meta);
    messageListEl.appendChild(wrapper);
}

// Append message và lưu vào cache
function appendMessage(message) {
    if (!messageListEl) return;

    // Render message vào DOM
    renderMessageToDOM(message);
    
    // Lưu message vào cache
    if (!messagesCache.has(message.roomId)) {
        messagesCache.set(message.roomId, []);
    }
    
    // Kiểm tra xem message đã tồn tại chưa (tránh duplicate)
    const roomMessages = messagesCache.get(message.roomId);
    const isDuplicate = roomMessages.some(msg => 
        msg.body === message.body && 
        msg.senderUserId === message.senderUserId &&
        msg.timestamp === message.timestamp
    );
    
    if (!isDuplicate) {
        roomMessages.push(message);
        saveMessagesToStorage(message.roomId);
    }
}

function markRoomUnread(roomId) {
    Array.from(roomListEl.children).forEach((child) => {
        if (parseInt(child.dataset.roomId, 10) === roomId) {
            child.classList.add('unread');
        }
    });
}

function resolveCounterpart(room) {
    if (!room) {
        console.warn('resolveCounterpart called with null/undefined room');
        return null;
    }

    // For HOST_DEPARTMENT: volunteer field contains department
    // For HOST_VOLUNTEERS: volunteer is null (group chat)
    if (room.roomType === 'HOST_DEPARTMENT') {
        return room.volunteer; // Department user
    }
    
    // For group chat, no single counterpart
    return null;
}

async function createVolunteerRoom() {
    if (!currentEventId) {
        alert('Không xác định được sự kiện.');
        return;
    }

    try {
        const response = await fetch(`/api/event-chat/rooms/create-volunteer-room?eventId=${currentEventId}`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to create room: ${response.status} ${errorText}`);
        }

        const newRoom = await response.json();
        console.log('Created volunteer room:', newRoom);
        
        // Reload rooms to show the new room
        await loadRooms();
        
        // Select the newly created room
        setTimeout(() => {
            const roomItem = Array.from(roomListEl.children).find(
                li => parseInt(li.dataset.roomId, 10) === newRoom.id
            );
            if (roomItem) {
                selectRoom(roomItem);
            }
        }, 100);
    } catch (error) {
        console.error('Error creating volunteer room:', error);
        alert('Không thể tạo phòng chat. ' + error.message);
    }
}

function scrollMessagesToBottom() {
    if (!messageListEl) return;
    messageListEl.scrollTop = messageListEl.scrollHeight;
}

function formatDate(value) {
    if (!value) return '';
    const date = typeof value === 'string' ? new Date(value) : value;
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        day: '2-digit',
        month: '2-digit'
    });
}

// Expose functions (if needed elsewhere)
window.connectToChat = connectToChat;
window.sendMessage = sendMessage;
window.loadHistory = loadHistory;
window.loadRooms = loadRooms;
window.createVolunteerRoom = createVolunteerRoom;

window.addEventListener('error',  e => {
    console.error('[window.error]', e.message, e.error?.stack || '');
});

window.addEventListener('unhandledrejection', e => {
    console.error('[unhandledrejection]', e.reason?.message || e.reason, e.reason?.stack || '');
});


