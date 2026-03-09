/** 카테고리/인덱싱 모달 제어 (main.js 로드 후 실행) */
var categoryNavWrapper = document.getElementById('category-nav-wrapper');

if (categoryBtn && categoryModal) {
    categoryBtn.addEventListener('click', function () {
        categoryModal.classList.toggle('active');
        if (categoryNavWrapper)
            categoryNavWrapper.classList.toggle('is-open');
    });
}

// 📌 인덱싱 모달만 위치 업데이트 (카테고리는 레이아웃 우측 인라인으로 표시)
function updateCategoryPosition() {
    if (!content || !indexModal) return;
    const contentRect = content.getBoundingClientRect();
    if (window.innerWidth >= 1024)
        indexModal.style.left = `${contentRect.left - 190}px`;
    else
        indexModal.style.left = '';
}

function debounce(fn, ms) {
    var t;
    return function () {
        clearTimeout(t);
        t = setTimeout(fn, ms);
    };
}
var scheduleUpdate = debounce(updateCategoryPosition, 150);

window.addEventListener('load', updateCategoryPosition);
window.addEventListener('resize', scheduleUpdate);
window.addEventListener('orientationchange', scheduleUpdate);
window.matchMedia('(min-width: 1024px)').addEventListener('change', updateCategoryPosition);
