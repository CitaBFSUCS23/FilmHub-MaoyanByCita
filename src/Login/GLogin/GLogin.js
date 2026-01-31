// GLogin页面JavaScript逻辑

document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
    
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
    
    // 移除了邮箱输入框自动跳转功能，允许用户自由输入
}

// 处理登录表单提交
function handleLogin(event) {
    event.preventDefault();
    
    const emailLocal = document.getElementById('email-local').value;
    const emailDomain = document.getElementById('email-domain').value;
    const password = document.getElementById('password').value;
    
    // 基本表单验证
    if (!validateForm(emailLocal, emailDomain, password)) {
        return;
    }
    
    // 发送登录请求
    loginUser(emailLocal, emailDomain, password);
}

// 表单验证
function validateForm(emailLocal, emailDomain, password) {
    hideError();
    
    if (!emailLocal || !emailDomain) {
        showMessage('请输入完整的邮箱地址', 'error');
        return false;
    }
    
    if (!password || password.length < 6 || password.length > 16) {
        showMessage('密码长度应为6-16位字符', 'error');
        return false;
    }
    
    return true;
}

// 发送登录请求
function loginUser(emailLocal, emailDomain, password) {
    const email = emailLocal + '@' + emailDomain;
    
    // 显示加载状态
    const loginBtn = document.querySelector('.login-btn');
    const originalText = loginBtn.textContent;
    loginBtn.textContent = '登录中...';
    loginBtn.disabled = true;
    
    fetch('/GLogin', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
            'email-local': emailLocal,
            'email-domain': emailDomain,
            'password': password
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            // 登录成功前先清除所有sessionStorage数据，避免之前的角色信息残留
            sessionStorage.clear();
            
            // 保存新的用户信息到sessionStorage
            sessionStorage.setItem('userEmail', email);
            sessionStorage.setItem('currentUser', email);
            
            // 设置新的userRoles字段，支持多角色
            const roles = ['general'];
            sessionStorage.setItem('userRoles', JSON.stringify(roles));
            
            // 跳转到AboutAccount页面
            window.location.href = '../../General/AboutAccount/AboutAccount.html';
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

// 显示错误信息


// 隐藏错误信息
function hideError() {
    const errorElement = document.getElementById('errorMessage');
    if (errorElement) {
        errorElement.style.display = 'none';
    }
}