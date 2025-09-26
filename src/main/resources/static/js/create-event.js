document.getElementById('uploadBtn').onclick = function () {
    document.getElementById('uploadInput').click();
};

document.getElementById('uploadInput').onchange = function (e) {
    const preview = document.getElementById('preview');
    preview.innerHTML = '';
    Array.from(e.target.files).forEach(file => {
        if (file.type.startsWith('image/')) {
            const img = document.createElement('img');
            img.src = URL.createObjectURL(file);
            img.style.maxWidth = '120px';
            img.style.margin = '8px';
            img.style.borderRadius = '8px';
            preview.appendChild(img);
        }
        // Có thể thêm xử lý video nếu muốn
    });
};