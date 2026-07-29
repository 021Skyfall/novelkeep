(function () {
    var button = document.querySelector('[data-back-to-top]');
    if (button) {
        function syncVisibility() {
            if (window.scrollY > 320) {
                button.classList.add('is-visible');
            } else {
                button.classList.remove('is-visible');
            }
        }

        button.addEventListener('click', function () {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });

        window.addEventListener('scroll', syncVisibility, { passive: true });
        syncVisibility();
    }
})();
