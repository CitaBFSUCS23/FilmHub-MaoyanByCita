// 全局变量
let publisherId, filmId;
let provinceSelect, citySelect;



// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 检查用户是否已登录
    const userEmail = sessionStorage.getItem('userEmail');
    if (!userEmail) {
        showMessage('先以用户身份登录！');
        setTimeout(() => {
            window.location.href = '/General/GLogin/GLogin.html';
        }, 1500);
        return;
    }
    
    // 获取sessionStorage中的发行商ID和影片ID
    publisherId = sessionStorage.getItem('selectedPublisherId');
    filmId = sessionStorage.getItem('selectedFilmId');
    
    // 加载电影详细信息
    loadFilmDetails(publisherId, filmId);
    
    // 初始化省市联动
    setupProvinceCity('provinceSelect', 'citySelect', '11', '01');
    
    // 设置默认日期为今天
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('dateSelect').value = today;
    
    // 设置日期最小值为今天
    document.getElementById('dateSelect').min = today;
    
    // 绑定查询按钮事件
    document.getElementById('searchBtn').addEventListener('click', searchSchedules);
});

// 加载电影详细信息
function loadFilmDetails(publisherId, filmId) {
    fetch(`/BuyTicket?action=getFilmDetails&publisherId=${publisherId}&filmId=${filmId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                updateFilmDetails(data.filmInfo);
            } else {
                showMessage(data.message, 'error');
            }
        })
        .catch(error => {
            showMessage(data.message, 'error');
        });
}

// 更新电影详细信息到页面
function updateFilmDetails(filmInfo) {
    // 格式化海报路径
    const formattedFilmId = String(filmInfo.filmId).padStart(6, '0');
    const posterPath = `/uploads/films/${filmInfo.publisherId}/${formattedFilmId}.jpg`;
    
    // 更新海报
    const posterImg = document.getElementById('film-poster');
    posterImg.src = posterPath;
    posterImg.alt = filmInfo.filmName;
    
    // 更新电影信息
    document.getElementById('film-name').textContent = filmInfo.filmName;
    document.getElementById('film-release-date').textContent = filmInfo.releaseDate;
    document.getElementById('film-types').textContent = filmInfo.filmTypes;
    document.getElementById('film-languages').textContent = filmInfo.languages;
    document.getElementById('film-visual-effects').textContent = filmInfo.visualEffects;
    document.getElementById('film-duration').textContent = `${filmInfo.duration}分钟`;
    document.getElementById('film-box-office').textContent = filmInfo.boxOffice;
    document.getElementById('film-synopsis').textContent = filmInfo.synopsis;
}

// 查询场次
function searchSchedules() {
    const province = document.getElementById('provinceSelect').value;
    const city = document.getElementById('citySelect').value;
    const date = document.getElementById('dateSelect').value;
    
    if (!province || !city || !date) {
        showMessage('请选择完整的查询条件！');
        return;
    }
    
    // 发送请求查询场次
    fetch('/BuyTicket', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            action: 'searchSchedule',
            publisherId: publisherId,
            filmId: filmId,
            province: province,
            city: city,
            date: date
        })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('网络请求失败');
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            updateScheduleList(data.cinemaSchedules);
        } else {
            showMessage(data.message);
            updateScheduleList({});
        }
    })
    .catch(error => {
        console.error('查询场次失败:', error);
        showMessage('查询场次失败，请稍后重试');
        updateScheduleList({});
    });
}

// 更新场次列表
function updateScheduleList(cinemaSchedules) {
    const scheduleContainer = document.getElementById('cinemaScheduleList');
    const template = document.getElementById('schedule-template');
    
    // 清空容器
    scheduleContainer.innerHTML = '';
    
    if (Object.keys(cinemaSchedules).length === 0) {
        scheduleContainer.innerHTML = '<div class="no-results">没有找到符合条件的场次</div>';
        return;
    }
    
    // 创建网格容器
    const gridContainer = document.createElement('div');
    gridContainer.className = 'schedule-grid';
    
    // 遍历每个影院的场次
    Object.keys(cinemaSchedules).forEach(cinemaName => {
        const schedules = cinemaSchedules[cinemaName];
        
        schedules.forEach(schedule => {
            // 克隆模板
            const clone = document.importNode(template.content, true);
            
            // 获取模板中的元素
            const scheduleBlock = clone.querySelector('.schedule-block');
            const cinemaNameEl = clone.querySelector('.cinema-name');
            const languageEl = clone.querySelector('.language');
            const showTimeEl = clone.querySelector('.show-time');
            const visualEffectsEl = clone.querySelector('.visual-effects');
            const fareEl = clone.querySelector('.fare');
            const form = clone.querySelector('form');
            
            // 设置场次信息
            cinemaNameEl.textContent = cinemaName;
            languageEl.textContent = schedule.language;
            showTimeEl.textContent = schedule.formattedTime;
            visualEffectsEl.textContent = schedule.visualEffect;
            fareEl.textContent = `${schedule.formattedFare}元`;
            
            // 设置表单数据
            form.querySelector('input[name="publisherId"]').value = schedule.publisherId;
            form.querySelector('input[name="filmId"]').value = schedule.filmId;
            form.querySelector('input[name="filmLanguage"]').value = schedule.language;
            form.querySelector('input[name="visualEffect"]').value = schedule.visualEffect;
            form.querySelector('input[name="cinemaProvinceCode"]').value = schedule.cinemaProvinceCode;
            form.querySelector('input[name="cinemaCityCode"]').value = schedule.cinemaCityCode;
            form.querySelector('input[name="cinemaId"]').value = schedule.cinemaId;
            form.querySelector('input[name="auditoriumId"]').value = schedule.auditoriumId;
            form.querySelector('input[name="scheduleId"]').value = schedule.scheduleId;
            
            // 设置表单ID
            const formId = `scheduleForm_${schedule.scheduleId}`;
            form.id = formId;
            
            // 设置点击事件
            scheduleBlock.onclick = () => selectSchedule(formId);
            
            // 添加到网格容器
            gridContainer.appendChild(clone);
        });
    });
    
    // 添加到页面
    scheduleContainer.appendChild(gridContainer);
}

// 选择场次，提交对应的表单
function selectSchedule(formId) {
    const form = document.getElementById(formId);
    if (form) {
        form.submit();
    } else {
        showMessage('场次不存在，请重新选择！');
    }
}

// 初始化省市联动
function setupProvinceCity(provinceSelectId, citySelectId, defaultProvinceCode = '11', defaultCityCode = '01') {
    provinceSelect = document.getElementById(provinceSelectId);
    citySelect = document.getElementById(citySelectId);
    
    // 绑定省份选择变化事件
    provinceSelect.addEventListener('change', function() {
        const selectedProvinceCode = this.value;
        if (selectedProvinceCode) {
            loadCities(selectedProvinceCode, null); // 省份变化时，城市列表自动更新
        }
    });
    
    // 先显示加载状态
    provinceSelect.innerHTML = '<option value="">加载中...</option>';
    citySelect.innerHTML = '<option value="">加载中...</option>';
    
    // 加载省份数据，默认选中北京
    loadProvinces(defaultProvinceCode, defaultCityCode);
}

// 加载省份数据
function loadProvinces(defaultProvinceCode, defaultCityCode) {
    fetch('/ProvinceCity')
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.status === 'success' && data.provinces) {
                // 清空省份选择框
                provinceSelect.innerHTML = '';
                
                // 添加省份选项
                data.provinces.forEach(province => {
                    const option = document.createElement('option');
                    option.value = province.code;
                    option.textContent = province.name;
                    provinceSelect.appendChild(option);
                });
                
                // 检查默认省份是否存在，不存在则使用第一个省份
                let provinceExists = false;
                for (let i = 0; i < provinceSelect.options.length; i++) {
                    if (provinceSelect.options[i].value === defaultProvinceCode) {
                        provinceExists = true;
                        break;
                    }
                }
                
                if (provinceExists) {
                    provinceSelect.value = defaultProvinceCode;
                } else if (provinceSelect.options.length > 0) {
                    provinceSelect.value = provinceSelect.options[0].value;
                    defaultProvinceCode = provinceSelect.value;
                }
                
                // 加载对应城市
                loadCities(defaultProvinceCode, defaultCityCode);
            } else {
                console.error('加载省份数据失败:', data.message);
                provinceSelect.innerHTML = '<option value="">加载失败</option>';
                citySelect.innerHTML = '<option value="">加载失败</option>';
            }
        })
        .catch(error => {
            console.error('加载省份数据失败:', error);
            provinceSelect.innerHTML = '<option value="">加载失败</option>';
            citySelect.innerHTML = '<option value="">加载失败</option>';
        });
}

// 加载城市数据
function loadCities(provinceCode, defaultCityCode) {
    // 显示加载状态
    citySelect.innerHTML = '<option value="">加载中...</option>';
    
    fetch(`/ProvinceCity?provinceCode=${provinceCode}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.status === 'success' && data.cities && data.cities.length > 0) {
                // 清空城市选择框
                citySelect.innerHTML = '';
                
                // 添加城市选项
                data.cities.forEach(city => {
                    const option = document.createElement('option');
                    option.value = city.code;
                    option.textContent = city.name;
                    citySelect.appendChild(option);
                });
                
                // 设置默认城市
                let citySet = false;
                
                // 优先使用指定的默认城市
                if (defaultCityCode) {
                    for (let i = 0; i < citySelect.options.length; i++) {
                        if (citySelect.options[i].value === defaultCityCode) {
                            citySelect.value = defaultCityCode;
                            citySet = true;
                            break;
                        }
                    }
                }
                
                // 如果指定的默认城市不存在，选择第一个城市
                if (!citySet && citySelect.options.length > 0) {
                    citySelect.value = citySelect.options[0].value;
                }
            } else {
                console.error('加载城市数据失败:', data.message);
                citySelect.innerHTML = '<option value="">暂无城市数据</option>';
            }
        })
        .catch(error => {
            console.error('加载城市数据失败:', error);
            citySelect.innerHTML = '<option value="">加载失败</option>';
        });
}