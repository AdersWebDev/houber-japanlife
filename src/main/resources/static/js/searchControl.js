/** 검색 결과 페이지 무한 스크롤 */
let keyword = document.querySelector('.searched-list') && document.querySelector('.searched-list').dataset.keyword || null;
let isLoading = false; // 중복 호출 방지
let cursorTime = null;
const searchContainer = document.querySelector('.searched-list');
const loadingIndicator = document.getElementById('loading');

// 📌 마지막 요소에서 커서 정보 가져오기
function updateCursorFromLastItem() {
    const lastSearchItem = searchContainer.querySelector('.searched-box:last-child a div');
    if (lastSearchItem) {
        cursorTime = lastSearchItem.dataset.time || null;
    }
}

// 📌 API 호출 함수
async function fetchMoreContent() {
    if (keyword == null || cursorTime == null) return
    try {
        const apiUrl = `/post/search?keyword=`
            + keyword
            + `&cursorTime=`
            + cursorTime;

        const response = await fetch(apiUrl);
        const data = await response.json();

        if (response.ok && data.length > 0) {
            // 콘텐츠 추가
            let content = '';
            data.forEach(item => {
                content += `
                <div class="searched-box">
                    <a href="${item.link}">
                        <div data-time="${item.dateTime}">
                            <h3>${item.title}</h3>
                            <p>${item.description}</p>
                            <div class="view-count-box">
                                <img src="/asset/viewCount.png" alt="게시글이 읽힌 수(counting post view)">
                                <span class="s">${item.view}</span>
                            </div>
                        </div>
                    </a>
                    <a href="${item.link}">
                        <div class="searched-box-thumbnail">
                            <img src="${item.thumbnailUrl}" alt="${item.title}">
                        </div>
                    </a>
                </div>
                `
            });
            searchContainer.insertAdjacentHTML('beforeend', content);

        } else {
            window.removeEventListener('scroll', handleScroll); // 스크롤 이벤트 제거
        }
    } catch (error) {
        console.error('데이터를 불러오는 중 오류 발생:', error);
    } finally {
        // 마지막 요소에서 커서 정보 업데이트
        updateCursorFromLastItem();
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
