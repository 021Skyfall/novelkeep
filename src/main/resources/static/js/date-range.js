(function () {
    function bindRange(input, fromHidden, toHidden, onChange) {
        if (!input || !window.flatpickr) {
            return null;
        }
        var initial = [];
        if (fromHidden && fromHidden.value) {
            initial.push(fromHidden.value);
        }
        if (toHidden && toHidden.value) {
            initial.push(toHidden.value);
        }
        return window.flatpickr(input, {
            mode: 'range',
            locale: window.flatpickr.l10ns && window.flatpickr.l10ns.ko
                ? window.flatpickr.l10ns.ko
                : 'default',
            dateFormat: 'Y-m-d',
            allowInput: false,
            defaultDate: initial.length ? initial : null,
            onChange: function (selectedDates, dateStr) {
                if (!fromHidden || !toHidden) {
                    return;
                }
                if (selectedDates.length === 0) {
                    fromHidden.value = '';
                    toHidden.value = '';
                } else if (selectedDates.length === 1) {
                    fromHidden.value = window.flatpickr.formatDate(selectedDates[0], 'Y-m-d');
                    toHidden.value = '';
                } else {
                    fromHidden.value = window.flatpickr.formatDate(selectedDates[0], 'Y-m-d');
                    toHidden.value = window.flatpickr.formatDate(selectedDates[1], 'Y-m-d');
                }
                if (typeof onChange === 'function' && selectedDates.length !== 1) {
                    onChange();
                }
            },
            onClose: function (selectedDates) {
                if (typeof onChange === 'function' && selectedDates.length === 1) {
                    onChange();
                }
            }
        });
    }

    window.NovelKeepDateRange = {
        bind: bindRange
    };
})();
