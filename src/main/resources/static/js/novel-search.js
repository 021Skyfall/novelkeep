(function () {
    var currentController = null;

    function getAsyncRoot() {
        return document.querySelector('[data-async-content]');
    }

    function setBusy(root, busy) {
        if (!root) {
            return;
        }
        root.setAttribute('aria-busy', busy ? 'true' : 'false');
        var status = root.querySelector('[data-async-status]');
        if (status) {
            status.classList.toggle('d-none', !busy);
        }
    }

    function replaceAsyncContent(html) {
        var parser = new DOMParser();
        var doc = parser.parseFromString(html, 'text/html');
        var next = doc.querySelector('[data-async-content]');
        var current = getAsyncRoot();
        if (!next || !current) {
            return false;
        }
        current.replaceWith(next);
        bindAll();
        return true;
    }

    function buildUrlFromForm(form) {
        var action = form.getAttribute('action') || window.location.pathname;
        var params = new URLSearchParams(new FormData(form));
        var query = params.toString();
        return query ? (action + '?' + query) : action;
    }

    function loadUrl(url, pushHistory) {
        var root = getAsyncRoot();
        if (!root) {
            window.location.href = url;
            return;
        }

        if (currentController) {
            currentController.abort();
        }
        currentController = new AbortController();
        setBusy(root, true);

        fetch(url, {
            method: 'GET',
            headers: {
                'X-Novelkeep-Partial': '1',
                'X-Requested-With': 'XMLHttpRequest',
                'Accept': 'text/html'
            },
            credentials: 'same-origin',
            signal: currentController.signal
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('partial failed');
            }
            return response.text();
        }).then(function (html) {
            if (!replaceAsyncContent(html)) {
                throw new Error('replace failed');
            }
            if (pushHistory) {
                window.history.pushState({ novelkeepAsync: true }, '', url);
            }
        }).catch(function (error) {
            if (error && error.name === 'AbortError') {
                return;
            }
            window.location.href = url;
        }).finally(function () {
            var nextRoot = getAsyncRoot();
            setBusy(nextRoot, false);
        });
    }

    function bindAutoSearch(form) {
        if (!form || form.dataset.boundSearch === '1') {
            return;
        }
        form.dataset.boundSearch = '1';

        var timer = null;
        var keywordInput = form.querySelector('[data-auto-search-keyword]');
        var selects = form.querySelectorAll('select[data-auto-search]');

        function runSearch(pushHistory) {
            if (timer) {
                clearTimeout(timer);
                timer = null;
            }
            var url = buildUrlFromForm(form);
            if (form.hasAttribute('data-async-search')) {
                loadUrl(url, pushHistory !== false);
                return;
            }
            if (typeof form.requestSubmit === 'function') {
                form.requestSubmit();
            } else {
                form.submit();
            }
        }

        selects.forEach(function (select) {
            select.addEventListener('change', function () {
                runSearch(true);
            });
        });

        if (keywordInput) {
            keywordInput.addEventListener('input', function (event) {
                if (event.isComposing || event.keyCode === 229) {
                    return;
                }
                if (timer) {
                    clearTimeout(timer);
                }
                timer = setTimeout(function () {
                    runSearch(true);
                }, 350);
            });

            keywordInput.addEventListener('compositionend', function () {
                if (timer) {
                    clearTimeout(timer);
                }
                timer = setTimeout(function () {
                    runSearch(true);
                }, 350);
            });

            keywordInput.addEventListener('keydown', function (event) {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    runSearch(true);
                }
            });
        }

        form.addEventListener('submit', function (event) {
            if (!form.hasAttribute('data-async-search')) {
                return;
            }
            event.preventDefault();
            runSearch(true);
        });
    }

    function bindPagination(root) {
        if (!root) {
            return;
        }
        root.querySelectorAll('[data-async-page]').forEach(function (link) {
            if (link.dataset.boundPage === '1') {
                return;
            }
            link.dataset.boundPage = '1';
            link.addEventListener('click', function (event) {
                var href = link.getAttribute('href');
                if (!href || link.closest('.disabled')) {
                    return;
                }
                event.preventDefault();
                loadUrl(href, true);
            });
        });
    }

    function bindReset(root) {
        if (!root) {
            return;
        }
        root.querySelectorAll('[data-async-reset]').forEach(function (link) {
            if (link.dataset.boundReset === '1') {
                return;
            }
            link.dataset.boundReset = '1';
            link.addEventListener('click', function (event) {
                var href = link.getAttribute('href');
                if (!href) {
                    return;
                }
                event.preventDefault();
                loadUrl(href, true);
            });
        });
    }

    function bindAll() {
        document.querySelectorAll('form[data-auto-search-form]').forEach(bindAutoSearch);
        var root = getAsyncRoot();
        bindPagination(root);
        bindReset(root);
    }

    window.novelkeepReloadAsync = function () {
        loadUrl(window.location.href, false);
    };

    window.addEventListener('popstate', function () {
        if (!getAsyncRoot()) {
            return;
        }
        loadUrl(window.location.href, false);
    });

    bindAll();
})();
