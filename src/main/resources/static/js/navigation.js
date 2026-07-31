(function () {
    var button = document.querySelector('[data-back-to-top]');
    if (button) {
        function syncVisibility() {
            if (window.scrollY > 320) {
                button.classList.add('is-visible');
            } else {
                button.classList.remove('is-visible');
            }
        }

        button.addEventListener('click', function () {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });

        window.addEventListener('scroll', syncVisibility, { passive: true });
        syncVisibility();
    }

    var becomeWriter = document.querySelector('[data-become-writer]');
    if (becomeWriter) {
        becomeWriter.addEventListener('click', function () {
            if (!window.NovelKeepConfirm) {
                return;
            }
            var logoutUrl = becomeWriter.getAttribute('data-logout-url') || '/logout';
            var message = [
                '이 버튼은 독자가 작가가 될 수 있는 장치입니다.',
                '각종 인증 절차를 거쳐 작가로 인증한 뒤 작품을 제작할 수 있습니다.',
                '',
                '지금은 테스트 환경이라 이 버튼을 눌러도 별도의 전환 동작은 없습니다.',
                '작가로 테스트하려면 역할 변경을 눌러 랜딩에서 작가를 다시 선택해 주세요.'
            ].join('\n');

            window.NovelKeepConfirm.open(message, {
                title: '작가가 되고 싶으신가요?',
                confirmText: '역할 변경',
                cancelText: '취소',
                tone: 'warning'
            }).then(function (confirmed) {
                if (!confirmed) {
                    return;
                }
                var form = document.createElement('form');
                form.method = 'post';
                form.action = logoutUrl;
                form.style.display = 'none';
                document.body.appendChild(form);
                form.submit();
            });
        });
    }
})();
