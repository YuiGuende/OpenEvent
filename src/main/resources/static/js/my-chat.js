let stompClient = null;
let currentRoomId = null;
let currentRecipientId = null;
let currentEventId = null;
let currentUserId = null;
let currentRoomType = null;
let currentRoomSubscription = null;

let roomListEl;
let messageListEl;
let roomTitleEl;
let messageInputEl;
let sendBtnEl;
let emptyStateEl;

let messagesCache = new Map();
let isSendingMessage = false; // Guard để tránh gửi message nhiều lần
let isInitialized = false; // Guard để tránh initialize nhiều lần

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
    const container = document.querySelector('.my-chat-wrapper');
    if (!container) {
        return;
    }

    // ✅ Guard: Tránh initialize nhiều lần
    if (isInitialized) {
        console.warn('My chat already initialized, skipping...');
        return;
    }

    currentUserId = parseInt(container.dataset.currentUserId, 10);
    
    if (!Number.isFinite(currentUserId)) {
        console.warn('My chat requires valid currentUserId');
        return;
    }

    roomListEl = document.getElementById('all-chat-room-list');
    messageListEl = document.getElementById('chat-message-list');
    roomTitleEl = document.getElementById('chat-room-title');
    messageInputEl = document.getElementById('chat-input');
    sendBtnEl = document.getElementById('chat-send-btn');
    emptyStateEl = document.getElementById('chat-empty-state');

    console.log('My chat initializing:', { userId: currentUserId });
    
    connectToChat();
    loadAllRooms();

    // ✅ Đảm bảo chỉ bind event listener 1 lần
    if (sendBtnEl) {
        // Remove existing listener nếu có (tránh duplicate)
        const newSendBtn = sendBtnEl.cloneNode(true);
        sendBtnEl.parentNode.replaceChild(newSendBtn, sendBtnEl);
        sendBtnEl = newSendBtn;
        
        sendBtnEl.addEventListener('click', sendMyChatMessage);
    }
    if (messageInputEl) {
        // Remove existing listener nếu có
        const newInput = messageInputEl.cloneNode(true);
        messageInputEl.parentNode.replaceChild(newInput, messageInputEl);
        messageInputEl = newInput;
        
        messageInputEl.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMyChatMessage();
            }
        });
    }
    
    isInitialized = true;
});

function connectToChat() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, () => {
        console.log('Connected to my chat WebSocket');
    }, (error) => {
        console.error('STOMP connection error', error);
    });
}

async function loadAllRooms() {
    if (!roomListEl) return;
    
    roomListEl.innerHTML = '<li class="my-chat-room-item" style="text-align: center; color: #6c757d; padding: 40px 20px;">Đang tải...</li>';
    
    try {
        // ✅ SỬA: Load tất cả rooms từ một API call thay vì load theo từng event
        // API này trả về TẤT CẢ rooms mà user là participant (không phụ thuộc vào event host/volunteer status)
        const res = await fetch(`/api/event-chat/rooms/all`, { credentials: 'include' });
        
        if (!res.ok) {
            throw new Error(`HTTP ${res.status}`);
        }
        
        const rooms = await res.json();
        const allRooms = Array.isArray(rooms) ? rooms : (rooms?.content || []);
        
        console.log('Loaded rooms from API:', allRooms);
        
        // ✅ VALIDATION: Lọc bỏ rooms không có id hợp lệ
        const validRooms = allRooms.filter(room => {
            if (!room) {
                console.warn('Skipping null/undefined room');
                return false;
            }
            if (room.id == null || room.id === undefined) {
                console.warn('Skipping room with invalid id:', room);
                return false;
            }
            return true;
        });
        
        if (validRooms.length === 0) {
            roomListEl.innerHTML = '<li class="my-chat-room-item" style="text-align: center; color: #6c757d; padding: 40px 20px;">Chưa có cuộc trò chuyện nào.</li>';
            return;
        }
        
        // Sắp xếp rooms: Group chat trước, sau đó theo event title
        validRooms.sort((a, b) => {
            // Ưu tiên HOST_VOLUNTEERS (group chat) trước
            if (a.roomType === 'HOST_VOLUNTEERS' && b.roomType !== 'HOST_VOLUNTEERS') return -1;
            if (a.roomType !== 'HOST_VOLUNTEERS' && b.roomType === 'HOST_VOLUNTEERS') return 1;
            
            // Sau đó sắp xếp theo event title
            const titleA = a.eventTitle || '';
            const titleB = b.eventTitle || '';
            if (titleA !== titleB) {
                return titleA.localeCompare(titleB, 'vi');
            }
            
            // Nếu cùng event, sắp xếp theo room type (HOST_DEPARTMENT sau HOST_VOLUNTEERS)
            if (a.roomType !== b.roomType) {
                return a.roomType === 'HOST_VOLUNTEERS' ? -1 : 1;
            }
            
            return 0;
        });
        
        renderAllRooms(validRooms);
    } catch (err) {
        console.error('loadAllRooms failed:', err);
        roomListEl.innerHTML = '<li class="my-chat-room-item" style="text-align: center; color: #dc3545; padding: 40px 20px;">Lỗi khi tải danh sách.</li>';
    }
}

function renderAllRooms(rooms) {
    if (!roomListEl) return;
    roomListEl.innerHTML = '';

    if (!Array.isArray(rooms) || rooms.length === 0) {
        roomListEl.innerHTML = '<li class="my-chat-room-item" style="text-align: center; color: #6c757d; padding: 40px 20px;">Chưa có cuộc trò chuyện nào.</li>';
        return;
    }

    rooms.forEach((room, index) => {
        // ✅ VALIDATION: Kiểm tra room và id hợp lệ
        if (!room) {
            console.warn('Skipping null/undefined room in renderAllRooms');
            return;
        }
        
        if (room.id == null || room.id === undefined) {
            console.error('Skipping room with invalid id:', room);
            return;
        }
        
        // ✅ Đảm bảo id là number
        const roomId = typeof room.id === 'number' ? room.id : parseInt(room.id, 10);
        if (!Number.isFinite(roomId) || roomId <= 0) {
            console.error('Skipping room with invalid numeric id:', room.id, room);
            return;
        }

        const li = document.createElement('li');
        li.className = 'my-chat-room-item';
        li.dataset.roomId = roomId.toString();
        li.dataset.roomType = room.roomType || 'HOST_VOLUNTEERS';
        // ✅ SỬA: Đối với HOST_DEPARTMENT, set eventId = 0 thay vì chuỗi rỗng
        if (room.roomType === 'HOST_DEPARTMENT') {
            li.dataset.eventId = '0'; // HOST_DEPARTMENT room không gắn với event cụ thể
        } else {
            li.dataset.eventId = room.eventId ? room.eventId.toString() : '';
        }

        let roomName = '';
        let roomSubtext = '';
        
        if (room.roomType === 'HOST_DEPARTMENT') {
            const host = room.host;
            const department = room.volunteer;
            
            if (host && host.userId === currentUserId) {
                roomName = department ? (department.name || department.email || 'Department') : 'Department';
                // Hiển thị event title nếu có
                if (room.eventTitle) {
                    roomSubtext = `${room.eventTitle} - Chat với Department`;
                } else {
                    roomSubtext = 'Chat với Department';
                }
                li.dataset.recipientId = (department && department.userId) ? department.userId.toString() : '2';
            } else {
                roomName = host ? (host.name || host.email || 'Host') : 'Host';
                // Hiển thị event title nếu có
                if (room.eventTitle) {
                    roomSubtext = `${room.eventTitle} - Chat với Host`;
                } else {
                    roomSubtext = 'Chat với Host';
                }
                li.dataset.recipientId = (host && host.userId) ? host.userId.toString() : '';
            }
        } else if (room.roomType === 'HOST_VOLUNTEERS') {
            // Hiển thị rõ event title
            roomName = room.eventTitle || `Sự kiện #${room.eventId || ''}`;
            roomSubtext = 'Group chat với Volunteers';
            if (room.participantCount != null && room.participantCount > 0) {
                roomSubtext += ` (${room.participantCount} người)`;
            }
            li.dataset.recipientId = '';
        }

        li.dataset.recipientName = roomName;

        li.innerHTML = `
            <div class="my-chat-room-name">${roomName}</div>
            <div class="my-chat-room-subtext">${roomSubtext}</div>
            ${room.roomType === 'HOST_VOLUNTEERS' ? '<span class="badge badge-group" style="background: #28a745; color: white; padding: 2px 8px; border-radius: 12px; font-size: 0.75rem; margin-top: 5px; display: inline-block;">Group</span>' : ''}
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
    // ✅ SỬA: Xử lý đúng khi eventId là chuỗi rỗng hoặc '0'
    let eventId = null;
    const eventIdStr = li.dataset.eventId;
    if (eventIdStr && eventIdStr.trim() !== '') {
        const parsed = parseInt(eventIdStr, 10);
        if (Number.isFinite(parsed)) {
            eventId = parsed;
        }
    }
    // Đối với HOST_DEPARTMENT, eventId phải là 0 hoặc null
    if (roomType === 'HOST_DEPARTMENT') {
        eventId = 0;
    }
    
    if (!Number.isFinite(roomId)) {
        console.error('Invalid roomId:', li.dataset.roomId);
        return;
    }

    Array.from(roomListEl.children).forEach((child) => child.classList.remove('active'));
    li.classList.add('active');

    currentRoomId = roomId;
    currentRecipientId = recipientId;
    currentRoomType = roomType;
    currentEventId = eventId;

    if (roomTitleEl) {
        roomTitleEl.textContent = li.dataset.recipientName || 'Chat';
    }

    if (emptyStateEl) {
        emptyStateEl.style.display = 'none';
    }

    subscribeToRoom(currentRoomId);
    loadHistory(currentRoomId);
}

function subscribeToRoom(roomId) {
    if (!roomId) {
        console.warn('subscribeToRoom called with invalid roomId:', roomId);
        return;
    }

    if (!stompClient) {
        console.warn('STOMP client not initialized, cannot subscribe to room');
        setTimeout(() => subscribeToRoom(roomId), 500);
        return;
    }

    if (!stompClient.connected) {
        console.warn('STOMP client not connected, will retry subscription to room:', roomId);
        const checkConnection = setInterval(() => {
            if (stompClient && stompClient.connected) {
                clearInterval(checkConnection);
                subscribeToRoom(roomId);
            }
        }, 200);
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
                
                const bodyStr = frame.body.toString().trim();
                
                if (!bodyStr.startsWith('{') || !bodyStr.endsWith('}')) {
                    console.warn('Received non-JSON message, skipping:', bodyStr.substring(0, 100));
                    return;
                }
                
                console.log('Received WebSocket message:', bodyStr);
                const payload = JSON.parse(bodyStr);
                
                if (!payload.roomId || !payload.body) {
                    console.warn('Invalid chat message format:', payload);
                    return;
                }
                
                handleIncomingMessage(payload);
            } catch (error) {
                console.error('Error parsing WebSocket message:', error);
            }
        });
        console.log('Successfully subscribed to room:', destination);
    } catch (error) {
        console.error('Error subscribing to room:', error);
    }
}

function loadHistory(roomId) {
    // ✅ VALIDATION: Kiểm tra roomId hợp lệ trước khi gọi API
    if (!roomId || !Number.isFinite(roomId) || roomId <= 0) {
        console.error('loadHistory called with invalid roomId:', roomId);
        return;
    }
    
    if (!messageListEl) {
        console.warn('loadHistory called but messageListEl is null');
        return;
    }

    // Load từ cache/storage trước
    const cachedMessages = messagesCache.get(roomId) || loadMessagesFromStorage(roomId);
    if (cachedMessages.length > 0) {
        messageListEl.innerHTML = '';
        cachedMessages.forEach((message) => {
            renderMessageToDOM(message);
        });
        scrollMessagesToBottom();
        console.log('Loaded', cachedMessages.length, 'cached messages for room', roomId);
    } else {
        messageListEl.innerHTML = '';
    }

    // Sau đó fetch từ API để cập nhật
    fetch(`/api/event-chat/rooms/${roomId}/messages?page=0&size=50`, {credentials: 'include'})
        .then((res) => {
            if (!res.ok) {
                throw new Error(`Failed to load messages: ${res.status}`);
            }
            return res.json();
        })
        .then((page) => {
            const messages = page && page.content ? page.content : [];
            
            const apiMessages = messages.map((message) => ({
                roomId: roomId,
                senderUserId: message.sender?.userId,
                senderName: message.sender?.name || message.sender?.email || 'Người dùng',
                body: message.body,
                timestamp: message.timestamp
            }));
            
            messagesCache.set(roomId, apiMessages);
            saveMessagesToStorage(roomId);
            
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
        });
}

// ✅ Đổi tên function để tránh conflict với chatbot.js và event-chat.js
function sendMyChatMessage() {
    // ✅ Guard: Tránh gửi message nhiều lần
    if (isSendingMessage) {
        console.warn('Message is already being sent, please wait...');
        return;
    }
    
    if (!stompClient || !messageInputEl || !currentRoomId) {
        return;
    }

    const content = messageInputEl.value.trim();
    if (!content) {
        return;
    }

    if (currentRoomType === 'HOST_DEPARTMENT' && !Number.isFinite(currentRecipientId)) {
        alert('Không xác định được đối tượng nhận tin.');
        return;
    }

    // ✅ Set flag để tránh gửi nhiều lần
    isSendingMessage = true;

    const payload = {
        // ✅ SỬA: Đảm bảo eventId = 0 cho HOST_DEPARTMENT
        eventId: (currentRoomType === 'HOST_DEPARTMENT') ? 0 : (currentEventId || 0),
        recipientUserId: currentRecipientId || (currentRoomType === 'HOST_VOLUNTEERS' ? currentUserId : null),
        content: content
    };

    try {
        stompClient.send('/app/event-chat.send', {}, JSON.stringify(payload));
        messageInputEl.value = '';
    } catch (error) {
        console.error('Error sending message:', error);
    } finally {
        // ✅ Reset flag sau 500ms để cho phép gửi message tiếp theo
        setTimeout(() => {
            isSendingMessage = false;
        }, 500);
    }
}

function handleIncomingMessage(message) {
    if (!message) return;

    if (currentRoomId !== message.roomId) {
        markRoomUnread(message.roomId);
        return;
    }

    appendMessage({
        roomId: message.roomId,
        senderUserId: message.senderUserId,
        senderName: message.senderName || 'Người dùng',
        body: message.body,
        timestamp: message.timestamp
    });
    scrollMessagesToBottom();
}

function renderMessageToDOM(message) {
    if (!messageListEl) return;

    const wrapper = document.createElement('div');
    wrapper.className = 'chat-message-item';

    const outgoing = message.senderUserId === currentUserId;
    wrapper.classList.add(outgoing ? 'from-me' : 'from-them');

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

function appendMessage(message) {
    if (!messageListEl) return;
    renderMessageToDOM(message);
    
    if (!messagesCache.has(message.roomId)) {
        messagesCache.set(message.roomId, []);
    }
    
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

// Expose functions
window.loadAllRooms = loadAllRooms;

