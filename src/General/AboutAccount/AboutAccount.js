// AboutAccount页面JavaScript逻辑

document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
    loadUserInfo();
    loadProvinces();
});

// 设置事件监听器
function setupEventListeners() {
    // 头像上传表单
    const avatarForm = document.getElementById('avatarForm');
    if (avatarForm) {
        avatarForm.addEventListener('submit', handleAvatarUpload);
    }
    
    // 个人信息更新按钮
    const updateBtn = document.querySelector('.update-btn');
    if (updateBtn) {
        updateBtn.addEventListener('click', handleInfoUpdate);
    }
    
    // 密码修改表单
    const passwordForm = document.getElementById('passwordForm');
    if (passwordForm) {
        passwordForm.addEventListener('submit', handlePasswordChange);
    }
    
    // 头像预览
    const avatarInput = document.getElementById('avatar');
    if (avatarInput) {
        avatarInput.addEventListener('change', handleAvatarPreview);
    }
    
    // 省份选择变化时加载城市
    const provinceSelect = document.getElementById('province');
    if (provinceSelect) {
        provinceSelect.addEventListener('change', loadCities);
    }
}

// 加载用户信息
function loadUserInfo() {
    // 获取用户邮箱
    const userEmail = sessionStorage.getItem('userEmail');
    
    fetch('/AboutAccount', {
        headers: {
            'X-User-Email': userEmail
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            // 填充用户信息
            document.getElementById('email').value = data.email || '';
            document.getElementById('nickname').value = data.nickname || '';
            document.getElementById('gender').value = data.gender || 'U';
            document.getElementById('tel').value = data.tel || '';
            document.getElementById('self-intro').value = data.selfIntro || '';
            document.getElementById('wallet').textContent = '¥' + (data.wallet || '0.00');
            document.getElementById('create-time').textContent = data.createTime || '';
            
            // 设置头像
            const avatarPreview = document.getElementById('avatar-preview');
            if (data.email) {
                avatarPreview.src = '/uploads/avatars/' + data.email + '.jpg';
            }
            
            // 设置省市选择
            if (data.provinceCode) {
                setTimeout(() => {
                    const provinceSelect = document.getElementById('province');
                    provinceSelect.value = data.provinceCode;
                    loadCities();
                    
                    // 设置城市选择
                    setTimeout(() => {
                        const citySelect = document.getElementById('city');
                        if (data.cityCode) {
                            citySelect.value = data.cityCode;
                        }
                    }, 500);
                }, 500);
            }
        } else {
            showMessage('无法加载用户信息，可能需要重新登录', 'error');
        }
    })
}

// 处理头像上传
function handleAvatarUpload(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    // 获取用户邮箱
    const userEmail = sessionStorage.getItem('userEmail');
    
    // 显示加载状态
    const uploadBtn = document.getElementById('upload-btn');
    const originalText = uploadBtn.textContent;
    uploadBtn.textContent = '上传中...';
    uploadBtn.disabled = true;
    
    fetch('/AboutAccount', {
        method: 'POST',
        headers: {
            'X-User-Email': userEmail
        },
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            showMessage(data.message, 'success');
            
            // 更新头像预览
            const avatarPreview = document.getElementById('avatar-preview');
            const userEmail = sessionStorage.getItem('userEmail');
            avatarPreview.src = '/uploads/avatars/' + userEmail + '.jpg';
            // 隐藏上传按钮
            uploadBtn.style.display = 'none';
            
            // 刷新页面以确保所有信息都已更新
            setTimeout(() => {
                window.location.reload();
            }, 1000);
        } else {
            showMessage(data.message, 'error');
        }
    })
    .finally(() => {
        // 恢复按钮状态
        uploadBtn.textContent = originalText;
        uploadBtn.disabled = false;
    });
}

// 处理个人信息更新
function handleInfoUpdate(event) {
    event.preventDefault();
    
    // 手动收集表单数据
    const params = new URLSearchParams();
    params.append('action', 'update');
    params.append('nickname', document.getElementById('nickname').value);
    params.append('gender', document.getElementById('gender').value);
    params.append('tel', document.getElementById('tel').value);
    params.append('self-intro', document.getElementById('self-intro').value);
    params.append('province', document.getElementById('province').value);
    params.append('city', document.getElementById('city').value);
    
    // 获取用户邮箱
    const userEmail = sessionStorage.getItem('userEmail');
    
    // 显示加载状态
    const updateBtn = document.querySelector('.update-btn');
    const originalText = updateBtn.textContent;
    updateBtn.textContent = '更新中...';
    updateBtn.disabled = true;
    
    fetch('/AboutAccount', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-User-Email': userEmail
        },
        body: params
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            showMessage(data.message, 'success');
        } else {
            showMessage(data.message, 'error');
        }
    })
    .finally(() => {
        // 恢复按钮状态
        updateBtn.textContent = originalText;
        updateBtn.disabled = false;
    });
}

// 处理密码修改
function handlePasswordChange(event) {
    event.preventDefault();
    
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    
    if (!validatePasswordForm()) {
        return;
    }
    
    const formData = new FormData(event.target);
    const params = new URLSearchParams(formData);
    params.append('action', 'changePassword');
    // 获取用户邮箱
    const userEmail = sessionStorage.getItem('userEmail');
    
    // 显示加载状态
    const submitBtn = event.target.querySelector('.submit-btn');
    const originalText = submitBtn.textContent;
    submitBtn.textContent = '提交中...';
    submitBtn.disabled = true;
    
    fetch('/AboutAccount', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-User-Email': userEmail
        },
        body: params
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            showMessage(data.message, 'success');
            hidePasswordForm();
            
            // 清空密码输入框
            document.getElementById('newPassword').value = '';
            document.getElementById('confirmPassword').value = '';
        } else {
            showMessage(data.message, 'error');
        }
    })
    .finally(() => {
        // 恢复按钮状态
        submitBtn.textContent = originalText;
        submitBtn.disabled = false;
    });
}

// 头像预览
function handleAvatarPreview(event) {
    const file = event.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            document.getElementById('avatar-preview').src = e.target.result;
            
            // 显示上传按钮
            document.getElementById('upload-btn').style.display = 'block';
        };
        reader.readAsDataURL(file);
    }
}

// 加载省份数据
function loadProvinces() {
    fetch('/ProvinceCity')
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            const provinceSelect = document.getElementById('province');
            provinceSelect.innerHTML = '';
            
            data.provinces.forEach(province => {
                const option = document.createElement('option');
                option.value = province.code;
                option.textContent = province.name;
                provinceSelect.appendChild(option);
            });
        }
    })
}

// 加载城市数据
function loadCities() {
    const provinceSelect = document.getElementById('province');
    const provinceCode = provinceSelect.value;
    
    if (!provinceCode) {
        const citySelect = document.getElementById('city');
        citySelect.innerHTML = '';
        return;
    }
    
    fetch(`/ProvinceCity?provinceCode=${provinceCode}`)
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            const citySelect = document.getElementById('city');
            citySelect.innerHTML = '';
            
            if (data.cities && data.cities.length > 0) {
                data.cities.forEach(city => {
                    const option = document.createElement('option');
                    option.value = city.code;
                    option.textContent = city.name;
                    citySelect.appendChild(option);
                });
            }
        }
    })
}

// 切换密码修改表单显示
function togglePasswordForm() {
    const passwordSection = document.getElementById('change-password-section');
    passwordSection.classList.toggle('hidden');
}

// 隐藏密码修改表单
function hidePasswordForm() {
    document.getElementById('change-password-section').classList.add('hidden');
}

// 验证密码表单
function validatePasswordForm() {
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    
    if (newPassword !== confirmPassword) {
        showMessage('两次输入的密码不一致', 'error');
        return false;
    }
    
    return true;
}

// 重置表单
function resetForm() {
    loadUserInfo();
    showMessage('表单已重置', 'success');
}

// 显示错误信息


// 显示成功信息
