(function () {
    function notify(message, type) {
        if (window.NovelKeepNotification) {
            if (type === 'error') {
                window.NovelKeepNotification.error(message || '요청을 처리하지 못했습니다.', { label: '펀딩 참여' });
                return;
            }
            window.NovelKeepNotification.success(message || '완료', { label: '펀딩 참여' });
            return;
        }
        window.alert(message || '완료');
    }

    function confirmParticipate(priceLabel) {
        var message = (priceLabel || '판매가') + '로 결제하고 참여할까요?\n같은 펀딩에는 한 번만 참여할 수 있습니다.';
        if (window.NovelKeepConfirm && typeof window.NovelKeepConfirm.open === 'function') {
            return window.NovelKeepConfirm.open(message, {
                title: '결제 확인',
                tone: 'warning',
                confirmText: '참여하기',
                cancelText: '취소'
            });
        }
        return Promise.resolve(window.confirm(message));
    }

    function formatQuantity(current, target) {
        return current + ' / ' + target + '부';
    }

    function markParticipated(card, data) {
        var actions = card.querySelector('[data-funding-actions]');
        if (!actions) {
            return;
        }
        var form = actions.querySelector('[data-funding-participate-form]');
        if (form) {
            form.remove();
        }
        var existingBtn = actions.querySelector('[data-funding-participate-btn]');
        if (existingBtn) {
            existingBtn.remove();
        }
        var disabled = actions.querySelector('button[disabled]');
        if (disabled) {
            disabled.remove();
        }

        var refundUrl = card.getAttribute('data-refund-url');
        if (refundUrl && !actions.querySelector('[data-funding-refund-form]')) {
            var refundForm = document.createElement('form');
            refundForm.method = 'post';
            refundForm.action = refundUrl;
            refundForm.className = 'd-inline';
            refundForm.setAttribute('data-funding-refund-form', '');
            refundForm.setAttribute('data-confirm-message', '진행 중인 펀딩 참여를 환불할까요?');
            refundForm.setAttribute('data-confirm-title', '환불 확인');
            refundForm.setAttribute('data-confirm-text', '예, 환불');
            refundForm.setAttribute('data-cancel-text', '아니오');
            refundForm.setAttribute('data-confirm-tone', 'warning');
            var refundBtn = document.createElement('button');
            refundBtn.type = 'submit';
            refundBtn.className = 'btn btn-outline-danger';
            refundBtn.setAttribute('data-funding-refund-btn', '');
            refundBtn.textContent = '환불하기';
            refundForm.appendChild(refundBtn);
            actions.insertBefore(refundForm, actions.firstChild);
        }

        var hint = actions.querySelector('[data-funding-hint]');
        if (hint) {
            hint.textContent = '이미 이 펀딩에 참여했습니다. 진행 중에는 환불할 수 있습니다.';
        }

        var quantity = card.querySelector('[data-funding-quantity]');
        if (quantity && data) {
            quantity.textContent = formatQuantity(data.currentQuantity, data.targetQuantity);
        }
        var percent = card.querySelector('[data-funding-percent]');
        var progress = card.querySelector('[data-funding-progress]');
        var bar = card.querySelector('[data-funding-bar]');
        if (percent && data) {
            percent.textContent = data.achievementPercent + '%';
        }
        if (progress && data) {
            progress.setAttribute('aria-valuenow', String(data.achievementPercent));
            progress.setAttribute('aria-label', '펀딩 달성률 ' + data.achievementPercent + '%');
        }
        if (bar && data) {
            bar.style.width = data.achievementPercent + '%';
        }
    }

    function submitParticipate(form) {
        var card = form.closest('[data-campaign-id]');
        if (!card) {
            return;
        }
        var url = card.getAttribute('data-participate-url') || form.getAttribute('action');
        var btn = form.querySelector('[data-funding-participate-btn]');
        if (btn) {
            btn.disabled = true;
        }
        fetch(url, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
            credentials: 'same-origin'
        }).then(function (response) {
            return response.json().then(function (data) {
                return { ok: response.ok, data: data };
            }).catch(function () {
                return { ok: response.ok, data: null };
            });
        }).then(function (result) {
            if (!result.ok || !result.data || !result.data.success) {
                throw new Error((result.data && result.data.message) || '참여에 실패했습니다.');
            }
            markParticipated(card, result.data);
            notify(result.data.message || '참여했습니다.');
        }).catch(function (error) {
            if (btn) {
                btn.disabled = false;
            }
            notify(error.message || '참여에 실패했습니다.', 'error');
        });
    }

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement) || !form.matches('[data-funding-participate-form]')) {
            return;
        }
        event.preventDefault();
        var card = form.closest('[data-campaign-id]');
        var priceLabel = card ? card.getAttribute('data-price-label') : '';
        confirmParticipate(priceLabel).then(function (ok) {
            if (ok) {
                submitParticipate(form);
            }
        });
    });
})();
