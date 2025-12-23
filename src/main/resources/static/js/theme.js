document.addEventListener("DOMContentLoaded", () => {
    const body = document.body;
    const themeBtn = document.getElementById('themeBtn');

    // Проверка сохраненной темы при загрузке
    if (localStorage.getItem('theme') === 'dark') {
        body.classList.add('dark-mode');
        if(themeBtn) themeBtn.innerText = '☀️';
    }
});

// Глобальная функция переключения (должна быть доступна из HTML onclick)
window.toggleTheme = function() {
    const body = document.body;
    const themeBtn = document.getElementById('themeBtn');

    body.classList.toggle('dark-mode');
    const isDark = body.classList.contains('dark-mode');

    if(themeBtn) themeBtn.innerText = isDark ? '☀️' : '🌙';
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
};