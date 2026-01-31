// 影院信息管理页面逻辑

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    loadCinemaInfo();
    setupEventListeners();
});

// 设置事件监听器
function setupEventListeners() {
    // 表单提交事件
    const cinemaForm = document.getElementById('cinemaForm');
    if (cinemaForm) {
        cinemaForm.addEventListener('submit', handleFormSubmit);
    }
    
    // 图片预览事件
    const cinemaImageInput = document.getElementById('cinemaImage');
    if (cinemaImageInput) {
        cinemaImageInput.addEventListener('change', handleCinemaImageChange);
    }
    
    // 添加管理员表单提交事件
    const addManagerForm = document.getElementById('addManagerForm');
    if (addManagerForm) {
        addManagerForm.addEventListener('submit', handleAddManager);
    }
}

// 添加影院信息参数到表单数据
function addCinemaInfoToFormData(formData) {
    const cinemaProvinceCode = sessionStorage.getItem('cinemaProvinceCode');
    const cinemaCityCode = sessionStorage.getItem('cinemaCityCode');
    const cinemaId = sessionStorage.getItem('cinemaId');
    
    formData.append('cinemaProvinceCode', cinemaProvinceCode);
    formData.append('cinemaCityCode', cinemaCityCode);
    formData.append('cinemaId', cinemaId);
    
    return formData;
}

// 加载影院信息
function loadCinemaInfo() {
    // 从sessionStorage获取影院信息
    const cinemaProvinceCode = sessionStorage.getItem('cinemaProvinceCode');
    const cinemaCityCode = sessionStorage.getItem('cinemaCityCode');
    const cinemaId = sessionStorage.getItem('cinemaId');
    
    fetch(`/AboutCinema?cinemaProvinceCode=${cinemaProvinceCode}&cinemaCityCode=${cinemaCityCode}&cinemaId=${cinemaId}`)
        .then(response => response.json())
        .then(data => {
            if (data.status === 'success') {
                displayCinemaInfo(data.cinemaInfo);
                loadManagerList();
            } else {
                showMessage(data.message, 'error');
            }
        })
}

// 显示影院信息
function displayCinemaInfo(cinemaInfo) {
    // 设置影院基本信息
    document.getElementById('cinemaProvinceCode').value = cinemaInfo.cinemaProvinceCode || '';
    document.getElementById('cinemaCityCode').value = cinemaInfo.cinemaCityCode || '';
    document.getElementById('cinemaId').value = cinemaInfo.cinemaId || '';
    document.getElementById('cinemaName').value = cinemaInfo.cinemaName || '';
    document.getElementById('cinemaAddress').value = cinemaInfo.cinemaAddress || '';
    
    // 设置影院图片
    const previewImg = document.getElementById('cinema-image-preview');
    if (previewImg && cinemaInfo.cinemaImagePath) {
        previewImg.src = cinemaInfo.cinemaImagePath;
        previewImg.onerror = function() {
            this.src = ''; // 如果图片加载失败，清空src
        };
    }
}

// 处理表单提交
function handleFormSubmit(event) {
    event.preventDefault();
    
    const form = document.getElementById('cinemaForm');
    const cinemaImageInput = document.getElementById('cinemaImage');
    
    // 显示加载状态
    const submitBtn = document.querySelector('.submit-btn');
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 保存中...';
    submitBtn.disabled = true;
    
    // 检查是否有图片文件需要上传
    if (cinemaImageInput.files && cinemaImageInput.files[0]) {
        // 使用FormData处理文件上传
        const formData = new FormData(form);
        
        // 添加影院信息到FormData
        addCinemaInfoToFormData(formData);
        
        fetch('/AboutCinema', {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.status === 'success') {
                showMessage(data.message, 'success');
                
                // 刷新页面以确保显示最新的图片
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                showMessage(data.message, 'error');
            }
        })
        .finally(() => {
            // 恢复按钮状态
            submitBtn.innerHTML = originalText;
            submitBtn.disabled = false;
        });
    } else {
        // 没有图片，使用普通表单提交
        const formData = new FormData(form);
        
        // 添加影院信息到表单数据
        addCinemaInfoToFormData(formData);
        
        // 直接构建URLSearchParams，包含所有参数
        const params = new URLSearchParams();
        
        // 添加表单原有参数
        for (const [key, value] of formData.entries()) {
            params.append(key, value);
        }
        
        fetch('/AboutCinema', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: params
        })
        .then(response => response.json())
        .then(data => {
            if (data.status === 'success') {
                showMessage(data.message, 'success');
                // 刷新页面以确保显示最新的信息和图片
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                showMessage(data.message, 'error');
            }
        })
        .finally(() => {
            // 恢复按钮状态
            submitBtn.innerHTML = originalText;
            submitBtn.disabled = false;
        });
    }
}

// 影院图片实时预览
function handleCinemaImageChange() {
    const fileInput = document.getElementById('cinemaImage');
    const previewImg = document.getElementById('cinema-image-preview');
    
    if (fileInput.files && fileInput.files[0]) {
        const file = fileInput.files[0];
        
        // 检查文件类型
        if (!file.type.startsWith('image/jpeg')) {
            showMessage('请选择JPG格式的图片', 'error');
            fileInput.value = '';
            return;
        }
        
        const reader = new FileReader();
        
        reader.onload = function(event) {
            previewImg.src = event.target.result;
        };
        
        reader.readAsDataURL(file);
    }
}

// 加载管理人员列表
function loadManagerList() {
    // 从sessionStorage获取影院信息
    const cinemaProvinceCode = sessionStorage.getItem('cinemaProvinceCode');
    const cinemaCityCode = sessionStorage.getItem('cinemaCityCode');
    const cinemaId = sessionStorage.getItem('cinemaId');
    
    // 通过AboutCinema接口获取影院信息，其中包含管理人员列表
    fetch(`/AboutCinema?cinemaProvinceCode=${cinemaProvinceCode}&cinemaCityCode=${cinemaCityCode}&cinemaId=${cinemaId}`)
        .then(response => response.json())
        .then(data => {
            if (data.status === 'success') {
                // 显示管理人员区域
                document.getElementById('managerSection').style.display = 'block';
                
                // 获取管理人员列表并显示
                const managers = data.cinemaInfo.managers || [];
                displayManagerList(managers);
            } else {

            }
        })
}

// 显示管理人员列表
function displayManagerList(managers) {
    const managerList = document.getElementById('managerList');
    if (!managerList) return;
    
    managerList.innerHTML = '';
    
    managers.forEach(manager => {
        const row = document.createElement('tr');
        
        const emailCell = document.createElement('td');
        emailCell.textContent = manager.email;
        
        const actionCell = document.createElement('td');
        
        // 获取当前用户邮箱
        const currentUserEmail = sessionStorage.getItem('userEmail') || '';
        
        if (manager.email === currentUserEmail) {
            // 当前用户，显示不可删除
            const span = document.createElement('span');
            span.textContent = '不可删除自己';
            span.style.color = 'gray';
            actionCell.appendChild(span);
        } else {
            // 其他管理员，显示删除按钮
            const deleteBtn = document.createElement('button');
            deleteBtn.textContent = '删除';
            deleteBtn.className = 'delete-btn';
            deleteBtn.onclick = function() {
                deleteManager(manager.email);
            };
            actionCell.appendChild(deleteBtn);
        }
        
        row.appendChild(emailCell);
        row.appendChild(actionCell);
        managerList.appendChild(row);
    });
}

// 添加管理员
function handleAddManager(event) {
    event.preventDefault();
    
    const emailLocalInput = document.getElementById('email-local');
    const emailDomainInput = document.getElementById('email-domain');
    
    const emailLocal = emailLocalInput.value.trim();
    const emailDomain = emailDomainInput.value.trim();
    
    if (!emailLocal || !emailDomain) {
        showMessage('请输入完整的邮箱地址', 'error');
        return;
    }
    
    const email = emailLocal + '@' + emailDomain;
    
    const submitBtn = event.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 添加中...';
    submitBtn.disabled = true;
    
    const formData = new FormData();
    formData.append('action', 'addManager');
    formData.append('email', email);
    
    // 添加影院信息到表单数据
    addCinemaInfoToFormData(formData);
    
    fetch('/AboutCinema', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams(formData)
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            showMessage(data.message, 'success');
            emailLocalInput.value = '';
            emailDomainInput.value = '';
            // 重新加载管理人员列表
            loadManagerList();
        } else {
            showMessage(data.message, 'error');
        }
    })
    .finally(() => {
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
    });
}

// 删除管理员
function deleteManager(email) {
    if (!confirm('确定要删除该管理员吗？')) {
        return;
    }
    
    const formData = new FormData();
    formData.append('action', 'deleteManager');
    formData.append('email', email);
    
    // 添加影院信息到表单数据
    addCinemaInfoToFormData(formData);
    
    fetch('/AboutCinema', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams(formData)
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            showMessage(data.message, 'success');
            // 重新加载管理人员列表
            loadManagerList();
        } else {
            showMessage(data.message, 'error');
        }
    })
}



// 页面加载后延迟刷新图片，确保获取最新版本
window.addEventListener('load', function() {
    setTimeout(function() {
        const cinemaImage = document.getElementById('cinema-image-preview');
        if (cinemaImage && cinemaImage.src) {
            const currentSrc = cinemaImage.src;
            // 移除可能存在的旧时间戳
            const baseUrl = currentSrc.split('?')[0];
            // 添加新的时间戳
            cinemaImage.src = baseUrl + '?' + new Date().getTime();
        }
    }, 1000);
});