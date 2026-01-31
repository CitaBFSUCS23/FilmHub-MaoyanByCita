// 公共登录状态检查脚本
// 在所有需要登录的页面中引入此脚本，确保用户已登录

// 检查用户登录状态
function checkLoginState() {
    // 获取当前页面路径
    const currentPath = window.location.pathname;
    
    // 排除登录页面（不需要检查）
    const loginPages = [
        '/Login/GLogin/GLogin.html',
        '/General/Register/Register.html'
    ]; 
    // 检查当前页面是否为登录页面
    const isLoginPage = loginPages.some(page => currentPath.includes(page));
    
    if (isLoginPage) {
        return true; // 登录页面不需要检查，返回true
    }
    
    // 检查sessionStorage中是否有用户信息
    const userEmail = sessionStorage.getItem('userEmail');
    const userRolesStr = sessionStorage.getItem('userRoles');
    
    // 如果没有用户信息，跳转到对应角色的登录页面
    if (!userEmail || !userRolesStr) {
        redirectToLogin();
        return false;
    }
    
    // 解析用户角色
    let userRoles = [];
    try {
        userRoles = JSON.parse(userRolesStr);
    } catch (e) {
        redirectToLogin();
        return false;
    }
    
    // 检查用户角色与页面路径是否匹配（任何角色匹配即可）
    if (!checkRoleAccess(userRoles, currentPath)) {
        redirectToLogin();
        return false;
    }
    
    // 用户已登录且有权限访问当前页面，返回true
    return true;
}

// 检查用户角色是否有权限访问当前页面（支持多角色）
function checkRoleAccess(userRoles, currentPath) {
    const roleAccessMap = {
        'general': [
            '/General/',
            '/PublicTool/',
            '/Login/',
            '/Manager/OpenCinema/',
            '/Publisher/BePublisher'
        ],
        'manager': [
            '/General/',
            '/Manager/',
            '/PublicTool/',
            '/Login/'
        ],
        'publisher': [
            '/General/',
            '/Publisher/',
            '/PublicTool/',
            '/Login/'
        ]
    };
    
    // 检查是否有任何角色有权限访问当前页面
    return userRoles.some(role => {
        const allowedPaths = roleAccessMap[role] || [];
        return allowedPaths.some(path => currentPath.includes(path));
    });
}

// 跳转到登录页面
function redirectToLogin() {
    // 清除所有sessionStorage数据
    sessionStorage.clear(); 
    // 跳转到登录页面
    window.location.href = '/Login/GLogin/GLogin.html';
}

// 获取当前用户信息
function getCurrentUser() {
    const userRolesStr = sessionStorage.getItem('userRoles');
    let userRoles = [];
    try {
        if (userRolesStr) {
            userRoles = JSON.parse(userRolesStr);
        }
    } catch (e) {
        userRoles = [];
    }
    
    return {
        email: sessionStorage.getItem('userEmail'),
        roles: userRoles,
        name: sessionStorage.getItem('currentUser')
    };
}

// 检查用户是否已登录
function isLoggedIn() {
    const user = getCurrentUser();
    return !!(user.email && user.roles && user.roles.length > 0);
}

// 页面加载时自动检查登录状态
document.addEventListener('DOMContentLoaded', function() {
    // 延迟执行，确保其他脚本已加载
    setTimeout(() => {
        checkLoginState();
    }, 100);
});

// 导出函数供其他脚本使用
window.CheckState = {
    checkLoginState,
    checkRoleAccess,
    redirectToLogin,
    getCurrentUser,
    isLoggedIn
};