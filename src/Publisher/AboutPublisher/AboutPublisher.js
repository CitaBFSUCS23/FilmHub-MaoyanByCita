// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    // 从sessionStorage获取发行商ID
    const publisherId = sessionStorage.getItem('publisherId');
    
    // 从Servlet获取发行商信息
    fetch('/AboutPublisher?action=info', {
        headers: {
            'X-Publisher-Id': publisherId
        }
    })
        .then(response => response.json())
        .then(data => {
        // 检查是否有错误
        if (data.status === 'error') {
            return;
        }
        
        // 更新页面中的发行商信息
        document.getElementById('publisherId').value = data.publisherId || '';
        document.getElementById('publisherName').value = data.publisherName || '';
        
        // 存储原始发行商信息到隐藏字段
        document.getElementById('originalPublisherName').value = data.publisherName || '';
        document.getElementById('originalPublisherNationality').value = data.publisherNationality || '';
        
        // 填充国籍下拉列表
        const nationalitySelect = document.getElementById('publisherNationality');
        nationalitySelect.innerHTML = '';
        
        // 确保nationalities存在且是数组
        if (data.nationalities && Array.isArray(data.nationalities)) {
            data.nationalities.forEach(nationality => {
                const option = document.createElement('option');
                option.value = nationality;
                option.textContent = nationality;
                if (nationality === data.publisherNationality) {
                    option.selected = true;
                }
                nationalitySelect.appendChild(option);
            });
        }
    })
    
    // 密码修改按钮事件
    document.getElementById('changePasswordBtn').addEventListener('click', function() {
        var passwordFields = document.getElementById('passwordFields');
        var newPasswordInput = document.getElementById('newPassword');
        var confirmPasswordInput = document.getElementById('confirmPassword');
        
        if (passwordFields.style.display === 'none') {
            passwordFields.style.display = 'block';
            this.textContent = '取消修改';
            newPasswordInput.required = true;
            confirmPasswordInput.required = true;
        } else {
            passwordFields.style.display = 'none';
            this.textContent = '修改密码';
            newPasswordInput.value = '';
            confirmPasswordInput.value = '';
            newPasswordInput.required = false;
            confirmPasswordInput.required = false;
        }
    });
    
    // 表单提交处理
    document.getElementById('publisherForm').addEventListener('submit', function(e) {
        e.preventDefault(); // 阻止默认表单提交
        
        // 表单验证
        var passwordFields = document.getElementById('passwordFields');
        var newPassword = document.getElementById('newPassword').value;
        var confirmPassword = document.getElementById('confirmPassword').value;
        
        if (passwordFields.style.display !== 'none') {
            if (newPassword === '' || confirmPassword === '') {
                showMessage('请输入密码', 'error');
                return false;
            }
            if (newPassword !== confirmPassword) {
                showMessage('两次输入的密码不一致', 'error');
                return false;
            }
        }
        
        // 获取发行商ID
        const publisherId = sessionStorage.getItem('publisherId');
        if (!publisherId) {
            showMessage('请先登录', 'error');
            return false;
        }
        
        // 准备表单数据
        const formData = new URLSearchParams();
        formData.append('publisherName', document.getElementById('publisherName').value);
        formData.append('publisherNationality', document.getElementById('publisherNationality').value);
        formData.append('newPassword', document.getElementById('newPassword').value);
        formData.append('originalPublisherName', document.getElementById('originalPublisherName').value);
        formData.append('originalPublisherNationality', document.getElementById('originalPublisherNationality').value);
        
        // 发送请求
        fetch('/AboutPublisher', {
            method: 'POST',
            headers: {
                'X-Publisher-Id': publisherId,
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.status === 'success' || !data.status) {
                showMessage(data.message, 'success');
                
                // 页面刷新
                window.location.reload();
            } else {
                showMessage(data.message, 'error');
            }
        })
        .catch(error => {
            console.error('更新失败:', error);
            showMessage('更新失败，请稍后重试', 'error');
        });
    });
});