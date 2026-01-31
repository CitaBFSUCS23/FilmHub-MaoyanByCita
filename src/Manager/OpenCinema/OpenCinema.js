// OpenCinema页面JavaScript逻辑

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', function() {

    // 初始化省份城市选择器
    loadProvinceData();
    
    // 设置表单提交事件
    document.getElementById('cinemaForm').addEventListener('submit', handleFormSubmit);
    
    // 设置省份选择变化事件
    document.getElementById('province').addEventListener('change', function() {
        loadCityData(this.value);
    });
});

// 加载省份数据
function loadProvinceData() {
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
                loadCityData();
            } else {
                showMessage('加载省份数据失败', 'error');
            }
        })
}

// 加载城市数据
function loadCityData() {
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
                
                // 默认选中第一个城市
                let firstCitySelected = false;
                
                data.cities.forEach(city => {
                    const option = document.createElement('option');
                    option.value = city.code;
                    option.textContent = city.name;
                    
                    // 默认选中第一个城市
                    if (!firstCitySelected) {
                        option.selected = true;
                        firstCitySelected = true;
                    }
                    
                    citySelect.appendChild(option);
                });
            } else {
                showMessage('加载城市数据失败', 'error');
            }
        })
}

// 处理表单提交
function handleFormSubmit(event) {
    event.preventDefault();
    
    submitCinemaData();
}

// 提交影院数据
function submitCinemaData() {
    const formData = new FormData(document.getElementById('cinemaForm'));
    
    // 显示加载状态
    const submitBtn = document.querySelector('.submit-btn');
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 提交中...';
    submitBtn.disabled = true;
    
    // 获取用户邮箱
    const userEmail = sessionStorage.getItem('userEmail');
    
    fetch('/OpenCinema', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-User-Email': userEmail
        },
        body: new URLSearchParams(formData)
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            // 显示成功页面
            showSuccessPage(data.cinemaId);
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

// 显示成功页面
function showSuccessPage(cinemaId) {
    const step1 = document.getElementById('step1');
    const step2 = document.getElementById('step2');
    const cinemaIdElement = document.getElementById('cinemaId');
    
    step1.classList.remove('active');
    step2.classList.add('active');
    cinemaIdElement.textContent = cinemaId;
}

// 复制影院ID到剪贴板
function copyCinemaId() {
    const cinemaId = document.getElementById('cinemaId').textContent;
    
    navigator.clipboard.writeText(cinemaId).then(function() {
        showMessage('影院ID已复制到剪贴板', 'success');
    }).catch(function() {
        showMessage('复制失败，请手动复制', 'error');
    });
}

// 显示错误消息


// 隐藏错误消息
function hideError() {
    const errorElement = document.getElementById('errorMessage');
    errorElement.style.display = 'none';
}