(function () {
    var panel = document.querySelector('[data-reader-activities-ajax]');
    var listHost = document.querySelector('[data-reader-activities-list]');
    if (!panel || !listHost) {
        return;
    }

    var searchUrl = panel.getAttribute('data-search-url') || '/reader/activities';
    var titleInput = panel.querySelector('[data-activities-title]');
    var tab = 'ALL';
    var orderStatus = '';
    var activityStatus = '';
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
    var activeActivity = panel.querySelector('[data-activity-status].is-active');
    if (activeActivity) {
        activityStatus = activeActivity.getAttribute('data-activity-status') || '';
    }

    var abortController = null;

    function syncStatusChips() {
        panel.querySelectorAll('[data-status-for]').forEach(function (btn) {
            var forTab = btn.getAttribute('data-status-for');
            var show = tab === 'ALL' || tab === forTab;
            btn.classList.toggle('d-none', !show);
        });
    }

    function clearStatusSelection() {
        orderStatus = '';
        activityStatus = '';
        panel.querySelectorAll('[data-order-status], [data-activity-status]').forEach(function (item) {
            var isAll = (item.getAttribute('data-order-status') || item.getAttribute('data-activity-status') || '') === '';
            item.classList.toggle('is-active', isAll && !item.hasAttribute('data-status-for'));
            if (item.hasAttribute('data-status-for')) {
                item.classList.remove('is-active');
            }
        });
        var allBtn = panel.querySelector('[data-activity-status=""]');
        if (allBtn) {
            allBtn.classList.add('is-active');
        }
    }

    function currentParams() {
        var params = new URLSearchParams();
        params.set('tab', tab || 'ALL');
        if (titleInput && titleInput.value.trim()) {
            params.set('novelTitle', titleInput.value.trim());
        }
        if (orderStatus) {
            params.set('orderStatus', orderStatus);
        }
        if (activityStatus) {
            params.set('activityStatus', activityStatus);
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
        syncStatusChips();
        listHost.classList.add('is-loading');
        fetch(searchUrl + '?' + params.toString(), {
            method: 'GET',
            headers: { 'X-Partial': '1', 'Accept': 'text/html' },
            credentials: 'same-origin',
            signal: abortController.signal
        })
            .then(function (res) {
                if (res.status === 401) {
                    window.location.href = '/?roleRequired=true';
                    throw new Error('auth');
                }
                if (!res.ok) {
                    throw new Error('search failed');
                }
                return res.text();
            })
            .then(function (html) {
                if (/<!doctype html|<html[\s>]/i.test(String(html || '').trim())) {
                    window.location.href = '/?roleRequired=true';
                    return;
                }
                listHost.innerHTML = html;
            })
            .catch(function (err) {
                if (err && (err.name === 'AbortError' || err.message === 'auth')) {
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
            clearStatusSelection();
            refresh();
        });
    });

    panel.querySelectorAll('[data-order-status]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            orderStatus = btn.getAttribute('data-order-status') || '';
            activityStatus = '';
            panel.querySelectorAll('[data-order-status], [data-activity-status]').forEach(function (item) {
                item.classList.toggle('is-active', item === btn);
            });
            if (tab === 'ALL' && orderStatus) {
                // keep ALL, filter by order status
            } else if (orderStatus) {
                tab = 'ORDER';
                panel.querySelectorAll('[data-activities-tab]').forEach(function (item) {
                    item.classList.toggle('is-active', (item.getAttribute('data-activities-tab') || '') === 'ORDER');
                });
            }
            refresh();
        });
    });

    panel.querySelectorAll('[data-activity-status]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            activityStatus = btn.getAttribute('data-activity-status') || '';
            orderStatus = '';
            panel.querySelectorAll('[data-order-status], [data-activity-status]').forEach(function (item) {
                item.classList.toggle('is-active', item === btn);
            });
            if (activityStatus === 'PARTICIPATING' && tab === 'ALL') {
                // stay on ALL with filter
            } else if (activityStatus === 'PARTICIPATING') {
                tab = 'ACTIVE';
                panel.querySelectorAll('[data-activities-tab]').forEach(function (item) {
                    item.classList.toggle('is-active', (item.getAttribute('data-activities-tab') || '') === 'ACTIVE');
                });
            } else if (activityStatus === 'REFUNDED' && tab !== 'ALL') {
                tab = 'REFUND';
                panel.querySelectorAll('[data-activities-tab]').forEach(function (item) {
                    item.classList.toggle('is-active', (item.getAttribute('data-activities-tab') || '') === 'REFUND');
                });
            }
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
            panel.querySelectorAll('[data-activities-tab]').forEach(function (item) {
                item.classList.toggle('is-active', (item.getAttribute('data-activities-tab') || '') === 'ALL');
            });
            clearStatusSelection();
            if (window.NovelKeepSortCycle) {
                var applied = window.NovelKeepSortCycle.applyDefault(panel);
                sortField = applied.field || '';
                sortDir = applied.dir || '';
            }
            refresh();
        });
    }

    window.addEventListener('pageshow', function (event) {
        if (event.persisted || (window.performance && performance.getEntriesByType
                && performance.getEntriesByType('navigation')[0]
                && performance.getEntriesByType('navigation')[0].type === 'back_forward')) {
            refresh();
        }
    });

    syncStatusChips();
})();
