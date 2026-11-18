package com.dairycoopms.dairycoopms.service;

import com.dairycoopms.dairycoopms.model.StaffSalary;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.coop.name:Dairy Cooperative Management System}")
    private String coopName;

    @Value("${app.coop.email:noreply@dairycoop.zm}")
    private String coopEmail;

    @Value("${app.coop.phone:+260 XXX XXX XXX}")
    private String coopPhone;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a rich HTML salary payment notification email to the staff member.
     * Runs asynchronously so it never blocks the HTTP response.
     */
    @Async
    public void sendSalaryPaymentNotification(StaffSalary sal, String methodLabel) {
        String recipientEmail = sal.getUser().getEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send salary email — no email for user {}", sal.getUser().getFullName());
            return;
        }

        // ── Month name ─────────────────────────────────────────────────────
        String[] months = {"","January","February","March","April","May","June",
                "July","August","September","October","November","December"};
        String period = months[Math.min(sal.getMonth(), 12)] + " " + sal.getYear();

        // ── Amounts ────────────────────────────────────────────────────────
        BigDecimal deductions = sal.getDeductions() != null ? sal.getDeductions() : BigDecimal.ZERO;
        boolean hasLoan = deductions.compareTo(BigDecimal.ZERO) > 0;
        String paidAt = sal.getPaidAt() != null
                ? sal.getPaidAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                : "—";

        // ── Method icon text ───────────────────────────────────────────────
        String methodIcon = switch (methodLabel) {
            case "Airtel Money"      -> "📱";
            case "MTN Mobile Money"  -> "📲";
            case "Bank Transfer"     -> "🏦";
            default                  -> "💵";
        };

        // ── HTML body ──────────────────────────────────────────────────────
        String html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width,initial-scale=1"/>
          <title>Salary Payment Notification</title>
          <style>
            body{margin:0;padding:0;background:#f0f4f8;font-family:'Segoe UI',Arial,sans-serif}
            .wrap{max-width:600px;margin:32px auto;background:#fff;border-radius:18px;overflow:hidden;box-shadow:0 8px 32px rgba(0,0,0,.10)}
            .header{background:linear-gradient(135deg,#064e3b 0%%,#065f46 60%%,#047857 100%%);padding:40px 36px 32px;text-align:center}
            .header-icon{width:64px;height:64px;background:rgba(255,255,255,.15);border-radius:16px;margin:0 auto 16px;display:flex;align-items:center;justify-content:center;font-size:30px;line-height:64px}
            .header h1{color:#fff;margin:0;font-size:26px;font-weight:800;letter-spacing:-.5px}
            .header p{color:rgba(255,255,255,.8);margin:8px 0 0;font-size:14px}
            .badge{display:inline-block;background:rgba(255,255,255,.2);border:1px solid rgba(255,255,255,.35);color:#fff;font-size:12px;font-weight:700;padding:4px 14px;border-radius:20px;margin-top:14px;text-transform:uppercase;letter-spacing:.07em}
            .body{padding:36px}
            .greeting{font-size:17px;font-weight:700;color:#0f1720;margin-bottom:6px}
            .subtitle{font-size:14px;color:#64748b;line-height:1.6;margin-bottom:28px}
            .section-label{font-size:10px;font-weight:800;text-transform:uppercase;letter-spacing:.12em;color:#94a3b8;margin-bottom:12px}
            .breakdown{background:#f8fafc;border-radius:14px;padding:22px 24px;margin-bottom:22px;border:1px solid #e2e8f0}
            .row{display:flex;justify-content:space-between;align-items:center;padding:9px 0;border-bottom:1px solid #f1f5f9}
            .row:last-child{border-bottom:none}
            .row-label{font-size:13px;color:#64748b;font-weight:500}
            .row-val{font-size:13px;color:#0f1720;font-weight:700;font-family:'Courier New',monospace}
            .row-deduct .row-label{color:#be123c}
            .row-deduct .row-val{color:#be123c}
            .net-row{background:linear-gradient(135deg,#f0fdf4,#dcfce7);border-radius:12px;padding:18px 24px;margin-top:8px;display:flex;justify-content:space-between;align-items:center;border:1.5px solid #86efac}
            .net-label{font-size:12px;font-weight:800;text-transform:uppercase;letter-spacing:.12em;color:#15803d}
            .net-val{font-size:24px;font-weight:900;color:#065f46;font-family:'Courier New',monospace}
            .method-box{background:#eff6ff;border:1.5px solid #bfdbfe;border-radius:12px;padding:16px 20px;margin-bottom:22px;display:flex;align-items:center;gap:14px}
            .method-icon{font-size:26px}
            .method-label{font-size:13px;font-weight:700;color:#1e3a8a}
            .method-ref{font-size:12px;color:#3b82f6;font-family:'Courier New',monospace;margin-top:3px}
            .loan-notice{background:#fff7ed;border:1.5px solid #fed7aa;border-radius:12px;padding:16px 20px;margin-bottom:22px}
            .loan-notice-title{font-size:13px;font-weight:800;color:#c2410c;margin-bottom:4px}
            .loan-notice-body{font-size:12px;color:#9a3412;line-height:1.6}
            .footer{background:#f8fafc;padding:28px 36px;text-align:center;border-top:1px solid #e2e8f0}
            .footer p{font-size:12px;color:#94a3b8;margin:4px 0;line-height:1.7}
            .footer strong{color:#64748b}
            .green{color:#16a34a}
          </style>
        </head>
        <body>
          <div class="wrap">
            <div class="header">
              <div class="header-icon">💼</div>
              <h1>Salary Payment Confirmed</h1>
              <p>%s</p>
              <div class="badge">%s Salary Period</div>
            </div>
            <div class="body">
              <div class="greeting">Dear %s,</div>
              <div class="subtitle">
                Your salary for <strong>%s</strong> has been successfully processed and disbursed.
                Please review the breakdown below and keep this email for your records.
              </div>

              <div class="section-label">Payment Breakdown</div>
              <div class="breakdown">
                <div class="row">
                  <span class="row-label">Basic Salary</span>
                  <span class="row-val">ZMW %s</span>
                </div>
                %s
                <div class="row">
                  <span class="row-label">Payment Date</span>
                  <span class="row-val">%s</span>
                </div>
              </div>

              <div class="net-row">
                <div>
                  <div class="net-label">Net Take-Home</div>
                  <div style="font-size:12px;color:#15803d;margin-top:2px">Amount credited to your account</div>
                </div>
                <div class="net-val">ZMW %s</div>
              </div>

              %s

              <div class="section-label" style="margin-top:22px">Payment Channel</div>
              <div class="method-box">
                <div class="method-icon">%s</div>
                <div>
                  <div class="method-label">%s</div>
                  <div class="method-ref">Ref: %s</div>
                </div>
              </div>

              %s

            </div>
            <div class="footer">
              <p><strong>%s</strong></p>
              <p>%s &nbsp;·&nbsp; %s</p>
              <p style="margin-top:12px;color:#cbd5e1;font-size:11px">
                This is an automated salary notification. Please do not reply to this email.
              </p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(
                coopName,
                period,
                sal.getUser().getFullName(),
                period,
                formatAmount(sal.getBasicSalary()),
                hasLoan ? """
                        <div class="row row-deduct">
                          <span class="row-label">Loan Deduction (instalment)</span>
                          <span class="row-val">− ZMW %s</span>
                        </div>""".formatted(formatAmount(deductions)) : "",
                paidAt,
                formatAmount(sal.getNetSalary()),
                hasLoan ? """
                        <div class="loan-notice">
                          <div class="loan-notice-title">⚠ Loan Deduction Applied</div>
                          <div class="loan-notice-body">
                            A loan instalment of <strong>ZMW %s</strong> has been deducted from your gross salary
                            as part of your cooperative loan repayment schedule. Your remaining loan balance has
                            been updated accordingly. Contact the accounts office for your updated loan statement.
                          </div>
                        </div>""".formatted(formatAmount(deductions)) : "",
                methodIcon,
                methodLabel,
                sal.getReferenceNo() != null ? sal.getReferenceNo() : "—",
                sal.getNotes() != null && !sal.getNotes().isBlank()
                        ? "<div class=\"section-label\" style=\"margin-top:22px\">Notes from Accounts Office</div>" +
                        "<div style=\"background:#f8fafc;border-radius:10px;padding:14px 18px;font-size:13px;color:#475569;border:1px solid #e2e8f0\">" +
                        sal.getNotes() + "</div>"
                        : "",
                coopName,
                coopEmail,
                coopPhone
        );

        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(coopEmail, coopName);
            helper.setTo(recipientEmail);
            helper.setSubject("Salary Payment — " + period + " | " + coopName);
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("Salary payment email sent to {} for period {}", recipientEmail, period);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send salary email to {}: {}", recipientEmail, e.getMessage());
        }
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}