// 📌 카테고리 버튼 클릭 이벤트
categoryBtn.addEventListener('click', function () {
    categoryModal.classList.toggle('active');
});

// 📌 화면 로드 및 리사이즈 시 위치 업데이트 함수
function updateCategoryPosition() {
    const contentRect = content.getBoundingClientRect();
    if (window.innerWidth >= 1024) {
        // 데스크탑 화면에서는 위치 설정
        categoryModal.style.left = `${contentRect.right + 10}px`;
        indexModal.style.left = `${contentRect.left - 190}px`;
        goTop.style.left = `${contentRect.right + 2}px`;
    } else {
        // 모바일 화면에서는 위치 초기화
        categoryModal.style.left = '';
        indexModal.style.left = '';
        goTop.style.left = '';
    }
}

// 📌 이벤트 리스너 등록
window.addEventListener('load', updateCategoryPosition);
window.addEventListener('resize', updateCategoryPosition);
window.addEventListener('orientationchange', updateCategoryPosition);
window.matchMedia('(min-width: 1024px)').addEventListener('change', updateCategoryPosition);
