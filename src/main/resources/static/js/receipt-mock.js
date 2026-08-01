(function () {
    if (window.NovelKeepReceiptMock) {
        return;
    }

    var active = null;

    function text(value, fallback) {
        var v = String(value == null ? '' : value).trim();
        return v || fallback || '-';
    }

    function close() {
        if (!active) {
            return;
        }
        document.removeEventListener('keydown', onKey);
        document.documentElement.classList.remove('nk-confirm-scroll-lock');
        active.remove();
        active = null;
    }

    function onKey(event) {
        if (event.key === 'Escape') {
            event.preventDefault();
            close();
        }
    }

    function openFromButton(button) {
        if (active) {
            return;
        }
        var title = text(button.getAttribute('data-receipt-title'), '작품');
        var part = text(button.getAttribute('data-receipt-part'), '본편');
        var kind = text(button.getAttribute('data-receipt-kind'), '내역');
        var qty = text(button.getAttribute('data-receipt-qty'), '1');
        var amount = text(button.getAttribute('data-receipt-amount'), '0원');
        var status = text(button.getAttribute('data-receipt-status'), '-');
        var paidAt = text(button.getAttribute('data-receipt-paid-at'), '-');
        var member = text(button.getAttribute('data-receipt-member'), '');
        var orderId = text(button.getAttribute('data-receipt-order-id'), '');
        var receiptNo = 'NK-' + String(Date.now()).slice(-8);

        var backdrop = document.createElement('div');
        backdrop.className = 'nk-receipt-backdrop';
        backdrop.addEventListener('click', function (event) {
            if (event.target === backdrop) {
                close();
            }
        });

        var dialog = document.createElement('section');
        dialog.className = 'nk-receipt-dialog';
        dialog.setAttribute('role', 'dialog');
        dialog.setAttribute('aria-modal', 'true');
        dialog.setAttribute('aria-labelledby', 'nk-receipt-title');

        var rows = [
            ['영수증 번호', receiptNo],
            ['구분', kind],
            ['작품', title],
            ['권', part],
            ['수량', qty + (qty.indexOf('부') >= 0 ? '' : '부')],
            ['결제금액', amount],
            ['상태', status],
            ['결제일시', paidAt]
        ];
        if (member) {
            rows.splice(2, 0, ['결제자', member]);
        }
        if (orderId) {
            rows.splice(1, 0, ['주문번호', orderId]);
        }

        var listHtml = rows.map(function (pair) {
            return '<li><span>' + pair[0] + '</span><span>' + pair[1] + '</span></li>';
        }).join('');

        dialog.innerHTML =
            '<p class="nk-receipt-brand">NovelKeep Receipt</p>'
            + '<h2 id="nk-receipt-title" class="nk-receipt-title">상세 내역</h2>'
            + '<ul class="nk-receipt-lines">' + listHtml + '</ul>'
            + '<p class="nk-receipt-note">체험용 영수증입니다. 실제 결제·세금계산서와 무관하며 화면 확인용으로만 사용합니다.</p>'
            + '<div class="nk-receipt-actions"><button type="button" class="btn btn-dark btn-sm">닫기</button></div>';

        dialog.querySelector('button').addEventListener('click', close);
        backdrop.appendChild(dialog);
        document.body.appendChild(backdrop);
        document.documentElement.classList.add('nk-confirm-scroll-lock');
        document.addEventListener('keydown', onKey);
        active = backdrop;
        dialog.querySelector('button').focus();
    }

    document.addEventListener('click', function (event) {
        var button = event.target.closest('[data-receipt-mock]');
        if (!button) {
            return;
        }
        event.preventDefault();
        openFromButton(button);
    });

    window.NovelKeepReceiptMock = {
        openFromButton: openFromButton
    };
})();
