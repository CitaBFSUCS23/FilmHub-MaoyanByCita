// Recharge页面JavaScript逻辑

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    loadCurrentBalance();
    setupEventListeners();
});

// 设置事件监听器
function setupEventListeners() {
    // 充值表单提交
    document.getElementById('recharge-form').addEventListener('submit', function(e) {
        e.preventDefault();
        submitRecharge();
    });
}

// 获取当前余额
function loadCurrentBalance() {
    const userEmail = sessionStorage.getItem('userEmail');
    if (!userEmail) return;
    
    fetch('/Recharge', {
        method: 'GET',
        headers: {
            'X-User-Email': userEmail
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            document.getElementById('current-balance').textContent = `¥${data.balance.toFixed(2)}`;
        } else {
            showMessage(data.message, 'error');
        }
    })
    .catch(error => {
        showMessage('获取余额失败，请稍后重试', 'error');
    });
}

// 提交充值请求
function submitRecharge() {
    const userEmail = sessionStorage.getItem('userEmail');
    if (!userEmail) return;
    
    // 验证表单
    const amountInput = document.getElementById('amount');
    const amount = parseFloat(amountInput.value);
    
    // 构建请求数据
    const params = new URLSearchParams();
    params.append('amount', amount.toFixed(2));
    
    // 显示加载状态
    const submitBtn = document.querySelector('.submit-btn');
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 充值中...';
    submitBtn.disabled = true;
    
    // 发送充值请求
    fetch('/Recharge', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-User-Email': userEmail
        },
        body: params.toString()
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
                showMessage(data.message, 'success');
                // 更新余额显示
                loadCurrentBalance();
                // 清空充值金额输入框，方便用户进行下一次充值
                document.getElementById('amount').value = '';
            } else {
                showMessage(data.message, 'error');
            }
    })
    .catch(error => {
        showMessage('充值失败，请稍后重试', 'error');
    })
    .finally(() => {
        // 恢复按钮状态
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
    });
}

// 显示错误信息


// 显示成功信息

