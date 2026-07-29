(function () {
    function showError(root, message) {
        var box = root ? root.querySelector('[data-action-error]') : document.querySelector('[data-action-error]');
        if (!box) {
            window.alert(message);
            return;
        }
        box.textContent = message;
        box.classList.remove('d-none');
    }

    function clearError(root) {
        var box = root ? root.querySelector('[data-action-error]') : document.querySelector('[data-action-error]');
        if (!box) {
            return;
        }
        box.textContent = '';
        box.classList.add('d-none');
    }

    function updateRecommendUi(scope, active, count) {
        var button = scope.querySelector('[data-action-button]');
        if (button) {
            var detail = !!scope.closest('[data-detail-actions]');
            button.textContent = active ? '추천 취소' : (detail ? '추천하기' : '추천');
            button.classList.toggle('btn-dark', active);
            button.classList.toggle('btn-outline-dark', !active);
        }
        var countBadge = null;
        var card = scope.closest('[data-novel-card]');
        if (card) {
            countBadge = card.querySelector('[data-recommend-count]');
        } else {
            var detailRoot = scope.closest('[data-detail-actions]');
            countBadge = detailRoot
                ? document.querySelector('[data-recommend-count]')
                : null;
        }
        if (countBadge && typeof count === 'number' && !Number.isNaN(count)) {
            countBadge.textContent = '추천 ' + count;
        }
    }

    function updateFavoriteUi(scope, active) {
        var button = scope.querySelector('[data-action-button]');
        if (!button) {
            return;
        }
        button.textContent = active ? '★' : '☆';
        button.classList.toggle('is-active', active);
        button.setAttribute('aria-label', active ? '내 즐겨찾기 해제' : '내 즐겨찾기');
    }

    function reloadAsyncContent() {
        if (typeof window.novelkeepReloadAsync === 'function') {
            window.novelkeepReloadAsync();
        }
    }

    function handleFavoriteResult(form, active) {
        updateFavoriteUi(form, active);
        var list = form.closest('[data-novel-list]');
        if (list && list.getAttribute('data-favorite-view') === 'true' && !active) {
            var card = form.closest('[data-novel-card]');
            if (card) {
                card.remove();
            }
            if (!list.querySelector('[data-novel-card]')) {
                reloadAsyncContent();
            }
        }
    }

    function handleRecommendResult(form, active, count) {
        updateRecommendUi(form, active, count);
        var list = form.closest('[data-novel-list]');
        if (list && list.getAttribute('data-recommend-sort') === 'true') {
            reloadAsyncContent();
        }
    }

    function submitAction(form) {
        var action = form.getAttribute('data-novel-action');
        var root = form.closest('[data-async-content]') || document;
        clearError(root);

        var button = form.querySelector('[data-action-button]');
        if (button) {
            button.disabled = true;
        }

        fetch(form.action, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
            credentials: 'same-origin',
            body: new FormData(form)
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('요청을 처리하지 못했습니다.');
            }
            return response.json();
        }).then(function (data) {
            if (action === 'favorite') {
                handleFavoriteResult(form, !!data.active);
            } else if (action === 'recommend') {
                handleRecommendResult(form, !!data.active, Number(data.recommendationCount));
            }
        }).catch(function () {
            showError(root, '잠시 후 다시 시도해 주세요.');
        }).finally(function () {
            if (button) {
                button.disabled = false;
            }
        });
    }

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }
        if (!form.hasAttribute('data-novel-action')) {
            return;
        }
        event.preventDefault();
        submitAction(form);
    });
})();
