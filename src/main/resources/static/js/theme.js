(() => {
    const storageKey = "novelkeep-theme";
    const lightTheme = "light";
    const darkTheme = "dark";

    function getSavedTheme() {
        try {
            return localStorage.getItem(storageKey);
        } catch {
            return null;
        }
    }

    function saveTheme(theme) {
        try {
            localStorage.setItem(storageKey, theme);
        } catch {
            // 저장이 차단된 환경에서도 현재 화면의 테마 전환은 유지한다.
        }
    }

    function applyTheme(theme) {
        const isDark = theme === darkTheme;
        document.documentElement.setAttribute("data-bs-theme", isDark ? darkTheme : lightTheme);

        document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
            const label = isDark ? "라이트 모드" : "다크 모드";
            button.setAttribute("aria-label", `${label}로 전환`);
            button.setAttribute("aria-pressed", String(isDark));

            const labelElement = button.querySelector("[data-theme-label]");
            if (labelElement) {
                labelElement.textContent = label;
            }
        });
    }

    const savedTheme = getSavedTheme();
    const initialTheme = savedTheme === lightTheme ? lightTheme : darkTheme;
    applyTheme(initialTheme);

    window.addEventListener("DOMContentLoaded", () => {
        applyTheme(initialTheme);

        document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
            button.addEventListener("click", () => {
                const currentTheme = document.documentElement.getAttribute("data-bs-theme");
                const nextTheme = currentTheme === darkTheme ? lightTheme : darkTheme;
                applyTheme(nextTheme);
                saveTheme(nextTheme);
            });
        });
    });
})();
