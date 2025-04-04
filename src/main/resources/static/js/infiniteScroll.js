// 무한 스크롤 설정
let cursorView = null;
let cursorId = null;
let cursorTime = null;
let isLoading = false; // 중복 호출 방지

const contentContainer = document.querySelector('.grid-items');
const loadingIndicator = document.getElementById('loading');

// 📌 스크롤 이벤트 등록

// 📌 URL에서 category 파라미터 가져오기
const urlParams = new URLSearchParams(window.location.search);
const category = urlParams.get('category') || 'hot_post'; // 기본값 설정

// 📌 마지막 요소에서 커서 정보 가져오기
function updateCursorFromLastItem() {
    const lastImg = contentContainer.querySelector('.grid-item:last-child img');
    if (lastImg) {
        cursorId = lastImg.dataset.id || null;
        cursorView = lastImg.dataset.view || null;
        cursorTime = lastImg.dataset.time || null;
    }
}

// 📌 API 호출 함수
async function fetchMoreContent() {

    try {
        // cursorTime 값이 "null" 문자열일 경우 null로 처리
        const actualCursorTime = cursorTime === "null" ? null : cursorTime;

        // 쿼리 파라미터를 배열로 관리하여 null 값인 경우 제외
        const queryParams = [
            `category=${category}`,
            cursorView ? `cursorView=${cursorView}` : null,
            cursorId ? `cursorId=${cursorId}` : null,
            actualCursorTime ? `cursorTime=${actualCursorTime}` : null // "null" 문자열도 제외
        ].filter(Boolean); // null 값을 필터링

        const apiUrl = `/post/list?${queryParams.join('&')}`;

        const response = await fetch(apiUrl);
        const data = await response.json();

        if (response.ok && data.length > 0) {
            // 콘텐츠 추가
            data.forEach(item => {
                const gridItem = document.createElement('div');
                gridItem.className = 'grid-item';

                const link = document.createElement('a');
                link.href = item.link;

                const imageWrapper = document.createElement('div');
                imageWrapper.classList.add('image-wrapper')
                const img = document.createElement('img');
                img.src = item.thumbnailUrl;
                img.alt = item.title;
                img.dataset.id = item.id;
                img.dataset.view = item.view;
                img.dataset.time = item.cursorTime;
                const divTitle = document.createElement('div');
                divTitle.classList.add('title');
                divTitle.innerText = item.title;

                imageWrapper.appendChild(img);
                imageWrapper.appendChild(divTitle);
                link.appendChild(imageWrapper);
                gridItem.appendChild(link);
                contentContainer.appendChild(gridItem);
            });

            // 마지막 요소에서 커서 정보 업데이트
            updateCursorFromLastItem();
        } else {
            console.log('더 이상 불러올 데이터가 없습니다.');
            window.removeEventListener('scroll', handleScroll); // 스크롤 이벤트 제거
        }
    } catch (error) {
        console.error('데이터를 불러오는 중 오류 발생:', error);
    } finally {
        isLoading = false;
        loadingIndicator.style.display = 'none';
    }
}
// 📌 스크롤 이벤트 리스너
let scrollTimeout = null;
function handleScroll() {
    if (scrollTimeout) return; // 이미 타이머가 설정된 경우 실행하지 않음
    if (isLoading) return;
    isLoading = true;
    loadingIndicator.style.display = 'block';
    scrollTimeout = setTimeout(() => {
        if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight - 200) {
            fetchMoreContent();
        }
        scrollTimeout = null; // 타이머 초기화
    }, 300); // 1초 딜레이
}

window.addEventListener('scroll', handleScroll);
// 📌 초기 커서 설정
document.addEventListener('DOMContentLoaded', () => {
    updateCursorFromLastItem();
});
