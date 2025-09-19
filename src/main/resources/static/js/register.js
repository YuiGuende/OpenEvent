
document.getElementById('clearReg').addEventListener('click', () => {
    document.getElementById('registerForm').reset();
    document.getElementById('registerResult').textContent = '';
});

document.getElementById('toggleRegPw').addEventListener('click', () => {
    const pwInput = document.getElementById('regPassword');
    const eyeIcon = document.getElementById('toggleRegPw');

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

async function handleRegister(e) {
    e.preventDefault();
    document.getElementById('regBtn').disabled = true;
    const payload = {
        email: document.getElementById('regEmail').value,
        password: document.getElementById('regPassword').value,
        role: document.getElementById('regRole').value,
        phoneNumber: document.getElementById('regPhone').value || null,
        organization: document.getElementById('regOrg').value || null
    };
    try {
        const res = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(payload)
        });
        const text = await res.text();
        if (!res.ok) {
            document.getElementById('registerResult').textContent = 'Lỗi đăng ký (' + res.status + '):\n' + text;
        } else {

            try {
                const json = JSON.parse(text);
                if (json.redirectPath) {
                    window.location.href = json.redirectPath;
                    return;
                }
            } catch (_) {}
            document.getElementById('registerResult').textContent = text;
        }
    } catch (err) {
        document.getElementById('registerResult').textContent = 'Lỗi: ' + err;
    } finally {
        document.getElementById('regBtn').disabled = false;
    }
}

document.getElementById('registerForm').addEventListener('submit', handleRegister);