/**
 * ajax-actions.js
 *
 * Exposes one global function:  dcAjax(btn, options)
 *
 * Call it from an onclick on a submit button — no event listener tricks needed:
 *
 *   <button type="button"
 *           onclick="dcAjax(this, {action:'disable_panel', loadingText:'Approving...'})">
 *       Approve
 *   </button>
 *
 * options.action values:
 *   disable_panel  – replaces closest [data-action-panel] with a success/fail badge
 *   remove_row     – fades out the closest <tr>
 *   reload_section – reloads the page 1s after success
 *   none           – toast only
 */
window.dcAjax = function (btn, opts) {
    var form = btn.closest('form') || btn.form;
    if (!form && opts && opts.formId) form = document.getElementById(opts.formId);
    if (!form) { console.error('dcAjax: no form found for button', btn); return; }

    var action = (opts && opts.action) || 'none';
    var loadingText = (opts && opts.loadingText) || 'Processing...';

    /* spinner */
    var origHtml = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML =
        '<span style="display:inline-block;width:13px;height:13px;border:2px solid currentColor;' +
        'border-top-color:transparent;border-radius:50%;vertical-align:-2px;margin-right:6px;' +
        'animation:_dc_spin .6s linear infinite"></span>' + loadingText;

    if (!document.getElementById('_dc_spin_kf')) {
        var s = document.createElement('style');
        s.id  = '_dc_spin_kf';
        s.textContent = '@keyframes _dc_spin{to{transform:rotate(360deg)}}';
        document.head.appendChild(s);
    }

    /* collect form fields (including _csrf hidden input) */
    var body = new URLSearchParams(new FormData(form)).toString();

    fetch(form.action, {
        method:      'POST',
        credentials: 'same-origin',
        headers: {
            'Content-Type':     'application/x-www-form-urlencoded',
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: body
    })
    .then(function (r) {
        return r.text().then(function (t) { return { ok: r.ok, status: r.status, text: t }; });
    })
    .then(function (r) {
        if (!r.ok) {
            btn.disabled = false; btn.innerHTML = origHtml;
            dcToast('Server error ' + r.status + ' — please try again.', false);
            console.error('dcAjax HTTP error', r.status, r.text.substring(0, 300));
            return;
        }
        var json;
        try { json = JSON.parse(r.text); }
        catch (ex) {
            btn.disabled = false; btn.innerHTML = origHtml;
            dcToast('Unexpected response — please try again.', false);
            return;
        }

        if (json.success) {
            dcToast(json.message || 'Done.', true);
            dcApplyAction(form, btn, action, json);
            dcUpdateBadges(json.badgeUpdates);
        } else {
            btn.disabled = false; btn.innerHTML = origHtml;
            dcToast(json.message || 'Action failed.', false);
        }
    })
    .catch(function (err) {
        btn.disabled = false; btn.innerHTML = origHtml;
        dcToast('Network error: ' + err.message, false);
        console.error('dcAjax fetch error', err);
    });
};

window.dcToast = function (msg, ok) {
    var wrap = document.getElementById('_dc_toasts');
    if (!wrap) {
        wrap = document.createElement('div');
        wrap.id = '_dc_toasts';
        wrap.style.cssText =
            'position:fixed;bottom:24px;right:24px;z-index:99999;' +
            'display:flex;flex-direction:column;gap:10px;' +
            'pointer-events:none;font-family:system-ui,sans-serif';
        document.body.appendChild(wrap);
    }
    var el = document.createElement('div');
    el.style.cssText =
        'pointer-events:auto;max-width:380px;padding:14px 18px;border-radius:12px;' +
        'font-size:13.5px;font-weight:500;display:flex;align-items:flex-start;gap:10px;' +
        'box-shadow:0 8px 32px rgba(0,0,0,.18);' +
        (ok ? 'background:#0f2747;color:#fff;border-left:4px solid #00c16a'
            : 'background:#fff1f2;color:#9f1239;border-left:4px solid #f43f5e');
    var icon = document.createElement('span');
    icon.textContent = ok ? '✓' : '✕';
    icon.style.cssText =
        'flex-shrink:0;width:20px;height:20px;border-radius:50%;' +
        'display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700;' +
        (ok ? 'background:#00c16a;color:#fff' : 'background:#f43f5e;color:#fff');
    var txt = document.createElement('span');
    txt.textContent = msg;
    el.appendChild(icon);
    el.appendChild(txt);
    wrap.appendChild(el);
    setTimeout(function () {
        el.style.transition = 'opacity .3s,transform .3s';
        el.style.opacity    = '0';
        el.style.transform  = 'translateX(16px)';
        setTimeout(function () { if (el.parentNode) el.parentNode.removeChild(el); }, 320);
    }, 5000);
};

window.dcApplyAction = function (form, btn, action, json) {
    if (action === 'remove_row') {
        var row = form.closest('tr');
        if (row) {
            row.style.transition = 'opacity .4s,transform .4s';
            row.style.opacity    = '0';
            row.style.transform  = 'translateX(16px)';
            setTimeout(function () { if (row.parentNode) row.parentNode.removeChild(row); }, 420);
        }

    } else if (action === 'disable_panel') {
        var panel = form.closest('[data-action-panel]');
        if (panel) {
            var status = json.newStatus || 'PROCESSED';
            var isOk   = (status === 'APPROVED');
            panel.style.transition = 'opacity .3s';
            panel.style.opacity    = '0';
            setTimeout(function () {
                panel.innerHTML =
                    '<div style="display:inline-flex;align-items:center;gap:8px;padding:12px 20px;' +
                    'border-radius:10px;font-size:14px;font-weight:600;margin-top:8px;' +
                    (isOk
                        ? 'background:#dcfce7;color:#166534;border:1.5px solid #bbf7d0">'
                        : 'background:#fee2e2;color:#991b1b;border:1.5px solid #fecaca">') +
                    (isOk ? '✓ ' : '✕ ') + status + '</div>';
                panel.style.opacity = '1';
            }, 320);
        }

    } else if (action === 'reload_section') {
        setTimeout(function () { window.location.reload(); }, 1200);
    }
    /* 'none' — toast only, nothing else */
};

window.dcUpdateBadges = function (updates) {
    if (!updates) return;
    Object.keys(updates).forEach(function (key) {
        var el = document.getElementById('nav-badge-' + key);
        if (!el) return;
        var n = Number(updates[key]);
        el.textContent = n;
        el.style.display = n > 0 ? '' : 'none';
    });
};