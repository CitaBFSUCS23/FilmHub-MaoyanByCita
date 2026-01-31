// 全局变量
let selectedSeats = [];
let seatPrice = 0;
let filmInfo = {};
let auditoriumInfo = {};
let disabledSeats = new Set();
let occupiedSeats = new Set();

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 从sessionStorage检查用户是否登录
    const userEmail = sessionStorage.getItem('userEmail');
    if (!userEmail) {
        showMessage('请先登录', 'error');
        setTimeout(() => {
            window.location.href = '../GLogin/GLogin.html';
        }, 2000);
        return;
    }
    
    // 从URL参数获取排片信息
    const urlParams = new URLSearchParams(window.location.search);
    const publisherId = urlParams.get('publisherId');
    const filmId = urlParams.get('filmId');
    const filmLanguage = urlParams.get('filmLanguage');
    const visualEffect = urlParams.get('visualEffect');
    const cinemaProvinceCode = urlParams.get('cinemaProvinceCode');
    const cinemaCityCode = urlParams.get('cinemaCityCode');
    const cinemaId = urlParams.get('cinemaId');
    const auditoriumId = urlParams.get('auditoriumId');
    const scheduleId = urlParams.get('scheduleId');
    
    // 验证参数
    if (!publisherId || !filmId || !filmLanguage || !visualEffect || !cinemaProvinceCode || !cinemaCityCode || !cinemaId || !auditoriumId || !scheduleId) {
        showMessage('请先选择场次', 'error');
        setTimeout(() => {
            window.location.href = '../BuyTicket/BuyTicket.html';
        }, 2000);
        return;
    }
    
    // 保存参数到隐藏字段
    document.getElementById('publisherId').value = publisherId;
    document.getElementById('filmId').value = filmId;
    document.getElementById('filmLanguage').value = filmLanguage;
    document.getElementById('visualEffect').value = visualEffect;
    document.getElementById('cinemaProvinceCode').value = cinemaProvinceCode;
    document.getElementById('cinemaCityCode').value = cinemaCityCode;
    document.getElementById('cinemaId').value = cinemaId;
    document.getElementById('auditoriumId').value = auditoriumId;
    document.getElementById('scheduleId').value = scheduleId;
    
    // 加载影厅和场次信息
    loadAuditoriumInfo(publisherId, filmId, filmLanguage, visualEffect, cinemaProvinceCode, cinemaCityCode, cinemaId, auditoriumId, scheduleId, userEmail);
    
    // 绑定购买按钮事件
    document.getElementById('buyBtn').addEventListener('click', submitOrder);
});

// 加载影厅和场次信息
function loadAuditoriumInfo(publisherId, filmId, filmLanguage, visualEffect, cinemaProvinceCode, cinemaCityCode, cinemaId, auditoriumId, scheduleId, userEmail) {
    $.ajax({
        url: '../Confirm?action=getAuditoriumInfo',
        type: 'POST',
        contentType: 'application/json; charset=UTF-8',
        headers: {
            'X-User-Email': userEmail
        },
        data: JSON.stringify({
            publisherId: publisherId,
            filmId: filmId,
            filmLanguage: filmLanguage,
            visualEffect: visualEffect,
            cinemaProvinceCode: cinemaProvinceCode,
            cinemaCityCode: cinemaCityCode,
            cinemaId: cinemaId,
            auditoriumId: auditoriumId,
            scheduleId: scheduleId
        }),
        dataType: 'json',
        success: function(data) {
            if (data.success) {
                // 更新页面信息
                updateFilmInfo(data.filmInfo);
                updateAuditoriumInfo(data.auditoriumInfo);
                
                // 保存座位信息
                disabledSeats = new Set(data.disabledSeats);
                occupiedSeats = new Set(data.occupiedSeats);
                seatPrice = data.auditoriumInfo.scheduleFare;
                
                // 生成座位表
                generateSeatGrid();
            } else {
                showMessage(data.message, 'error');
                setTimeout(() => {
                    window.location.href = '../BuyTicket/BuyTicket.html';
                }, 2000);
            }
        },
        error: function() {
            showMessage(data.message, 'error');
        }
    });
}

// 更新电影信息
function updateFilmInfo(info) {
    filmInfo = info;
    document.getElementById('film-name').textContent = info.filmName;
    document.getElementById('cinema-name').textContent = info.cinemaName;
}

// 更新影厅信息
function updateAuditoriumInfo(info) {
    auditoriumInfo = info;
}

// 生成座位表
function generateSeatGrid() {
    const rowCount = auditoriumInfo.auditoriumRowCount;
    const colCount = auditoriumInfo.auditoriumColCount;
    
    const seatHeader = document.getElementById('seat-header');
    const seatBody = document.getElementById('seat-body');
    
    // 清空现有内容
    seatHeader.innerHTML = '<tr><th class="corner-header"></th></tr>';
    seatBody.innerHTML = '';
    
    // 生成列号
    const headerRow = seatHeader.querySelector('tr');
    for (let col = 1; col <= colCount; col++) {
        const th = document.createElement('th');
        th.className = 'column-header';
        th.textContent = col;
        headerRow.appendChild(th);
    }
    
    // 生成行号和座位
    for (let row = 1; row <= rowCount; row++) {
        const tr = document.createElement('tr');
        
        // 行号
        const rowHeader = document.createElement('td');
        rowHeader.className = 'row-header';
        rowHeader.textContent = row;
        tr.appendChild(rowHeader);
        
        // 座位
        for (let col = 1; col <= colCount; col++) {
            const seatKey = row + '_' + col;
            const isDisabled = disabledSeats.has(seatKey);
            const isOccupied = occupiedSeats.has(seatKey);
            const isSelectable = !isDisabled && !isOccupied;
            
            const td = document.createElement('td');
            td.className = 'seat-cell';
            
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.id = 'seat_' + row + '_' + col;
            checkbox.className = 'seat-checkbox ' + (isDisabled ? 'disabled' : (isOccupied ? 'occupied' : 'available'));
            checkbox.dataset.row = row;
            checkbox.dataset.col = col;
            
            if (isDisabled) {
                checkbox.disabled = true;
            }
            if (isOccupied) {
                checkbox.disabled = true;
                checkbox.checked = true;
            }
            
            // 添加点击事件
            checkbox.addEventListener('change', handleSeatChange);
            
            td.appendChild(checkbox);
            tr.appendChild(td);
        }
        
        seatBody.appendChild(tr);
    }
}

// 处理座位选择变化
function handleSeatChange(event) {
    const checkbox = event.target;
    const row = checkbox.dataset.row;
    const col = checkbox.dataset.col;
    const seatKey = row + '_' + col;
    
    if (checkbox.checked) {
        // 选中座位
        selectedSeats.push(seatKey);
        addSeatTag(row, col);
    } else {
        // 取消选中
        selectedSeats = selectedSeats.filter(key => key !== seatKey);
        removeSeatTag(row, col);
    }
    
    updateTotal();
}

// 添加座位标签
function addSeatTag(row, col) {
    const seatTag = document.createElement('div');
    seatTag.className = 'seat-tag';
    seatTag.innerHTML = row + "排" + col + "列 " + seatPrice + "元" +
        '<span class="remove-seat" onclick="removeSeat(' + row + ', ' + col + ')">×</span>';
    seatTag.id = "tag_" + row + "_" + col;
    document.getElementById('selectedSeats').appendChild(seatTag);
}

// 移除座位标签
function removeSeatTag(row, col) {
    const tag = document.getElementById("tag_" + row + "_" + col);
    if (tag) {
        tag.remove();
    }
}

// 移除座位
function removeSeat(row, col) {
    const seatKey = row + '_' + col;
    const seatElement = document.getElementById("seat_" + row + "_" + col);
    
    // 从选中列表中移除
    selectedSeats = selectedSeats.filter(key => key !== seatKey);
    
    // 取消复选框选中状态
    seatElement.checked = false;
    
    // 移除标签
    removeSeatTag(row, col);
    
    updateTotal();
}

// 更新总计金额
function updateTotal() {
    const total = selectedSeats.length * seatPrice;
    document.getElementById('totalAmount').textContent = "¥" + total;
    
    // 更新购买按钮状态
    const buyBtn = document.getElementById('buyBtn');
    buyBtn.disabled = selectedSeats.length === 0;
}

// 提交订单
function submitOrder() {
    if (selectedSeats.length === 0) {
        showMessage('请先选择座位', 'error');
        return;
    }
    
    // 设置选中的座位
    document.getElementById('selectedSeatsInput').value = selectedSeats.join(',');
    
    // 获取表单数据
    const form = document.getElementById('orderForm');
    
    // 从sessionStorage获取用户邮箱
    const userEmail = sessionStorage.getItem('userEmail');
    
    // 使用AJAX提交表单
    $.ajax({
        url: '../Confirm?action=submitOrder',
        type: 'POST',
        data: $("#orderForm").serialize(),
        contentType: 'application/x-www-form-urlencoded',
        headers: {
            'X-User-Email': userEmail
        },
        success: function(data) {
            if (data.success) {
                showMessage(data.message, 'success');
                // 重定向到我的订单页面
                setTimeout(() => {
                    window.location.href = data.redirect;
                }, 2000);
            } else {
                showMessage(data.message, 'error');
            }
        },
        error: function(xhr, status, error) {
            console.error('提交订单失败:', error);
            showMessage('提交订单失败，请稍后重试', 'error');
        }
    });
}

// 显示消息
