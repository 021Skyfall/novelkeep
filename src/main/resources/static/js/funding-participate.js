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

    function ensureModal() {
        var modal = document.getElementById('fundingParticipateModal');
        if (modal) {
            return modal;
        }
        modal = document.createElement('div');
        modal.id = 'fundingParticipateModal';
        modal.className = 'modal fade';
        modal.tabIndex = -1;
        modal.setAttribute('aria-hidden', 'true');
        modal.innerHTML =
            '<div class="modal-dialog modal-dialog-centered">' +
            '  <div class="modal-content">' +
            '    <div class="modal-header">' +
            '      <h2 class="modal-title h5 fw-bold mb-0">참여 수량</h2>' +
            '      <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button>' +
            '    </div>' +
            '    <div class="modal-body">' +
            '      <p class="text-secondary small mb-3" data-participate-price-hint></p>' +
            '      <label class="form-label fw-semibold" for="fundingParticipateQty">수량 (부)</label>' +
            '      <input id="fundingParticipateQty" class="form-control" type="number" min="1" max="99" value="1">' +
            '      <p class="small text-secondary mt-2 mb-0">같은 펀딩에는 한 번만 참여할 수 있습니다.</p>' +
            '    </div>' +
            '    <div class="modal-footer">' +
            '      <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">취소</button>' +
            '      <button type="button" class="btn btn-dark" data-participate-confirm>결제하고 참여</button>' +
            '    </div>' +
            '  </div>' +
            '</div>';
        document.body.appendChild(modal);
        return modal;
    }

    function askQuantity(priceLabel) {
        var modalEl = ensureModal();
        var qtyInput = modalEl.querySelector('#fundingParticipateQty');
        var hint = modalEl.querySelector('[data-participate-price-hint]');
        if (hint) {
            hint.textContent = (priceLabel || '판매가') + ' × 수량으로 결제됩니다.';
        }
        if (qtyInput) {
            qtyInput.value = '1';
        }
        return new Promise(function (resolve) {
            var confirmBtn = modalEl.querySelector('[data-participate-confirm]');
            var instance = window.bootstrap
                ? window.bootstrap.Modal.getOrCreateInstance(modalEl)
                : null;
            function cleanup(result) {
                if (confirmBtn) {
                    confirmBtn.removeEventListener('click', onConfirm);
                }
                modalEl.removeEventListener('hidden.bs.modal', onHidden);
                resolve(result);
            }
            function onConfirm() {
                var qty = qtyInput ? parseInt(qtyInput.value, 10) : 1;
                if (!Number.isFinite(qty) || qty < 1 || qty > 99) {
                    notify('수량은 1~99부로 입력해 주세요.', 'error');
                    return;
                }
                if (instance) {
                    instance.hide();
                }
                cleanup(qty);
            }
            function onHidden() {
                cleanup(null);
            }
            if (confirmBtn) {
                confirmBtn.addEventListener('click', onConfirm);
            }
            modalEl.addEventListener('hidden.bs.modal', onHidden, { once: true });
            if (instance) {
                instance.show();
            } else {
                var fallback = window.prompt('참여 수량을 입력하세요 (1~99)', '1');
                var qty = fallback == null ? null : parseInt(fallback, 10);
                cleanup(Number.isFinite(qty) && qty >= 1 && qty <= 99 ? qty : null);
            }
        });
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
            refundForm.setAttribute(
                'data-confirm-message',
                '참여 수량 ' + (card.getAttribute('data-my-quantity') || '1') + '부 결제를 환불할까요?'
            );
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

    function submitParticipate(form, quantity) {
        var card = form.closest('[data-campaign-id]');
        if (!card) {
            return;
        }
        var url = card.getAttribute('data-participate-url') || form.getAttribute('action');
        var btn = form.querySelector('[data-funding-participate-btn]');
        if (btn) {
            btn.disabled = true;
        }
        var body = new URLSearchParams();
        body.set('quantity', String(quantity));
        fetch(url, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            credentials: 'same-origin',
            body: body.toString()
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
            if (quantity) {
                card.setAttribute('data-my-quantity', String(quantity));
            }
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
        askQuantity(priceLabel).then(function (quantity) {
            if (quantity) {
                submitParticipate(form, quantity);
            }
        });
    });
})();
