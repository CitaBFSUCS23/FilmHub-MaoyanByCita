// 影院影厅管理页面JavaScript逻辑

// 分步策略变量和函数
let currentStep = 1;
let selectedVisualEffects = [];
let disabledSeats = [];

// 排序相关变量和函数
let currentSortField = 'auditoriumId';
let currentSortOrder = 'asc';

// 用户信息
let cinemaProvinceCode = '';
let cinemaCityCode = '';
let cinemaId = '';

// 页面加载完成后执行
window.onload = function() {
    // 从sessionStorage获取用户信息
    cinemaProvinceCode = sessionStorage.getItem('cinemaProvinceCode');
    cinemaCityCode = sessionStorage.getItem('cinemaCityCode');
    cinemaId = sessionStorage.getItem('cinemaId');
    
    // 检查用户信息是否存在
    if (!cinemaProvinceCode || !cinemaCityCode || !cinemaId) {
        showMessage('未找到影院信息');
        setTimeout(() => {
            window.location.href = '../MLogin/MLogin.html';
        }, 1500);
        return;
    }
    
    // 加载影厅列表和视觉效果
    loadAuditoriums();
    loadVisualEffects();
    
    // 设置排序
    sortAuditoriums();
};

// 加载影厅列表
function loadAuditoriums() {
    // 创建请求头
    const headers = new Headers();
    headers.append('X-Cinema-Province-Code', cinemaProvinceCode);
    headers.append('X-Cinema-City-Code', cinemaCityCode);
    headers.append('X-Cinema-ID', cinemaId);
    
    fetch('../../Auditoriums', {
        method: 'GET',
        headers: headers
    })
    .then(response => response.json())
    .then(data => {
        renderAuditoriums(data.auditoriumList);
    })
    .catch(error => {
        console.error('加载影厅列表失败:', error);
        showMessage('加载影厅列表失败');
    });
}

// 渲染影厅列表
function renderAuditoriums(auditoriumList) {
    const container = document.getElementById('auditorium-items-container');
    const noResults = document.getElementById('no-results');
    
    container.innerHTML = '';
    
    if (auditoriumList.length === 0) {
        noResults.style.display = 'block';
        return;
    }
    
    noResults.style.display = 'none';
    
    auditoriumList.forEach(auditorium => {
        const auditoriumItem = createAuditoriumItem(auditorium);
        container.appendChild(auditoriumItem);
    });
    
    sortAuditoriums();
}

// 创建影厅项
function createAuditoriumItem(auditorium) {
    const itemDiv = document.createElement('div');
    itemDiv.className = 'film-item auditorium-item';
    
    const auditoriumInfoDiv = document.createElement('div');
    auditoriumInfoDiv.className = 'film-info auditorium-info';
    
    // 影厅编号
    const idRow = document.createElement('div');
    idRow.className = 'detail-row';
    idRow.innerHTML = `
        <span class="label">影厅编号:</span>
        <span class="value">${auditorium.auditoriumId}</span>
    `;
    auditoriumInfoDiv.appendChild(idRow);
    
    // 影厅名称
    if (auditorium.auditoriumName) {
        const nameRow = document.createElement('div');
        nameRow.className = 'detail-row';
        nameRow.innerHTML = `
            <span class="label">影厅名称:</span>
            <span class="value">${auditorium.auditoriumName}</span>
        `;
        auditoriumInfoDiv.appendChild(nameRow);
    }
    
    // 场厅容量
    const capacityRow = document.createElement('div');
    capacityRow.className = 'detail-row';
    capacityRow.innerHTML = `
        <span class="label">场厅容量:</span>
        <span class="value">${auditorium.rowCount} 排 × ${auditorium.colCount} 列 (共 ${auditorium.capacity} 座)</span>
    `;
    auditoriumInfoDiv.appendChild(capacityRow);
    
    // 支持效果
    const effectsRow = document.createElement('div');
    effectsRow.className = 'detail-row';
    effectsRow.innerHTML = `
        <span class="label">支持效果:</span>
        <span class="value">${auditorium.visualEffects}</span>
    `;
    auditoriumInfoDiv.appendChild(effectsRow);
    
    // 下一场电影信息
    const hasSchedule = auditorium.nextFilmName && auditorium.nextShowStartTime;
    
    if (hasSchedule) {
        const scheduleDiv = document.createElement('div');
        scheduleDiv.className = 'schedule-info';
        
        const filmNameRow = document.createElement('div');
        filmNameRow.className = 'detail-row';
        filmNameRow.innerHTML = `
            <span class="label">下一场电影:</span>
            <span class="value film-name">${auditorium.nextFilmName}</span>
        `;
        scheduleDiv.appendChild(filmNameRow);
        
        const timeRow = document.createElement('div');
        timeRow.className = 'detail-row';
        timeRow.innerHTML = `
            <span class="label">放映时间:</span>
            <span class="value">${formatDateTime(auditorium.nextShowStartTime, auditorium.nextShowEndTime)}</span>
        `;
        scheduleDiv.appendChild(timeRow);
        
        if (auditorium.nextFilmLanguage) {
            const languageRow = document.createElement('div');
            languageRow.className = 'detail-row';
            languageRow.innerHTML = `
                <span class="label">语言:</span>
                <span class="value">${auditorium.nextFilmLanguage}</span>
            `;
            scheduleDiv.appendChild(languageRow);
        }
        
        if (auditorium.scheduleFare) {
            const fareRow = document.createElement('div');
            fareRow.className = 'detail-row';
            fareRow.innerHTML = `
                <span class="label">票价:</span>
                <span class="value">¥${auditorium.scheduleFare.toFixed(2)}</span>
            `;
            scheduleDiv.appendChild(fareRow);
        }
        
        auditoriumInfoDiv.appendChild(scheduleDiv);
    } else {
        const noScheduleRow = document.createElement('div');
        noScheduleRow.className = 'detail-row';
        noScheduleRow.innerHTML = `
            <span class="label">排片状态:</span>
            <span class="value no-schedule">暂无排片</span>
        `;
        auditoriumInfoDiv.appendChild(noScheduleRow);
    }
    
    itemDiv.appendChild(auditoriumInfoDiv);
    
    return itemDiv;
}

// 格式化日期时间
function formatDateTime(startTimeStr, endTimeStr) {
    const startTime = new Date(startTimeStr);
    const startDate = startTime.toISOString().split('T')[0];
    const startTimeStrFormatted = startTime.toTimeString().split(' ')[0];
    
    if (!endTimeStr) {
        return `${startDate} ${startTimeStrFormatted}`;
    }
    
    const endTime = new Date(endTimeStr);
    const endTimeStrFormatted = endTime.toTimeString().split(' ')[0];
    
    return `${startDate} ${startTimeStrFormatted} - ${endTimeStrFormatted}`;
}

// 加载所有视觉效果
function loadVisualEffects() {
    fetch('../../Auditoriums?action=getVisualEffects', {
        method: 'GET'
    })
    .then(response => response.json())
    .then(data => {
        renderVisualEffects(data.visualEffects);
    })
    .catch(error => {
        console.error('加载视觉效果失败:', error);
        showMessage('加载视觉效果失败');
    });
}

// 渲染视觉效果选项
function renderVisualEffects(visualEffects) {
    const container = document.getElementById('visualEffectsContainer');
    container.innerHTML = '';
    
    // 确保visualEffects是一个数组
    if (!Array.isArray(visualEffects)) {
        visualEffects = [];
    }
    
    visualEffects.forEach(effect => {
        const checkboxItem = document.createElement('div');
        checkboxItem.className = 'checkbox-item';
        checkboxItem.innerHTML = `
            <input type="checkbox" id="visualEffect-${effect}" name="visualEffects" value="${effect}" onchange="toggleVisualEffect(this)">
            <label for="visualEffect-${effect}">${effect}</label>
        `;
        container.appendChild(checkboxItem);
    });
}

// 切换视觉效果选择
function toggleVisualEffect(checkbox) {
    const effect = checkbox.value;
    
    if (checkbox.checked) {
        selectedVisualEffects.push(effect);
    } else {
        selectedVisualEffects = selectedVisualEffects.filter(e => e !== effect);
    }
    
    // 更新隐藏字段
    document.getElementById('selectedVisualEffects').value = JSON.stringify(selectedVisualEffects);
}

// 设置排序字段
function setSortField(btn) {
    document.querySelectorAll('.sort-field-btn').forEach(button => {
        button.classList.remove('active');
    });
    btn.classList.add('active');
    currentSortField = btn.dataset.field;
    sortAuditoriums();
}

// 设置排序规则
function setSortOrder(btn) {
    document.querySelectorAll('.sort-order-btn').forEach(button => {
        button.classList.remove('active');
    });
    btn.classList.add('active');
    currentSortOrder = btn.dataset.order;
    sortAuditoriums();
}

// 排序影厅
function sortAuditoriums() {
    const auditoriumList = document.querySelectorAll('.auditorium-item');
    const auditoriumArray = Array.from(auditoriumList);
    
    // 排序逻辑
    auditoriumArray.sort((a, b) => {
        let valueA, valueB;
        
        switch (currentSortField) {
            case 'auditoriumId':
                valueA = parseInt(a.querySelector('.auditorium-info .detail-row:first-child .value').textContent);
                valueB = parseInt(b.querySelector('.auditorium-info .detail-row:first-child .value').textContent);
                break;
                
            case 'capacity':
                const capacityTextA = a.querySelector('.auditorium-info .detail-row:nth-child(3) .value').textContent;
                const capacityTextB = b.querySelector('.auditorium-info .detail-row:nth-child(3) .value').textContent;
                valueA = parseInt(capacityTextA.match(/共 (\d+) 座/)[1]);
                valueB = parseInt(capacityTextB.match(/共 (\d+) 座/)[1]);
                break;
                
            case 'nextShowTime':
                const timeElementA = a.querySelector('.schedule-info .detail-row:nth-child(2) .value');
                const timeElementB = b.querySelector('.schedule-info .detail-row:nth-child(2) .value');
                
                if (!timeElementA && !timeElementB) return 0;
                if (!timeElementA) return 1; // 没有排片的排在后面
                if (!timeElementB) return -1;
                
                const timeTextA = timeElementA.textContent.trim();
                const timeTextB = timeElementB.textContent.trim();
                
                // 解析时间字符串
                const dateA = new Date(timeTextA.split(' ')[0] + 'T' + timeTextA.split(' ')[1]);
                const dateB = new Date(timeTextB.split(' ')[0] + 'T' + timeTextB.split(' ')[1]);
                
                valueA = dateA.getTime();
                valueB = dateB.getTime();
                break;
                
            case 'auditoriumName':
                const nameElementA = a.querySelector('.auditorium-info .detail-row:nth-child(2) .value');
                const nameElementB = b.querySelector('.auditorium-info .detail-row:nth-child(2) .value');
                
                // 如果没有影厅名称，使用影厅编号
                if (!nameElementA) {
                    valueA = a.querySelector('.auditorium-info .detail-row:first-child .value').textContent;
                } else {
                    valueA = nameElementA.textContent;
                }
                
                if (!nameElementB) {
                    valueB = b.querySelector('.auditorium-info .detail-row:first-child .value').textContent;
                } else {
                    valueB = nameElementB.textContent;
                }
                break;
        }
        
        // 比较逻辑
        if (currentSortOrder === 'asc') {
            return valueA < valueB ? -1 : valueA > valueB ? 1 : 0;
        } else {
            return valueA > valueB ? -1 : valueA < valueB ? 1 : 0;
        }
    });
    
    // 重新排列DOM元素
    const auditoriumListContainer = document.querySelector('.auditorium-list');
    auditoriumArray.forEach(item => {
        auditoriumListContainer.appendChild(item);
    });
}

// 新影厅相关函数
function addNewAuditorium() {
    // 获取新影厅ID
    fetch('../../Auditoriums?action=newId', {
        method: 'GET',
        headers: {
            'X-Cinema-Province-Code': cinemaProvinceCode,
            'X-Cinema-City-Code': cinemaCityCode,
            'X-Cinema-ID': cinemaId
        }
    })
    .then(response => response.json())
    .then(data => {
        const newAuditoriumId = data.auditoriumId;
        const fullAuditoriumId = data.fullAuditoriumId;
        
        // 设置新影厅ID
        document.getElementById('auditoriumId').value = newAuditoriumId;
        document.getElementById('fullAuditoriumId').value = fullAuditoriumId;
        
        // 显示表单
        const form = document.getElementById('new-auditorium-form');
        form.style.display = 'block';
        window.scrollTo({ top: form.offsetTop, behavior: 'smooth' });
        
        // 重置步骤
        currentStep = 1;
        selectedVisualEffects = [];
        disabledSeats = [];
        
        // 更新进度条和表单显示
        goToStep(1);
    })
    .catch(error => {
        console.error('获取新影厅ID失败:', error);
        showMessage('获取新影厅ID失败');
    });
}

function cancelNewAuditorium() {
    const form = document.getElementById('new-auditorium-form');
    form.style.display = 'none';
    // 重置表单
    document.getElementById('auditorium-form').reset();
    // 重置布局预览
    document.getElementById('layout-preview').innerHTML = '';
    // 重置步骤变量
    currentStep = 1;
    selectedVisualEffects = [];
    disabledSeats = [];
    
    // 重置所有视觉效果复选框
    document.querySelectorAll('input[name="visualEffects"]').forEach(checkbox => {
        checkbox.checked = false;
    });
}

// 跳转到指定步骤
function goToStep(step) {
    // 验证：只有在第一步完成的情况下才能进入第二步
    if (step > 1 && currentStep < 2) {
        if (!validateStep1()) {
            showMessage('请先完成第一步的基本信息填写');
            return;
        }
    }
    
    // 更新当前步骤
    currentStep = step;
    
    // 更新进度条
    $('.progress-step').removeClass('active');
    for (let i = 1; i <= step; i++) {
        $('#step' + i).addClass('active');
    }
    
    // 显示对应步骤的表单
    $('.form-step').removeClass('active');
    $('#form-step' + step).addClass('active');
    
    // 如果进入第二步，生成座位布局并设置隐藏字段值
    if (step === 2) {
        // 设置第二步中的隐藏字段值
        const rows = document.getElementById('rows').value;
        const cols = document.getElementById('cols').value;
        const auditoriumName = document.getElementById('auditoriumName').value;
        
        document.getElementById('rows-display').value = rows;
        document.getElementById('cols-display').value = cols;
        document.getElementById('auditoriumName-display').value = auditoriumName;
        
        setTimeout(generateLayout, 100); // 延迟执行，确保DOM已更新
    } else if (step === 1) {
        // 重新启用第一步的输入字段
        document.getElementById('rows').disabled = false;
        document.getElementById('cols').disabled = false;
        document.getElementById('auditoriumName').disabled = false;
    }
}

// 验证第一步的表单
function validateStep1() {
    const form = document.getElementById('auditorium-form');
    const rowsInput = document.getElementById('rows');
    const colsInput = document.getElementById('cols');
    const visualEffects = document.querySelectorAll('input[name="visualEffects"]:checked');
    
    // 重置所有自定义验证
    rowsInput.setCustomValidity('');
    colsInput.setCustomValidity('');
    
    // 检查座位数输入
    if (!rowsInput.checkValidity() || !colsInput.checkValidity()) {
        rowsInput.reportValidity();
        colsInput.reportValidity();
        return false;
    }

    // 检查影厅名，如果没有输入则使用默认值
    const auditoriumNameInput = document.getElementById('auditoriumName');
    let auditoriumName = auditoriumNameInput.value.trim();
    if (!auditoriumName) {
        auditoriumName = "Common Auditorium";
        auditoriumNameInput.value = auditoriumName;
    }
    
    // 检查视觉效果
    if (visualEffects.length === 0) {
        showMessage('请至少选择一种视觉效果'); 
        return false;
    } else {
        // 更新隐藏字段
        const effectsArray = [];
        visualEffects.forEach(effect => {
            effectsArray.push(effect.value);
        });
        document.getElementById('selectedVisualEffects').value = JSON.stringify(effectsArray);
    }
    
    return true;
}

// 验证并跳转到第二步
function validateAndGoToStep2() {
    if (validateStep1()) {
        // 将步骤1的输入值复制到步骤2的显示字段中
        const auditoriumName = document.getElementById('auditoriumName').value;
        const rows = document.getElementById('rows').value;
        const cols = document.getElementById('cols').value;
        
        document.getElementById('auditoriumName-display').value = auditoriumName;
        document.getElementById('rows-display').value = rows;
        document.getElementById('cols-display').value = cols;
        
        goToStep(2);
    }
}

// 生成座位布局
function generateLayout() {
    const rowsInput = document.getElementById('rows');
    const colsInput = document.getElementById('cols');
    let preview = document.getElementById('layout-preview');
    
    if (!rowsInput || !colsInput || !preview) { // 检查必要的DOM元素是否存在
        return;
    }
    
    const rows = parseInt(rowsInput.value);
    const cols = parseInt(colsInput.value);
    
    let html = '';
    html += '<div class="screen-indicator">↓ 屏幕在此 ↓</div>';
    html += '<div class="seat-layout-container">';
    html += '<table class="seat-layout-table">';
    
    html += '<thead><tr>';
    html += '<th class="corner-header"></th>';
    for (let j = 1; j <= cols; j++) {
        html += '<th class="column-header">' + j + '</th>';
    }
    html += '</tr></thead>';
    
    html += '<tbody>';
    for (let i = 1; i <= rows; i++) {
        html += '<tr>';
        html += '<td class="row-header">' + i + '</td>';
        for (let j = 1; j <= cols; j++) {
            const seatId = i + '-' + j;
            const isDisabled = disabledSeats.some(s => s.row === i && s.col === j);
            html += '<td class="seat-cell">';
            html += '<input type="checkbox" class="seat-checkbox" data-seat="' + seatId + '"';
            if (isDisabled) {
                html += ' checked';
            }
            html += ' onchange="toggleSeat(this)">';
            html += '</td>';
        }
        html += '</tr>';
    }
    html += '</tbody>';
    
    html += '</table>';
    html += '</div>';
    
    preview.innerHTML = html;
    
    updateHiddenField();
}

// 切换座位状态
function toggleSeat(checkbox) {
    const seatId = checkbox.dataset.seat;
    const [row, col] = seatId.split('-').map(Number);
    
    if (checkbox.checked) {
        disabledSeats.push({ "row": row, "col": col });
        checkbox.parentElement.classList.add('disabled');
    } else {
        disabledSeats = disabledSeats.filter(s => s.row !== row || s.col !== col);
        checkbox.parentElement.classList.remove('disabled');
    }
    updateHiddenField();
}

// 更新隐藏字段
function updateHiddenField() {
    document.getElementById('auditoriumLayout').value = JSON.stringify(disabledSeats);
}

// 提交新影厅
function submitAuditorium(event) {
    event.preventDefault();
    
    // 获取表单数据
    const form = document.getElementById('auditorium-form');
    
    // 构建URL编码的参数字符串
    const params = new URLSearchParams();
    
    // 添加表单字段
    params.append('auditoriumId', document.getElementById('auditoriumId').value);
    params.append('auditoriumName', document.getElementById('auditoriumName-display').value);
    params.append('rows', document.getElementById('rows-display').value);
    params.append('cols', document.getElementById('cols-display').value);
    params.append('selectedVisualEffects', document.getElementById('selectedVisualEffects').value);
    params.append('auditoriumLayout', document.getElementById('auditoriumLayout').value);
    
    // 添加影院信息
    params.append('cinemaProvinceCode', cinemaProvinceCode);
    params.append('cinemaCityCode', cinemaCityCode);
    params.append('cinemaId', cinemaId);
    
    // 发送请求
    fetch('../../Auditoriums', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: params.toString()
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            showMessage('影厅添加成功');
            cancelNewAuditorium();
            loadAuditoriums(); // 重新加载影厅列表
        } else {
            showMessage('添加失败: ' + data.message);
        }
    })
    .catch(error => {
        console.error('提交影厅失败:', error);
        showMessage('提交影厅失败');
    });
}

// 显示消息
