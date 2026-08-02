(function () {
    var CLOSE_READY_MESSAGE =
        '마감하면 성공으로 판정됩니다.\n\n'
        + '· 목표 부수 달성 + 전체 회차 공개 + 부 완결 상태입니다.\n'
        + '· 바로 주문이 생기지 않습니다.\n'
        + '· 운영자가 승인한 뒤 주문이 접수됩니다.\n'
        + '· 승인 전까지 목록에 「성공 · 승인 대기」로 남습니다.';

    var CLOSE_FAIL_MESSAGE =
        '마감하면 실패로 판정됩니다.\n\n'
        + '· 목표 부수 미달입니다.\n'
        + '· 바로 환불되지 않습니다.\n'
        + '· 운영자가 승인한 뒤 환불이 진행됩니다.\n'
        + '· 승인 전까지 목록에 「실패 · 승인 대기」로 남습니다.';

    var CLOSE_BLOCKED_HINT =
        '성공 마감하려면 해당 부의 전체 회차가 공개되어 있고, 부가 완결 처리되어야 합니다.';

    function notify(message, type) {
        if (window.NovelKeepNotification) {
            if (type === 'error') {
                window.NovelKeepNotification.error(message || '요청을 처리하지 못했습니다.', { label: '펀딩 관리' });
                return;
            }
            window.NovelKeepNotification.success(message || '완료', { label: '펀딩 관리' });
            return;
        }
        openPopup(message || '완료', type === 'error' ? '오류' : '안내');
    }

    function openPopup(message, title, options) {
        var settings = options || {};
        if (window.NovelKeepConfirm) {
            return window.NovelKeepConfirm.open(String(message || ''), {
                title: title || settings.title || '안내',
                confirmText: settings.confirmText || '확인',
                cancelText: settings.cancelText || '닫기',
                tone: settings.tone || 'warning'
            });
        }
        // confirm-dialog 미로드 시에도 native alert 대신 간단 팝업
        return new Promise(function (resolve) {
            var backdrop = document.createElement('div');
            backdrop.className = 'nk-confirm-backdrop';
            var dialog = document.createElement('section');
            dialog.className = 'nk-confirm-dialog is-warning';
            dialog.innerHTML = '<h2 class="nk-confirm-title"></h2><p class="nk-confirm-message"></p>'
                + '<div class="nk-confirm-actions">'
                + '<button type="button" class="btn btn-warning nk-confirm-button">확인</button>'
                + '<button type="button" class="btn btn-outline-secondary nk-confirm-button">닫기</button></div>';
            dialog.querySelector('.nk-confirm-title').textContent = title || '안내';
            dialog.querySelector('.nk-confirm-message').textContent = String(message || '');
            function close(ok) {
                backdrop.remove();
                resolve(ok);
            }
            dialog.querySelectorAll('button')[0].addEventListener('click', function () { close(true); });
            dialog.querySelectorAll('button')[1].addEventListener('click', function () { close(false); });
            backdrop.appendChild(dialog);
            document.body.appendChild(backdrop);
        });
    }

    function openCloseConfirm(message, title, options) {
        var settings = options || {};
        var confirmDisabled = !!settings.confirmDisabled;
        var hint = settings.hint || '';
        if (!confirmDisabled && !hint) {
            return openPopup(message, title, settings);
        }
        return new Promise(function (resolve) {
            var backdrop = document.createElement('div');
            backdrop.className = 'nk-confirm-backdrop';
            var dialog = document.createElement('section');
            dialog.className = 'nk-confirm-dialog is-warning'
                + (confirmDisabled ? ' is-close-blocked' : '');
            dialog.setAttribute('role', 'alertdialog');
            dialog.setAttribute('aria-modal', 'true');

            var titleEl = document.createElement('h2');
            titleEl.className = 'nk-confirm-title';
            titleEl.textContent = title || '펀딩 마감';

            var messageEl = document.createElement('p');
            messageEl.className = 'nk-confirm-message';
            messageEl.style.whiteSpace = 'pre-line';
            messageEl.textContent = String(message || '');

            var actions = document.createElement('div');
            actions.className = 'nk-confirm-actions';

            var confirmButton = document.createElement('button');
            confirmButton.type = 'button';
            confirmButton.className = 'btn nk-confirm-button btn-warning'
                + (confirmDisabled ? ' is-close-blocked' : '');
            confirmButton.textContent = settings.confirmText || '예, 마감';
            confirmButton.disabled = confirmDisabled;
            if (confirmDisabled) {
                confirmButton.setAttribute('aria-disabled', 'true');
                confirmButton.title = '전체 회차 공개와 부 완결 후 마감할 수 있습니다.';
            }

            var cancelButton = document.createElement('button');
            cancelButton.type = 'button';
            cancelButton.className = confirmDisabled
                ? 'btn nk-confirm-button is-close-dismiss'
                : 'btn nk-confirm-button btn-outline-secondary';
            cancelButton.textContent = settings.cancelText || '아니오';

            function close(confirmed) {
                document.documentElement.classList.remove('nk-confirm-scroll-lock');
                backdrop.remove();
                resolve(confirmed);
            }

            cancelButton.addEventListener('click', function () { close(false); });
            confirmButton.addEventListener('click', function () {
                if (!confirmButton.disabled) {
                    close(true);
                }
            });
            backdrop.addEventListener('click', function (event) {
                if (event.target === backdrop) {
                    close(false);
                }
            });

            dialog.appendChild(titleEl);
            dialog.appendChild(messageEl);
            if (hint) {
                var hintEl = document.createElement('p');
                hintEl.className = 'publishing-close-blocked-hint';
                hintEl.textContent = hint;
                dialog.appendChild(hintEl);
            }
            actions.appendChild(confirmButton);
            actions.appendChild(cancelButton);
            dialog.appendChild(actions);
            backdrop.appendChild(dialog);
            document.body.appendChild(backdrop);
            document.documentElement.classList.add('nk-confirm-scroll-lock');
            (confirmDisabled ? cancelButton : confirmButton).focus();
        });
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
        var metaView = form.querySelector('[data-funding-meta-view]');
        if (metaView) {
            metaView.classList.toggle('d-none', editing);
        }
        var editFields = form.querySelector('[data-funding-edit-fields]');
        if (editFields) {
            editFields.classList.toggle('d-none', !editing);
        }
        form.querySelectorAll('[data-funding-close], [data-funding-cancel]').forEach(function (btn) {
            btn.classList.toggle('d-none', editing);
        });
    }

    function formatMetaDateTime(value) {
        if (!value) {
            return '';
        }
        return String(value).replace('T', ' ').slice(0, 16);
    }

    function syncFundingMetaView(form) {
        var endInput = form.querySelector('[name="endAt"]');
        var targetInput = form.querySelector('[name="targetQuantity"]');
        var priceInput = form.querySelector('[name="priceAmount"]');
        var endLabel = form.querySelector('[data-funding-meta-end]');
        var targetLabel = form.querySelector('[data-funding-meta-target]');
        if (endLabel && endInput) {
            endLabel.textContent = '종료 ' + formatMetaDateTime(endInput.value);
        }
        if (targetLabel && targetInput && priceInput) {
            var target = Number(targetInput.value || 0);
            var price = Number(priceInput.value || 0);
            targetLabel.textContent = '목표 ' + target + '부 · ' + price.toLocaleString('ko-KR') + '원';
        }
        var card = form.closest('[data-campaign-card]');
        if (card && targetInput) {
            var qty = card.querySelector('[data-campaign-qty]');
            if (qty) {
                var current = String(qty.textContent || '').split('/')[0];
                qty.textContent = current + '/' + targetInput.value + '부';
            }
        }
    }

    function formatPrice(value) {
        var num = Number(value || 0);
        return num.toLocaleString('ko-KR') + '원';
    }

    function bindCampaignForms(root) {
        (root || document).querySelectorAll('[data-funding-edit-form]').forEach(function (form) {
            if (form.dataset.bound === '1') {
                return;
            }
            form.dataset.bound = '1';
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
                        syncFundingMetaView(form);
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

                openPopup(
                    cancelBtn.getAttribute('data-confirm-delete') || '취소할까요?',
                    cancelBtn.getAttribute('data-confirm-title') || '펀딩 취소',
                    {
                        confirmText: cancelBtn.getAttribute('data-confirm-text') || '예, 취소',
                        cancelText: '아니오',
                        tone: 'danger'
                    }
                ).then(function (confirmed) {
                    if (confirmed) {
                        sendCancel();
                    }
                });
            });

            form.addEventListener('click', function (event) {
                var closeBtn = event.target.closest('[data-funding-close]');
                if (!closeBtn || !form.contains(closeBtn)) {
                    return;
                }
                event.preventDefault();
                var url = closeBtn.getAttribute('data-close-url');
                if (!url) {
                    return;
                }

                var goalMet = closeBtn.getAttribute('data-close-goal-met') === 'true';
                var allPublished = closeBtn.getAttribute('data-close-all-published') === 'true';
                var partCompleted = closeBtn.getAttribute('data-close-part-completed') === 'true';
                var contentReady = allPublished && partCompleted;
                var confirmMessage = goalMet ? CLOSE_READY_MESSAGE : CLOSE_FAIL_MESSAGE;
                if (closeBtn.getAttribute('data-volume-over') === 'true') {
                    var volumeChars = closeBtn.getAttribute('data-volume-chars') || '';
                    confirmMessage += '\n\n[분량 안내]\n'
                        + '해당 작품은 ' + volumeChars + '자 입니다. '
                        + '공개 회차 합이 10만 자를 넘습니다. 출판 담당과 문의가 필요합니다.';
                }

                function sendClose() {
                    closeBtn.disabled = true;
                    fetch(url, {
                        method: 'POST',
                        headers: csrfHeaders(),
                        credentials: 'same-origin'
                    })
                        .then(function (res) {
                            return res.json().then(function (data) {
                                return { ok: res.ok, data: data };
                            }).catch(function () {
                                return { ok: false, data: null };
                            });
                        })
                        .then(function (result) {
                            if (!result.ok || !result.data || !result.data.success) {
                                return openPopup(
                                    (result.data && result.data.message) || '마감에 실패했습니다.',
                                    '마감 실패',
                                    { confirmText: '확인', cancelText: '닫기', tone: 'danger' }
                                );
                            }
                            var outcomeTitle = result.data.closeOutcome === 'SUCCESS'
                                ? '성공 마감 · 승인 대기'
                                : '실패 마감 · 승인 대기';
                            return openPopup(result.data.message || '마감했습니다.', outcomeTitle, {
                                confirmText: '확인',
                                cancelText: '닫기',
                                tone: 'warning'
                            }).then(function () {
                                window.location.reload();
                            });
                        })
                        .catch(function () {
                            openPopup('마감에 실패했습니다.', '마감 실패', {
                                confirmText: '확인',
                                cancelText: '닫기',
                                tone: 'danger'
                            });
                        })
                        .finally(function () {
                            closeBtn.disabled = false;
                        });
                }

                openCloseConfirm(confirmMessage, closeBtn.getAttribute('data-confirm-title') || '펀딩 마감', {
                    confirmText: closeBtn.getAttribute('data-confirm-text') || '예, 마감',
                    cancelText: '아니오',
                    tone: 'warning',
                    confirmDisabled: goalMet && !contentReady,
                    hint: goalMet && !contentReady ? CLOSE_BLOCKED_HINT : ''
                }).then(function (confirmed) {
                    if (confirmed) {
                        sendClose();
                    }
                });
            });
        });
    }

    function initAjaxSearch() {
        var panel = document.querySelector('[data-publishing-ajax]');
        var listHost = document.querySelector('[data-publishing-list]');
        if (!panel || !listHost) {
            return;
        }

        var searchUrl = panel.getAttribute('data-search-url') || '/writer/publishing';
        var titleInput = panel.querySelector('[data-publishing-title]');
        var status = '';
        var orderStatus = '';
        var sortField = panel.getAttribute('data-active-sort-field') || '';
        var sortDir = panel.getAttribute('data-active-sort-dir') || '';
        var activeStatus = panel.querySelector('[data-publishing-status].is-active');
        if (activeStatus) {
            status = activeStatus.getAttribute('data-publishing-status') || '';
        }
        var activeOrderStatus = panel.querySelector('[data-order-status].is-active');
        if (activeOrderStatus) {
            orderStatus = activeOrderStatus.getAttribute('data-order-status') || '';
        }

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
            if (status) {
                params.set('status', status);
            }
            if (orderStatus) {
                params.set('orderStatus', orderStatus);
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
                    bindCampaignForms(listHost);
                })
                .catch(function (err) {
                    if (err && (err.name === 'AbortError' || err.message === 'auth')) {
                        return;
                    }
                    notify('검색에 실패했습니다.', 'error');
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
        var titleSearchBtn = panel.querySelector('[data-publishing-search-btn]');
        if (titleSearchBtn) {
            titleSearchBtn.addEventListener('click', function (event) {
                event.preventDefault();
                refresh();
            });
        }

        panel.querySelectorAll('[data-publishing-status]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                status = btn.getAttribute('data-publishing-status') || '';
                panel.querySelectorAll('[data-publishing-status]').forEach(function (item) {
                    item.classList.toggle('is-active', item === btn);
                });
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
                status = '';
                orderStatus = '';
                panel.querySelectorAll('[data-publishing-status]').forEach(function (item) {
                    item.classList.toggle('is-active', (item.getAttribute('data-publishing-status') || '') === '');
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
    }

    bindCampaignForms(document);
    initAjaxSearch();
})();
