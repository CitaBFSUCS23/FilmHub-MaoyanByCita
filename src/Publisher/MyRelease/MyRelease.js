// MyRelease页面JavaScript逻辑
window.currentPage = 1;
window.totalPages = 1;
let searchKeyword = '';

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    loadFilmData();
    setupEventListeners();
});

// 设置事件监听器
function setupEventListeners() {
    // 搜索表单提交
    document.getElementById('searchForm').addEventListener('submit', function(e) {
        e.preventDefault();
        searchKeyword = document.getElementById('searchInput').value;
        window.currentPage = 1;
        loadFilmData();
    });
    
    // 新影片表单提交
    document.getElementById('release-form').addEventListener('submit', function(e) {
        e.preventDefault();
        validateForm(e);
    });
    
    // 日期验证
    document.getElementById('releaseDate').addEventListener('change', validateDates);
    document.getElementById('endDate').addEventListener('change', validateDates);
    
    // 海报预览
    document.getElementById('filmPoster').addEventListener('change', function(e) {
        previewImage(this);
    });
}

// 加载影片数据
function loadFilmData() {
    let url = '/MyRelease?page=' + window.currentPage;
    if (searchKeyword) {
        url += '&search=' + encodeURIComponent(searchKeyword);
    }
    
    // 从sessionStorage获取发行商ID
    const publisherId = sessionStorage.getItem('publisherId');
    
    fetch(url, {
        headers: {
            'X-Publisher-Id': publisherId
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.status === 'success') {
                renderFilmList(data.filmList);
                // 保存分页信息
                window.totalPages = data.totalPages;
                window.currentPage = data.currentPage;
                
                renderPagination(data.totalPages, data.currentPage, data.totalFilms);
                
                // 保存元数据用于新影片表单
                window.filmTypes = data.filmTypes;
                window.filmLanguages = data.filmLanguages;
                window.visualEffects = data.visualEffects;
                window.displayNewFilmId = data.displayNewFilmId;
                window.newFilmId = data.newFilmId;
                window.publisherId = data.publisherId;
                
                // 更新新影片表单中的隐藏字段
                document.getElementById('displayFilmId').value = data.displayNewFilmId;
                document.getElementById('filmId').value = data.newFilmId;
                document.getElementById('publisherId').value = data.publisherId;
                
                // 渲染表单选项
                renderFormOptions(data.filmTypes, data.filmLanguages, data.visualEffects);
            } else {
                showMessage('加载失败: ' + data.message, 'error');
            }
        });
}

// 渲染影片列表
function renderFilmList(films) {
    const filmListContainer = document.getElementById('filmList');
    
    if (films.length === 0) {
        filmListContainer.innerHTML = `
            <div class="no-films">
                <i class="fas fa-film" style="font-size: 48px; color: #c0c0c0; margin-bottom: 20px;"></i>
                <p>暂无发行影片</p>
                <p style="color: #666; font-size: 14px;">点击"添加新影片"按钮开始发布您的第一部影片</p>
            </div>
        `;
        return;
    }
    
    let html = '';
    films.forEach(film => {
        // 每个影片数据中包含正确的发行商ID，直接使用
        const filmId = film.filmId;
        const posterPath = `/uploads/films/${film.publisherId}/${filmId}.jpg`;
        
        html += `
            <div class="film-item">
                <div class="film-poster">
                    <img src="${posterPath}" alt="${film.filmName}">
                </div>
                <div class="film-info">
                    <div class="film-details">
                        <h3>${film.filmName}</h3>
                        <div class="detail-row">
                            <span class="label">影片编号:</span>
                            <span class="value">${film.filmId}</span>
                        </div>
                        <div class="detail-row">
                            <span class="label">发行日期:</span>
                            <span class="value">${film.publishDate}</span>
                        </div>
                        <div class="detail-row">
                            <span class="label">上映期间:</span>
                            <span class="value">${film.releaseDate} - ${film.finishedDate}</span>
                        </div>
                        <div class="detail-row">
                            <span class="label">时长:</span>
                            <span class="value">${film.duration}分钟</span>
                        </div>
                        <div class="detail-row">
                            <span class="label">最低票价:</span>
                            <span class="value">¥${film.minFare}</span>
                        </div>
                        <div class="detail-row">
                            <span class="label">票房:</span>
                            <span class="value">¥${film.boxOffice}</span>
                        </div>
                        <div class="detail-row">
                            <span class="label">类型:</span>
                            <span class="value">${film.filmTypes}</span>
                        </div>
                        <div class="detail-row">
                            <span class="label">语言:</span>
                            <span class="value">${film.filmLanguages}</span>
                        </div>
                        <div class="detail-row">
                            <span class="label">效果:</span>
                            <span class="value">${film.visualEffects}</span>
                        </div>
                        <div class="detail-row synopsis">
                            <span class="label">简介:</span>
                            <span class="value">${film.synopsis}</span>
                        </div>
                    </div>
                </div>
            </div>
        `;
    });
    
    filmListContainer.innerHTML = html;
}

// 渲染分页控件（简化版本：始终显示所有控件，只在不能操作时禁用）
function renderPagination(totalPages, currentPage, totalFilms) {
    const paginationContainer = document.getElementById('pagination');
    
    // 简化逻辑：始终显示所有分页控件，只在不能操作时禁用
    const prevDisabled = currentPage <= 1 ? ' disabled' : '';
    const nextDisabled = currentPage >= totalPages ? ' disabled' : '';
    
    // 创建统一的HTML结构
    const html = `
        <div class="pagination">
            <div class="pagination-info">共 ${totalFilms} 部影片</div>
            
            <button class="btn-page${prevDisabled}" onclick="goToPage(${Math.max(1, currentPage - 1)})" ${prevDisabled ? 'disabled' : ''}>上一页</button>
            
            <div class="pagination-controls">
                <span>第</span>
                <input type="number" id="pageInput" value="${currentPage}" min="1" max="${totalPages}" step="1" onchange="goToPage(parseInt(this.value))">
                <span>页 / 共 ${totalPages} 页</span>
            </div>
            
            <button class="btn-page${nextDisabled}" onclick="goToPage(${Math.min(totalPages, currentPage + 1)})" ${nextDisabled ? 'disabled' : ''}>下一页</button>
        </div>
    `;
    
    paginationContainer.innerHTML = html;
}

// 跳转到指定页面
function goToPage(page) {
    // 获取总页数
    const totalPages = window.totalPages || 1;
    
    // 确保页码在有效范围内
    if (isNaN(page) || page < 1) {
        page = 1;
    } else if (page > totalPages) {
        page = totalPages;
    }
    
    // 更新当前页码并重新加载数据
    window.currentPage = page;
    loadFilmData();
}

// 渲染表单选项
function renderFormOptions(filmTypes, filmLanguages, visualEffects) {
    // 渲染影片类型
    const filmTypesContainer = document.getElementById('filmTypesContainer');
    filmTypesContainer.innerHTML = filmTypes.map(type => 
        `<label><input type="checkbox" name="filmType" value="${type}"> ${type}</label>`
    ).join('');
    
    // 渲染影片语言
    const filmLanguagesContainer = document.getElementById('filmLanguagesContainer');
    filmLanguagesContainer.innerHTML = filmLanguages.map(lang => 
        `<label><input type="checkbox" name="filmLanguage" value="${lang}"> ${lang}</label>`
    ).join('');
    
    // 渲染视觉效果
    const visualEffectsContainer = document.getElementById('visualEffectsContainer');
    visualEffectsContainer.innerHTML = visualEffects.map(effect => 
        `<label><input type="checkbox" name="visualEffect" value="${effect}" ${effect === '2D' ? 'checked disabled onclick="return false;"' : ''}> ${effect}</label>`
    ).join('');
}

// 新影片相关函数
function addNewFilm() {
    const form = document.getElementById('new-film-form');
    form.style.display = 'block';
    window.scrollTo({ top: form.offsetTop, behavior: 'smooth' });
}

function cancelNewFilm() {
    const form = document.getElementById('new-film-form');
    form.style.display = 'none';
    // 重置表单
    document.getElementById('release-form').reset();
    const preview = document.getElementById('preview');
    const noPreview = document.getElementById('noPreview');
    const posterName = document.getElementById('poster-name');
    if (preview) preview.style.display = 'none';
    if (noPreview) noPreview.style.display = 'block';
    if (posterName) posterName.style.display = 'none';
}

function validateForm(event) {
    event.preventDefault();
    
    // 验证影片类型（1-3种）
    const filmTypes = document.querySelectorAll('input[name="filmType"]:checked');
    if (filmTypes.length < 1 || filmTypes.length > 3) {
        showMessage('请选择1-3种影片类型');
        return false;
    }

    const filmLanguages = document.querySelectorAll('input[name="filmLanguage"]:checked');
    if (filmLanguages.length === 0) {
        showMessage('请至少选择一种支持语言');
        return false;
    }

    const filmVisualEffects = document.querySelectorAll('input[name="visualEffect"]:checked');
    if (filmVisualEffects.length === 0) {
        showMessage('请至少选择一种视觉效果');
        return false;
    }
    
    // 验证日期
    if (!validateDates()) {
        return false;
    }
    
    // 验证并格式化最低票价
    const minFareInput = document.getElementById('minFare');
    let minFare = minFareInput.value;
    
    // 使用fetch提交表单到后端处理
    const form = document.getElementById('release-form');
    const formData = new FormData(form);
    
    // 从sessionStorage获取发行商ID
    const publisherId = sessionStorage.getItem('publisherId');
    
    fetch(form.action, {
        method: 'POST',
        headers: {
            'X-Publisher-Id': publisherId
        },
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            showMessage(data.message, 'success');
            // 强制刷新页面，避免数据重复显示
            window.location.reload();
        } else if (data.status === 'error') {
            showMessage('发布失败：' + data.message, 'error');
        } else {
            showMessage('发布失败：未知错误', 'error');
        }
    })
    
    return false;
}

function validateDates() {
    const releaseDateInput = document.getElementById('releaseDate');
    const endDateInput = document.getElementById('endDate');
    if (!releaseDateInput || !endDateInput) return false;
    
    const releaseDate = releaseDateInput.value;
    const endDate = endDateInput.value;
    
    if (releaseDate && endDate && endDate < releaseDate) {
        showMessage('结映日期不能早于首映日期');
        // 清空两个日期字段
        releaseDateInput.value = '';
        endDateInput.value = '';
        // 聚焦到首映日期字段
        releaseDateInput.focus();
        return false; // 返回false表示验证失败
    }
    return true; // 返回true表示验证通过
}

function previewImage(input) {
    const preview = document.getElementById('preview');
    const noPreview = document.getElementById('noPreview');
    const posterName = document.getElementById('poster-name');
    
    if (!preview || !noPreview || !posterName) return;
    
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function(e) {
            preview.src = e.target.result;
            preview.style.display = 'block';
            noPreview.style.display = 'none';
        }
        reader.readAsDataURL(input.files[0]);
        
        // 显示文件名
        posterName.textContent = input.files[0].name;
        posterName.style.display = 'block';
    } else {
        preview.style.display = 'none';
        noPreview.style.display = 'block';
        posterName.style.display = 'none';
    }
}

