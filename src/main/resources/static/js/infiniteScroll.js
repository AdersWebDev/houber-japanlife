/** 리스트 페이지 무한 스크롤 ( .grid-items, #loading 존재 시에만 동작 ) */
// 무한 스크롤 설정
let cursorView = null;
let cursorId = null;
let cursorTime = null;
let isLoading = false; // 중복 호출 방지

const contentContainer = document.querySelector('.grid-items');
const loadingIndicator = document.getElementById('loading');

// 📌 URL에서 category 파라미터 가져오기
const urlParams = new URLSearchParams(window.location.search);
const category = urlParams.get('category') || 'hot_post'; // 기본값 설정

// 📌 마지막 요소에서 커서 정보 가져오기
function updateCursorFromLastItem() {
    if (!contentContainer) return;
    const lastImg = contentContainer.querySelector('.grid-item:last-child img');
    if (lastImg) {
        cursorId = lastImg.dataset.id || null;
        cursorView = lastImg.dataset.view || null;
        cursorTime = lastImg.dataset.time || null;
    }
}

// 📌 API 호출 함수
async function fetchMoreContent() {
    if (!contentContainer || !loadingIndicator) return;
    try {
        const actualCursorTime = cursorTime === "null" ? null : cursorTime;
        const queryParams = [
            `category=${category}`,
            cursorView ? `cursorView=${cursorView}` : null,
            cursorId ? `cursorId=${cursorId}` : null,
            actualCursorTime ? `cursorTime=${actualCursorTime}` : null
        ].filter(Boolean);

        const apiUrl = `/post/list?${queryParams.join('&')}`;
        const response = await fetch(apiUrl);
        const data = await response.json();

        if (response.ok && data.length > 0) {
            data.forEach(item => {
                const gridItem = document.createElement('div');
                gridItem.className = 'grid-item';

                const link = document.createElement('a');
                link.href = item.link;

                const imageWrapper = document.createElement('div');
                imageWrapper.classList.add('image-wrapper');
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
            updateCursorFromLastItem();
        } else {
            window.removeEventListener('scroll', handleScroll);
        }
    } catch (error) {
        console.error('데이터를 불러오는 중 오류 발생:', error);
    } finally {
        isLoading = false;
        if (loadingIndicator) loadingIndicator.style.display = 'none';
    }
}

let scrollTimeout = null;
function handleScroll() {
    if (!contentContainer || !loadingIndicator) return;
    if (scrollTimeout) return;
    if (isLoading) return;
    isLoading = true;
    loadingIndicator.style.display = 'block';
    scrollTimeout = setTimeout(() => {
        if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight - 200) {
            fetchMoreContent();
        }
        scrollTimeout = null;
    }, 300);
}

// 스크롤 리스너는 컨테이너와 로딩 요소가 모두 있을 때만 등록
if (contentContainer && loadingIndicator) {
    window.addEventListener('scroll', handleScroll);
}

document.addEventListener('DOMContentLoaded', () => {
    if (contentContainer) updateCursorFromLastItem();
});
