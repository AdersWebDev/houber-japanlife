const slider = document.querySelector('.slider');
const items = document.querySelectorAll('.slider .item');
const dotsContainer = document.querySelector('.dots-container');

let currentIndex = 0;
let isDragging = false;
let startPos = 0;
let currentTranslate = 0;
let prevTranslate = 0;

// 🔹 1. 점(dot) 생성
items.forEach((_, index) => {
    const dot = document.createElement('div');
    dot.classList.add('dot');
    if (index === 0) dot.classList.add('active');
    dot.addEventListener('click', () => goToSlide(index));
    dotsContainer.appendChild(dot);
});

const dots = document.querySelectorAll('.dot');

// 🔹 2. 슬라이드 이동 함수
function updateSliderPosition() {
    slider.style.transform = `translateX(-${currentIndex * 150}px)`;
    dots.forEach((dot, index) => {
        dot.classList.toggle('active', index === currentIndex);
    });
}

// 🔹 3. 마우스 & 터치 이벤트
slider.addEventListener('touchstart', startDrag);
slider.addEventListener('touchmove', drag);
slider.addEventListener('touchend', endDrag);

function getPositionX(e) {
    return e.type.includes('touch') ? e.touches[0].clientX : e.clientX;
}
function startDrag(e) {
    isDragging = true;
    startPos = getPositionX(e);
    slider.classList.add('grabbing');
}

function drag(e) {
    if (!isDragging) return;
    const currentPosition = getPositionX(e);
    currentTranslate = prevTranslate + currentPosition - startPos;
    slider.style.transform = `translateX(${currentTranslate}px)`;
}

function endDrag() {
    isDragging = false;
    slider.classList.remove('grabbing');
    const movedBy = currentTranslate - prevTranslate;

    if (movedBy < -50 && currentIndex < items.length - 1) currentIndex++;
    if (movedBy > 50 && currentIndex > 0) currentIndex--;

    updateSliderPosition();
    prevTranslate = -currentIndex * 150; // 각 슬라이드의 넓이에 맞춤
}


// 🔹 5. 점(dot) 클릭 이동
function goToSlide(index) {
    currentIndex = index;
    updateSliderPosition();
}
