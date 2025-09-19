document.addEventListener("DOMContentLoaded", () => {
    const container = document.querySelector('.container');
    const registerBtn = document.querySelector('.register-btn');
    const loginBtn = document.querySelector('.login-btn');

    // 🔄 Toggle giữa login và register
    if (registerBtn) registerBtn.addEventListener('click', () => container.classList.add('active'));
    if (loginBtn) loginBtn.addEventListener('click', () => container.classList.remove('active'));

    // 📌 Hàm toggle show/hide password
    function setupTogglePassword(inputId, toggleId) {
        const pwInput = document.getElementById(inputId);
        const eyeIcon = document.getElementById(toggleId);

        if (!pwInput || !eyeIcon) return;

        eyeIcon.addEventListener("click", () => {
            const isHidden = pwInput.type === "password";
            pwInput.type = isHidden ? "text" : "password";
            eyeIcon.classList.replace(isHidden ? "bx-show" : "bx-hide", isHidden ? "bx-hide" : "bx-show");
        });
    }

    // Áp dụng cho login + register
    setupTogglePassword("loginPassword", "toggleLoginPw");
    setupTogglePassword("regPassword", "toggleRegPw");

    // 📌 Helper: lấy query param
    function getQueryParam(name) {
        const url = new URL(window.location.href);
        return url.searchParams.get(name);
    }

    // Hiện notice sau khi đăng ký thành công
    if (getQueryParam('registered') === '1') {
        const notice = document.getElementById('notice');
        if (notice) {
            notice.textContent = 'Đăng ký thành công! Vui lòng đăng nhập.';
            notice.classList.add('success');
            notice.style.display = 'block';
        }
    }

    // 📌 Submit Login
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const btn = document.getElementById('loginBtn');
            if (btn) btn.disabled = true;

            const payload = {
                email: document.getElementById('loginEmail')?.value || '',
                password: document.getElementById('loginPassword')?.value || ''
            };

            try {
                const res = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify(payload)
                });

                const text = await res.text();
                const resultEl = document.getElementById('loginResult');
                if (!res.ok) {
                    if (resultEl) resultEl.textContent = `Lỗi đăng nhập (${res.status}):\n${text}`;
                } else {
                    try {
                        const json = JSON.parse(text);
                        if (json.redirectPath) {
                            window.location.href = json.redirectPath;
                            return;
                        }
                    } catch (_) {}
                    if (resultEl) resultEl.textContent = text;
                }
            } catch (err) {
                const resultEl = document.getElementById('loginResult');
                if (resultEl) resultEl.textContent = 'Lỗi: ' + err;
            } finally {
                if (btn) btn.disabled = false;
            }
        });
    }

    // 📌 Submit Register
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const btn = document.getElementById('regBtn');
            if (btn) btn.disabled = true;

            const payload = {
                email: document.getElementById('regEmail')?.value || '',
                password: document.getElementById('regPassword')?.value || '',
                role: document.getElementById('regRole')?.value || 'USER',
                phone: document.getElementById('regPhone')?.value || '',
                organization: document.getElementById('regOrg')?.value || ''
            };

            try {
                const res = await fetch('/api/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                const text = await res.text();
                const resultEl = document.getElementById('registerResult');
                if (!res.ok) {
                    if (resultEl) resultEl.textContent = `Lỗi đăng ký (${res.status}):\n${text}`;
                } else {
                    // Sau khi đăng ký thành công → quay lại trang login
                    window.location.href = "/login?registered=1";
                }
            } catch (err) {
                const resultEl = document.getElementById('registerResult');
                if (resultEl) resultEl.textContent = 'Lỗi: ' + err;
            } finally {
                if (btn) btn.disabled = false;
            }
        });
    }
});
