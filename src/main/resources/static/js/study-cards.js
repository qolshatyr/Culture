// Используем StudyConfig для получения данных
const qs = StudyConfig.questions;
let i = 0;
const scene = document.querySelector('.scene');

function render() {
    if (!qs || qs.length === 0) return;
    const q = qs[i];

    // Фронтальная сторона
    document.getElementById('qText').innerText = q.questionText;
    document.getElementById('topic').innerText = q.topic || 'General';

    // Задняя сторона
    document.getElementById('ansKey').innerText = q.correctAnswer;
    let txt = "";
    if(q.correctAnswer==='A') txt=q.optionA;
    if(q.correctAnswer==='B') txt=q.optionB;
    if(q.correctAnswer==='C') txt=q.optionC;
    if(q.correctAnswer==='D') txt=q.optionD;
    if(q.correctAnswer==='E') txt=q.optionE;
    document.getElementById('ansText').innerText = txt;

    // Счетчики
    document.getElementById('idx').innerText = i+1;
    document.getElementById('total').innerText = qs.length;

    // Сброс переворота при смене вопроса
    scene.classList.remove('flipped');
}

function flip() { scene.classList.toggle('flipped'); }
function nav(d) { i+=d; if(i<0)i=qs.length-1; if(i>=qs.length)i=0; render(); }

document.addEventListener('keydown', e => {
    if(e.code==='Space') flip();
    if(e.code==='ArrowRight') nav(1);
    if(e.code==='ArrowLeft') nav(-1);
});

// --- 1. SWIPE GESTURES (Для мобильных) ---
let touchStartX = 0;
let touchEndX = 0;
const minSwipeDistance = 50; // Минимальная дистанция для свайпа

document.addEventListener('touchstart', e => {
    touchStartX = e.changedTouches[0].screenX;
}, {passive: true});

document.addEventListener('touchend', e => {
    touchEndX = e.changedTouches[0].screenX;
    handleSwipe();
}, {passive: true});

function handleSwipe() {
    const distance = touchEndX - touchStartX;
    if (Math.abs(distance) < minSwipeDistance) return;

    if (distance < 0) {
        // Свайп влево (палец движется влево) -> Следующий вопрос
        nav(1);
    } else {
        // Свайп вправо -> Предыдущий вопрос
        nav(-1);
    }
}

// --- 2. 3D TILT EFFECT (Для десктопа) ---
// Эффект параллакса при наведении мыши
scene.addEventListener('mousemove', (e) => {
    // Отключаем эффект, если устройство сенсорное (чтобы не мешать скроллу/свайпу)
    if (window.matchMedia("(pointer: coarse)").matches) return;

    const rect = scene.getBoundingClientRect();
    // Координаты мыши внутри элемента
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    // Вычисляем центр
    const centerX = rect.width / 2;
    const centerY = rect.height / 2;

    // Вычисляем поворот (чувствительность / 20)
    const rotateY = ((x - centerX) / 20); // Поворот вокруг оси Y зависит от X
    const rotateX = -((y - centerY) / 20); // Поворот вокруг оси X зависит от Y (инвертируем)

    // Применяем трансформацию.
    // transition: none нужен для мгновенного отклика при движении
    scene.style.transition = 'none';
    scene.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`;
});

// Сброс при уходе мыши
scene.addEventListener('mouseleave', () => {
    // Возвращаем плавную анимацию для сброса
    scene.style.transition = 'transform 0.5s ease-out';
    scene.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg)';
});

render();