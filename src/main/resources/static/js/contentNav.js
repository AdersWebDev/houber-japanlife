// const guidSection = document.getElementById('guide');
// const lifeSection = document.getElementById('life');
//
// guidSection.querySelectorAll('.nav-item').forEach(item => {
//     item.addEventListener('click', function () {
//         // 모든 항목의 active 클래스 제거
//         guidSection.querySelectorAll('.nav-item').forEach(nav => nav.classList.remove('active'));
//         // 현재 클릭된 항목에 active 클래스 추가
//         this.classList.add('active');
//     });
// });
// lifeSection.querySelectorAll('.nav-item').forEach(item => {
//     item.addEventListener('click', function () {
//         // 모든 항목의 active 클래스 제거
//         lifeSection.querySelectorAll('.nav-item').forEach(nav => nav.classList.remove('active'));
//         // 현재 클릭된 항목에 active 클래스 추가
//         this.classList.add('active');
//     });
// });
//
//
// document.addEventListener('click', function () {
//     const guideMoreButton = guidSection.getElementById('guide-more');
//     const guideNav = guidSection.querySelector('.content-nav');
//
//     guideMoreButton.addEventListener('click', (event) => {
//         event.preventDefault(); // 기본 링크 동작 방지
//
//         // 현재 활성화된 카테고리 가져오기
//         const activeNav = guideNav.querySelector('.nav-item.active');
//         const category = activeNav.getAttribute('data-category');
//
//         // 페이지 이동
//         window.location.href = `/guide/more?category=${category}`;
//     });
// });
// document.addEventListener('click', function () {
//     const guideMoreButton = lifeSection.getElementById('life-more');
//     const guideNav = lifeSection.querySelector('.content-nav');
//
//     guideMoreButton.addEventListener('click', (event) => {
//         event.preventDefault(); // 기본 링크 동작 방지
//
//         // 현재 활성화된 카테고리 가져오기
//         const activeNav = guideNav.querySelector('.nav-item.active');
//         const category = activeNav.getAttribute('data-category');
//
//         // 페이지 이동
//         window.location.href = `/life/more?category=${category}`;
//     });
// });
