(function () {
    var TYPE_META = {
        success: { label: "완료", duration: 4500 },
        error: { label: "오류", duration: 6500 },
        warning: { label: "안내", duration: 6000 },
        info: { label: "알림", duration: 5000 }
    };

    function normalizeType(type) {
        if (type === "danger") {
            return "error";
        }
        return TYPE_META[type] ? type : "info";
    }

    function getStack() {
        var stack = document.querySelector("[data-notification-stack]");
        if (stack) {
            return stack;
        }

        stack = document.createElement("div");
        stack.className = "nk-notification-stack";
        stack.setAttribute("data-notification-stack", "");
        stack.setAttribute("aria-live", "polite");
        stack.setAttribute("aria-relevant", "additions");
        document.body.appendChild(stack);
        return stack;
    }

    function remove(notification) {
        if (!notification || notification.classList.contains("is-leaving")) {
            return;
        }
        notification.classList.add("is-leaving");
        window.setTimeout(function () {
            notification.remove();
        }, 180);
    }

    function push(message, type, options) {
        var text = String(message || "").trim();
        if (!text) {
            return null;
        }

        var normalizedType = normalizeType(type);
        var meta = TYPE_META[normalizedType];
        var settings = options || {};
        var notification = document.createElement("div");
        notification.className = "nk-push-notification is-" + normalizedType;
        notification.setAttribute("role", normalizedType === "error" ? "alert" : "status");

        var typeBadge = document.createElement("span");
        typeBadge.className = "nk-notification-type";
        typeBadge.textContent = settings.label || meta.label;

        var messageBox = document.createElement("span");
        messageBox.className = "nk-notification-message";
        messageBox.textContent = text;

        var closeButton = document.createElement("button");
        closeButton.className = "nk-notification-close";
        closeButton.type = "button";
        closeButton.setAttribute("aria-label", "알림 닫기");
        closeButton.textContent = "×";
        closeButton.addEventListener("click", function () {
            remove(notification);
        });

        notification.appendChild(typeBadge);
        notification.appendChild(messageBox);
        notification.appendChild(closeButton);
        getStack().appendChild(notification);

        var duration = Number(settings.duration);
        if (!Number.isFinite(duration)) {
            duration = meta.duration;
        }
        if (duration > 0) {
            window.setTimeout(function () {
                remove(notification);
            }, duration);
        }
        return notification;
    }

    function typeFromElement(element) {
        var explicit = element.getAttribute("data-notification-type");
        if (explicit) {
            return explicit;
        }
        if (element.classList.contains("alert-success")) {
            return "success";
        }
        if (element.classList.contains("alert-danger")) {
            return "error";
        }
        if (element.classList.contains("alert-warning")) {
            return "warning";
        }
        return "info";
    }

    function collectServerNotifications() {
        document.querySelectorAll("[data-push-notification]").forEach(function (element) {
            push(element.textContent, typeFromElement(element), {
                label: element.getAttribute("data-notification-label") || undefined
            });
            element.remove();
        });
    }

    window.NovelKeepNotification = {
        push: push,
        success: function (message, options) {
            return push(message, "success", options);
        },
        error: function (message, options) {
            return push(message, "error", options);
        },
        warning: function (message, options) {
            return push(message, "warning", options);
        },
        info: function (message, options) {
            return push(message, "info", options);
        }
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", collectServerNotifications);
    } else {
        collectServerNotifications();
    }
})();
