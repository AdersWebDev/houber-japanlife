document.querySelectorAll('.slider').forEach(slider => {
    let isDragging = false;
    let startPos = 0;
    let currentTranslate = 0;
    let prevTranslate = 0;
    let animationID = 0;
    let isClick = true;
    let movedDistance = 0;

    function getPositionX(e) {
        return e.type.includes('touch') ? e.touches[0].clientX : e.clientX;
    }

    function setSliderPosition() {
        slider.style.transform = `translateX(${currentTranslate}px)`;
    }

    function animation() {
        setSliderPosition();
        if (isDragging) requestAnimationFrame(animation);
    }

    function startDrag(e) {
        isDragging = true;
        isClick = true;
        startPos = getPositionX(e);
        movedDistance = 0;
        slider.classList.add('dragging');
        animationID = requestAnimationFrame(animation);
    }

    function drag(e) {
        if (!isDragging) return;

        const currentPosition = getPositionX(e);
        const movedBy = currentPosition - startPos;
        movedDistance = Math.abs(movedBy);

        if (movedDistance > 5) isClick = false;

        currentTranslate = prevTranslate + movedBy;

        const maxTranslate = 0;
        const minTranslate = slider.offsetWidth - slider.scrollWidth;
        currentTranslate = Math.max(minTranslate, Math.min(currentTranslate, maxTranslate));
    }

    function endDrag() {
        isDragging = false;
        cancelAnimationFrame(animationID);
        slider.classList.remove('dragging');
        prevTranslate = currentTranslate;
    }

    // 이벤트 등록
    slider.addEventListener('mousedown', startDrag);
    slider.addEventListener('mousemove', drag);
    slider.addEventListener('mouseup', endDrag);
    slider.addEventListener('mouseleave', endDrag);

    slider.addEventListener('touchstart', startDrag);
    slider.addEventListener('touchmove', drag);
    slider.addEventListener('touchend', endDrag);

    // 링크 클릭 방지
    slider.querySelectorAll('a').forEach(link => {
        link.addEventListener('click', (e) => {
            if (!isClick) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    });
});
