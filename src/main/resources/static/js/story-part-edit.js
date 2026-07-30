(function () {
    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("[data-part-edit-form]").forEach(function (form) {
            var title = form.querySelector("[data-part-edit-title]");
            var status = form.querySelector("[data-part-edit-status]");
            var button = form.querySelector("[data-part-edit-toggle]");

            if (!title || !status || !button) {
                return;
            }

            button.addEventListener("click", function (event) {
                if (form.classList.contains("is-editing")) {
                    return;
                }

                event.preventDefault();
                form.classList.add("is-editing");
                title.readOnly = false;
                title.classList.remove("bg-body-secondary");
                status.disabled = false;
                button.type = "submit";
                button.textContent = "저장";
                button.classList.remove("btn-outline-dark");
                button.classList.add("btn-dark");
                button.setAttribute("aria-expanded", "true");
                title.focus();
                title.select();
            });
        });
    });
})();
