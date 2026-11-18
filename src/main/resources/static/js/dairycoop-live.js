/**
 * DairyCoop Live Updates — dairycoop-live.js
 * ─────────────────────────────────────────────────────────────────────────────
 * Single universal script included in every portal via the shared nav fragments.
 * Polls /api/live/poll every 5 seconds, detects status changes, shows toasts,
 * updates badge counts, and reloads the page when an action becomes available.
 *
 * Works for all roles: ACCOUNTS_OFFICER, SALES, ADMIN, MEMBER, Company portal.
 * Zero dependencies — plain ES5 compatible JavaScript.
 * ─────────────────────────────────────────────────────────────────────────────
 */
(function () {
    'use strict';

    var POLL_INTERVAL = 5000;   // ms between polls
    var RELOAD_DELAY  = 2000;   // ms after toast before reloading

    // Status transitions that mean the UI needs fresh action buttons
    var ACTIONABLE = ['APPROVED', 'PO_APPROVED', 'PO_SUBMITTED',
                      'INVOICED', 'PAID', 'REJECTED', 'DELIVERED'];

    // Human-readable labels
    var LABELS = {
        PENDING:      'Pending Review',
        APPROVED:     'Approved — Submit PO',
        REJECTED:     'Rejected',
        PO_CREATED:   'PO Drafted',
        PO_SUBMITTED: 'PO Submitted — Awaiting Approval',
        PO_APPROVED:  'PO Approved — Confirm Delivery',
        DELIVERED:    'Delivered',
        INVOICED:     'Invoice Ready — Pay Now',
        PAID:         'Payment Confirmed ✓'
    };

    // ── State ─────────────────────────────────────────────────────────────────
    var knownInquiryStatus = {};   // id → last known status
    var knownBadges        = null; // last known badge counts
    var reloadScheduled    = false;

    // ── Toast ─────────────────────────────────────────────────────────────────
    function toast(msg, type) {
        var wrap = document.getElementById('_dc_toasts');
        if (!wrap) {
            wrap = document.createElement('div');
            wrap.id = '_dc_toasts';
            wrap.style.cssText =
                'position:fixed;top:20px;right:20px;z-index:99999;' +
                'display:flex;flex-direction:column;gap:10px;' +
                'pointer-events:none;font-family:system-ui,sans-serif;' +
                'max-width:340px;';
            document.body.appendChild(wrap);
        }

        var colors = {
            success: { bg: '#0f2747', border: '#00c16a', icon: '✅' },
            warning: { bg: '#431407', border: '#fb923c', icon: '⚠️' },
            info:    { bg: '#0f2747', border: '#60a5fa', icon: '🔔' },
            error:   { bg: '#450a0a', border: '#f87171', icon: '❌' }
        };
        var c = colors[type] || colors.info;

        var el = document.createElement('div');
        el.style.cssText =
            'pointer-events:auto;padding:13px 16px;border-radius:12px;' +
            'font-size:13px;font-weight:500;display:flex;align-items:flex-start;gap:10px;' +
            'box-shadow:0 8px 24px rgba(0,0,0,.22);' +
            'background:' + c.bg + ';color:#fff;' +
            'border-left:4px solid ' + c.border + ';' +
            'animation:dcSlideIn .25s ease;';
        el.innerHTML =
            '<span style="flex-shrink:0;font-size:16px">' + c.icon + '</span>' +
            '<span>' + msg + '</span>';

        // Inject animation if not already present
        if (!document.getElementById('_dc_toast_css')) {
            var style = document.createElement('style');
            style.id = '_dc_toast_css';
            style.textContent =
                '@keyframes dcSlideIn{from{opacity:0;transform:translateX(24px)}to{opacity:1;transform:none}}';
            document.head.appendChild(style);
        }

        wrap.appendChild(el);
        setTimeout(function () {
            el.style.transition = 'opacity .4s,transform .4s';
            el.style.opacity    = '0';
            el.style.transform  = 'translateX(20px)';
            setTimeout(function () { el.parentNode && el.parentNode.removeChild(el); }, 400);
        }, 8000);
    }

    // ── Badge updater ─────────────────────────────────────────────────────────
    // Updates any element with data-live-badge="key" on the page
    function updateBadges(newBadges) {
        if (!newBadges) return;

        Object.keys(newBadges).forEach(function (key) {
            var count    = newBadges[key];
            var prevCount = knownBadges ? (knownBadges[key] || 0) : null;

            // Update DOM elements
            document.querySelectorAll('[data-live-badge="' + key + '"]').forEach(function (el) {
                el.textContent   = count;
                el.style.display = count > 0 ? '' : 'none';
            });

            // Skip change detection on first poll (seeding state)
            if (prevCount === null) return;
            if (count === prevCount) return;

            // ── unreadReports increased → new report arrived ──────────────────
            if (key === 'unreadReports' && count > prevCount) {
                var diff = count - prevCount;
                toast(diff + ' new finance report' + (diff > 1 ? 's' : '') +
                      ' submitted by the Accounts Officer', 'info');
                // On the reports page: show a banner instead of force-reloading
                // so we don't interrupt the admin if they're mid-review
                if (window.location.pathname.indexOf('/admin/reports') >= 0) {
                    showNewReportBanner(diff);
                }
            }

            // ── pendingLoans increased → new loan application ─────────────────
            if (key === 'pendingLoans' && count > prevCount) {
                var diff2 = count - prevCount;
                toast(diff2 + ' new loan application' + (diff2 > 1 ? 's' : '') + ' waiting for review', 'info');
            }
        });
    }

    // ── Status badge updater ──────────────────────────────────────────────────
    function updateStatusBadge(id, status) {
        var el = document.querySelector('[data-status-badge="' + id + '"]');
        if (!el) return;
        el.textContent = LABELS[status] || status;
        var colors = {
            PAID:        'background:#dcfce7;color:#166534',
            INVOICED:    'background:#dbeafe;color:#1e40af',
            REJECTED:    'background:#fee2e2;color:#991b1b',
            PO_APPROVED: 'background:#fef9c3;color:#713f12',
            APPROVED:    'background:#f0fdf4;color:#166534',
            PO_SUBMITTED:'background:#f5f3ff;color:#5b21b6'
        };
        el.style.cssText = 'display:inline-block;padding:4px 10px;border-radius:8px;' +
            'font-size:12px;font-weight:600;' + (colors[status] || 'background:#f1f5f9;color:#334155');
    }

    // ── New report banner ─────────────────────────────────────────────────────
    // Shows a non-disruptive banner at the top of the page instead of
    // force-reloading. The admin clicks "View now" when ready.
    function showNewReportBanner(count) {
        if (document.getElementById('_dc_report_banner')) return; // already showing
        var banner = document.createElement('div');
        banner.id = '_dc_report_banner';
        banner.style.cssText =
            'position:fixed;top:0;left:0;right:0;z-index:99998;' +
            'background:#1e3a5f;color:#fff;padding:10px 20px;' +
            'display:flex;align-items:center;justify-content:space-between;gap:12px;' +
            'font-family:system-ui,sans-serif;font-size:13px;font-weight:500;' +
            'box-shadow:0 2px 12px rgba(0,0,0,.3);';
        banner.innerHTML =
            '<span>🔔 ' + count + ' new finance report' + (count > 1 ? 's' : '') +
            ' submitted by the Accounts Officer</span>' +
            '<div style="display:flex;gap:8px;flex-shrink:0">' +
              '<button onclick="window.location.reload()" ' +
                'style="background:#00c16a;color:#fff;border:none;padding:6px 14px;' +
                'border-radius:6px;font-size:12px;font-weight:700;cursor:pointer">' +
                'View now</button>' +
              '<button onclick="this.parentElement.parentElement.remove()" ' +
                'style="background:rgba(255,255,255,.15);color:#fff;border:none;' +
                'padding:6px 12px;border-radius:6px;font-size:12px;cursor:pointer">' +
                'Dismiss</button>' +
            '</div>';
        document.body.insertBefore(banner, document.body.firstChild);
    }

    // ── Reload (once) ─────────────────────────────────────────────────────────
    function scheduleReload() {
        if (reloadScheduled) return;
        reloadScheduled = true;
        setTimeout(function () { window.location.reload(); }, RELOAD_DELAY);
    }

    // ── Process poll response ─────────────────────────────────────────────────
    function process(data) {
        // ── Badge counts ──────────────────────────────────────────────────────
        if (data.badges) {
            updateBadges(data.badges);
            knownBadges = data.badges;
        }

        // ── Inquiry status changes ────────────────────────────────────────────
        var inquiries = data.inquiries || [];
        inquiries.forEach(function (inq) {
            var prev = knownInquiryStatus[inq.id];

            if (prev === undefined) {
                // First poll — seed state, no toast
                knownInquiryStatus[inq.id] = inq.status;
                return;
            }

            if (prev === inq.status) return; // no change

            // Status changed!
            knownInquiryStatus[inq.id] = inq.status;

            // Determine toast type
            var type = 'info';
            if (inq.status === 'REJECTED')                type = 'error';
            else if (inq.status === 'PAID')               type = 'success';
            else if (inq.status === 'INVOICED')           type = 'warning';

            var label = inq.ref
                ? inq.ref + ': ' + (LABELS[inq.status] || inq.status)
                : (LABELS[inq.status] || inq.status);
            toast(label, type);

            // Update the status badge in the DOM if it exists
            updateStatusBadge(inq.id, inq.status);

            // Reload for transitions that change what actions are available
            if (ACTIONABLE.indexOf(inq.status) >= 0) {
                scheduleReload();
            }
        });
    }

    // ── Poll ──────────────────────────────────────────────────────────────────
    function poll() {
        fetch('/api/live/poll', {
            credentials: 'same-origin',
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
        .then(function (r) {
            if (r.status === 401 || r.status === 403) return null; // not logged in
            return r.json();
        })
        .then(function (data) {
            if (data) process(data);
        })
        .catch(function () {
            /* silent — network hiccup, will retry next interval */
        });
    }

    // ── Boot ──────────────────────────────────────────────────────────────────
    window.addEventListener('load', function () {
        poll();                              // immediate seed poll
        setInterval(poll, POLL_INTERVAL);   // then every 5 seconds
    });

    // Expose for manual triggering and badge pre-seeding from templates
    window.dcLive = {
        poll: poll,
        toast: toast,
        // Call this from a template to pre-seed a badge value from server-rendered data.
        // Prevents the first poll from treating an existing value as "no change".
        seedBadge: function (key, value) {
            if (!knownBadges) knownBadges = {};
            knownBadges[key] = value;
        }
    };

})();