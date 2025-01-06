const slider = document.querySelector('.slider');
const items = document.querySelectorAll('.slider .item');
const dotsContainer = document.querySelector('.dots-container');

let isMain = dotsContainer != null;
let currentIndex = 0;
let isDragging = false;
let isClick = true; // 클릭 여부를 판단
let startPos = 0;
let currentTranslate = 0;
let prevTranslate = 0;

// 🔹 X 좌표 가져오기
function getPositionX(e) {
    return e.type.includes('touch') ? e.touches[0].clientX : e.clientX;
}

// 🔹 이미지 및 링크 드래그 방지
slider.querySelectorAll('a, img').forEach(el => {
    el.addEventListener('dragstart', (e) => e.preventDefault());
});

// 🔹 1. 점(dot) 생성
if (isMain) {
    items.forEach((_, index) => {
        const dot = document.createElement('div');
        dot.classList.add('dot');
        if (index === 0) dot.classList.add('active');
        dot.addEventListener('click', () => goToSlide(index));
        dotsContainer.appendChild(dot);
    });
}

const dots = document.querySelectorAll('.dot');

// 🔹 2. 슬라이드 이동 함수
function updateSliderPosition() {
    slider.style.transition = 'transform 0.3s ease-in-out';
    slider.style.transform = `translateX(-${currentIndex * 150}px)`;

    if (isMain) {
        dots.forEach((dot, index) => {
            dot.classList.toggle('active', index === currentIndex);
        });
    }
    prevTranslate = -currentIndex * 150; // 현재 위치 저장
}

// 🔹 3. 드래그 이벤트
function startDrag(e) {
    isDragging = false; // 초기값 설정
    isClick = true; // 클릭 가능 상태
    startPos = getPositionX(e);
    slider.classList.add('dragging');
    slider.style.transition = 'none'; // 드래그 중 transition 제거
}

function drag(e) {
    if (!startPos) return; // 드래그 시작점이 없으면 리턴
    const currentPosition = getPositionX(e);
    const movedBy = currentPosition - startPos;

    // 클릭 여부 판단
    if (Math.abs(movedBy) > 5) isClick = false;

    currentTranslate = prevTranslate + movedBy;
    slider.style.transform = `translateX(${currentTranslate}px)`;
}

function endDrag() {
    if (!startPos) return;
    slider.classList.remove('dragging');

    const movedBy = currentTranslate - prevTranslate;

    // 슬라이드 이동 범위에 따라 인덱스 업데이트
    if (movedBy < -50 && currentIndex < items.length - 1) currentIndex++;
    if (movedBy > 50 && currentIndex > 0) currentIndex--;

    updateSliderPosition();

    // 초기화
    isDragging = false;
    startPos = 0;
}

// 🔹 4. 점(dot) 클릭 이동
function goToSlide(index) {
    currentIndex = index;
    updateSliderPosition();
}

// 🔹 5. 클릭 이벤트 처리 (링크 이동 방지)
slider.querySelectorAll('.item a').forEach(link => {
    link.addEventListener('click', (e) => {
        if (!isClick) {
            e.preventDefault(); // 드래그 중 클릭 방지
        }
    });
});

// 🔹 6. 이벤트 리스너 등록
slider.addEventListener('mousedown', startDrag);
slider.addEventListener('mousemove', drag);
slider.addEventListener('mouseup', endDrag);
slider.addEventListener('mouseleave', endDrag);
slider.addEventListener('touchstart', startDrag);
slider.addEventListener('touchmove', drag);
slider.addEventListener('touchend', endDrag);

// 🔹 7. 브라우저 창 크기 변경 대응
window.addEventListener('resize', updateSliderPosition);
