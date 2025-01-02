categoryBtn.addEventListener('click', function () {
    if (categoryModal.classList.contains('active')) {
        categoryModal.classList.remove('active');
    } else {
        categoryModal.classList.add('active');
    }
})
window.addEventListener('load', function () {
    const contentRect = content.getBoundingClientRect();
    goTop.style.right = `${contentRect.left}px`;
})
function updateCategoryPosition () {
    const contentRect = content.getBoundingClientRect();
    categoryModal.style.left = `${contentRect.left + 680}px`;
    indexModal.style.left = `${contentRect.left - 190}px`;
}
// 미디어 쿼리 설정
const mediaQuery = window.matchMedia('(min-width: 1024px)');

// 미디어 쿼리 상태 변화 감지
function handleMediaChange(event) {
    if (event.matches) {

        updateCategoryPosition();
        window.addEventListener('resize', updateCategoryPosition);
    } else {

        categoryModal.style.left = '';
        window.removeEventListener('resize', updateCategoryPosition);
    }
}

// 초기 실행
handleMediaChange(mediaQuery);

// 미디어 쿼리 변경 시 리스너 실행
mediaQuery.addEventListener('change', handleMediaChange);
