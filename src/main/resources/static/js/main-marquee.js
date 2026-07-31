(function () {
    var DRAG_THRESHOLD = 8;

    function prefersReducedMotion() {
        return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    }

    function trackGap(track) {
        var styles = window.getComputedStyle(track);
        return Number.parseFloat(styles.columnGap || styles.gap || "0") || 0;
    }

    function bindMarquee(root) {
        var viewport = root.querySelector("[data-marquee-viewport]");
        var track = root.querySelector("[data-marquee-track]");
        if (!viewport || !track || track.children.length === 0) {
            return;
        }

        var intervalMs = Number(root.getAttribute("data-marquee-interval") || "5000");
        if (!Number.isFinite(intervalMs) || intervalMs < 1000) {
            intervalMs = 5000;
        }

        var offset = 0;
        var loopWidth = 0;
        var step = 0;
        var originals = [];
        var pointerActive = false;
        var dragging = false;
        var moved = false;
        var startX = 0;
        var lastX = 0;
        var activePointerId = null;
        var timerId = 0;
        var resumeId = 0;

        function clearTimers() {
            if (timerId) {
                window.clearInterval(timerId);
                timerId = 0;
            }
            if (resumeId) {
                window.clearTimeout(resumeId);
                resumeId = 0;
            }
        }

        function applyOffset(animated) {
            track.classList.toggle("is-animating", !!animated);
            track.style.transform = "translate3d(" + (-offset) + "px, 0, 0)";
        }

        function normalizeOffset() {
            if (loopWidth <= 0) {
                return;
            }
            while (offset < 0) {
                offset += loopWidth;
            }
            while (offset >= loopWidth) {
                offset -= loopWidth;
            }
        }

        function disableNativeDrag(node) {
            if (node && node.setAttribute) {
                node.setAttribute("draggable", "false");
            }
            if (node && node.querySelectorAll) {
                node.querySelectorAll("a, img").forEach(function (el) {
                    el.setAttribute("draggable", "false");
                });
            }
        }

        function clearSelection() {
            var selection = window.getSelection && window.getSelection();
            if (selection && selection.removeAllRanges) {
                selection.removeAllRanges();
            }
        }

        function measure() {
            originals = Array.prototype.slice.call(track.children).filter(function (node) {
                return !node.hasAttribute("data-marquee-clone");
            });
            track.querySelectorAll("[data-marquee-clone]").forEach(function (node) {
                node.remove();
            });
            originals.forEach(function (node) {
                disableNativeDrag(node);
                var clone = node.cloneNode(true);
                clone.setAttribute("data-marquee-clone", "true");
                clone.setAttribute("aria-hidden", "true");
                clone.tabIndex = -1;
                disableNativeDrag(clone);
                track.appendChild(clone);
            });

            var gap = trackGap(track);
            step = originals.length > 0
                ? originals[0].getBoundingClientRect().width + gap
                : 0;
            loopWidth = originals.reduce(function (sum, node) {
                return sum + node.getBoundingClientRect().width;
            }, 0) + gap * originals.length;

            normalizeOffset();
            applyOffset(false);
        }

        function advance() {
            if (pointerActive || dragging || prefersReducedMotion() || step <= 0) {
                return;
            }
            offset += step;
            if (offset >= loopWidth) {
                applyOffset(true);
                window.setTimeout(function () {
                    if (pointerActive || dragging) {
                        return;
                    }
                    offset -= loopWidth;
                    applyOffset(false);
                }, 520);
                return;
            }
            applyOffset(true);
        }

        function startAuto() {
            clearTimers();
            if (prefersReducedMotion()) {
                return;
            }
            timerId = window.setInterval(advance, intervalMs);
        }

        function pauseAuto(resumeDelay) {
            clearTimers();
            if (typeof resumeDelay === "number") {
                resumeId = window.setTimeout(startAuto, resumeDelay);
            }
        }

        function beginDrag(event) {
            dragging = true;
            moved = true;
            lastX = event.clientX;
            pauseAuto();
            clearSelection();
            track.classList.remove("is-animating");
            root.classList.add("is-dragging");
            if (event.cancelable) {
                event.preventDefault();
            }
            try {
                viewport.setPointerCapture(event.pointerId);
            } catch (ignored) {
                // ignore capture failures on unsupported targets
            }
        }

        function endPointer(event) {
            if (!pointerActive || (activePointerId != null && event.pointerId !== activePointerId)) {
                return;
            }
            pointerActive = false;
            activePointerId = null;
            if (dragging) {
                dragging = false;
                root.classList.remove("is-dragging");
                clearSelection();
                if (viewport.hasPointerCapture && viewport.hasPointerCapture(event.pointerId)) {
                    viewport.releasePointerCapture(event.pointerId);
                }
            }
            pauseAuto(intervalMs);
        }

        function pointerDown(event) {
            if (event.pointerType === "mouse" && event.button !== 0) {
                return;
            }
            pointerActive = true;
            dragging = false;
            moved = false;
            activePointerId = event.pointerId;
            startX = event.clientX;
            lastX = event.clientX;
            pauseAuto();
        }

        function pointerMove(event) {
            if (!pointerActive || event.pointerId !== activePointerId) {
                return;
            }
            if (!dragging) {
                if (Math.abs(event.clientX - startX) < DRAG_THRESHOLD) {
                    return;
                }
                beginDrag(event);
            }
            var delta = event.clientX - lastX;
            lastX = event.clientX;
            if (delta === 0) {
                return;
            }
            offset -= delta;
            normalizeOffset();
            applyOffset(false);
            if (event.cancelable) {
                event.preventDefault();
            }
        }

        function suppressClickAfterDrag(event) {
            if (moved) {
                event.preventDefault();
                event.stopPropagation();
                moved = false;
            }
        }

        root.addEventListener("mouseenter", function () {
            if (!pointerActive && !dragging) {
                pauseAuto();
            }
        });
        root.addEventListener("mouseleave", function () {
            if (!pointerActive && !dragging) {
                startAuto();
            }
        });

        viewport.addEventListener("pointerdown", pointerDown);
        viewport.addEventListener("pointermove", pointerMove, { passive: false });
        viewport.addEventListener("pointerup", endPointer);
        viewport.addEventListener("pointercancel", endPointer);
        viewport.addEventListener("lostpointercapture", endPointer);
        viewport.addEventListener("dragstart", function (event) {
            event.preventDefault();
        });
        viewport.addEventListener("click", suppressClickAfterDrag, true);

        window.addEventListener("resize", function () {
            measure();
            startAuto();
        });

        measure();
        startAuto();
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("[data-marquee]").forEach(bindMarquee);
    });
})();
