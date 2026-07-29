(function () {
    document.querySelectorAll('form[data-confirm-delete]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            var message = form.getAttribute('data-confirm-delete')
                || '작품을 삭제할까요? 삭제 후에는 되돌릴 수 없습니다.';
            if (!window.confirm(message)) {
                event.preventDefault();
            }
        });
    });
})();
