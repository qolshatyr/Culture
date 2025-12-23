// --- НАСТРОЙКА ЗВУКА ---
// 1. Загружаем настройку из памяти (по умолчанию включено, если записи нет)
let soundOn = localStorage.getItem('quiz_sound') !== 'off';

const sCorrect = new Audio('https://cdn.pixabay.com/download/audio/2021/08/04/audio_bb630cc098.mp3?filename=success-1-6297.mp3');
const sWrong = new Audio('https://cdn.pixabay.com/download/audio/2022/03/10/audio_c8c8a73467.mp3?filename=wrong-answer-129254.mp3');
const sWin = new Audio('https://cdn.pixabay.com/download/audio/2022/10/24/audio_0df9765799.mp3?filename=winfanfare-6959.mp3');

// 2. Устанавливаем громкость на 60%
sCorrect.volume = 0.6;
sWrong.volume = 0.6;
sWin.volume = 0.6;

// Обновляем иконку при загрузке страницы в соответствии с сохраненной настройкой
const btn = document.getElementById('soundBtn');
if (btn) {
    btn.innerText = soundOn ? '🔊' : '🔇';
}

function toggleSound() {
    soundOn = !soundOn;
    // Обновляем иконку
    document.getElementById('soundBtn').innerText = soundOn ? '🔊' : '🔇';
    // 3. Сохраняем выбор в память браузера
    localStorage.setItem('quiz_sound', soundOn ? 'on' : 'off');
}

function play(type) {
    if (!soundOn) return;
    // Сбрасываем время, чтобы звук можно было проиграть повторно быстро
    if (type === 'c') { sCorrect.currentTime=0; sCorrect.play(); }
    if (type === 'w') { sWrong.currentTime=0; sWrong.play(); }
    if (type === 'f') { sWin.currentTime=0; sWin.play(); }
}

// --- ЛОГИКА ИГРЫ ---
let questions = [];
try {
    // Используем QuizConfig, который определен в HTML
    questions = JSON.parse(QuizConfig.questionsJson);
} catch (e) { console.error("Error parsing JSON", e); }

let idx = 0, score = 0, locked = false, seconds = 0, timerInt;
let streak = 0;

if (questions && questions.length > 0) {
    document.getElementById('totalNum').innerText = questions.length;
    timerInt = setInterval(() => {
        seconds++;
        const m = Math.floor(seconds / 60).toString().padStart(2, '0');
        const s = (seconds % 60).toString().padStart(2, '0');
        document.getElementById('timerDisplay').innerText = `${m}:${s}`;
    }, 1000);
    render();
} else {
    document.getElementById('questionText').innerText = "No questions found!";
}

function shuffle(array) {
    for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]];
    }
}

document.addEventListener('keydown', e => {
    if (!locked && ['1','2','3','4','5'].includes(e.key)) {
        const btns = document.querySelectorAll('.option-btn');
        if (btns[e.key-1]) btns[e.key-1].click();
    }
});

function render() {
    // Если вопросы кончились — завершаем игру
    if (idx >= questions.length) return finish();

    const qCard = document.querySelector('.question-card');
    const opts = document.getElementById('optionsArea');

    // --- СБРОС АНИМАЦИИ ---
    qCard.classList.remove('fade-enter-active');
    qCard.classList.add('fade-enter');
    opts.style.opacity = '0';

    // FORCE REFLOW
    void qCard.offsetWidth;

    setTimeout(() => {
        const q = questions[idx];
        locked = false;

        // Обновляем тексты и прогресс-бар
        document.getElementById('currentNum').innerText = idx + 1;
        document.getElementById('questionText').innerText = q.questionText;
        document.getElementById('progressBar').style.width = ((idx) / questions.length) * 100 + '%';

        opts.innerHTML = '';

        // Собираем варианты ответов
        let variants = [
            {k:'A',t:q.optionA}, {k:'B',t:q.optionB}, {k:'C',t:q.optionC}, {k:'D',t:q.optionD}, {k:'E',t:q.optionE}
        ].filter(v => v.t && v.t.trim());

        shuffle(variants);
        const labels = ['A','B','C','D','E'];

        // Создаем кнопки
        variants.forEach((v, i) => {
            const btn = document.createElement('div');
            btn.className = 'option-btn';
            btn.innerHTML = `
                        <div class="option-key">${labels[i]}</div>
                        <span class="flex-grow-1">${v.t}</span>
                        <span class="key-hint">Key ${i+1}</span>
                    `;
            btn.onclick = () => check(btn, v.k, q.correctAnswer);
            btn.dataset.k = v.k;
            opts.appendChild(btn);
        });

        // --- ЗАПУСК ПЛАВНОГО ПОЯВЛЕНИЯ ---
        requestAnimationFrame(() => {
            qCard.classList.add('fade-enter-active');
            opts.style.opacity = '1';
        });
    }, 50);
}

function check(btn, k, corr) {
    if (locked) return;
    locked = true;
    const cleanK = k ? k.trim().toUpperCase() : "";
    const cleanCorr = corr ? corr.trim().toUpperCase() : "";

    if (cleanK === cleanCorr) {
        // Правильный ответ
        btn.classList.add('btn-correct');
        score++; streak++;
        if (streak > 1) {
            document.getElementById('streakBox').style.display = 'flex';
            document.getElementById('streakVal').innerText = "x" + streak;
        }
        play('c');
    } else {
        // Неправильный ответ
        btn.classList.add('btn-wrong');

        const qCard = document.querySelector('.question-card');
        qCard.classList.remove('shake');
        void qCard.offsetWidth; // Force Reflow
        qCard.classList.add('shake');

        setTimeout(() => {
            qCard.classList.remove('shake');
        }, 500);

        document.querySelectorAll('.option-btn').forEach(b => {
            if (b.dataset.k === cleanCorr) b.classList.add('btn-correct');
        });
        streak = 0;
        document.getElementById('streakBox').style.display = 'none';
        play('w');
    }
    document.getElementById('scoreVal').innerText = score;
    setTimeout(() => { idx++; render(); }, 1200);
}

function finish() {
    clearInterval(timerInt);
    document.getElementById('gameArea').style.display = 'none';
    document.getElementById('inputNameArea').style.display = 'block';
    document.getElementById('progressBar').style.width = '100%';

    // 1. Показываем очки
    document.getElementById('finalScore').innerText = score + " / " + questions.length;

    // 2. Считаем и показываем проценты
    const percentage = Math.round((score / questions.length) * 100);
    document.getElementById('accuracyVal').innerText = percentage + "%";

    // 3. Логика звезд (с анимацией)
    const stars = document.querySelectorAll('#starRating span');

    if (percentage > 0) {
        setTimeout(() => stars[0].classList.add('active'), 250);
    }
    if (percentage >= 50) {
        setTimeout(() => stars[1].classList.add('active'), 650);
    }
    if (percentage >= 80) {
        setTimeout(() => stars[2].classList.add('active'), 1050);
    }

    // Победный звук (если набрал 50%+)
    if (percentage >= 50) {
        setTimeout(() => {
            play('f');
            confetti({particleCount:150, spread:70, origin:{y:0.6}});
        }, 650);
    }
}

function submitScore() {
    const name = document.getElementById('username').value.trim() || "Anon";
    const btn = document.getElementById('saveBtn');
    btn.disabled = true;
    btn.innerText = "Saving...";

    fetch(QuizConfig.submitUrl, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            name: name, score: score, totalQuestions: questions.length,
            timeFormatted: document.getElementById('timerDisplay').innerText,
            timeSeconds: seconds,
            category: QuizConfig.categoryId
        })
    })
        .then(r => r.json())
        .then(() => {
            window.location.href = "/leaderboard?highlight=" + encodeURIComponent(name);
        })
        .catch(() => { btn.disabled=false; alert("Error"); });
}