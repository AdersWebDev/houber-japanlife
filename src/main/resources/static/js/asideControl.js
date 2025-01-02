indexController.addEventListener('click', function () {
    if (indexController.classList.contains('active')) {

        indexModal.classList.remove('active');

        indexModal.classList.add('closing');
        indexController.classList.remove('active');

        indexModal.addEventListener('transitionend', () => {
            indexModal.classList.remove('closing');
        }, { once: true });
    } else {
        indexModal.classList.add('active');
        indexController.classList.add('active');
    }
})
document.addEventListener('DOMContentLoaded', () => {
    const headers = document.querySelectorAll('#detail-content h2, #detail-content h3');
    const indexItems = document.querySelectorAll('#indexing-modal li');

    let currentActiveIndex = -1;

    // 스크롤 시 활성화 상태 업데이트
    const onScroll = () => {
        let activeIndex = currentActiveIndex;

        headers.forEach((header, index) => {
            const rect = header.getBoundingClientRect();
            const viewportHeight = window.innerHeight;

            if (rect.top <= viewportHeight / 2 && rect.bottom >= viewportHeight / 2) {
                activeIndex = index;
            }
        });

        // Active 클래스 업데이트
        if (activeIndex !== currentActiveIndex && activeIndex !== -1) {
            indexItems.forEach(item => item.classList.remove('active'));
            indexItems[activeIndex].classList.add('active');
            currentActiveIndex = activeIndex;
        }
    };

    // 각 목차 항목에 클릭 이벤트 추가
    indexItems.forEach((item) => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const targetId = item.querySelector('a').getAttribute('href').substring(1);
            const targetElement = document.getElementById(targetId);
            if (targetElement) {
                targetElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        });
    });

    window.addEventListener('scroll', onScroll, { passive: true });
    window.addEventListener('resize', onScroll, { passive: true });
    onScroll();
});
