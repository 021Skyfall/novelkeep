(function () {
    function findFilter(target) {
        return target && target.closest ? target.closest('[data-genre-filter]') : null;
    }

    function syncHiddenInputs(root) {
        var holder = root.querySelector('[data-genre-hidden]');
        if (!holder) {
            return;
        }
        holder.innerHTML = '';
        root.querySelectorAll('[data-genre-badge].is-active').forEach(function (button) {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = 'genres';
            input.value = button.getAttribute('data-genre-value');
            holder.appendChild(input);
        });
    }

    function submitSearch(root) {
        var form = root.closest('form');
        if (!form) {
            return;
        }
        syncHiddenInputs(root);
        if (typeof form.requestSubmit === 'function') {
            form.requestSubmit();
            return;
        }
        var submitEvent = new Event('submit', { bubbles: true, cancelable: true });
        if (form.dispatchEvent(submitEvent)) {
            form.submit();
        }
    }

    function ensureExpandedState(root) {
        var extras = root.querySelector('[data-genre-extra]');
        var toggle = root.querySelector('[data-genre-expand]');
        if (!extras || !toggle) {
            return;
        }
        var hasExtraSelected = extras.querySelector('[data-genre-badge].is-active') != null;
        if (hasExtraSelected) {
            extras.classList.remove('d-none');
            toggle.textContent = '주요 장르만';
            toggle.setAttribute('aria-expanded', 'true');
        }
    }

    document.addEventListener('click', function (event) {
        var badge = event.target.closest('[data-genre-badge]');
        if (badge) {
            var filter = findFilter(badge);
            if (!filter) {
                return;
            }
            event.preventDefault();
            badge.classList.toggle('is-active');
            submitSearch(filter);
            return;
        }

        var toggle = event.target.closest('[data-genre-expand]');
        if (!toggle) {
            return;
        }
        var root = findFilter(toggle);
        if (!root) {
            return;
        }
        event.preventDefault();
        var extras = root.querySelector('[data-genre-extra]');
        if (!extras) {
            return;
        }
        var expanded = extras.classList.toggle('d-none') === false;
        toggle.textContent = expanded ? '주요 장르만' : '전체 장르';
        toggle.setAttribute('aria-expanded', expanded ? 'true' : 'false');
    });

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-genre-filter]').forEach(ensureExpandedState);
    });
})();
