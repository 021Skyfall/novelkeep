(function () {
    var input = document.querySelector('[data-char-input]');
    var counter = document.querySelector('[data-char-count]');
    if (!input || !counter) {
        return;
    }

    function countCharacters(value) {
        return value.replace(/\r/g, '').replace(/\n/g, '').length;
    }

    function refresh() {
        counter.textContent = String(countCharacters(input.value || ''));
    }

    input.addEventListener('input', refresh);
    refresh();
})();
