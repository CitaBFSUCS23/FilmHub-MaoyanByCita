// Income页面JavaScript逻辑

document.addEventListener('DOMContentLoaded', function() {
    // 检查登录状态
    if (!checkLoginStatus()) {
        return;
    }
    
    // 获取DOM元素
    const prevYearBtn = document.getElementById('prevYearBtn');
    const nextYearBtn = document.getElementById('nextYearBtn');
    const prevMonthBtn = document.getElementById('prevMonthBtn');
    const nextMonthBtn = document.getElementById('nextMonthBtn');
    const yearInput = document.getElementById('yearInput');
    const monthInput = document.getElementById('monthInput');
    const calendarBody = document.getElementById('calendarBody');
    const totalAmount = document.querySelector('.total-amount');
    
    // 当前显示的年份和月份
    let currentYear;
    let currentMonth;
    
    // 当前日期
    const today = new Date();
    const currentDate = {
        year: today.getFullYear(),
        month: today.getMonth() + 1,
        day: today.getDate()
    };
    
    // 影院信息
    let cinemaInfo;
    
    // 初始化页面
    initPage();
    
    // 事件监听器
    prevYearBtn.addEventListener('click', function() {
        currentYear--;
        updateDateSelection();
        loadIncomeData();
    });
    
    nextYearBtn.addEventListener('click', function() {
        currentYear++;
        updateDateSelection();
        loadIncomeData();
    });
    
    prevMonthBtn.addEventListener('click', function() {
        if (currentMonth === 1) {
            currentMonth = 12;
            currentYear--;
        } else {
            currentMonth--;
        }
        updateDateSelection();
        loadIncomeData();
    });
    
    nextMonthBtn.addEventListener('click', function() {
        if (currentMonth === 12) {
            currentMonth = 1;
            currentYear++;
        } else {
            currentMonth++;
        }
        updateDateSelection();
        loadIncomeData();
    });
    
    yearInput.addEventListener('change', function() {
        let year = parseInt(this.value);
        year = Math.max(2000, Math.min(2100, year));
        currentYear = year;
        this.value = year;
        loadIncomeData();
    });
    
    monthInput.addEventListener('change', function() {
        let month = parseInt(this.value);
        month = Math.max(1, Math.min(12, month));
        currentMonth = month;
        this.value = month;
        loadIncomeData();
    });
    
    /**
     * 初始化页面
     */
    function initPage() {
        // 设置初始显示的年份和月份为当前日期
        currentYear = currentDate.year;
        currentMonth = currentDate.month;
        
        // 更新日期选择器
        updateDateSelection();
        
        // 获取影院信息
        loadCinemaInfo();
    }
    
    /**
     * 获取影院信息
     */
    function loadCinemaInfo() {
        // 从sessionStorage获取影院信息
        const cinemaProvinceCode = sessionStorage.getItem('cinemaProvinceCode');
        const cinemaCityCode = sessionStorage.getItem('cinemaCityCode');
        const cinemaId = sessionStorage.getItem('cinemaId');
        
        if (!cinemaProvinceCode || !cinemaCityCode || !cinemaId) {
            showMessage('未找到影院信息，请重新登录', 'error');
            window.location.href = '../MLogin/MLogin.html';
            return;
        }
        
        cinemaInfo = {
            provinceCode: cinemaProvinceCode,
            cityCode: cinemaCityCode,
            id: cinemaId
        };
        
        // 加载收入数据
        loadIncomeData();
    }
    
    /**
     * 更新日期选择器
     */
    function updateDateSelection() {
        yearInput.value = currentYear;
        monthInput.value = currentMonth;
    }
    
    /**
     * 加载收入数据
     */
    function loadIncomeData() {
        if (!cinemaInfo) return;
        
        // 显示加载状态
        showLoadingState(true);
        
        // 发送请求获取收入数据
        fetch('../../IncomeServlet', {
            method: 'GET',
            headers: {
                'X-Cinema-Province-Code': cinemaInfo.provinceCode,
                'X-Cinema-City-Code': cinemaInfo.cityCode,
                'X-Cinema-ID': cinemaInfo.id,
                'X-Year': currentYear.toString(),
                'X-Month': currentMonth.toString()
            }
        })
        .then(response => response.json())
        .then(data => {
            showLoadingState(false);
            
            if (data.status === 'success') {
                // 生成日历
                generateCalendar(data.dailyIncome);
                // 更新总计
                updateTotalIncome(data.dailyIncome);
            } else {
                showMessage(data.message, 'error');
                // 生成空日历
                generateCalendar({});
            }
        })
        .catch(error => {
            showLoadingState(false);
            console.error('Error loading income data:', error);
            showMessage('网络错误，无法获取收入数据', 'error');
            // 生成空日历
            generateCalendar({});
        });
    }
    
    /**
     * 生成日历
     */
    function generateCalendar(dailyIncome) {
        // 清空日历
        calendarBody.innerHTML = '';
        
        // 创建日期对象
        const date = new Date(currentYear, currentMonth - 1, 1);
        
        // 获取当月第一天是星期几（0-6，0表示星期日）
        let firstDayOfWeek = date.getDay();
        if (firstDayOfWeek === 0) firstDayOfWeek = 7; // 调整为1-7，1表示星期一
        
        // 获取当月天数
        const daysInMonth = new Date(currentYear, currentMonth, 0).getDate();
        
        let dayCounter = 1;
        let rowCounter = 0;
        
        // 生成最多6行（可能包含下个月的部分日期）
        while (rowCounter < 6 && dayCounter <= daysInMonth) {
            const row = document.createElement('tr');
            
            // 生成7列（星期日到星期六）
            for (let col = 1; col <= 7; col++) {
                const cell = document.createElement('td');
                
                // 填充日期
                if ((rowCounter === 0 && col >= firstDayOfWeek) || rowCounter > 0) {
                    if (dayCounter <= daysInMonth) {
                        // 创建日期数字元素
                        const dayNumber = document.createElement('div');
                        dayNumber.className = 'day-number';
                        dayNumber.textContent = dayCounter;
                        
                        // 添加到单元格
                        cell.appendChild(dayNumber);
                        
                        // 检查是否有收入数据
                        const dateKey = `${currentYear}-${String(currentMonth).padStart(2, '0')}-${String(dayCounter).padStart(2, '0')}`;
                        if (dailyIncome[dateKey] && dailyIncome[dateKey] > 0) {
                            const income = document.createElement('div');
                            income.className = 'income';
                            income.textContent = `+${(dailyIncome[dateKey]).toFixed(2)}元`;
                            cell.appendChild(income);
                        }
                        
                        // 检查是否是当前日期
                        if (currentYear === currentDate.year && 
                            currentMonth === currentDate.month && 
                            dayCounter === currentDate.day) {
                            // 可以添加当前日期高亮样式
                        }
                        
                        dayCounter++;
                    }
                }
                
                row.appendChild(cell);
            }
            
            calendarBody.appendChild(row);
            rowCounter++;
        }
    }
    
    /**
     * 更新总计收入
     */
    function updateTotalIncome(dailyIncome) {
        let total = 0;
        
        // 计算总收入
        for (const date in dailyIncome) {
            if (dailyIncome.hasOwnProperty(date)) {
                total += dailyIncome[date];
            }
        }
        
        // 更新显示，保持与原样式一致（显示+号）
        totalAmount.textContent = `+${total.toFixed(2)}元`;
    }
    
    /**
     * 显示加载状态
     */
    function showLoadingState(isLoading) {
        if (isLoading) {
            // 清空日历并显示加载指示器
            calendarBody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 20px;">加载中...</td></tr>';
            totalAmount.textContent = '+0.00元';
        }
    }
    
    /**
     * 显示消息
     */

    
    // 为按钮添加Win98风格的点击效果
    const navBtns = document.querySelectorAll('.nav-btn');
    navBtns.forEach(btn => {
        btn.addEventListener('mousedown', function() {
            this.style.borderStyle = 'inset';
            this.style.transform = 'translate(2px, 2px)';
        });
        
        btn.addEventListener('mouseup', function() {
            this.style.borderStyle = 'outset';
            this.style.transform = 'translate(0, 0)';
        });
        
        btn.addEventListener('mouseleave', function() {
            this.style.borderStyle = 'outset';
            this.style.transform = 'translate(0, 0)';
        });
    });
});