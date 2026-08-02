(function () {
    var panel = document.querySelector('[data-admin-funding-ajax]');
    var listHost = document.querySelector('[data-admin-funding-list]');
    if (!panel || !listHost) {
        return;
    }

    var searchUrl = panel.getAttribute('data-search-url') || '/admin/fundings';
    var titleInput = panel.querySelector('[data-admin-title]');
    var fromInput = panel.querySelector('[data-admin-from]');
    var toInput = panel.querySelector('[data-admin-to]');
    var rangeInput = panel.querySelector('[data-date-range-input]');
    var rangePicker = null;
    var csvLink = panel.querySelector('[data-admin-export-csv]');
    var jsonLink = panel.querySelector('[data-admin-export-json]');
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

    var abortController = null;

    function currentParams() {
        var params = new URLSearchParams();
        if (titleInput && titleInput.value.trim()) {
            params.set('novelTitle', titleInput.value.trim());
        }
        if (fromInput && fromInput.value) {
            params.set('closedFrom', fromInput.value);
        }
        if (toInput && toInput.value) {
            params.set('closedTo', toInput.value);
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
        return params;
    }

    function syncExportLinks() {
        var query = currentParams().toString();
        if (csvLink) {
            csvLink.setAttribute('href', '/admin/fundings/export.csv' + (query ? '?' + query : ''));
        }
        if (jsonLink) {
            jsonLink.setAttribute('href', '/admin/fundings/export.json' + (query ? '?' + query : ''));
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

    syncExportLinks();

    if (titleInput) {
        titleInput.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                refresh();
            }
        });
    }
    var titleSearchBtn = panel.querySelector('[data-admin-search-btn]');
    if (titleSearchBtn) {
        titleSearchBtn.addEventListener('click', function (event) {
            event.preventDefault();
            refresh();
        });
    }
    function syncRangeBounds() {
        // flatpickr handles bounds; keep no-op for compatibility
    }

    if (window.NovelKeepDateRange) {
        rangePicker = window.NovelKeepDateRange.bind(rangeInput, fromInput, toInput, refresh);
    } else {
        if (fromInput) {
            fromInput.addEventListener('change', function () {
                refresh();
            });
        }
        if (toInput) {
            toInput.addEventListener('change', function () {
                refresh();
            });
        }
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
            if (fromInput) {
                fromInput.value = '';
            }
            if (toInput) {
                toInput.value = '';
            }
            if (rangePicker) {
                rangePicker.clear();
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

    syncExportLinks();
})();
