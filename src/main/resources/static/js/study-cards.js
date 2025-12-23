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

render();