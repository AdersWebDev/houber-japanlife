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
