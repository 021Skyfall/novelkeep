window.addEventListener("DOMContentLoaded", () => {
    const partMode = document.querySelector("[data-part-mode]");
    const firstPartField = document.querySelector("[data-first-part-field]");

    if (!partMode || !firstPartField) {
        return;
    }

    const updateFirstPartField = () => {
        firstPartField.hidden = partMode.value !== "MULTI";
    };

    partMode.addEventListener("change", updateFirstPartField);
    updateFirstPartField();
});
