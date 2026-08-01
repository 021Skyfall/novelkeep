(function () {
    var panel = document.querySelector('[data-reader-activities-ajax]');
    var listHost = document.querySelector('[data-reader-activities-list]');
    if (!panel || !listHost) {
        return;
    }

    var searchUrl = panel.getAttribute('data-search-url') || '/reader/activities';
    var titleInput = panel.querySelector('[data-activities-title]');
    var orderStatusPanel = panel.querySelector('[data-order-status-panel]');
    var tab = 'ALL';
    var orderStatus = '';
    var sortField = panel.getAttribute('data-active-sort-field') || '';
    var sortDir = panel.getAttribute('data-active-sort-dir') || '';
    var activeTab = panel.querySelector('[data-activities-tab].is-active');
    if (activeTab) {
        tab = activeTab.getAttribute('data-activities-tab') || 'ALL';
    }
    var activeOrder = panel.querySelector('[data-order-status].is-active');
    if (activeOrder) {
        orderStatus = activeOrder.getAttribute('data-order-status') || '';
    }

    var abortController = null;

    function syncOrderPanel() {
        if (!orderStatusPanel) {
            return;
        }
        orderStatusPanel.classList.toggle('d-none', tab !== 'ORDER');
    }

    function currentParams() {
        var params = new URLSearchParams();
        params.set('tab', tab || 'ALL');
        if (titleInput && titleInput.value.trim()) {
            params.set('novelTitle', titleInput.value.trim());
        }
        if (tab === 'ORDER' && orderStatus) {
            params.set('orderStatus', orderStatus);
        }
        if (sortField && sortDir) {
            params.set('sortField', sortField);
            params.set('sortDir', sortDir);
        } else {
            params.set('unsorted', 'true');
        }
        return params;
    }

    function refresh() {
        if (abortController) {
            abortController.abort();
        }
        abortController = new AbortController();
        var params = currentParams();
        params.set('partial', '1');
        syncOrderPanel();
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
        titleInput.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                refresh();
            }
        });
    }
    var titleSearchBtn = panel.querySelector('[data-activities-search-btn]');
    if (titleSearchBtn) {
        titleSearchBtn.addEventListener('click', function (event) {
            event.preventDefault();
            refresh();
        });
    }

    panel.querySelectorAll('[data-activities-tab]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            tab = btn.getAttribute('data-activities-tab') || 'ALL';
            panel.querySelectorAll('[data-activities-tab]').forEach(function (item) {
                item.classList.toggle('is-active', item === btn);
            });
            if (tab !== 'ORDER') {
                orderStatus = '';
                panel.querySelectorAll('[data-order-status]').forEach(function (item) {
                    item.classList.toggle('is-active', (item.getAttribute('data-order-status') || '') === '');
                });
            }
            refresh();
        });
    });

    panel.querySelectorAll('[data-order-status]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            orderStatus = btn.getAttribute('data-order-status') || '';
            panel.querySelectorAll('[data-order-status]').forEach(function (item) {
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
            tab = 'ALL';
            orderStatus = '';
            panel.querySelectorAll('[data-activities-tab]').forEach(function (item) {
                item.classList.toggle('is-active', (item.getAttribute('data-activities-tab') || '') === 'ALL');
            });
            panel.querySelectorAll('[data-order-status]').forEach(function (item) {
                item.classList.toggle('is-active', (item.getAttribute('data-order-status') || '') === '');
            });
            if (window.NovelKeepSortCycle) {
                var applied = window.NovelKeepSortCycle.applyDefault(panel);
                sortField = applied.field || '';
                sortDir = applied.dir || '';
            }
            refresh();
        });
    }

    syncOrderPanel();
})();
