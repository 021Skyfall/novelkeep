(function () {
    function showActionMessage(action, active) {
        if (!window.NovelKeepNotification) {
            return;
        }
        if (action === 'favorite') {
            window.NovelKeepNotification.success(
                active ? '작품을 내 즐겨찾기에 추가했습니다.' : '작품을 내 즐겨찾기에서 해제했습니다.',
                { label: active ? '즐겨찾기 추가' : '즐겨찾기 해제' }
            );
        } else if (action === 'recommend') {
            window.NovelKeepNotification.success(
                active ? '이 작품을 추천했습니다.' : '이 작품의 추천을 취소했습니다.',
                { label: active ? '작품 추천' : '추천 취소' }
            );
        }
    }

    function showError(action) {
        var favorite = action === 'favorite';
        var message = favorite
            ? '즐겨찾기 상태를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.'
            : '추천 상태를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.';
        if (window.NovelKeepNotification) {
            window.NovelKeepNotification.error(message, {
                label: favorite ? '즐겨찾기 처리 실패' : '추천 처리 실패'
            });
            return;
        }
        window.alert(message);
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
                showActionMessage(action, !!data.active);
            } else if (action === 'recommend') {
                handleRecommendResult(form, !!data.active, Number(data.recommendationCount));
                showActionMessage(action, !!data.active);
            }
        }).catch(function () {
            showError(action);
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
