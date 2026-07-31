(function () {
    function notify(message, type) {
        if (window.NovelKeepNotification) {
            if (type === 'error') {
                window.NovelKeepNotification.error(message || '요청을 처리하지 못했습니다.', { label: '출판 펀딩' });
                return;
            }
            window.NovelKeepNotification.success(message || '완료', { label: '출판 펀딩' });
            return;
        }
        window.alert(message || '완료');
    }

    function pad(n) {
        return String(n).padStart(2, '0');
    }

    function toLocalInputValue(date) {
        return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate())
            + 'T' + pad(date.getHours()) + ':' + pad(date.getMinutes());
    }

    function defaultRange(minDays) {
        var start = new Date();
        start.setSeconds(0, 0);
        var end = new Date(start.getTime());
        end.setDate(end.getDate() + (minDays || 7));
        return { start: toLocalInputValue(start), end: toLocalInputValue(end) };
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

    function reloadParts() {
        var root = document.querySelector('[data-parts-root]');
        var url = root && root.getAttribute('data-parts-partial');
        if (!root || !url) {
            return Promise.resolve();
        }
        return fetch(url, { headers: { 'Accept': 'text/html' }, credentials: 'same-origin' })
            .then(function (res) { return res.text(); })
            .then(function (html) {
                root.innerHTML = html;
                if (window.NovelKeepDetailWriter && typeof window.NovelKeepDetailWriter.bind === 'function') {
                    window.NovelKeepDetailWriter.bind(root);
                }
            });
    }

    var modalEl = document.getElementById('fundingPartModal');
    var form = document.getElementById('fundingPartForm');
    if (!modalEl || !form) {
        return;
    }

    var modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    var titleEl = document.getElementById('fundingPartModalLabel');
    var partLabel = modalEl.querySelector('[data-funding-part-label]');
    var novelIdInput = modalEl.querySelector('[data-funding-novel-id]');
    var partIdInput = modalEl.querySelector('[data-funding-part-id]');
    var startHidden = modalEl.querySelector('[data-funding-start-hidden]');
    var startInput = modalEl.querySelector('[data-funding-start]');
    var endInput = modalEl.querySelector('[data-funding-end]');
    var targetInput = modalEl.querySelector('[data-funding-target]');
    var priceInput = modalEl.querySelector('[data-funding-price]');
    var submitBtn = modalEl.querySelector('[data-funding-submit]');
    var notices = modalEl.querySelector('[data-funding-notices]');
    var startHint = modalEl.querySelector('[data-funding-start-hint]');
    var warn = modalEl.querySelector('[data-volume-warn]');
    var mode = 'start';
    var campaignId = null;
    var chars = 0;

    function updateWarn() {
        if (!warn) return;
        var limit = Number(modalEl.dataset.limit || 100000);
        warn.classList.toggle('d-none', !(chars > limit));
    }

    function openModal(button) {
        mode = button.getAttribute('data-funding-modal') || 'start';
        campaignId = button.getAttribute('data-campaign-id');
        chars = Number(button.getAttribute('data-chars') || 0);
        novelIdInput.value = button.getAttribute('data-novel-id') || '';
        partIdInput.value = button.getAttribute('data-part-id') || '';
        partLabel.textContent = button.getAttribute('data-part-label') || '대상 부';

        var minDays = Number(modalEl.dataset.minDays || 7);
        var defaults = defaultRange(Math.max(minDays, 14));

        if (mode === 'edit') {
            titleEl.textContent = '펀딩 수정';
            submitBtn.textContent = '저장';
            notices.classList.add('d-none');
            startInput.value = button.getAttribute('data-start') || '';
            startInput.readOnly = true;
            startInput.disabled = true;
            startHint.textContent = '시작 후 시작일은 수정할 수 없습니다.';
            startHidden.value = startInput.value;
            endInput.value = button.getAttribute('data-end') || '';
            targetInput.value = button.getAttribute('data-target') || '';
            priceInput.value = button.getAttribute('data-price') || '';
            form.action = '/writer/publishing/campaigns/' + campaignId;
        } else {
            titleEl.textContent = '출판 펀딩 시작';
            submitBtn.textContent = '펀딩 시작';
            notices.classList.remove('d-none');
            startInput.readOnly = false;
            startInput.disabled = false;
            startHint.textContent = '현재 시간 이후';
            startInput.value = defaults.start;
            startHidden.value = defaults.start;
            endInput.value = defaults.end;
            targetInput.value = modalEl.dataset.minTarget || 10;
            priceInput.value = 15000;
            form.action = '/writer/publishing/campaigns';
        }
        updateWarn();
        modal.show();
    }

    document.addEventListener('click', function (event) {
        var button = event.target.closest('[data-funding-modal]');
        if (!button) return;
        event.preventDefault();
        openModal(button);
    });

    startInput.addEventListener('change', function () {
        if (!startInput.disabled) {
            startHidden.value = startInput.value;
        }
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        if (!startInput.disabled) {
            startHidden.value = startInput.value;
        }
        var body = new FormData(form);
        if (startInput.disabled && startHidden.value) {
            body.set('startAt', startHidden.value);
        }
        submitBtn.disabled = true;
        fetch(form.action, {
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
                    notify((result.data && result.data.message) || '요청을 처리하지 못했습니다.', 'error');
                    return;
                }
                notify(result.data.message || '완료', 'success');
                modal.hide();
                return reloadParts();
            })
            .catch(function () {
                notify('요청을 처리하지 못했습니다.', 'error');
            })
            .finally(function () {
                submitBtn.disabled = false;
            });
    });
})();
