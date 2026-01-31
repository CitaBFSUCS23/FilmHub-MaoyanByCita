// 全局变量
let currentAuditorium = null;
let currentDate = null;
let currentFilm = null;
let filmDataMap = {};

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', function() {
    // 从sessionStorage获取影院信息
    const cinemaProvinceCode = sessionStorage.getItem('cinemaProvinceCode');
    const cinemaCityCode = sessionStorage.getItem('cinemaCityCode');
    const cinemaId = sessionStorage.getItem('cinemaId');
    
    if (!cinemaProvinceCode || !cinemaCityCode || !cinemaId) {
        showMessage('未找到影院信息，请重新登录', 'error');
        setTimeout(() => {
            window.location.href = '../MLogin/MLogin.html';
        }, 2000);
        return;
    }
    
    // 加载影厅列表
    loadAuditoriums(cinemaProvinceCode, cinemaCityCode, cinemaId);
});

// 加载影厅列表
function loadAuditoriums(provinceCode, cityCode, cinemaId) {
    $.ajax({
        url: '../PlayMovies?action=getAuditoriums',
        type: 'GET',
        data: {
            provinceCode: provinceCode,
            cityCode: cityCode,
            cinemaId: cinemaId
        },
        headers: {
            'X-Cinema-Province-Code': provinceCode,
            'X-Cinema-City-Code': cityCode,
            'X-Cinema-Id': cinemaId
        },
        dataType: 'json',
        success: function(data) {
            if (data.status === 'success') {
                renderAuditoriums(data.auditoriums);
            } else {
                showMessage('加载影厅失败: ' + data.message, 'error');
            }
        },
        error: function(xhr, status, error) {
            showMessage('加载影厅失败: ' + error, 'error');
        }
    });
}

// 渲染影厅列表
function renderAuditoriums(auditoriums) {
    const auditoriumGrid = document.getElementById('auditorium-grid');
    const template = document.getElementById('auditorium-template');
    
    auditoriumGrid.innerHTML = '';
    
    auditoriums.forEach(auditorium => {
        const clone = document.importNode(template.content, true);
        clone.querySelector('.auditorium-id').textContent = auditorium.auditoriumId;
        clone.querySelector('.auditorium-name').textContent = auditorium.auditoriumName;
        clone.querySelector('.auditorium-item').setAttribute('onclick', `selectAuditorium('${auditorium.auditoriumId}')`);
        
        auditoriumGrid.appendChild(clone);
    });
}

// 选择影厅
function selectAuditorium(auditoriumId) {
    // 移除所有影厅项的选中状态
    document.querySelectorAll('.auditorium-item').forEach(function(item) {
        item.classList.remove('selected');
    });
    
    // 为当前选择的影厅项添加选中状态
    const auditoriumItems = document.querySelectorAll('.auditorium-item');
    auditoriumItems.forEach(function(item) {
        const onclickAttr = item.getAttribute('onclick');
        if (onclickAttr && onclickAttr.includes("'" + auditoriumId + "'")) {
            item.classList.add('selected');
        }
    });
    
    currentAuditorium = auditoriumId;
    
    // 生成日期选项
    generateDateOptions();
    
    // 显示日期选择区域
    document.getElementById('date-section').style.display = 'block';
}

// 生成日期选项
function generateDateOptions() {
    const dateGrid = document.getElementById('date-grid');
    const template = document.getElementById('date-template');
    
    dateGrid.innerHTML = '';
    
    // 生成未来7天的日期
    const dateOptions = [];
    const dateFormat = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });
    const weekdayFormat = new Intl.DateTimeFormat('zh-CN', { weekday: 'short' });
    
    const today = new Date();
    for (let i = 1; i <= 7; i++) {
        const date = new Date(today);
        date.setDate(today.getDate() + i);
        
        const formattedDate = dateFormat.format(date);
        const parts = formattedDate.split('/');
        const year = parts[0];
        const month = parts[1];
        const day = parts[2];
        const dateStr = `${year}-${month}-${day}`;
        
        dateOptions.push({
            date: dateStr,
            year: year,
            month: month,
            day: day,
            weekday: weekdayFormat.format(date)
        });
    }
    
    // 渲染日期选项
    dateOptions.forEach(dateInfo => {
        const clone = document.importNode(template.content, true);
        clone.querySelector('.date-day').textContent = dateInfo.day + '日';
        clone.querySelector('.date-weekday').textContent = dateInfo.weekday;
        clone.querySelector('.date-month').textContent = dateInfo.month + '月';
        clone.querySelector('.date-item').setAttribute('onclick', `selectDate('${dateInfo.date}')`);
        
        dateGrid.appendChild(clone);
    });
}

// 选择日期
function selectDate(date) {
    // 移除所有日期项的选中状态
    document.querySelectorAll('.date-item').forEach(function(item) {
        item.classList.remove('selected');
    });
    
    // 为当前选择的日期项添加选中状态
    const dateItems = document.querySelectorAll('.date-item');
    dateItems.forEach(function(item) {
        const onclickAttr = item.getAttribute('onclick');
        if (onclickAttr && onclickAttr.includes("'" + date + "'")) {
            item.classList.add('selected');
        }
    });
    
    currentDate = date;
    
    // 加载可放映影片
    loadAvailableFilms();
    
    // 显示影片选择区域
    document.getElementById('film-section').style.display = 'block';
}

// 加载可放映影片
function loadAvailableFilms() {
    const cinemaProvinceCode = sessionStorage.getItem('cinemaProvinceCode');
    const cinemaCityCode = sessionStorage.getItem('cinemaCityCode');
    const cinemaId = sessionStorage.getItem('cinemaId');
    
    $.ajax({
        url: '../PlayMovies?action=getAvailableFilms',
        type: 'GET',
        data: {
            provinceCode: cinemaProvinceCode,
            cityCode: cinemaCityCode,
            cinemaId: cinemaId,
            auditoriumId: currentAuditorium,
            date: currentDate
        },
        headers: {
            'X-Cinema-Province-Code': cinemaProvinceCode,
            'X-Cinema-City-Code': cinemaCityCode,
            'X-Cinema-Id': cinemaId
        },
        dataType: 'json',
        success: function(data) {
            if (data.status === 'success') {
                renderFilms(data.films);
            } else {
                showMessage('加载影片失败: ' + data.message, 'error');
            }
        },
        error: function(xhr, status, error) {
            showMessage('加载影片失败: ' + error, 'error');
        }
    });
}

// 渲染影片列表
function renderFilms(films) {
    const filmGrid = document.getElementById('film-grid');
    const template = document.getElementById('film-template');
    
    filmGrid.innerHTML = '';
    filmDataMap = {};
    
    if (films.length === 0) {
        filmGrid.innerHTML = '<div class="no-films">没有找到符合条件的影片</div>';
        return;
    }
    
    films.forEach(film => {
        const filmKey = `${film.publisherId}_${film.filmId}`;
        filmDataMap[filmKey] = film;
        
        // 格式化影片ID
        const formattedPublisherId = film.publisherId.padStart(6, '0');
        const formattedFilmId = film.filmId.padStart(6, '0');
        const formattedFullId = formattedPublisherId + formattedFilmId;
        
        const clone = document.importNode(template.content, true);
        clone.querySelector('.film-title').textContent = film.filmName;
        clone.querySelector('.film-id').textContent = formattedFullId;
        clone.querySelector('.publisher-name').textContent = film.publisherName;
        
        // 添加隐藏的影片数据
        const hiddenInput = document.createElement('input');
        hiddenInput.type = 'hidden';
        hiddenInput.id = `filmData_${filmKey}`;
        hiddenInput.value = JSON.stringify(film);
        
        const filmItem = clone.querySelector('.film-item');
        filmItem.setAttribute('onclick', `selectFilm('${filmKey}')`);
        filmItem.appendChild(hiddenInput);
        
        filmGrid.appendChild(clone);
    });
}

// 选择影片
function selectFilm(filmKey) {
    // 移除所有影片项的选中状态
    document.querySelectorAll('.film-item').forEach(function(item) {
        item.classList.remove('selected');
    });
    
    // 为当前选择的影片项添加选中状态
    const filmItems = document.querySelectorAll('.film-item');
    filmItems.forEach(function(item) {
        const onclickAttr = item.getAttribute('onclick');
        if (onclickAttr && onclickAttr.includes("'" + filmKey + "'")) {
            item.classList.add('selected');
        }
    });
    
    currentFilm = filmKey;
    const film = filmDataMap[filmKey];
    
    // 更新语言选项
    updateLanguageOptions(film);
    
    // 更新视觉效果选项
    updateVisualEffectOptions(film);
    
    // 更新票价输入框的最小限制和占位符
    const fareInput = document.querySelector('input[name="fare"]');
    if (fareInput && film.Film_Min_Fare) {
        fareInput.min = film.Film_Min_Fare;
        fareInput.placeholder = `发行商定价：不低于 ${film.Film_Min_Fare}`;
    }
    
    // 显示排片设置区域
    document.getElementById('scheduleSection').style.display = 'block';
}

// 更新语言选项
function updateLanguageOptions(film) {
    const languageSelect = document.getElementById('languageSelect');
    languageSelect.innerHTML = '<option value="">请选择语言</option>';
    
    if (film && film.languages) {
        film.languages.forEach(language => {
            const option = document.createElement('option');
            option.value = language;
            option.textContent = language;
            languageSelect.appendChild(option);
        });
    }
}

// 更新视觉效果选项
function updateVisualEffectOptions(film) {
    const effectSelect = document.getElementById('visualEffectSelect');
    effectSelect.innerHTML = '<option value="">请选择视觉效果</option>';
    
    if (film && film.visualEffects) {
        film.visualEffects.forEach(effect => {
            const option = document.createElement('option');
            option.value = effect;
            option.textContent = effect;
            effectSelect.appendChild(option);
        });
    }
}

// 计算结束时间
function calculateEndTime() {
    const startTime = document.getElementById('startTime').value;
    
    if (!startTime || !currentFilm || !filmDataMap[currentFilm]) {
        return;
    }
    
    const film = filmDataMap[currentFilm];
    const duration = film.duration;
    
    if (!duration) {
        return;
    }
    
    const [hours, minutes] = startTime.split(':').map(Number);
    const startDate = new Date();
    startDate.setHours(hours, minutes, 0, 0);
    
    const endDate = new Date(startDate.getTime() + duration * 60000);
    const endTime = endDate.getHours().toString().padStart(2, '0') + ':' + 
                  endDate.getMinutes().toString().padStart(2, '0');
    
    document.getElementById('endTime').value = endTime;
}

// 提交排片信息
function submitSchedule() {
    const cinemaProvinceCode = sessionStorage.getItem('cinemaProvinceCode');
    const cinemaCityCode = sessionStorage.getItem('cinemaCityCode');
    const cinemaId = sessionStorage.getItem('cinemaId');
    
    // 验证表单
    if (!currentAuditorium || !currentDate || !currentFilm) {
        showMessage('请完成所有选择步骤', 'error');
        return;
    }
    
    const selectedLanguage = document.getElementById('languageSelect').value;
    const visualEffect = document.getElementById('visualEffectSelect').value;
    const fare = document.querySelector('input[name="fare"]').value;
    const startTime = document.getElementById('startTime').value;
    
    if (!selectedLanguage || !visualEffect || !fare || !startTime) {
        showMessage('请填写完整的排片信息', 'error');
        return;
    }
    
    // 解析filmKey为发行商ID和影片ID
    const filmParts = currentFilm.split('_');
    const publisherId = filmParts[0];
    const filmId = filmParts[1];
    
    // 构建请求数据
    const scheduleData = {
        cinemaProvinceCode: cinemaProvinceCode,
        cinemaCityCode: cinemaCityCode,
        cinemaId: cinemaId,
        selectedAuditorium: currentAuditorium,
        selectedDate: currentDate,
        selectedFilm: currentFilm,
        selectedLanguage: selectedLanguage,
        visualEffect: visualEffect,
        fare: fare,
        startTime: startTime,
        publisherId: publisherId,
        filmId: filmId
    };
    
    $.ajax({
        url: '../PlayMovies?action=submitSchedule',
        type: 'POST',
        contentType: 'application/json; charset=UTF-8',
        data: JSON.stringify(scheduleData),
        headers: {
            'X-Cinema-Province-Code': cinemaProvinceCode,
            'X-Cinema-City-Code': cinemaCityCode,
            'X-Cinema-Id': cinemaId
        },
        dataType: 'json',
        success: function(data) {
            if (data.status === 'success') {
                showMessage('排片成功！', 'success');
                setTimeout(() => {
                    window.location.href = '../Auditoriums/Auditoriums.html';
                }, 2000);
            } else {
                showMessage('排片添加失败：' + data.message, 'error');
            }
        },
        error: function(xhr, status, error) {
            let errorMessage = '排片添加失败：';
            try {
                const response = JSON.parse(xhr.responseText);
                errorMessage += response.message;
            } catch (e) {
                errorMessage += error;
            }
            showMessage(errorMessage, 'error');
        }
    });
}

// 重置页面
function resetPage() {
    currentAuditorium = null;
    currentDate = null;
    currentFilm = null;
    filmDataMap = {};
    
    // 重置UI
    document.querySelectorAll('.auditorium-item').forEach(item => item.classList.remove('selected'));
    document.querySelectorAll('.date-item').forEach(item => item.classList.remove('selected'));
    document.querySelectorAll('.film-item').forEach(item => item.classList.remove('selected'));
    
    document.getElementById('date-section').style.display = 'none';
    document.getElementById('film-section').style.display = 'none';
    document.getElementById('scheduleSection').style.display = 'none';
    
    // 重置表单
    document.getElementById('languageSelect').innerHTML = '<option value="">请选择语言</option>';
    document.getElementById('visualEffectSelect').innerHTML = '<option value="">请选择视觉效果</option>';
    document.querySelector('input[name="fare"]').value = '';
    document.querySelector('input[name="fare"]').placeholder = '请先选择影片';
    document.getElementById('startTime').value = '';
    document.getElementById('endTime').value = '';
}

// 显示消息
