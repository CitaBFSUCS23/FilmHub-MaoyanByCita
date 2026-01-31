// Register页面JavaScript逻辑

let currentStep = 1;
let verifiedEmail = '';

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
    
    // 从URL参数获取步骤
    const urlParams = new URLSearchParams(window.location.search);
    const step = urlParams.get('step');
    if (step) {
        goToStep(parseInt(step));
    }
    
    // 加载省份数据
    loadProvinces();
});

// 设置事件监听器
function setupEventListeners() {
    // 邮箱验证按钮
    const verifyBtn = document.getElementById('verifyBtn');
    if (verifyBtn) {
        verifyBtn.addEventListener('click', verifyEmail);
    }
    
    // 注册表单提交
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', handleRegister);
    }
    
    // 返回按钮
    const backBtn = document.getElementById('backBtn');
    if (backBtn) {
        backBtn.addEventListener('click', function() {
            goToStep(1);
        });
    }
    
    // 邮箱输入框变化时重置验证状态
    const emailLocal = document.getElementById('email-local');
    const emailDomain = document.getElementById('email-domain');
    
    if (emailLocal && emailDomain) {
        emailLocal.addEventListener('input', resetEmailVerification);
        emailDomain.addEventListener('input', resetEmailVerification);
    }
    
    // 省份选择变化时加载城市
    const provinceSelect = document.getElementById('province');
    if (provinceSelect) {
        provinceSelect.addEventListener('change', loadCities);
    }
}

// 重置邮箱验证状态
function resetEmailVerification() {
    verifiedEmail = '';
}

// 跳转到指定步骤
function goToStep(step) {
    // 如果是通过URL参数直接进入第三步（注册成功），跳过邮箱验证
    const urlParams = new URLSearchParams(window.location.search);
    const directStep = urlParams.get('step');
    
    // 只有在已经验证邮箱的情况下才能进入第二步
    if (step > 1 && !verifiedEmail && directStep !== '3') {
        showMessage('请先验证邮箱');
        return;
    }
    
    // 只有在已经完成第二步的情况下才能进入第三步
    if (step > 2 && currentStep < 3 && directStep !== '3') {
        showMessage('请先完成信息填写');
        return;
    }
    
    // 如果回退到第一步，重置邮箱验证状态和清空邮箱输入
    if (step === 1) {
        verifiedEmail = '';
        // 清空邮箱输入框
        document.getElementById('email-local').value = '';
        document.getElementById('email-domain').value = '';
    }
    
    // 更新当前步骤
    currentStep = step;
    
    // 更新进度条
    const progressSteps = document.querySelectorAll('.progress-step');
    progressSteps.forEach((stepElement, index) => {
        stepElement.classList.remove('active');
        if (index + 1 <= step) {
            stepElement.classList.add('active');
        }
    });
    
    // 显示对应步骤的表单
    const formSteps = document.querySelectorAll('.form-step');
    formSteps.forEach((formStep, index) => {
        formStep.classList.remove('active');
        if (index + 1 === step) {
            formStep.classList.add('active');
        }
    });
    
    // 如果是第三步（注册成功），禁用进度条跳转
    if (step === 3) {
        progressSteps.forEach(stepElement => {
            stepElement.style.cursor = 'not-allowed';
            stepElement.onclick = null;
        });
    } else {
        progressSteps.forEach(stepElement => {
            stepElement.style.cursor = 'pointer';
            const stepNum = stepElement.id.replace('step', '');
            stepElement.onclick = function() {
                goToStep(parseInt(stepNum));
            };
        });
    }
}

// 验证邮箱
function verifyEmail() {
    const emailLocal = document.getElementById('email-local').value;
    const emailDomain = document.getElementById('email-domain').value;
    
    if (!emailLocal || !emailDomain) {
        showMessage('请输入完整邮箱');
        return;
    }
    
    const email = emailLocal + '@' + emailDomain;
    
    // 显示加载状态
    const verifyBtn = document.getElementById('verifyBtn');
    const originalText = verifyBtn.textContent;
    verifyBtn.textContent = '验证中...';
    verifyBtn.disabled = true;
    
    // 发送验证请求
    fetch('/Register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
            'step': '1',
            'email': email
        })
    })
    .then(response => response.text())
    .then(response => {
        if (response.includes('user-exists')) {
            showMessage('用户已存在');
        } else if (response.includes('email-valid')) {
            verifiedEmail = email;
            document.getElementById('hidden-email').value = email;
            goToStep(2);
        } else {
            showMessage('验证失败，请重试');
        }
    })
    .finally(() => {
        // 恢复按钮状态
        verifyBtn.textContent = originalText;
        verifyBtn.disabled = false;
    });
}

// 处理注册表单提交
function handleRegister(event) {
    event.preventDefault();
    
    if (!validateForm()) {
        return;
    }
    
    // 显示加载状态
    const submitBtn = document.getElementById('submitBtn');
    const originalText = submitBtn.textContent;
    submitBtn.textContent = '注册中...';
    submitBtn.disabled = true;
    
    // 收集表单数据
    const formData = new FormData(document.getElementById('registerForm'));
    
    // 添加step参数
    const params = new URLSearchParams(formData);
    params.append('step', '2');
    
    // 发送注册请求
    fetch('/Register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            // 设置currentStep为3，允许跳转到第三步
            currentStep = 3;
            goToStep(3);
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

// 验证表单
function validateForm() {
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirm-password').value;
    
    if (password !== confirmPassword) {
        showMessage('两次输入的密码不一致');
        return false;
    }
    
    return true;
}

// 加载省份数据
function loadProvinces() {
    fetch('/ProvinceCity')
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            const provinceSelect = document.getElementById('province');
            provinceSelect.innerHTML = '';
            
            let selectedProvinceCode = '11'; // 默认选中北京(11)
            
            data.provinces.forEach(province => {
                const option = document.createElement('option');
                option.value = province.code;
                option.textContent = province.name;
                
                // 默认选中北京
                if (province.code === selectedProvinceCode) {
                    option.selected = true;
                }
                
                provinceSelect.appendChild(option);
            });
            
            // 加载默认省份的城市
            loadCities();
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
    .then(response => {
        return response.json();
    })
    .then(data => {
        if (data.status === 'success') {
            const citySelect = document.getElementById('city');
            citySelect.innerHTML = '';
            
            // 默认选中第一个城市
            let firstCitySelected = false;
            
            data.cities.forEach(city => {
                const option = document.createElement('option');
                option.value = city.code;
                option.textContent = city.name;
                
                // 默认选中第一个城市
                option.selected = true;
                firstCitySelected = true;
                
                citySelect.appendChild(option);
            });

        } else {

        }
    })
}

