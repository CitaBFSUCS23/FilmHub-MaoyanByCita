// 全局变量
let currentPage = 1;
let totalPages = 1;
let currentStatus = '';
let orderList = [];
let countdownTimers = [];

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 检查用户是否已登录
        const userEmail = sessionStorage.getItem('userEmail');
        if (!userEmail) {
            showMessage('请先登录！', 'error');
            setTimeout(() => {
                window.location.href = '/General/GLogin/GLogin.html';
            }, 1000);
            return;
        }
    
    // 初始化事件监听器
    initEventListeners();
    
    // 加载订单列表
    loadOrders();
});

// 初始化事件监听器
function initEventListeners() {
    // 筛选按钮事件
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            // 更新筛选状态
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            
            // 获取筛选状态
            currentStatus = this.dataset.status;
            
            // 重置页码并重新加载订单
            currentPage = 1;
            loadOrders();
        });
    });
    
    // 分页按钮事件
    document.getElementById('prevPage').addEventListener('click', function() {
        if (currentPage > 1) {
            currentPage--;
            loadOrders();
        }
    });
    
    document.getElementById('nextPage').addEventListener('click', function() {
        if (currentPage < totalPages) {
            currentPage++;
            loadOrders();
        }
    });
    
    // 页码输入框事件
    document.getElementById('pageInput').addEventListener('change', function() {
        let page = parseInt(this.value);
        if (isNaN(page) || page < 1) {
            page = 1;
        } else if (page > totalPages) {
            page = totalPages;
        }
        currentPage = page;
        loadOrders();
    });
    
    // 订单号弹窗关闭事件
    document.getElementById('orderNumberModal').addEventListener('click', function() {
        this.style.display = 'none';
    });
}

// 加载订单列表
function loadOrders() {
    // 清除现有计时器
    clearAllCountdowns();
    
    const userEmail = sessionStorage.getItem('userEmail');
    const pageSize = 5;
    
    // 构建请求参数
    const params = new URLSearchParams({
        page: currentPage,
        pageSize: pageSize
    });
    
    if (currentStatus) {
        params.append('status', currentStatus);
    }
    
    // 发送请求加载订单
    fetch(`/MyOrders?${params.toString()}`, {
        headers: {
            'X-User-Email': userEmail
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('网络请求失败');
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
                // 更新订单列表
                orderList = data.orders;
                totalPages = data.totalPages;
                
                // 渲染订单列表
                renderOrders();
                
                // 更新分页控件
                updatePagination();
            } else {
                showMessage(data.message, 'error');
            }
    })
    .catch(error => {
        console.error('加载订单失败:', error);
        showMessage('加载订单失败，请稍后重试', 'error');
    });
}

// 渲染订单列表
function renderOrders() {
    const orderListContainer = document.getElementById('orderList');
    const template = document.getElementById('order-template');
    
    // 清空订单列表
    orderListContainer.innerHTML = '';
    
    if (orderList.length === 0) {
        orderListContainer.innerHTML = '<div class="no-orders"><p>您还没有任何订单</p></div>';
        return;
    }
    
    // 渲染每个订单
    orderList.forEach(order => {
        // 克隆模板
        const clone = document.importNode(template.content, true);
        
        // 设置订单信息
        const posterImg = clone.querySelector('.order-poster img');
        const filmName = clone.querySelector('.film-name');
        const statusHint = clone.querySelector('.order-status-hint');
        const cinemaName = clone.querySelector('.cinema-name');
        const auditoriumName = clone.querySelector('.auditorium-name');
        const seatInfo = clone.querySelector('.seat-info');
        const filmLanguage = clone.querySelector('.film-language');
        const visualEffect = clone.querySelector('.visual-effect');
        const fare = clone.querySelector('.fare');
        const createTime = clone.querySelector('.create-time');
        const showTime = clone.querySelector('.show-time');
        const viewOrderNumberBtn = clone.querySelector('.view-order-number');
        const statusActions = clone.querySelector('.status-actions');
        
        // 构建海报路径
        const publisherId = String(order.scheduleFilmPublisherId).padStart(6, '0');
        const filmId = String(order.scheduleFilmId).padStart(6, '0');
        const posterPath = `/uploads/films/${publisherId}/${filmId}.jpg`;
        
        // 设置海报
        posterImg.src = posterPath;
        posterImg.alt = order.filmName;
        
        // 设置订单基本信息
        filmName.textContent = order.filmName;
        statusHint.textContent = order.statusDescription;
        statusHint.title = '订单状态';
        cinemaName.textContent = order.cinemaName;
        auditoriumName.textContent = order.auditoriumName;
        seatInfo.textContent = `${order.orderRowNo}排${order.orderColNo}列`;
        filmLanguage.textContent = order.scheduleFilmLanguage;
        visualEffect.textContent = order.scheduleVisualEffect;
        fare.textContent = `¥${order.scheduleFare}`;
        createTime.textContent = formatDateTime(order.orderCreateTime);
        showTime.textContent = formatDateTime(order.scheduleShowTime);
        
        // 设置订单号按钮事件
        viewOrderNumberBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            showOrderNumber(order);
        });
        
        // 根据订单状态显示操作按钮
        renderOrderActions(statusActions, order);
        
        // 设置未支付订单的倒计时
        if (order.statusDescription === '未付' || order.statusDescription === '未支付') {
            // 在状态提示之后添加倒计时元素
            const countdownElement = document.createElement('span');
            countdownElement.className = 'order-countdown';
            statusHint.parentNode.appendChild(countdownElement);
            
            // 检查订单是否已经超时
            const createTime = new Date(order.orderCreateTime).getTime();
            const timeoutTime = createTime + 10 * 60 * 1000; // 10分钟后超时
            const now = Date.now();
            
            if (now >= timeoutTime) {
                // 订单已超时，直接显示"已超时"
                countdownElement.textContent = '已超时';
            } else {
            // 订单未超时，启动倒计时
            function updateCountdown() {
                const now = Date.now();
                const remainingTime = timeoutTime - now;
                
                if (remainingTime > 0) {
                    const minutes = Math.floor(remainingTime / (60 * 1000));
                    const seconds = Math.floor((remainingTime % (60 * 1000)) / 1000);
                    countdownElement.textContent = `${minutes}分${seconds}秒`;
                } else {
                    countdownElement.textContent = '已超时';
                    clearInterval(timer);
                }
            }
            // 立即更新一次
            updateCountdown();
            // 每秒更新一次
            const timer = setInterval(updateCountdown, 1000);
            // 保存计时器引用，以便后续清除
            countdownTimers.push(timer);
        }
        }
        // 添加到订单列表
        orderListContainer.appendChild(clone);
    });
}



// 渲染订单操作按钮
function renderOrderActions(container, order) {
    container.innerHTML = '';
    
    const status = order.statusDescription;
    
    if (status === '未付') {
        // 取消订单按钮
        const cancelBtn = document.createElement('button');
        cancelBtn.className = 'order-btn cancel';
        cancelBtn.textContent = '取消订单';
        cancelBtn.addEventListener('click', function() {
            cancelOrder(order);
        });
        container.appendChild(cancelBtn);
        
        // 立即支付按钮
        const payBtn = document.createElement('button');
        payBtn.className = 'order-btn pay';
        payBtn.textContent = '立即支付';
        payBtn.addEventListener('click', function() {
            payOrder(order);
        });
        container.appendChild(payBtn);
    } else if (status === '已付') {
        // 申请退票按钮
        const refundBtn = document.createElement('button');
        refundBtn.className = 'order-btn refund';
        refundBtn.textContent = '申请退票';
        refundBtn.addEventListener('click', function() {
            refundOrder(order);
        });
        container.appendChild(refundBtn);
    }
}

// 显示订单号
function showOrderNumber(order) {
    // 构建订单号
    const publisherId = String(order.scheduleFilmPublisherId).padStart(6, '0');
    const filmId = String(order.scheduleFilmId).padStart(6, '0');
    const provinceCode = String(order.scheduleCinemaProvinceCode).padStart(2, '0');
    const cityCode = String(order.scheduleCinemaCityCode).padStart(2, '0');
    const cinemaId = String(order.scheduleCinemaId).padStart(4, '0');
    const auditoriumId = String(order.scheduleAuditoriumId).padStart(3, '0');
    const scheduleId = String(order.scheduleId).padStart(3, '0');
    const rowNo = String(order.orderRowNo).padStart(2, '0');
    const colNo = String(order.orderColNo).padStart(2, '0');
    
    // 直接处理SQL Timestamp字符串，保持与数据库一致
    // SQL Timestamp格式：2026-01-19 05:07:18.475
    // 拆分时间戳为各部分
    const timeParts = order.orderCreateTime.split(' ');
    const datePart = timeParts[0];
    const timePart = timeParts[1];
    
    // 处理日期部分（2026-01-19 → 20260119）
    const formattedDate = datePart.replace(/-/g, '');
    
    // 处理时间部分（05:07:18.475 → 050718475）
    const formattedTime = timePart.replace(/:/g, '').replace(/\./g, '');
    
    // 组合成最终的时间戳部分
    const formattedCreateTime = formattedDate + formattedTime;
    
    // 构建订单号
    const orderNumber = publisherId + filmId + provinceCode + cityCode + cinemaId + auditoriumId + scheduleId + rowNo + colNo + formattedCreateTime;
    
    // 显示订单号
    document.getElementById('orderNumber').textContent = orderNumber;
    document.getElementById('orderNumberModal').style.display = 'flex';
}

// 取消订单
function cancelOrder(order) {
    if (confirm('确定要取消该订单吗？')) {
        const userEmail = sessionStorage.getItem('userEmail');
        
        fetch('/MyOrders', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            },
            body: JSON.stringify({
                action: 'cancelOrder',
                filmPublisherId: order.scheduleFilmPublisherId,
                filmId: order.scheduleFilmId,
                filmLanguage: order.scheduleFilmLanguage,
                visualEffect: order.orderVisualEffect,
                cinemaProvinceCode: order.scheduleCinemaProvinceCode,
                cinemaCityCode: order.scheduleCinemaCityCode,
                cinemaId: order.scheduleCinemaId,
                auditoriumId: order.scheduleAuditoriumId,
                scheduleId: order.scheduleId,
                rowNo: order.orderRowNo,
                colNo: order.orderColNo,
                createTime: order.orderCreateTime
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
                showMessage(data.message, 'success');
                loadOrders();
            } else {
                showMessage(data.message, 'error');
            }
        })
        .catch(error => {
            console.error('取消订单失败:', error);
            showMessage('取消订单失败，请稍后重试', 'error');
        });
    }
}

// 支付订单
function payOrder(order) {
    if (confirm('确定要立即支付该订单吗？')) {
        const userEmail = sessionStorage.getItem('userEmail');
        
        fetch('/MyOrders', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            },
            body: JSON.stringify({
                action: 'payOrder',
                filmPublisherId: order.scheduleFilmPublisherId,
                filmId: order.scheduleFilmId,
                filmLanguage: order.scheduleFilmLanguage,
                visualEffect: order.orderVisualEffect,
                cinemaProvinceCode: order.scheduleCinemaProvinceCode,
                cinemaCityCode: order.scheduleCinemaCityCode,
                cinemaId: order.scheduleCinemaId,
                auditoriumId: order.scheduleAuditoriumId,
                scheduleId: order.scheduleId,
                rowNo: order.orderRowNo,
                colNo: order.orderColNo,
                createTime: order.orderCreateTime
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
                showMessage(data.message, 'success');
                loadOrders();
            } else {
                showMessage(data.message, 'error');
            }
        })
        .catch(error => {
            console.error('支付订单失败:', error);
            showMessage('支付订单失败，请稍后重试', 'error');
        });
    }
}

// 申请退票
function refundOrder(order) {
    if (confirm('确定要申请退票吗？')) {
        const userEmail = sessionStorage.getItem('userEmail');
        
        fetch('/MyOrders', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Email': userEmail
            },
            body: JSON.stringify({
                action: 'refundOrder',
                filmPublisherId: order.scheduleFilmPublisherId,
                filmId: order.scheduleFilmId,
                filmLanguage: order.scheduleFilmLanguage,
                visualEffect: order.orderVisualEffect,
                cinemaProvinceCode: order.scheduleCinemaProvinceCode,
                cinemaCityCode: order.scheduleCinemaCityCode,
                cinemaId: order.scheduleCinemaId,
                auditoriumId: order.scheduleAuditoriumId,
                scheduleId: order.scheduleId,
                rowNo: order.orderRowNo,
                colNo: order.orderColNo,
                createTime: order.orderCreateTime
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
                showMessage(data.message, 'success');
                loadOrders();
            } else {
                showMessage(data.message, 'error');
            }
        })
        .catch(error => {
            console.error('申请退票失败:', error);
            showMessage('申请退票失败，请稍后重试', 'error');
        });
    }
}

// 更新分页控件
function updatePagination() {
    // 更新总页数
    document.getElementById('totalPages').textContent = totalPages;
    
    // 更新页码输入框
    const pageInput = document.getElementById('pageInput');
    pageInput.value = currentPage;
    pageInput.max = totalPages;
    
    // 更新分页按钮状态
    document.getElementById('prevPage').disabled = currentPage <= 1;
    document.getElementById('nextPage').disabled = currentPage >= totalPages;
}

// 清除所有倒计时计时器
function clearAllCountdowns() {
    countdownTimers.forEach(timer => clearInterval(timer));
    countdownTimers = [];
}

// 格式化日期时间
function formatDateTime(dateTimeStr) {
    const date = new Date(dateTimeStr);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}