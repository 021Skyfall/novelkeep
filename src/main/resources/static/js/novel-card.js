(function () {
    function isInteractiveTarget(target) {
        return !!(target.closest('button, a, input, select, textarea, form[data-novel-action], [data-tag-more]'));
    }

    function bindCardClick(card) {
        if (card.dataset.boundCardClick === '1') {
            return;
        }
        card.dataset.boundCardClick = '1';
        card.addEventListener('click', function (event) {
            if (isInteractiveTarget(event.target)) {
                return;
            }
            var href = card.getAttribute('data-novel-href');
            if (href) {
                window.location.href = href;
            }
        });
        card.addEventListener('keydown', function (event) {
            if (event.key !== 'Enter' && event.key !== ' ') {
                return;
            }
            if (isInteractiveTarget(event.target)) {
                return;
            }
            var href = card.getAttribute('data-novel-href');
            if (!href) {
                return;
            }
            event.preventDefault();
            window.location.href = href;
        });
        if (!card.hasAttribute('tabindex')) {
            card.setAttribute('tabindex', '0');
            card.setAttribute('role', 'link');
        }
    }

    function measureTagRow(row) {
        var list = row.querySelector('[data-tag-list]');
        var more = row.querySelector('[data-tag-more]');
        if (!list || !more) {
            return;
        }
        row.classList.remove('is-expanded');
        more.classList.add('d-none');
        more.setAttribute('aria-expanded', 'false');
        // force layout with collapsed max-height
        var overflows = list.scrollHeight > list.clientHeight + 1;
        more.classList.toggle('d-none', !overflows);
    }

    function bindTagRow(row) {
        if (row.dataset.boundTagRow === '1') {
            return;
        }
        row.dataset.boundTagRow = '1';
        measureTagRow(row);
    }

    function bindAll() {
        document.querySelectorAll('[data-novel-href]').forEach(bindCardClick);
        document.querySelectorAll('[data-tag-row]').forEach(bindTagRow);
    }

    document.addEventListener('click', function (event) {
        var more = event.target.closest('[data-tag-more]');
        if (!more) {
            return;
        }
        var row = more.closest('[data-tag-row]');
        if (!row) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        var expanded = row.classList.toggle('is-expanded');
        more.setAttribute('aria-expanded', expanded ? 'true' : 'false');
        more.textContent = expanded ? '접기' : '...';
        if (!expanded) {
            measureTagRow(row);
        }
    });

    window.novelkeepBindNovelCards = bindAll;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', bindAll);
    } else {
        bindAll();
    }

    window.addEventListener('resize', function () {
        document.querySelectorAll('[data-tag-row]').forEach(function (row) {
            if (!row.classList.contains('is-expanded')) {
                measureTagRow(row);
            }
        });
    });
})();
