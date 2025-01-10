document.addEventListener("DOMContentLoaded", function () {
    const iframe = document.querySelector("iframe");

    if (iframe) {
        iframe.onerror = function () {
            alert("동영상을 로드하는 중 오류가 발생했습니다. 광고 차단기를 비활성화하거나 URL을 확인해주세요.");
        };

        iframe.onload = function () {
            console.log("YouTube iframe loaded successfully.");
        };
    }
});
