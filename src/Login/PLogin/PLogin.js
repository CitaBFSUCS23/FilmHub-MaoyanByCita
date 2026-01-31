// PLogin页面JavaScript逻辑

document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
    loadUserEmail();
    
    // 检查URL参数中的错误信息
    const urlParams = new URLSearchParams(window.location.search);
    const error = urlParams.get('error');
    if (error) {
        showMessage(error, 'error');
    }
});

// 设置事件监听器
function setupEventListeners() {
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }
}

// 加载当前用户邮箱
function loadUserEmail() {
    const userEmail = sessionStorage.getItem('userEmail');
    if (userEmail) {
        document.getElementById('email').value = userEmail;
    } else {
        // 如果没有用户邮箱，跳转到GLogin页面
        window.location.href = '../GLogin/GLogin.html';
    }
}

// 处理登录表单提交
function handleLogin(event) {
    event.preventDefault();
    
    const publisherId = document.getElementById('publisherId').value;
    const password = document.getElementById('password').value;
    
    // 发送登录请求
    loginPublisher(publisherId, password);
}

// 发送登录请求
function loginPublisher(publisherId, password) {
    // 显示加载状态
    const loginBtn = document.querySelector('.login-btn');
    const originalText = loginBtn.textContent;
    loginBtn.textContent = '验证中...';
    loginBtn.disabled = true;
    
    // 获取用户邮箱
    const userEmail = sessionStorage.getItem('userEmail');
    
    fetch('/PLogin', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-User-Email': userEmail
        },
        body: new URLSearchParams({
            'publisherId': publisherId,
            'password': password
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            // 登录成功，保留现有的角色信息，只清除与发行商相关的旧数据
            sessionStorage.removeItem('publisherId');
            
            // 获取现有角色，如果没有则使用默认值
            let existingRoles = JSON.parse(sessionStorage.getItem('userRoles')) || [];
            
            // 添加新角色，避免重复
            if (!existingRoles.includes('general')) {
                existingRoles.push('general');
            }
            if (!existingRoles.includes('publisher')) {
                existingRoles.push('publisher');
            }
            
            // 设置合并后的角色信息
            sessionStorage.setItem('userRoles', JSON.stringify(existingRoles));
            sessionStorage.setItem('publisherId', publisherId);
            
            // 跳转到AboutPublisher页面
            window.location.href = '../../Publisher/AboutPublisher/AboutPublisher.html';
        } else {
            showMessage(data.message, 'error');
        }
    })
    .finally(() => {
        // 恢复按钮状态
        loginBtn.textContent = originalText;
        loginBtn.disabled = false;
    });
}

// 隐藏错误信息
function hideError() {
    const errorElement = document.getElementById('errorMessage');
    if (errorElement) {
        errorElement.style.display = 'none';
    }
}