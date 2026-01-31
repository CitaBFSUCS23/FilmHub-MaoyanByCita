// 全局变量，用于存储当前的筛选条件和页码
let currentStatus = null;
let currentSearchKeyword = '';
let currentPage = 1;
let pageSize = 5;

// 页面加载完成后初始化
window.onload = function() {
    // 初始化页面，加载第一页数据
    loadFilms();
    
    // 添加搜索表单的提交事件监听
    document.getElementById('search-form').addEventListener('submit', function(e) {
        e.preventDefault();
        currentSearchKeyword = document.getElementById('searchInput').value;
        currentPage = 1; // 重置页码到第一页
        loadFilms();
    });
};

// 根据当前筛选条件和页码加载电影数据
function loadFilms() {
    // 构建请求URL，包含筛选条件和分页参数
    let url = `/FilmScan?page=${currentPage}&pageSize=${pageSize}`;
    
    if (currentStatus) {
        url += `&status=${currentStatus}`;
    }
    
    if (currentSearchKeyword) {
        url += `&search=${encodeURIComponent(currentSearchKeyword)}`;
    }
    
    // 发送GET请求获取电影数据
    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            // 更新电影列表
            updateFilmList(data.films);
            // 更新分页控件
            updatePagination(data.totalFilms, data.totalPages, currentPage);
        })
        .catch(error => {
            console.error('加载电影数据失败:', error);
            showMessage('加载电影数据失败，请稍后重试', 'error');
        });
}

// 更新电影列表
function updateFilmList(films) {
    const filmListContainer = document.getElementById('film-list');
    const template = document.getElementById('film-item-template');
    
    // 清空容器
    filmListContainer.innerHTML = '';
    
    if (films.length === 0) {
        filmListContainer.innerHTML = '<div class="no-films">没有找到符合条件的影片</div>';
        return;
    }
    
    films.forEach(film => {
        // 格式化海报路径
        const formattedFilmId = String(film.filmId).padStart(6, '0');
        const posterPath = `/uploads/films/${film.publisherId}/${formattedFilmId}.jpg`;
        
        // 判断影片是否可购票（在映状态）
        const isPurchasable = film.releaseStatus.trim() === 'HotFilming';
        
        // 克隆模板
        const clone = document.importNode(template.content, true);
        const filmItem = clone.querySelector('.film-item');
        const filmPoster = clone.querySelector('.film-poster');
        const img = clone.querySelector('img');
        const filmName = clone.querySelector('.film-name');
        const filmDuration = clone.querySelector('.film-duration');
        const filmBoxOffice = clone.querySelector('.film-box-office');
        const filmTypes = clone.querySelector('.film-types');
        const filmStatus = clone.querySelector('.film-status');
        const filmSynopsis = clone.querySelector('.film-synopsis');
        
        // 设置海报
        img.src = posterPath;
        img.alt = film.filmName;
        
        // 设置影片信息
        filmName.textContent = film.filmName;
        filmDuration.textContent = `${film.duration}分钟`;
        filmBoxOffice.textContent = film.boxOffice.toFixed(2);
        filmTypes.textContent = film.filmTypes;
        filmStatus.textContent = getStatusText(film.releaseStatus);
        filmSynopsis.textContent = film.synopsis;
        
        // 设置购票功能
        if (isPurchasable) {
            filmPoster.classList.add('clickable');
            filmPoster.onclick = () => buyTicket(film.publisherId, film.filmId);
        } else {
            filmPoster.classList.remove('clickable');
        }
        
        // 添加到容器
        filmListContainer.appendChild(filmItem);
    });
}

// 更新分页控件
function updatePagination(totalFilms, totalPages, currentPage) {
    const paginationContainer = document.getElementById('pagination');
    
    if (totalPages <= 1) {
        paginationContainer.innerHTML = '';
        return;
    }
    
    let paginationHtml = '';
    
    // 上一页按钮
    paginationHtml += `<a href="#" class="btn-page ${currentPage <= 1 ? 'disabled' : ''}" onclick="goToPage(${currentPage - 1})">上一页</a>`;
    
    // 页码显示
    paginationHtml += `<span style="margin: 0 10px; font-size: 14px;">第 ${currentPage} / ${totalPages} 页</span>`;
    
    // 下一页按钮
    paginationHtml += `<a href="#" class="btn-page ${currentPage >= totalPages ? 'disabled' : ''}" onclick="goToPage(${currentPage + 1})">下一页</a>`;
    
    paginationContainer.innerHTML = paginationHtml;
}

// 状态筛选函数
function filterByStatus(status) {
    // 更新当前筛选状态
    currentStatus = status;
    currentPage = 1; // 重置页码到第一页
    
    // 更新筛选按钮的激活状态
    updateFilterButtons(status);
    
    // 重新加载电影数据
    loadFilms();
}

// 更新筛选按钮的激活状态
function updateFilterButtons(activeStatus) {
    // 获取所有筛选按钮
    const filterButtons = document.querySelectorAll('.filter-btn');
    
    // 移除所有按钮的激活状态
    filterButtons.forEach(button => {
        button.classList.remove('active');
    });
    
    // 根据当前激活的状态添加激活类
    if (!activeStatus) {
        document.getElementById('filter-all').classList.add('active');
    } else if (activeStatus === 'HotFilming') {
        document.getElementById('filter-hot').classList.add('active');
    } else if (activeStatus === 'UpComing') {
        document.getElementById('filter-upcoming').classList.add('active');
    } else if (activeStatus === 'HasFinished') {
        document.getElementById('filter-finished').classList.add('active');
    }
}

// 跳转到指定页码
function goToPage(page) {
    if (page < 1) return;
    currentPage = page;
    loadFilms();
}

// 购票功能
function buyTicket(publisherId, filmId) {
    // 将影片信息存储到sessionStorage中
    sessionStorage.setItem('selectedPublisherId', publisherId);
    sessionStorage.setItem('selectedFilmId', filmId);
    
    // 跳转到购票页面
    window.location.href = '/General/BuyTicket/BuyTicket.html';
}

// 获取状态的中文显示文本
function getStatusText(status) {
    // 去除状态字符串两端的空格
    const trimmedStatus = status.trim();
    switch (trimmedStatus) {
        case 'HotFilming':
            return '在映';
        case 'UpComing':
            return '预热';
        case 'HasFinished':
            return '历史';
        default:
            return '未知';
    }
}

