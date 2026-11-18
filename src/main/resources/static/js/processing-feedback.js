/**
 * processing-feedback.js
 *
 * Gives every form submission a deliberate "processing" feel instead of an
 * instant jump to the next page. Drop a single <script> tag in any page and
 * it applies automatically to every <form> on that page — no per-button
 * wiring needed.
 *
 * Behaviour:
 *   - POST forms (real actions: approve, confirm, generate, sign, send, pay...)
 *     show a spinner immediately, then hold for ~2 seconds before the action
 *     actually goes through, so it reads as deliberate rather than instant.
 *   - GET forms (filters, date pickers, search) show a quick spinner but are
 *     NOT artificially delayed — those should stay snappy.
 *   - Plays nicely with existing onsubmit="return confirm(...)" handlers:
 *     if the user cancels that dialog, this script does nothing.
 *   - Side benefit: the button is disabled the instant it's clicked, which
 *     also prevents accidental double-submission from a fast double-click.
 *
 * To adjust the artificial delay, change PROCESSING_DELAY_MS below.
 */
(function () {
    var PROCESSING_DELAY_MS = 2000;

    var style = document.createElement('style');
    style.textContent =
        '.pf-spinner{display:inline-block;width:13px;height:13px;' +
        'border:2px solid currentColor;border-top-color:transparent;' +
        'border-radius:50%;opacity:.85;vertical-align:-2px;margin-right:6px;' +
        'animation:pf-spin .6s linear infinite}' +
        '@keyframes pf-spin{to{transform:rotate(360deg)}}' +
        'button[disabled].pf-processing{cursor:wait;opacity:.85}';
    document.head.appendChild(style);

    document.addEventListener('submit', function (e) {
        if (e.defaultPrevented) return; // a confirm() dialog or other handler already cancelled this

        var form = e.target;
        if (!(form instanceof HTMLFormElement)) return;

        var btn = form.querySelector('button[type="submit"]:not([disabled])');
        if (!btn) return;

        // Forms with data-ajax-action are handled entirely by ajax-actions.js
        // (which has its own spinner + fetch). Skip them here to avoid conflict.
        if (form.dataset.ajaxAction !== undefined) return;

        var method = (form.getAttribute('method') || 'get').toLowerCase();
        var label = btn.dataset.processingLabel || 'Processing...';

        btn.disabled = true;
        btn.classList.add('pf-processing');
        btn.dataset.originalHtml = btn.innerHTML;

        if (method !== 'post') {
            // Filter/search forms: instant spinner, no artificial delay.
            btn.innerHTML = '<span class="pf-spinner"></span>' + btn.textContent.trim();
            return;
        }

        // Real action: hold the spinner for a moment before actually submitting.
        e.preventDefault();
        btn.innerHTML = '<span class="pf-spinner"></span>' + label;
        setTimeout(function () {
            form.submit();
        }, PROCESSING_DELAY_MS);
    }, false);
})();