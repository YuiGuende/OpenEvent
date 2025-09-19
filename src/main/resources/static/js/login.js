const container = document.querySelector('.container');
const registerBtn = document.querySelector('.register-btn');
const loginBtn = document.querySelector('.login-btn');


registerBtn.addEventListener('click', () => {
    container.classList.add('active')
});
loginBtn.addEventListener('click', () => {
    container.classList.remove('active')
});

function getQueryParam(name) {
    const url = new URL(window.location.href);
    return url.searchParams.get(name);
}
(function showNotice() {
    if (getQueryParam('registered') === '1') {
        document.getElementById('notice').textContent = 'Đăng ký thành công! Vui lòng đăng nhập.';
    }
})();

document.getElementById('toggleLoginPw').addEventListener('click', () => {
    const pwInput = document.getElementById('loginPassword');
    const eyeIcon = document.getElementById('toggleLoginPw');

    if (pwInput.type === 'password') {
        pwInput.type = 'text';
        eyeIcon.classList.remove('bx-show');
        eyeIcon.classList.add('bx-hide');
    } else {
        pwInput.type = 'password';
        eyeIcon.classList.remove('bx-hide');
        eyeIcon.classList.add('bx-show');
    }
});



async function handleLogin(e) {
    e.preventDefault();
    document.getElementById('loginBtn').disabled = true;
    const payload = {
        email: document.getElementById('loginEmail').value,
        password: document.getElementById('loginPassword').value
    };
    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(payload)
        });
        const text = await res.text();
        if (!res.ok) {
            document.getElementById('loginResult').textContent = 'Lỗi đăng nhập (' + res.status + '):\n' + text;
        } else {
            try {
                const json = JSON.parse(text);
                if (json.redirectPath) {
                    window.location.href = json.redirectPath;
                    return;
                }
            } catch (_) {}
            document.getElementById('loginResult').textContent = text;
        }
    } catch (err) {
        document.getElementById('loginResult').textContent = 'Lỗi: ' + err;
    } finally {
        document.getElementById('loginBtn').disabled = false;
    }
}

document.getElementById('loginForm').addEventListener('submit', handleLogin);


