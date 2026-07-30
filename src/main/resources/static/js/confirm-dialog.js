(function () {
    var activeDialog = null;

    function createButton(text, className) {
        var button = document.createElement("button");
        button.type = "button";
        button.className = "btn nk-confirm-button " + className;
        button.textContent = text;
        return button;
    }

    function open(message, options) {
        var text = String(message || "").trim();
        if (!text) {
            return Promise.resolve(false);
        }

        if (activeDialog) {
            return Promise.resolve(false);
        }

        var settings = options || {};
        var tone = settings.tone === "warning" ? "warning" : "danger";
        var previousFocus = document.activeElement;

        return new Promise(function (resolve) {
            var backdrop = document.createElement("div");
            backdrop.className = "nk-confirm-backdrop";

            var dialog = document.createElement("section");
            dialog.className = "nk-confirm-dialog is-" + tone;
            dialog.setAttribute("role", "alertdialog");
            dialog.setAttribute("aria-modal", "true");
            dialog.setAttribute("aria-labelledby", "nk-confirm-title");
            dialog.setAttribute("aria-describedby", "nk-confirm-message");

            var title = document.createElement("h2");
            title.id = "nk-confirm-title";
            title.className = "nk-confirm-title";
            title.textContent = settings.title || "중요한 작업 확인";

            var messageBox = document.createElement("p");
            messageBox.id = "nk-confirm-message";
            messageBox.className = "nk-confirm-message";
            messageBox.textContent = text;

            var actions = document.createElement("div");
            actions.className = "nk-confirm-actions";

            var cancelButton = createButton(settings.cancelText || "아니오", "btn-outline-secondary");
            var confirmClass = tone === "danger" ? "btn-danger" : "btn-warning";
            var confirmButton = createButton(settings.confirmText || "예", confirmClass);

            function close(confirmed) {
                document.removeEventListener("keydown", handleKeydown);
                document.documentElement.classList.remove("nk-confirm-scroll-lock");
                backdrop.remove();
                activeDialog = null;
                if (previousFocus && typeof previousFocus.focus === "function") {
                    previousFocus.focus();
                }
                resolve(confirmed);
            }

            function handleKeydown(event) {
                if (event.key === "Escape") {
                    event.preventDefault();
                    close(false);
                    return;
                }
                if (event.key !== "Tab") {
                    return;
                }

                var first = cancelButton;
                var last = confirmButton;
                if (event.shiftKey && document.activeElement === first) {
                    event.preventDefault();
                    last.focus();
                } else if (!event.shiftKey && document.activeElement === last) {
                    event.preventDefault();
                    first.focus();
                }
            }

            cancelButton.addEventListener("click", function () {
                close(false);
            });
            confirmButton.addEventListener("click", function () {
                close(true);
            });
            backdrop.addEventListener("click", function (event) {
                if (event.target === backdrop) {
                    close(false);
                }
            });

            actions.appendChild(cancelButton);
            actions.appendChild(confirmButton);
            dialog.appendChild(title);
            dialog.appendChild(messageBox);
            dialog.appendChild(actions);
            backdrop.appendChild(dialog);
            document.body.appendChild(backdrop);
            document.documentElement.classList.add("nk-confirm-scroll-lock");
            document.addEventListener("keydown", handleKeydown);
            activeDialog = backdrop;
            cancelButton.focus();
        });
    }

    function confirmForm(form, submitter) {
        var message = form.getAttribute("data-confirm-message")
            || form.getAttribute("data-confirm-delete")
            || "이 작업을 진행할까요?";
        var deleting = form.hasAttribute("data-confirm-delete");

        open(message, {
            title: form.getAttribute("data-confirm-title")
                || (deleting ? "영구 삭제 확인" : "작업 확인"),
            confirmText: form.getAttribute("data-confirm-text")
                || (deleting ? "예, 삭제" : "예"),
            cancelText: form.getAttribute("data-cancel-text") || "아니오",
            tone: form.getAttribute("data-confirm-tone")
                || (deleting ? "danger" : "warning")
        }).then(function (confirmed) {
            if (!confirmed) {
                return;
            }
            form.setAttribute("data-confirmed", "true");
            if (typeof form.requestSubmit === "function") {
                form.requestSubmit(submitter || undefined);
            } else {
                form.submit();
            }
        });
    }

    document.addEventListener("submit", function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }
        if (!form.hasAttribute("data-confirm-message") && !form.hasAttribute("data-confirm-delete")) {
            return;
        }
        if (form.getAttribute("data-confirmed") === "true") {
            form.removeAttribute("data-confirmed");
            return;
        }

        event.preventDefault();
        confirmForm(form, event.submitter);
    });

    window.NovelKeepConfirm = {
        open: open
    };
})();
