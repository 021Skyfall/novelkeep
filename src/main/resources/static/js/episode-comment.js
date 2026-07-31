(function () {
    function notify(message, label) {
        if (!message) {
            return;
        }
        if (window.NovelKeepNotification) {
            window.NovelKeepNotification.success(message, { label: label || '댓글' });
            return;
        }
        window.alert(message);
    }

    function notifyError(message) {
        if (window.NovelKeepNotification) {
            window.NovelKeepNotification.error(message || '요청을 처리하지 못했습니다.', { label: '댓글' });
            return;
        }
        window.alert(message || '요청을 처리하지 못했습니다.');
    }

    function reloadComments(root) {
        var main = root.closest('.episode-read-main');
        var url = main ? main.getAttribute('data-comments-partial') : null;
        var panel = root.querySelector('[data-comments-panel]');
        if (!url || !panel) {
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
            if (!response.ok) {
                throw new Error('reload failed');
            }
            return response.text();
        }).then(function (html) {
            panel.innerHTML = html;
            window.scrollTo(0, scrollY);
        });
    }

    function submitAjax(form) {
        var root = form.closest('[data-comments-root]');
        if (!root) {
            return;
        }
        var scrollY = window.scrollY;
        var body = new FormData(form);
        fetch(form.getAttribute('action'), {
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
            return reloadComments(root).then(function () {
                window.scrollTo(0, scrollY);
                notify(result.data.message, result.data.action === 'deleted' ? '댓글 삭제'
                    : (result.data.action === 'updated' ? '댓글 수정' : '댓글 등록'));
            });
        }).catch(function (error) {
            window.scrollTo(0, scrollY);
            notifyError(error.message);
        });
    }

    document.addEventListener('click', function (event) {
        var replyToggle = event.target.closest('[data-comment-reply-toggle]');
        if (replyToggle) {
            var replyTargetId = replyToggle.getAttribute('data-target');
            var replyForm = replyTargetId ? document.getElementById(replyTargetId) : null;
            if (!replyForm) {
                return;
            }
            replyForm.classList.remove('d-none');
            var replyTextarea = replyForm.querySelector('textarea');
            if (replyTextarea) {
                replyTextarea.focus();
            }
            return;
        }

        var replyCancel = event.target.closest('[data-comment-reply-cancel]');
        if (replyCancel) {
            var cancelReplyForm = replyCancel.closest('.comment-reply-form');
            if (!cancelReplyForm) {
                return;
            }
            cancelReplyForm.classList.add('d-none');
            var cancelTextarea = cancelReplyForm.querySelector('textarea');
            if (cancelTextarea) {
                cancelTextarea.value = '';
            }
            return;
        }

        var toggle = event.target.closest('[data-comment-edit-toggle]');
        if (toggle) {
            var targetId = toggle.getAttribute('data-target');
            var form = targetId ? document.getElementById(targetId) : null;
            var item = toggle.closest('.comment-reply-body, .comment-item');
            if (!form || !item) {
                return;
            }
            var view = item.querySelector('[data-comment-view]');
            form.classList.remove('d-none');
            if (view) {
                view.classList.add('d-none');
            }
            return;
        }

        var cancel = event.target.closest('[data-comment-edit-cancel]');
        if (cancel) {
            var editForm = cancel.closest('.comment-edit-form');
            var editItem = cancel.closest('.comment-reply-body, .comment-item');
            if (!editForm || !editItem) {
                return;
            }
            editForm.classList.add('d-none');
            var editView = editItem.querySelector('[data-comment-view]');
            if (editView) {
                editView.classList.remove('d-none');
            }
        }
    });

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement) || !form.hasAttribute('data-ajax-comment')) {
            return;
        }
        event.preventDefault();

        var message = form.getAttribute('data-confirm-message');
        if (message && window.NovelKeepConfirm) {
            window.NovelKeepConfirm.open(message, {
                title: form.getAttribute('data-confirm-title') || '댓글 삭제',
                confirmText: '삭제',
                cancelText: '취소',
                tone: 'danger'
            }).then(function (confirmed) {
                if (confirmed) {
                    submitAjax(form);
                }
            });
            return;
        }
        submitAjax(form);
    });
})();
