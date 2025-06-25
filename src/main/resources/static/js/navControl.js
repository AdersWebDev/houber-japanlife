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
        const categoryRect =categoryModal.getBoundingClientRect();
        const floatingRect = floatingDiv.getBoundingClientRect();
        floatingDiv.style.right = '';
        floatingDiv.style.left = `${categoryRect.right - floatingRect.width}px`;
        if (indexModal)
            indexModal.style.left = `${contentRect.left - 190}px`;
        consultingBox.style.scale = `1`;
    } else {
        // 모바일 화면에서는 위치 초기화
        categoryModal.style.left = '';
        floatingDiv.style.bottom = `${70  + 30}px`;
        floatingDiv.style.left = '';
        floatingDiv.style.right = `${14}px`;
        if (indexModal)
            indexModal.style.left = '';
        consultingBox.style.scale = `0.9`;
    }
}

// 📌 이벤트 리스너 등록
window.addEventListener('load', updateCategoryPosition);
window.addEventListener('resize', updateCategoryPosition);
window.addEventListener('orientationchange', updateCategoryPosition);
window.matchMedia('(min-width: 1024px)').addEventListener('change', updateCategoryPosition);
