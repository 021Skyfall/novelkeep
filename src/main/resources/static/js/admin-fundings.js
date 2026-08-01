(function () {
    var panel = document.querySelector('[data-admin-funding-ajax]');
    var listHost = document.querySelector('[data-admin-funding-list]');
    if (!panel || !listHost) {
        return;
    }

    var searchUrl = panel.getAttribute('data-search-url') || '/admin/fundings';
    var titleInput = panel.querySelector('[data-admin-title]');
    var defaultApproval = 'AWAITING';
    var approval = '';
    var status = '';
    var sortField = panel.getAttribute('data-active-sort-field') || '';
    var sortDir = panel.getAttribute('data-active-sort-dir') || '';
    var activeApproval = panel.querySelector('[data-admin-approval].is-active');
    var activeStatus = panel.querySelector('[data-admin-status].is-active');
    if (activeApproval) {
        approval = activeApproval.getAttribute('data-admin-approval') || '';
    }
    if (activeStatus) {
        status = activeStatus.getAttribute('data-admin-status') || '';
    }

    var timer = null;
    var abortController = null;

    function refresh() {
        if (abortController) {
            abortController.abort();
        }
        abortController = new AbortController();
        var params = new URLSearchParams();
        params.set('partial', '1');
        if (titleInput && titleInput.value.trim()) {
            params.set('novelTitle', titleInput.value.trim());
        }
        if (approval) {
            params.set('approval', approval);
        }
        if (status) {
            params.set('status', status);
        }
        if (sortField && sortDir) {
            params.set('sortField', sortField);
            params.set('sortDir', sortDir);
        } else {
            params.set('unsorted', 'true');
        }
        listHost.classList.add('is-loading');
        fetch(searchUrl + '?' + params.toString(), {
            method: 'GET',
            headers: { 'X-Partial': '1', 'Accept': 'text/html' },
            credentials: 'same-origin',
            signal: abortController.signal
        })
            .then(function (res) {
                if (!res.ok) {
                    throw new Error('search failed');
                }
                return res.text();
            })
            .then(function (html) {
                listHost.innerHTML = html;
            })
            .catch(function (err) {
                if (err && err.name === 'AbortError') {
                    return;
                }
            })
            .finally(function () {
                listHost.classList.remove('is-loading');
            });
    }

    if (titleInput) {
        titleInput.addEventListener('input', function () {
            clearTimeout(timer);
            timer = setTimeout(refresh, 280);
        });
    }

    panel.querySelectorAll('[data-admin-approval]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            approval = btn.getAttribute('data-admin-approval') || '';
            panel.querySelectorAll('[data-admin-approval]').forEach(function (item) {
                item.classList.toggle('is-active', item === btn);
            });
            refresh();
        });
    });

    panel.querySelectorAll('[data-admin-status]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            status = btn.getAttribute('data-admin-status') || '';
            panel.querySelectorAll('[data-admin-status]').forEach(function (item) {
                item.classList.toggle('is-active', item === btn);
            });
            refresh();
        });
    });

    if (window.NovelKeepSortCycle) {
        window.NovelKeepSortCycle.bind(panel, function (field, dir) {
            sortField = field || '';
            sortDir = dir || '';
            refresh();
        });
    }

    var resetBtn = panel.querySelector('[data-search-reset]');
    if (resetBtn) {
        resetBtn.addEventListener('click', function () {
            if (titleInput) {
                titleInput.value = '';
            }
            approval = defaultApproval;
            status = '';
            panel.querySelectorAll('[data-admin-approval]').forEach(function (item) {
                item.classList.toggle(
                    'is-active',
                    (item.getAttribute('data-admin-approval') || '') === defaultApproval
                );
            });
            panel.querySelectorAll('[data-admin-status]').forEach(function (item) {
                item.classList.toggle('is-active', (item.getAttribute('data-admin-status') || '') === '');
            });
            if (window.NovelKeepSortCycle) {
                var applied = window.NovelKeepSortCycle.applyDefault(panel);
                sortField = applied.field || '';
                sortDir = applied.dir || '';
            }
            refresh();
        });
    }
})();
