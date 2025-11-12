let stompClient = null;
let currentRoomId = null;
let currentRecipientId = null;
let currentEventId = null;
let currentUserId = null;

let roomListEl;
let messageListEl;
let roomTitleEl;
let messageInputEl;
let sendBtnEl;
let emptyStateEl;

document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('event-chat');
    if (!container) {
        return;
    }

    currentEventId = parseInt(container.dataset.eventId, 10);
    currentUserId = parseInt(container.dataset.currentUserId, 10);

    roomListEl = document.getElementById('chat-room-list');
    messageListEl = document.getElementById('chat-message-list');
    roomTitleEl = document.getElementById('chat-room-title');
    messageInputEl = document.getElementById('chat-input');
    sendBtnEl = document.getElementById('chat-send-btn');
    emptyStateEl = document.getElementById('chat-empty-state');

    if (!Number.isFinite(currentEventId) || !Number.isFinite(currentUserId)) {
        console.warn('Event chat requires both eventId and currentUserId.');
        return;
    }

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
});

function connectToChat() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, () => {
        stompClient.subscribe(`/queue/event-chat/${currentUserId}`, (frame) => {
            const payload = JSON.parse(frame.body);
            handleIncomingMessage(payload);
        });
    }, (error) => {
        console.error('STOMP connection error', error);
    });
}

function loadRooms() {
    fetch(`/api/event-chat/rooms/${currentEventId}`, {credentials: 'include'})
        .then((res) => {
            if (!res.ok) {
                throw new Error(`Failed to load rooms: ${res.status}`);
            }
            return res.json();
        })
        .then((rooms) => {
            renderRoomList(Array.isArray(rooms) ? rooms : []);
        })
        .catch((err) => {
            console.error(err);
        });
}

function renderRoomList(rooms) {
    if (!roomListEl) return;
    roomListEl.innerHTML = '';

    if (!rooms.length) {
        const li = document.createElement('li');
        li.className = 'chat-room-empty';
        li.textContent = 'Chưa có cuộc trò chuyện nào.';
        roomListEl.appendChild(li);
        return;
    }

    rooms.forEach((room, index) => {
        const li = document.createElement('li');
        li.className = 'chat-room-item';
        li.dataset.roomId = room.id;

        const counterpart = resolveCounterpart(room);
        li.dataset.recipientId = counterpart?.userId || '';
        li.dataset.recipientName = counterpart?.name || counterpart?.displayName || counterpart?.email || `User ${counterpart?.userId}`;

        li.innerHTML = `
            <div class="chat-room-name">${li.dataset.recipientName}</div>
            <div class="chat-room-subtext">${room.createdAt ? formatDate(room.createdAt) : ''}</div>
        `;

        li.addEventListener('click', () => {
            selectRoom(li);
        });

        roomListEl.appendChild(li);

        if (index === 0) {
            selectRoom(li);
        }
    });
}

function selectRoom(li) {
    if (!li) return;

    Array.from(roomListEl.children).forEach((child) => child.classList.remove('active'));
    li.classList.add('active');

    currentRoomId = parseInt(li.dataset.roomId, 10);
    currentRecipientId = parseInt(li.dataset.recipientId, 10);

    if (roomTitleEl) {
        roomTitleEl.textContent = li.dataset.recipientName || 'Chat';
    }

    if (emptyStateEl) {
        emptyStateEl.style.display = 'none';
    }
    if (messageListEl) {
        messageListEl.innerHTML = '';
    }

    loadHistory(currentRoomId);
}

function loadHistory(roomId) {
    if (!roomId || !messageListEl) return;

    fetch(`/api/event-chat/rooms/${roomId}/messages?page=0&size=50`, {credentials: 'include'})
        .then((res) => {
            if (!res.ok) {
                throw new Error(`Failed to load messages: ${res.status}`);
            }
            return res.json();
        })
        .then((page) => {
            const messages = page && page.content ? page.content : [];
            messageListEl.innerHTML = '';
            messages.forEach((message) => {
                appendMessage({
                    roomId: roomId,
                    senderUserId: message.sender?.userId,
                    body: message.body,
                    timestamp: message.timestamp
                });
            });
            scrollMessagesToBottom();
        })
        .catch((err) => {
            console.error(err);
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

    if (!Number.isFinite(currentRecipientId)) {
        alert('Không xác định được đối tượng nhận tin.');
        return;
    }

    const payload = {
        eventId: currentEventId,
        recipientUserId: currentRecipientId,
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

    appendMessage(message);
    scrollMessagesToBottom();
}

function appendMessage(message) {
    if (!messageListEl) return;

    const wrapper = document.createElement('div');
    wrapper.className = 'chat-message-item';

    const outgoing = message.senderUserId === currentUserId;
    wrapper.classList.add(outgoing ? 'from-me' : 'from-them');

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

function markRoomUnread(roomId) {
    Array.from(roomListEl.children).forEach((child) => {
        if (parseInt(child.dataset.roomId, 10) === roomId) {
            child.classList.add('unread');
        }
    });
}

function resolveCounterpart(room) {
    if (!room) return null;

    const host = room.host;
    const volunteer = room.volunteer;

    const normalizeUser = (user) => {
        if (!user) return null;
        const accountEmail = user.account?.email;
        return {
            userId: user.userId,
            name: user.name,
            email: accountEmail,
            displayName: user.name || accountEmail || `User ${user.userId}`
        };
    };

    const normalizedHost = normalizeUser(host);
    const normalizedVolunteer = normalizeUser(volunteer);

    if (normalizedHost && normalizedHost.userId !== currentUserId) {
        return normalizedHost;
    }
    return normalizedVolunteer;
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


