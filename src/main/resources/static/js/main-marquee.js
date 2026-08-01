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
        var pageMode = root.getAttribute("data-marquee-mode") === "page";

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

        function snapToStep(animated) {
            if (step <= 0 || loopWidth <= 0) {
                return;
            }
            normalizeOffset();
            var index = Math.round(offset / step);
            var target = index * step;

            // 끝 → 처음은 복제 구간(loopWidth)으로 전진한 뒤 조용히 0으로 맞춤 (되감기 점프 방지)
            if (target >= loopWidth) {
                offset = loopWidth;
                applyOffset(!!animated);
                window.setTimeout(function () {
                    if (dragging) {
                        return;
                    }
                    offset = 0;
                    applyOffset(false);
                }, animated ? 520 : 0);
                return;
            }

            if (target < 0) {
                target = 0;
            }
            offset = target;
            applyOffset(!!animated);
        }

        function wrapLoopSilently() {
            if (loopWidth <= 0) {
                return;
            }
            if (offset >= loopWidth) {
                offset -= loopWidth;
                applyOffset(false);
            } else if (offset < 0) {
                offset += loopWidth;
                applyOffset(false);
            }
        }

        function refreshHoverState() {
            root.classList.add("is-drag-settling");
            track.style.pointerEvents = "none";
            window.requestAnimationFrame(function () {
                track.style.pointerEvents = "";
                root.classList.remove("is-drag-settling");
            });
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

            var gap = trackGap(track);
            var slideWidth = pageMode ? viewport.clientWidth : 0;
            originals.forEach(function (node) {
                if (pageMode && slideWidth > 0) {
                    node.style.flex = "0 0 " + slideWidth + "px";
                    node.style.width = slideWidth + "px";
                    node.style.maxWidth = slideWidth + "px";
                }
                disableNativeDrag(node);
                var clone = node.cloneNode(true);
                clone.setAttribute("data-marquee-clone", "true");
                clone.setAttribute("aria-hidden", "true");
                clone.tabIndex = -1;
                clone.removeAttribute("data-bound-card-click");
                clone.querySelectorAll("[data-bound-tag-row]").forEach(function (el) {
                    el.removeAttribute("data-bound-tag-row");
                });
                clone.querySelectorAll("a, button, input, textarea, select").forEach(function (el) {
                    el.setAttribute("tabindex", "-1");
                });
                disableNativeDrag(clone);
                track.appendChild(clone);
            });

            if (pageMode && slideWidth > 0) {
                step = slideWidth + gap;
                loopWidth = step * originals.length;
            } else {
                step = originals.length > 0
                    ? originals[0].getBoundingClientRect().width + gap
                    : 0;
                loopWidth = originals.reduce(function (sum, node) {
                    return sum + node.getBoundingClientRect().width;
                }, 0) + gap * originals.length;
            }

            normalizeOffset();
            applyOffset(false);
            if (typeof window.novelkeepBindNovelCards === "function") {
                window.novelkeepBindNovelCards();
            }
        }

        function advance() {
            if (pointerActive || dragging || prefersReducedMotion() || step <= 0 || loopWidth <= 0) {
                return;
            }
            // 카드 단위로만 맞추고, 끝은 복제본으로 자연스럽게 이어진다
            var nearest = Math.round(offset / step) * step;
            if (Math.abs(offset - nearest) > 0.5) {
                if (nearest >= loopWidth) {
                    offset = 0;
                } else if (nearest < 0) {
                    offset = 0;
                } else {
                    offset = nearest;
                }
                applyOffset(false);
            }

            offset += step;
            applyOffset(true);
            if (offset >= loopWidth) {
                window.setTimeout(function () {
                    if (pointerActive || dragging) {
                        return;
                    }
                    wrapLoopSilently();
                }, 520);
            }
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

        function blurMarqueeFocus() {
            var active = document.activeElement;
            if (active && root.contains(active) && typeof active.blur === "function") {
                active.blur();
            }
        }

        function beginDrag(event) {
            dragging = true;
            moved = true;
            lastX = event.clientX;
            pauseAuto();
            clearSelection();
            blurMarqueeFocus();
            track.classList.remove("is-animating");
            root.classList.add("is-dragging");
            root.classList.remove("is-drag-settling");
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
            var wasDragging = dragging;
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
            if (wasDragging) {
                blurMarqueeFocus();
                snapToStep(true);
                refreshHoverState();
            }
            pauseAuto(intervalMs);
        }

        function isInteractiveTarget(target) {
            if (!target || !target.closest) {
                return false;
            }
            var el = target.closest("button, a, input, select, textarea, label, [data-tag-more], form[data-novel-action]");
            if (!el) {
                return false;
            }
            // 슬라이드 루트 자체가 <a>인 벨트(펀딩·신작·완결)는 드래그 허용
            if (el.parentElement === track) {
                return false;
            }
            return true;
        }

        function pointerDown(event) {
            if (event.pointerType === "mouse" && event.button !== 0) {
                return;
            }
            if (isInteractiveTarget(event.target)) {
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
            root.classList.remove("is-drag-settling");
            track.style.pointerEvents = "";
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
