(function () {
    function notify(message, type) {
        if (window.NovelKeepNotification) {
            if (type === 'error') {
                window.NovelKeepNotification.error(message || '요청을 처리하지 못했습니다.', { label: '펀딩 관리' });
                return;
            }
            window.NovelKeepNotification.success(message || '완료', { label: '펀딩 관리' });
            return;
        }
        window.alert(message || '완료');
    }

    function csrfHeaders() {
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        var headers = { 'Accept': 'application/json' };
        if (token && header) {
            headers[header.getAttribute('content')] = token.getAttribute('content');
        }
        return headers;
    }

    function setEditing(form, editing) {
        form.querySelectorAll('[data-editable]').forEach(function (input) {
            input.disabled = !editing;
        });
        var toggle = form.querySelector('[data-funding-edit-toggle]');
        if (toggle) {
            toggle.textContent = editing ? '저장' : '수정';
            toggle.dataset.mode = editing ? 'save' : 'edit';
        }
    }

    function formatPrice(value) {
        var num = Number(value || 0);
        return num.toLocaleString('ko-KR') + '원';
    }

    document.querySelectorAll('[data-funding-edit-form]').forEach(function (form) {
        setEditing(form, false);

        form.addEventListener('click', function (event) {
            var toggle = event.target.closest('[data-funding-edit-toggle]');
            if (!toggle || !form.contains(toggle)) {
                return;
            }
            event.preventDefault();
            if (toggle.dataset.mode !== 'save') {
                setEditing(form, true);
                return;
            }

            var body = new FormData(form);
            var startValue = form.querySelector('[data-funding-start-value]');
            if (startValue) {
                body.set('startAt', startValue.value);
            }
            toggle.disabled = true;
            fetch(form.getAttribute('action'), {
                method: 'POST',
                body: body,
                headers: csrfHeaders(),
                credentials: 'same-origin'
            })
                .then(function (res) {
                    return res.json().then(function (data) {
                        return { ok: res.ok, data: data };
                    });
                })
                .then(function (result) {
                    if (!result.ok || !result.data || !result.data.success) {
                        notify((result.data && result.data.message) || '저장에 실패했습니다.', 'error');
                        return;
                    }
                    notify(result.data.message || '저장했습니다.', 'success');
                    setEditing(form, false);
                    var pct = form.closest('[data-campaign-card]');
                    if (pct) {
                        var percent = result.data.achievementPercent;
                        var pctEl = pct.querySelector('[data-campaign-pct]');
                        var bar = pct.querySelector('[data-campaign-bar]');
                        var progress = pct.querySelector('[data-campaign-progress]');
                        if (pctEl) pctEl.textContent = percent + '%';
                        if (bar) bar.style.width = percent + '%';
                        if (progress) progress.setAttribute('aria-valuenow', String(percent));
                        var priceInput = form.querySelector('[name="priceAmount"]');
                        var priceLabel = pct.querySelector('[data-campaign-price-label]');
                        if (priceInput && priceLabel) {
                            priceLabel.textContent = formatPrice(priceInput.value);
                        }
                    }
                })
                .catch(function () {
                    notify('저장에 실패했습니다.', 'error');
                })
                .finally(function () {
                    toggle.disabled = false;
                });
        });

        form.addEventListener('click', function (event) {
            var cancelBtn = event.target.closest('[data-funding-cancel]');
            if (!cancelBtn || !form.contains(cancelBtn)) {
                return;
            }
            event.preventDefault();
            var url = cancelBtn.getAttribute('data-cancel-url');
            if (!url) {
                return;
            }

            function sendCancel() {
                cancelBtn.disabled = true;
                fetch(url, {
                    method: 'POST',
                    headers: csrfHeaders(),
                    credentials: 'same-origin'
                })
                    .then(function (res) {
                        return res.json().then(function (data) {
                            return { ok: res.ok, data: data };
                        });
                    })
                    .then(function (result) {
                        if (!result.ok || !result.data || !result.data.success) {
                            notify((result.data && result.data.message) || '취소에 실패했습니다.', 'error');
                            return;
                        }
                        notify(result.data.message || '취소했습니다.', 'success');
                        var card = form.closest('[data-campaign-card]');
                        if (card) {
                            card.remove();
                        }
                    })
                    .catch(function () {
                        notify('취소에 실패했습니다.', 'error');
                    })
                    .finally(function () {
                        cancelBtn.disabled = false;
                    });
            }

            var message = cancelBtn.getAttribute('data-confirm-delete');
            if (message && window.NovelKeepConfirm) {
                window.NovelKeepConfirm.open(message, {
                    title: cancelBtn.getAttribute('data-confirm-title') || '펀딩 취소',
                    confirmText: cancelBtn.getAttribute('data-confirm-text') || '예, 취소',
                    cancelText: '아니오',
                    tone: 'danger'
                }).then(function (confirmed) {
                    if (confirmed) {
                        sendCancel();
                    }
                });
                return;
            }
            if (window.confirm(message || '취소할까요?')) {
                sendCancel();
            }
        });
    });

    var search = document.querySelector('[data-publishing-search]');
    var cards = document.querySelectorAll('[data-campaign-card]');
    var empty = document.querySelector('[data-campaign-empty]');
    if (search && cards.length) {
        search.addEventListener('input', function () {
            var q = search.value.trim().toLowerCase();
            var visible = 0;
            cards.forEach(function (card) {
                var text = (card.dataset.searchText || '').toLowerCase();
                var show = !q || text.includes(q);
                card.classList.toggle('d-none', !show);
                if (show) visible += 1;
            });
            if (empty) empty.classList.toggle('d-none', visible > 0 || !q);
        });
    }
})();
