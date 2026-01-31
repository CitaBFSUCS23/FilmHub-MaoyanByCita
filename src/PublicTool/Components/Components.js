// 加载公共组件
function loadComponents() {
    // 加载Components.html
    fetch('/PublicTool/Components/Components.html')
        .then(response => response.text())
        .then(html => {
            document.body.insertAdjacentHTML('afterbegin', html);
            // 初始化导航板拖拽功能
            initNavigationBoardDrag();
        })
}

// 导航板拖拽功能
function initNavigationBoardDrag() {
    const navigationBoard = document.getElementById('navigationBoard');
    if (!navigationBoard) return;

    let isDragging = false;
    let offsetX, offsetY;

    // 鼠标按下事件
    navigationBoard.addEventListener('mousedown', function(e) {
        // 只允许在头部拖拽
        if (e.target.closest('.navigation-board-header') || e.target === navigationBoard) {
            isDragging = true;
            offsetX = e.clientX - navigationBoard.getBoundingClientRect().left;
            offsetY = e.clientY - navigationBoard.getBoundingClientRect().top;
            navigationBoard.classList.add('dragging');
        }
    });

    // 鼠标移动事件
    document.addEventListener('mousemove', function(e) {
        if (!isDragging) return;

        // 计算新位置
        let newLeft = e.clientX - offsetX;
        let newTop = e.clientY - offsetY;

        // 限制在视窗内
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;
        const boardWidth = navigationBoard.offsetWidth;
        const boardHeight = navigationBoard.offsetHeight;

        newLeft = Math.max(0, Math.min(newLeft, viewportWidth - boardWidth));
        newTop = Math.max(0, Math.min(newTop, viewportHeight - boardHeight));

        // 更新位置
        navigationBoard.style.left = newLeft + 'px';
        navigationBoard.style.top = newTop + 'px';
        navigationBoard.style.right = 'auto';
        navigationBoard.style.bottom = 'auto';
    });

    // 鼠标释放事件
    document.addEventListener('mouseup', function() {
        if (isDragging) {
            isDragging = false;
            navigationBoard.classList.remove('dragging');
        }
    });

    // 鼠标离开窗口事件
    document.addEventListener('mouseleave', function() {
        if (isDragging) {
            isDragging = false;
            navigationBoard.classList.remove('dragging');
        }
    });
}

// 检测用户角色并显示对应的导航内容
function detectUserRole() {
    // 从sessionStorage或URL参数获取用户角色信息
    const currentUser = sessionStorage.getItem('currentUser') || 'Guest';
    
    // 获取多角色信息
    const userRolesStr = sessionStorage.getItem('userRoles');
    let userRoles = [];
    if (userRolesStr) {
        try {
            userRoles = JSON.parse(userRolesStr);
        } catch (e) {
            userRoles = [];
        }
    }
    
    // 确定用户是否已登录 - 使用CheckState中的isLoggedIn函数
    const isLoggedIn = CheckState.isLoggedIn();
    
    // 确定用户角色
    const hasGeneralRole = userRoles.includes('general');
    const hasManager = userRoles.includes('manager');
    const hasPublisher = userRoles.includes('publisher');
    
    return { currentUser, userRoles, isLoggedIn, hasGeneralRole, hasManager, hasPublisher };
}

// 根据用户角色显示对应的导航内容
function updateNavigationContent() {
    const { currentUser, userRoles, isLoggedIn, hasGeneralRole, hasManager, hasPublisher } = detectUserRole();
    const navigationContent = document.getElementById('navigationContent');
    const logoutLink = document.getElementById('logoutLink');
    
    if (!navigationContent || !logoutLink) return;
    
    // 隐藏所有身份卡片
    const allRoleCards = navigationContent.querySelectorAll('.role-card');
    allRoleCards.forEach(card => {
        card.style.display = 'none';
    });
    
    // 显示登出项
    const logoutItem = navigationContent.querySelector('.logout-item');
    if (logoutItem) {
        logoutItem.style.display = 'block';
        logoutLink.textContent = `Logout (Current user: ${currentUser})`;
        logoutLink.href = getLogoutUrl();
    }
    
    // 根据用户角色显示对应的身份卡片
    if (isLoggedIn) {
        // 显示普通用户卡片（所有登录用户都能看到）
        if (hasGeneralRole) {
            const generalCard = navigationContent.querySelector('.general-card');
            if (generalCard) generalCard.style.display = 'block';
        }
        
        // 检查并显示manager角色卡片
        if (hasManager) {
            const managerCard = navigationContent.querySelector('.manager-card');
            if (managerCard) managerCard.style.display = 'block';
        }
        
        // 检查并显示publisher角色卡片
        if (hasPublisher) {
            const publisherCard = navigationContent.querySelector('.publisher-card');
            if (publisherCard) publisherCard.style.display = 'block';
        }
    } else {
        // 显示guest角色卡片（未登录时）
        const guestCard = navigationContent.querySelector('.guest-card');
        if (guestCard) guestCard.style.display = 'block';
    }
}

// 获取登出URL - 统一跳转到GLogin页面
function getLogoutUrl() {
    return '../../Login/GLogin/GLogin.html';
}

// 处理登出操作
function handleLogout() {
    // 暴力清除所有sessionStorage数据
    sessionStorage.clear();
    
    // 统一跳转到GLogin页面
    const logoutUrl = getLogoutUrl();
    
    // 清除sessionStorage后跳转
    setTimeout(() => {
        window.location.href = logoutUrl;
    }, 100);
}

// 检查登录状态，如果未登录则跳转到登录页面
function checkLoginStatus() {
    const currentPath = window.location.pathname;
    
    // 排除登录和注册页面（不需要登录验证）
    if (currentPath.includes('/Login/') || currentPath.includes('/General/Register/Register.html')) {
        return true;
    }
    
    // 使用CheckState中的checkLoginState函数
    return CheckState.checkLoginState();
}

// 页面加载完成后加载组件并更新导航内容
document.addEventListener('DOMContentLoaded', function() {
    // 先检查登录状态
    if (!checkLoginStatus()) {
        return;
    }
    
    loadComponents();
    
    // 延迟执行以确保组件已加载
    setTimeout(() => {
        updateNavigationContent();
    }, 100);
});