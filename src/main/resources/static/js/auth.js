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

            const email = document.getElementById('loginEmail')?.value || '';
            const password = document.getElementById('loginPassword')?.value || '';
            const redirectUrl = getQueryParam('redirect') || getQueryParam('redirectUrl') || '';

            const payload = {
                email: email,
                password: password
            };

            try {
                const res = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify(payload)
                });

                if (!res.ok) {
                    // Handle error response - display error below email input
                    let errorMessage = 'Đăng nhập thất bại';
                    try {
                        const errorData = await res.json();
                        errorMessage = errorData.error || 'Đăng nhập thất bại';
                    } catch (parseError) {
                        // If JSON parsing fails, try to get text
                        try {
                            const errorText = await res.text();
                            errorMessage = errorText || 'Đăng nhập thất bại';
                        } catch (textError) {
                            errorMessage = 'Đăng nhập thất bại. Vui lòng thử lại.';
                        }
                    }
                    
                    // Display error message below email input
                    displayLoginError(errorMessage);
                } else {
                    // Handle success response
                    try {
                        const json = await res.json();
                        
                        // Priority 1: Check for redirect parameter in URL
                        if (redirectUrl) {
                            alert('✓ Đăng nhập thành công! Đang chuyển hướng...');
                            // Use window.location.replace to ensure session is maintained
                            setTimeout(() => {
                                window.location.replace(redirectUrl);
                            }, 500);
                            return;
                        }
                        
                        // Priority 2: Use redirectPath from API response
                        if (json.redirectPath) {
                            alert('✓ Đăng nhập thành công! Đang chuyển hướng...');
                            setTimeout(() => {
                                window.location.replace(json.redirectPath);
                            }, 500);
                            return;
                        }
                        
                        // Fallback: reload page to sync session, then redirect to home
                        alert('✓ Đăng nhập thành công!');
                        // Reload page first to ensure session is synced
                        setTimeout(() => {
                            window.location.reload();
                        }, 500);
                    } catch (parseError) {
                        alert('✓ Đăng nhập thành công!');
                        setTimeout(() => {
                            window.location.href = '/';
                        }, 500);
                    }
                }
            } catch (err) {
                displayLoginError('Lỗi kết nối: ' + err.message);
            } finally {
                if (btn) btn.disabled = false;
            }
        });
    }
    
    // Function to display login error below email input
    function displayLoginError(message) {
        // Remove existing error message if any
        const existingError = document.getElementById('loginError');
        if (existingError) {
            existingError.remove();
        }
        
        // Create error message element
        const errorDiv = document.createElement('div');
        errorDiv.id = 'loginError';
        errorDiv.style.color = 'red';
        errorDiv.style.fontSize = '14px';
        errorDiv.style.marginTop = '-10px';
        errorDiv.style.marginBottom = '10px';
        errorDiv.style.paddingLeft = '5px';
        errorDiv.innerHTML = '<i class=\'bx bx-error-circle\'></i> ' + message;
        
        // Find email input box and insert error message after it
        const emailInput = document.getElementById('loginEmail');
        if (emailInput) {
            const emailInputBox = emailInput.closest('.input-box');
            if (emailInputBox && emailInputBox.parentNode) {
                // Insert after the email input box
                emailInputBox.parentNode.insertBefore(errorDiv, emailInputBox.nextSibling);
            }
        }
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
                phoneNumber: document.getElementById('regPhone')?.value || '',
            };

            try {
                const res = await fetch('/api/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                const resultEl = document.getElementById('registerResult');
                
                if (!res.ok) {
                    // Handle error response
                    try {
                        const errorData = await res.json();
                        const errorMessage = errorData.error || 'Đăng ký thất bại';
                        alert('✗ ' + errorMessage);
                        if (resultEl) {
                            resultEl.textContent = errorMessage;
                            resultEl.style.color = 'red';
                        }
                    } catch (parseError) {
                        // If JSON parsing fails, try to get text
                        try {
                            const errorText = await res.text();
                            alert('✗ ' + (errorText || 'Đăng ký thất bại'));
                            if (resultEl) {
                                resultEl.textContent = errorText || 'Đăng ký thất bại';
                                resultEl.style.color = 'red';
                            }
                        } catch (textError) {
                            alert('✗ Đăng ký thất bại. Vui lòng thử lại.');
                        }
                    }
                } else {
                    // Handle success response
                    alert('✓ Đăng ký thành công! Đang chuyển hướng đến trang đăng nhập...');
                    if (resultEl) {
                        resultEl.textContent = 'Đăng ký thành công! Đang chuyển hướng đến trang đăng nhập...';
                        resultEl.style.color = 'green';
                    }
                    setTimeout(() => {
                        window.location.href = "/login?registered=1";
                    }, 1500);
                }
            } catch (err) {
                alert('✗ Lỗi kết nối: ' + err.message);
                const resultEl = document.getElementById('registerResult');
                if (resultEl) {
                    resultEl.textContent = 'Lỗi kết nối: ' + err.message;
                    resultEl.style.color = 'red';
                }
            } finally {
                if (btn) btn.disabled = false;
            }
        });
    }
});
