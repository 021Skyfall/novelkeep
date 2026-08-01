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

    function visibleClampTags(list) {
        return Array.prototype.slice.call(list.querySelectorAll('[data-clamp-tag]'))
            .filter(function (tag) {
                return !tag.classList.contains('d-none');
            });
    }

    function wrapsPastFirstLine(list, more) {
        var tags = visibleClampTags(list);
        if (tags.length === 0) {
            return false;
        }
        var lineTop = tags[0].offsetTop;
        var tagWraps = tags.some(function (tag) {
            return tag.offsetTop > lineTop + 1;
        });
        var moreWraps = more && !more.classList.contains('d-none') && more.offsetTop > lineTop + 1;
        return tagWraps || moreWraps;
    }

    function measureInlineTagRow(row, list, more) {
        var tags = Array.prototype.slice.call(list.querySelectorAll('[data-clamp-tag]'));
        tags.forEach(function (tag) {
            tag.classList.remove('d-none');
        });
        more.classList.add('d-none');
        more.textContent = '...';
        more.setAttribute('aria-expanded', 'false');

        if (tags.length === 0 || !wrapsPastFirstLine(list, more)) {
            return;
        }

        more.classList.remove('d-none');
        for (var i = tags.length - 1; i >= 0; i--) {
            if (!wrapsPastFirstLine(list, more)) {
                break;
            }
            tags[i].classList.add('d-none');
        }
    }

    function measureMaxHeightTagRow(row, list, more) {
        more.classList.add('d-none');
        more.setAttribute('aria-expanded', 'false');
        more.textContent = '...';
        var overflows = list.scrollHeight > list.clientHeight + 1;
        more.classList.toggle('d-none', !overflows);
    }

    function measureTagRow(row) {
        var list = row.querySelector('[data-tag-list]');
        var more = row.querySelector('[data-tag-more]');
        if (!list || !more) {
            return;
        }
        row.classList.remove('is-expanded');
        if (row.getAttribute('data-tag-mode') === 'inline') {
            measureInlineTagRow(row, list, more);
            return;
        }
        measureMaxHeightTagRow(row, list, more);
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
        var list = row.querySelector('[data-tag-list]');
        var expanded = row.classList.toggle('is-expanded');
        more.setAttribute('aria-expanded', expanded ? 'true' : 'false');
        more.textContent = expanded ? '접기' : '...';
        if (row.getAttribute('data-tag-mode') === 'inline' && list) {
            var tags = list.querySelectorAll('[data-clamp-tag]');
            if (expanded) {
                tags.forEach(function (tag) {
                    tag.classList.remove('d-none');
                });
                more.classList.remove('d-none');
            } else {
                measureTagRow(row);
            }
            return;
        }
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
