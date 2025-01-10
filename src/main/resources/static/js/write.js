// 스타일 적용 함수
const editor = document.getElementById('editor');
const uploadedImageIds = [];
function addThumbnail() {
    const box = `
        <div class="detail-thumbnail">
            <input type="file" accept="image/*" onchange="uploadImage(event)" style="display:none;">
            <button onclick="triggerThumbnailUpload(this)">이미지 추가</button>
            <img src="" alt="">
        </div>
    `;
    editor.insertAdjacentHTML('beforeend', box);
}
function addImg() {
    const box = `
        <input type="file" accept="image/*" onchange="uploadImage(event, this)" style="display:none;">
        <button onclick="triggerThumbnailUpload(this)">이미지 추가</button>
        <img src="" alt="">
    `;
    editor.insertAdjacentHTML('beforeend', box);
}
function h2() {
    const box = `
        <div class="middle-title-box">
            <h2>중제목을 입력해주세요</h2>
        </div>
    `
    editor.insertAdjacentHTML('beforeend', box);
}
function h3() {
    const box = `
         <h3>소제목을 입력해주세요</h3>
    `
    editor.insertAdjacentHTML('beforeend', box);
}
function p() {
    const box =`<p>-</p>`
    editor.insertAdjacentHTML('beforeend', box);
}
function checkList(){
    const box =`<p class="checkList">-</p>`
    editor.insertAdjacentHTML('beforeend', box);
}
function checkListLi(){
    const box =`<p class="checkList-li">-</p>`
    editor.insertAdjacentHTML('beforeend', box);
}
function highlight(){
    const editor = document.getElementById('editor');

    // 현재 선택 영역 확인
    const selection = window.getSelection();

    if (selection.rangeCount > 0) {
        const range = selection.getRangeAt(0); // 선택된 텍스트 범위 가져오기
        const span = document.createElement('span');
        span.className = 'highlight';
        span.textContent = selection.toString(); // 선택한 텍스트를 span에 삽입

        // 기존 범위 대체
        range.deleteContents(); // 기존 선택 영역 삭제
        range.insertNode(span); // span을 선택 영역에 삽입
    } else {
        // 선택된 영역이 없을 경우, 에디터 끝에 하이라이트 추가
        const span = document.createElement('span');
        span.className = 'highlight';
        span.textContent = '-';
        editor.appendChild(span);
    }
}
function line() {
    const box =`<hr>`
    editor.insertAdjacentHTML('beforeend', box);
}
// 스타일 초기화 함수
function clearStyle() {
    const editor = document.getElementById('editor');
    editor.innerHTML = ''; // 모두 지우기
}
// 파일 선택 창 열기
// 파일 선택 창 열기
function triggerThumbnailUpload(button) {
    const fileInput = button.previousElementSibling; // input[type="file"]
    fileInput.click();
}

// 이미지 업로드 처리
function uploadImage(event, inputElement) {
    const file = event.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    fetch('/file', {
        method: 'POST',
        body: formData,
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('이미지 업로드 실패');
            }
            return response.json();
        })
        .then(data => {
            let img;

            // 📌 1. 썸네일 (addThumbnail)
            const thumbnailContainer = event.target.closest('.detail-thumbnail');
            if (thumbnailContainer) {
                img = thumbnailContainer.querySelector('img');

                // 이미지 설정
                img.src = data.url;
                img.alt = data.alt;
                img.style.display = 'block';

                // input 제거
                event.target.remove();
                // 버튼 제거
                const button = thumbnailContainer.querySelector('button');
                if (button) button.remove();
            }
            // 📌 2. 일반 이미지 (addImg)
            else if (inputElement) {
                img = inputElement.nextElementSibling.nextElementSibling; // img 태그

                if (img) {
                    img.src = data.url;
                    img.alt = data.alt;
                    img.style.display = 'block';
                }

                // 버튼 제거
                const button = inputElement.nextElementSibling; // "이미지 추가" 버튼
                // input 제거
                inputElement.remove();
                if (button) button.remove();
            }

            // 📌 이미지 ID 저장
            uploadedImageIds.push(data.id);
        })
        .catch(error => {
            console.error('Error:', error);
            alert('이미지 업로드에 실패했습니다.');
        });
}


// 게시글 데이터 전송
function submitPost() {
    const editor = document.getElementById('editor');
    const content = editor.innerHTML;

    const postData = {
        imgList: uploadedImageIds,
        category: "working_holiday", // 카테고리 선택 기능 추가 가능
        thumbnailUrl: uploadedImageIds.length > 0 ? document.querySelector('.detail-thumbnail img').src : null,
        title: "제목을 입력하세요", // 제목 입력 필드 추가 가능
        description: "설명을 입력하세요", // 설명 입력 필드 추가 가능
        keyword: "키워드 입력", // 키워드 입력 필드 추가 가능
        content: content
    };

    fetch('/post', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(postData)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('게시글 저장 실패');
            }
            return response.json();
        })
        .then(data => {
            alert('게시글이 성공적으로 저장되었습니다!');
            console.log('Response:', data);
        })
        .catch(error => {
            console.error('Error:', error);
            alert('게시글 저장에 실패했습니다.');
        });
}

