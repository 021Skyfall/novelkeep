(function () {
    var panel = document.querySelector('[data-admin-orders-ajax]');
    var listHost = document.querySelector('[data-admin-orders-list]');
    if (!panel || !listHost) {
        return;
    }

    var searchUrl = panel.getAttribute('data-search-url') || '/admin/orders';
    var titleInput = panel.querySelector('[data-orders-title]');
    var fromInput = panel.querySelector('[data-orders-from]');
    var toInput = panel.querySelector('[data-orders-to]');
    var csvLink = panel.querySelector('[data-orders-export-csv]');
    var jsonLink = panel.querySelector('[data-orders-export-json]');
    var status = '';
    var sortField = panel.getAttribute('data-active-sort-field') || '';
    var sortDir = panel.getAttribute('data-active-sort-dir') || '';
    var activeStatus = panel.querySelector('[data-orders-status].is-active');
    if (activeStatus) {
        status = activeStatus.getAttribute('data-orders-status') || '';
    }

    var timer = null;
    var abortController = null;

    function currentParams() {
        var params = new URLSearchParams();
        if (titleInput && titleInput.value.trim()) {
            params.set('novelTitle', titleInput.value.trim());
        }
        if (status) {
            params.set('status', status);
        }
        if (fromInput && fromInput.value) {
            params.set('orderedFrom', fromInput.value);
        }
        if (toInput && toInput.value) {
            params.set('orderedTo', toInput.value);
        }
        if (sortField && sortDir) {
            params.set('sortField', sortField);
            params.set('sortDir', sortDir);
        } else {
            params.set('unsorted', 'true');
        }
        return params;
    }

    function syncExportLinks() {
        var query = currentParams().toString();
        if (csvLink) {
            csvLink.setAttribute('href', '/admin/orders/export.csv' + (query ? '?' + query : ''));
        }
        if (jsonLink) {
            jsonLink.setAttribute('href', '/admin/orders/export.json' + (query ? '?' + query : ''));
        }
    }

    function refresh() {
        if (abortController) {
            abortController.abort();
        }
        abortController = new AbortController();
        var params = currentParams();
        params.set('partial', '1');
        syncExportLinks();
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

    function scheduleRefresh() {
        clearTimeout(timer);
        timer = setTimeout(refresh, 280);
    }

    if (titleInput) {
        titleInput.addEventListener('input', scheduleRefresh);
    }
    if (fromInput) {
        fromInput.addEventListener('change', refresh);
    }
    if (toInput) {
        toInput.addEventListener('change', refresh);
    }

    panel.querySelectorAll('[data-orders-status]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            status = btn.getAttribute('data-orders-status') || '';
            panel.querySelectorAll('[data-orders-status]').forEach(function (item) {
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
            if (fromInput) {
                fromInput.value = '';
            }
            if (toInput) {
                toInput.value = '';
            }
            status = '';
            panel.querySelectorAll('[data-orders-status]').forEach(function (item) {
                item.classList.toggle('is-active', (item.getAttribute('data-orders-status') || '') === '');
            });
            if (window.NovelKeepSortCycle) {
                var applied = window.NovelKeepSortCycle.applyDefault(panel);
                sortField = applied.field || '';
                sortDir = applied.dir || '';
            }
            refresh();
        });
    }

    syncExportLinks();
})();
