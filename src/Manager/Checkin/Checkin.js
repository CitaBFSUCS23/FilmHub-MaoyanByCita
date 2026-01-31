// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 检查用户是否已登录
    const userEmail = sessionStorage.getItem('userEmail');
    if (!userEmail) {
        showMessage('请先登录！', 'error');
        setTimeout(() => {
            window.location.href = '/General/GLogin/GLogin.html';
        }, 1000);
        return;
    }
    
    // 初始化表单提交事件
    initFormSubmit();
});

// 初始化表单提交事件
function initFormSubmit() {
    const form = document.getElementById('checkinForm');
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        handleCheckin();
    });
}

// 处理验票请求
function handleCheckin() {
    const orderNumber = document.getElementById('orderNumber').value;
    const userEmail = sessionStorage.getItem('userEmail');
    
    // 准备请求数据
    const requestData = { orderNumber: orderNumber };
    
    // 发送验票请求
    fetch('/Checkin', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-User-Email': userEmail
        },
        body: JSON.stringify(requestData)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('网络请求失败');
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            showMessage(data.message, 'success');
            // 清空输入框
            document.getElementById('orderNumber').value = '';
        } else {
            showMessage(data.message, 'error');
        }
    })
    .catch(error => {
        showMessage('验票失败，请稍后重试', 'error');
    });
}

