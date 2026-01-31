// 公共消息展示函数
function showMessage(message, type = 'info') {
    let messageDiv = document.createElement('div');
    messageDiv.id = 'message';
    messageDiv.className = 'message';
    document.body.appendChild(messageDiv);
    messageDiv.textContent = message;
    messageDiv.className = `message ${type}`;
    messageDiv.style.display = 'block';
    setTimeout(() => {
        messageDiv.style.display = 'none';
    }, 3000);
}