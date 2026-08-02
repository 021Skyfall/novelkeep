(function () {
    var LABELS = {
        partCreate: '권(부) 추가',
        partUpdate: '권(부) 수정',
        partDelete: '권(부) 삭제',
        episodeDelete: '회차 삭제',
        episodeBulk: '회차 일괄 처리'
    };

    function notify(message, label) {
        if (!message) {
            return;
        }
        if (window.NovelKeepNotification) {
            window.NovelKeepNotification.success(message, { label: label || '작품 관리' });
            return;
        }
        window.alert(message);
    }

    function notifyError(message) {
        if (window.NovelKeepNotification) {
            window.NovelKeepNotification.error(message || '요청을 처리하지 못했습니다.', { label: '작품 관리' });
            return;
        }
        window.alert(message || '요청을 처리하지 못했습니다.');
    }

    function bindPanels(root) {
        root.querySelectorAll('[data-toggle-panel]').forEach(function (button) {
            button.addEventListener('click', function () {
                var panelId = button.getAttribute('data-toggle-panel');
                var panel = panelId ? document.getElementById(panelId) : null;
                if (!panel) {
                    return;
                }
                var open = panel.classList.contains('d-none');
                root.querySelectorAll('.detail-inline-panel').forEach(function (other) {
                    other.classList.add('d-none');
                });
                root.querySelectorAll('[data-toggle-panel]').forEach(function (otherBtn) {
                    otherBtn.setAttribute('aria-expanded', 'false');
                });
                if (open) {
                    panel.classList.remove('d-none');
                    button.setAttribute('aria-expanded', 'true');
                    var focusEl = panel.querySelector('input, textarea, select');
                    if (focusEl) {
                        focusEl.focus();
                    }
                }
            });
        });

        root.querySelectorAll('[data-close-panel]').forEach(function (button) {
            button.addEventListener('click', function () {
                var panel = button.closest('.detail-inline-panel');
                if (panel) {
                    panel.classList.add('d-none');
                }
                root.querySelectorAll('[data-toggle-panel]').forEach(function (toggle) {
                    toggle.setAttribute('aria-expanded', 'false');
                });
            });
        });

        refreshBulkState(root);
    }

    function selectedEpisodeIds(root) {
        return Array.prototype.map.call(
            root.querySelectorAll('[data-episode-check]:checked'),
            function (input) {
                return input.value;
            }
        );
    }

    function refreshBulkState(root) {
        var toolbar = root.querySelector('[data-bulk-toolbar]');
        var checks = root.querySelectorAll('[data-episode-check]');
        var selected = root.querySelectorAll('[data-episode-check]:checked');
        var fundingLockedSelected = false;
        selected.forEach(function (input) {
            if (input.getAttribute('data-funding-locked') === 'true') {
                fundingLockedSelected = true;
            }
        });
        if (toolbar) {
            var countEl = toolbar.querySelector('[data-bulk-count]');
            var selectAll = toolbar.querySelector('[data-select-all-episodes]');
            if (countEl) {
                countEl.textContent = selected.length + '개 선택';
            }
            if (selectAll) {
                selectAll.checked = checks.length > 0 && selected.length === checks.length;
                selectAll.indeterminate = selected.length > 0 && selected.length < checks.length;
            }
        }
        root.querySelectorAll('[data-bulk-action]').forEach(function (button) {
            var disabled = selected.length === 0;
            if (button.getAttribute('data-bulk-action') === 'DELETE' && fundingLockedSelected) {
                disabled = true;
            }
            button.disabled = disabled;
        });
    }

    function reloadParts(root) {
        var url = root.getAttribute('data-parts-partial');
        if (!url) {
            return Promise.resolve();
        }
        var scrollY = window.scrollY;
        return fetch(url, {
            headers: {
                'X-Requested-With': 'XMLHttpRequest',
                'Accept': 'text/html'
            },
            credentials: 'same-origin'
        }).then(function (response) {
            if (response.status === 401) {
                window.location.href = '/?roleRequired=true';
                throw new Error('auth');
            }
            if (!response.ok) {
                throw new Error('목록을 다시 불러오지 못했습니다.');
            }
            return response.text();
        }).then(function (html) {
            if (/<!doctype html|<html[\s>]/i.test(String(html || '').trim())) {
                window.location.href = '/?roleRequired=true';
                return;
            }
            root.innerHTML = html;
            bindPanels(root);
            window.scrollTo(0, scrollY);
        });
    }

    function submitAjax(form, root) {
        var scrollY = window.scrollY;
        var kind = form.getAttribute('data-ajax-content');
        var label = LABELS[kind] || '작품 관리';
        fetch(form.getAttribute('action'), {
            method: 'POST',
            headers: {
                'X-Requested-With': 'XMLHttpRequest',
                'Accept': 'application/json'
            },
            body: new FormData(form),
            credentials: 'same-origin'
        }).then(function (response) {
            return response.json().then(function (data) {
                return { ok: response.ok, data: data };
            }).catch(function () {
                return { ok: response.ok, data: null };
            });
        }).then(function (result) {
            if (!result.ok || !result.data || !result.data.ok) {
                throw new Error((result.data && result.data.message) || '요청을 처리하지 못했습니다.');
            }
            return reloadParts(root).then(function () {
                window.scrollTo(0, scrollY);
                notify(result.data.message, label);
            });
        }).catch(function (error) {
            window.scrollTo(0, scrollY);
            notifyError(error.message);
        });
    }

    function handleAjaxSubmit(form, root) {
        var message = form.getAttribute('data-confirm-delete');
        if (message && window.NovelKeepConfirm) {
            window.NovelKeepConfirm.open(message, {
                title: form.getAttribute('data-confirm-title') || '영구 삭제 확인',
                confirmText: form.getAttribute('data-confirm-text') || '예, 삭제',
                cancelText: '아니오',
                tone: 'danger'
            }).then(function (confirmed) {
                if (confirmed) {
                    submitAjax(form, root);
                }
            });
            return;
        }
        submitAjax(form, root);
    }

    function submitBulk(root, action, button) {
        var toolbar = root.querySelector('[data-bulk-toolbar]');
        var url = toolbar ? toolbar.getAttribute('data-bulk-url') : null;
        var ids = selectedEpisodeIds(root);
        if (!url || ids.length === 0) {
            return;
        }

        function send() {
            var scrollY = window.scrollY;
            var body = new FormData();
            body.append('action', action);
            ids.forEach(function (id) {
                body.append('episodeIds', id);
            });
            fetch(url, {
                method: 'POST',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    'Accept': 'application/json'
                },
                body: body,
                credentials: 'same-origin'
            }).then(function (response) {
                return response.json().then(function (data) {
                    return { ok: response.ok, data: data };
                }).catch(function () {
                    return { ok: response.ok, data: null };
                });
            }).then(function (result) {
                if (!result.ok || !result.data || !result.data.ok) {
                    throw new Error((result.data && result.data.message) || '요청을 처리하지 못했습니다.');
                }
                return reloadParts(root).then(function () {
                    window.scrollTo(0, scrollY);
                    notify(result.data.message, LABELS.episodeBulk);
                });
            }).catch(function (error) {
                window.scrollTo(0, scrollY);
                notifyError(error.message);
            });
        }

        var confirmMessage = button.getAttribute('data-confirm-message')
            || button.getAttribute('data-confirm-delete');
        if (confirmMessage && window.NovelKeepConfirm) {
            var deleting = button.hasAttribute('data-confirm-delete');
            var tone = button.getAttribute('data-confirm-tone')
                || (deleting ? 'danger' : 'warning');
            window.NovelKeepConfirm.open(confirmMessage, {
                title: button.getAttribute('data-confirm-title')
                    || (deleting ? '영구 삭제 확인' : '작업 확인'),
                confirmText: button.getAttribute('data-confirm-text')
                    || (deleting ? '예, 삭제' : '예'),
                cancelText: '아니오',
                tone: tone
            }).then(function (confirmed) {
                if (confirmed) {
                    send();
                }
            });
            return;
        }
        send();
    }

    document.addEventListener('DOMContentLoaded', function () {
        var root = document.querySelector('[data-parts-root]');
        if (!root) {
            return;
        }
        bindPanels(root);

        root.addEventListener('submit', function (event) {
            var form = event.target;
            if (!(form instanceof HTMLFormElement) || !form.hasAttribute('data-ajax-content')) {
                return;
            }
            event.preventDefault();
            event.stopPropagation();
            handleAjaxSubmit(form, root);
        });

        root.addEventListener('change', function (event) {
            var target = event.target;
            if (!(target instanceof HTMLInputElement)) {
                return;
            }
            if (target.hasAttribute('data-select-all-episodes')) {
                root.querySelectorAll('[data-episode-check]').forEach(function (input) {
                    input.checked = target.checked;
                });
                root.querySelectorAll('[data-select-part-episodes]').forEach(function (input) {
                    input.checked = target.checked;
                });
                refreshBulkState(root);
                return;
            }
            if (target.hasAttribute('data-select-part-episodes')) {
                var partId = target.getAttribute('data-part-id');
                var list = root.querySelector('[data-part-episodes="' + partId + '"]');
                if (list) {
                    list.querySelectorAll('[data-episode-check]').forEach(function (input) {
                        input.checked = target.checked;
                    });
                }
                refreshBulkState(root);
                return;
            }
            if (target.hasAttribute('data-episode-check')) {
                refreshBulkState(root);
            }
        });

        root.addEventListener('click', function (event) {
            var button = event.target.closest('[data-bulk-action]');
            if (!button || !root.contains(button)) {
                return;
            }
            event.preventDefault();
            submitBulk(root, button.getAttribute('data-bulk-action'), button);
        });
    });

    window.NovelKeepDetailWriter = {
        bind: function (root) {
            if (root) {
                bindPanels(root);
            }
        }
    };
})();
