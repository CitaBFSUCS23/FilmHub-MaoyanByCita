// DeleteAccount页面JavaScript逻辑

document.addEventListener('DOMContentLoaded', function() {
    // 检查登录状态
    if (!checkLoginStatus()) {
        return;
    }
    
    // 获取DOM元素
    const confirmInput = document.getElementById('confirmText');
    const deleteBtn = document.getElementById('deleteBtn');
    const cancelBtn = document.getElementById('cancelBtn');
    const deleteForm = document.getElementById('deleteForm');
    const daysRegistered = document.getElementById('daysRegistered');
    
    // 初始化页面
    initPage();
    
    // 输入验证
    confirmInput.addEventListener('input', function() {
        validateInput();
    });
    
    // 表单提交处理
    deleteForm.addEventListener('submit', function(e) {
        e.preventDefault();
        handleDeleteAccount();
    });
    
    // 取消按钮点击事件
    cancelBtn.addEventListener('click', function() {
        window.location.href = '../AboutAccount/AboutAccount.html';
    });
    
    /**
     * 初始化页面
     */
    function initPage() {
        // 加载用户注册天数
        loadUserRegistrationDays();
        
        // 初始验证
        validateInput();
    }
    
    /**
     * 加载用户注册天数
     */
    function loadUserRegistrationDays() {
        const userEmail = sessionStorage.getItem('userEmail');
        
        if (!userEmail) {
            showMessage('无法获取用户信息，请重新登录', 'error');
            return;
        }
        
        // 发送请求获取用户信息
        fetch('../../AboutAccount', {
            method: 'GET',
            headers: {
                'X-User-Email': userEmail
            }
        })
        .then(response => response.json())
        .then(data => {
            if (data.status === 'success') {
                const createTime = data.createTime;
                if (createTime) {
                    const days = calculateDaysRegistered(createTime);
                    daysRegistered.textContent = days;
                }
            } else {
                showMessage('无法获取用户信息', 'error');
            }
        })
        .catch(error => {
            console.error('Error loading user info:', error);
            showMessage('网络错误，无法获取用户信息', 'error');
        });
    }
    
    /**
     * 计算注册天数
     */
    function calculateDaysRegistered(createTime) {
        const createDate = new Date(createTime);
        const currentDate = new Date();
        const diffTime = currentDate - createDate;
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        return diffDays > 0 ? diffDays : 1;
    }
    
    /**
     * 验证输入
     */
    function validateInput() {
        const isValid = confirmInput.value === '我已了解风险，确认注销';
        deleteBtn.disabled = !isValid;
        
        if (isValid) {
            confirmInput.classList.add('valid');
            confirmInput.classList.remove('invalid');
        } else {
            confirmInput.classList.add('invalid');
            confirmInput.classList.remove('valid');
        }
    }
    
    /**
     * 处理账户删除
     */
    function handleDeleteAccount() {
        const userEmail = sessionStorage.getItem('userEmail');
        
        if (!userEmail) {
            showMessage('无法获取用户信息，请重新登录', 'error');
            return;
        }
        
        // 确认对话框
        if (!confirm('确定要删除账户吗？此操作不可撤销！')) {
            return;
        }
        
        // 显示加载状态
        setLoadingState(true);
        
        // 发送删除请求
        fetch('../../DeleteAccountServlet', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            },
            body: JSON.stringify({
                action: 'delete'
            })
        })
        .then(response => response.json())
        .then(data => {
            setLoadingState(false);
            
            if (data.status === 'success') {
                // 删除成功，清除sessionStorage并跳转到登录页面
                sessionStorage.clear();
                showMessage('账号已注销。期待与您的再会', 'success');
                
                // 延迟跳转，让用户看到成功消息
                setTimeout(() => {
                    window.location.href = '../../Login/GLogin/GLogin.html';
                }, 2000);
            } else {
                showMessage(data.message, 'error');
            }
        })
        .catch(error => {
            setLoadingState(false);
            console.error('Error deleting account:', error);
            showMessage('网络错误，删除失败', 'error');
        });
    }
    
    /**
     * 设置加载状态
     */
    function setLoadingState(isLoading) {
        if (isLoading) {
            deleteForm.classList.add('loading');
            deleteBtn.disabled = true;
            deleteBtn.textContent = '删除中...';
        } else {
            deleteForm.classList.remove('loading');
            deleteBtn.disabled = !confirmInput.validity.valid;
            deleteBtn.textContent = '确认删除';
        }
    }
    
    /**
     * 显示消息
     */

});