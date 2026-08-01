(function () {
    function nextDir(current) {
        if (!current) {
            return 'ASC';
        }
        if (current === 'ASC') {
            return 'DESC';
        }
        return '';
    }

    function label(base, dir) {
        if (dir === 'ASC') {
            return base + ' ↑';
        }
        if (dir === 'DESC') {
            return base + ' ↓';
        }
        return base;
    }

    function syncButtons(root, activeField, activeDir) {
        (root || document).querySelectorAll('[data-sort-field]').forEach(function (btn) {
            var field = btn.getAttribute('data-sort-field') || '';
            var base = btn.getAttribute('data-sort-label') || btn.textContent.replace(/\s*[↑↓]\s*$/, '').trim();
            btn.setAttribute('data-sort-label', base);
            var active = field === activeField && !!activeDir;
            var dir = active ? activeDir : '';
            btn.classList.toggle('is-active', active);
            btn.setAttribute('data-sort-dir', dir);
            btn.textContent = label(base, dir);
        });
    }

    function bind(root, onChange) {
        var scope = root || document;

        scope.querySelectorAll('[data-sort-field]').forEach(function (btn) {
            if (btn.dataset.sortBound === '1') {
                return;
            }
            btn.dataset.sortBound = '1';
            var base = btn.getAttribute('data-sort-label')
                    || btn.textContent.replace(/\s*[↑↓]\s*$/, '').trim();
            btn.setAttribute('data-sort-label', base);
            btn.addEventListener('click', function () {
                var field = btn.getAttribute('data-sort-field') || '';
                var currentField = scope.getAttribute('data-active-sort-field') || '';
                var currentDir = scope.getAttribute('data-active-sort-dir') || '';
                var nextField = field;
                var next = '';
                if (currentField === field) {
                    next = nextDir(currentDir);
                    if (!next) {
                        nextField = '';
                    }
                } else {
                    next = 'ASC';
                }
                scope.setAttribute('data-active-sort-field', nextField);
                scope.setAttribute('data-active-sort-dir', next);
                syncButtons(scope, nextField, next);
                if (typeof onChange === 'function') {
                    onChange(nextField || null, next || null);
                }
            });
        });
        syncButtons(
            scope,
            scope.getAttribute('data-active-sort-field') || '',
            scope.getAttribute('data-active-sort-dir') || ''
        );
    }

    function applyDefault(scope) {
        if (!scope) {
            return { field: '', dir: '' };
        }
        var field = scope.getAttribute('data-default-sort-field') || '';
        var dir = scope.getAttribute('data-default-sort-dir') || 'DESC';
        scope.setAttribute('data-active-sort-field', field);
        scope.setAttribute('data-active-sort-dir', dir);
        syncButtons(scope, field, dir);
        return { field: field, dir: dir };
    }

    window.NovelKeepSortCycle = {
        bind: bind,
        syncButtons: syncButtons,
        applyDefault: applyDefault,
        label: label
    };
})();
