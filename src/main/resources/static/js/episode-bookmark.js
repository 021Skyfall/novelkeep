(function () {
    function updateButton(button, bookmarked) {
        button.classList.toggle('is-active', bookmarked);
        button.textContent = bookmarked ? '책갈피 해제' : '책갈피 저장';
        button.setAttribute('aria-pressed', bookmarked ? 'true' : 'false');
    }

    function showMessage(message, bookmarked) {
        if (!message || !window.NovelKeepNotification) {
            return;
        }
        window.NovelKeepNotification.success(message, {
            label: bookmarked ? "책갈피 저장" : "책갈피 해제"
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-bookmark-form]').forEach(function (form) {
            form.addEventListener('submit', function (event) {
                event.preventDefault();
                var button = form.querySelector('[data-bookmark-button]');
                if (!button || button.disabled) {
                    return;
                }
                button.disabled = true;
                fetch(form.getAttribute('action'), {
                    method: 'POST',
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest',
                        'Accept': 'application/json'
                    },
                    credentials: 'same-origin'
                }).then(function (response) {
                    if (!response.ok) {
                        throw new Error('bookmark failed');
                    }
                    return response.json();
                }).then(function (data) {
                    updateButton(button, !!data.bookmarked);
                    showMessage(data.message || '', !!data.bookmarked);
                }).catch(function () {
                    form.submit();
                }).finally(function () {
                    button.disabled = false;
                });
            });
        });
    });
})();
