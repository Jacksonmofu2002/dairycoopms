package com.dairycoopms.dairycoopms.controller.admin;

import com.dairycoopms.dairycoopms.model.ReportSubmission;
import com.dairycoopms.dairycoopms.model.User;
import com.dairycoopms.dairycoopms.repository.ReportSubmissionRepository;
import com.dairycoopms.dairycoopms.repository.UserRepository;
import com.dairycoopms.dairycoopms.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminService                adminService;
    private final UserRepository              userRepository;
    private final ReportSubmissionRepository  reportRepo;

    public AdminReportController(AdminService adminService,
                                 UserRepository userRepository,
                                 ReportSubmissionRepository reportRepo) {
        this.adminService  = adminService;
        this.userRepository = userRepository;
        this.reportRepo    = reportRepo;
    }

    private User currentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    // ── GET /admin/reports — notification list ───────────────────────────────
    @GetMapping
    public String reportsPage(@AuthenticationPrincipal UserDetails ud, Model model) {
        var admin = currentUser(ud);
        model.addAttribute("user", admin);
        model.addAttribute("mustChangePassword", admin.isMustChangePassword());

        List<ReportSubmission> sent = reportRepo.findBySentAtNotNullOrderBySentAtDesc();
        long unreadCount = reportRepo.countByReadByAdminFalseAndSentAtNotNull();

        model.addAttribute("reportSubmissions", sent);
        model.addAttribute("unreadCount", unreadCount);
        return "admin/reports/index";
    }

    // ── GET /admin/reports/{id}/analyse — open & analyse report ─────────────
    @GetMapping("/{id}/analyse")
    public String analyseReport(@PathVariable UUID id,
                                @AuthenticationPrincipal UserDetails ud,
                                Model model) {
        var admin = currentUser(ud);
        model.addAttribute("user", admin);
        model.addAttribute("mustChangePassword", admin.isMustChangePassword());

        var submission = reportRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        // Mark as read
        if (!submission.isReadByAdmin()) {
            submission.setReadByAdmin(true);
            submission.setReadAt(LocalDateTime.now());
            reportRepo.save(submission);
        }

        model.addAttribute("submission", submission);
        model.addAttribute("reportJson", submission.getReportData());
        return "admin/reports/analyse";
    }

    // ── POST /admin/reports/{id}/mark-read ──────────────────────────────────
    @PostMapping("/{id}/mark-read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markRead(@PathVariable UUID id) {
        reportRepo.findById(id).ifPresent(s -> {
            s.setReadByAdmin(true);
            s.setReadAt(LocalDateTime.now());
            reportRepo.save(s);
        });
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ── GET /admin/reports/unread-count — for badge polling ──────────────────
    @GetMapping("/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unreadCount() {
        return ResponseEntity.ok(Map.of("count",
                reportRepo.countByReadByAdminFalseAndSentAtNotNull()));
    }
}