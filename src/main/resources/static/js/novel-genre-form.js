(function () {
    function selectedCount(root) {
        return root.querySelectorAll('[data-genre-form-badge].is-active').length;
    }

    function syncInputs(root) {
        var holder = root.querySelector('[data-genre-form-hidden]');
        if (!holder) {
            return;
        }
        holder.innerHTML = '';
        root.querySelectorAll('[data-genre-form-badge].is-active').forEach(function (button) {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = 'genres';
            input.value = button.getAttribute('data-genre-value');
            holder.appendChild(input);
        });
    }

    function updateCount(root) {
        var count = selectedCount(root);
        var max = Number(root.getAttribute('data-max-genres') || '5');
        var counter = root.querySelector('[data-genre-count]');
        if (counter) {
            counter.textContent = count + ' / ' + max;
        }
        root.querySelectorAll('[data-genre-form-badge]').forEach(function (button) {
            var active = button.classList.contains('is-active');
            button.classList.toggle('is-disabled', !active && count >= max);
            button.disabled = !active && count >= max;
        });
        return count;
    }

    function showError(root, message) {
        var error = root.querySelector('[data-genre-form-error]');
        if (!error) {
            return;
        }
        error.textContent = message || '';
        error.classList.toggle('is-visible', !!message);
    }

    function bind(root) {
        if (!root || root.getAttribute('data-genre-form-bound') === '1') {
            return;
        }
        root.setAttribute('data-genre-form-bound', '1');
        updateCount(root);
        syncInputs(root);

        root.querySelectorAll('[data-genre-form-badge]').forEach(function (button) {
            button.addEventListener('click', function () {
                var max = Number(root.getAttribute('data-max-genres') || '5');
                var active = button.classList.contains('is-active');
                if (!active && selectedCount(root) >= max) {
                    showError(root, '장르는 최대 ' + max + '개까지 선택할 수 있습니다.');
                    return;
                }
                button.classList.toggle('is-active');
                showError(root, '');
                updateCount(root);
                syncInputs(root);
            });
        });

        var form = root.closest('form');
        if (form) {
            form.addEventListener('submit', function (event) {
                syncInputs(root);
                var count = selectedCount(root);
                if (count < 1) {
                    event.preventDefault();
                    showError(root, '장르를 하나 이상 선택해 주세요.');
                    return;
                }
                var max = Number(root.getAttribute('data-max-genres') || '5');
                if (count > max) {
                    event.preventDefault();
                    showError(root, '장르는 최대 ' + max + '개까지 선택할 수 있습니다.');
                }
            });
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-genre-form]').forEach(bind);
    });
})();
