package com.dairycoopms.dairycoopms.controller.admin;

import com.dairycoopms.dairycoopms.model.Cow;
import com.dairycoopms.dairycoopms.model.User;
import com.dairycoopms.dairycoopms.repository.CowAssignmentRepository;
import com.dairycoopms.dairycoopms.repository.UserRepository;
import com.dairycoopms.dairycoopms.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * AdminCowController
 *
 * URLs handled:
 *   GET  /admin/cows           – list all cows
 *   GET  /admin/cows/add       – show add cow form
 *   POST /admin/cows/add       – record new cow
 *   GET  /admin/cows/{id}      – cow detail + assignment history
 *   POST /admin/cows/{id}/status – update cow status
 */
@Controller
@RequestMapping("/admin/cows")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCowController {

    private final AdminService            adminService;
    private final CowAssignmentRepository cowAssignmentRepository;
    private final UserRepository          userRepository;

    public AdminCowController(AdminService adminService,
                              CowAssignmentRepository cowAssignmentRepository,
                              UserRepository userRepository) {
        this.adminService            = adminService;
        this.cowAssignmentRepository = cowAssignmentRepository;
        this.userRepository          = userRepository;
    }

    private User currentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    // ── GET /admin/cows ──────────────────────────────────────────────────────
    @GetMapping
    public String listCows(Model model) {
        var cows = adminService.getAllCows();
        model.addAttribute("cows",           cows);
        model.addAttribute("totalCows",      cows.size());
        model.addAttribute("unassignedCount", adminService.getUnassignedCows().size());
        model.addAttribute("assignedCount",   cowAssignmentRepository.countByActiveTrue());
        model.addAttribute("allMembers",     adminService.getMembersByStatus("APPROVED"));

        // Active assignments map
        var activeMap = new java.util.HashMap<java.util.UUID, com.dairycoopms.dairycoopms.model.CowAssignment>();
        cowAssignmentRepository.findByActiveTrue().forEach(a -> activeMap.put(a.getCow().getId(), a));
        model.addAttribute("activeAssignments", activeMap);

        // Build cowId -> image source (base64 portrait or breed-based fallback)
        java.util.Map<String, String> breedToFile = new java.util.HashMap<>();
        breedToFile.put("Friesian",    "friesian.jpg");
        breedToFile.put("Holstein",    "holstein.jpg");
        breedToFile.put("Jersey",      "jersey.jpg");
        breedToFile.put("Ayrshire",    "ayrshire.jpg");
        breedToFile.put("Brown Swiss", "brown_swiss.jpg");
        breedToFile.put("Hereford",    "hereford.jpg");
        breedToFile.put("Angus",       "angus.jpg");
        breedToFile.put("Brahman",     "brahman.jpg");
        breedToFile.put("Simmental",   "simmental.jpg");
        breedToFile.put("Crossbreed",  "crossbreed.jpg");
        breedToFile.put("Boran",       "boran.jpg");
        breedToFile.put("Tonga",       "local.jpg");
        breedToFile.put("Local",       "local.jpg");

        var cowImageMap = new java.util.HashMap<java.util.UUID, String>();
        for (var cow : cows) {
            // If cow has a portrait (base64), use it; otherwise use breed image
            if (cow.getPortrait() != null && !cow.getPortrait().isEmpty()) {
                // Portrait is already base64, convert to data URI
                cowImageMap.put(cow.getId(), "data:image/jpeg;base64," + cow.getPortrait());
            } else {
                // Fallback to breed-based static image
                String file = breedToFile.getOrDefault(cow.getBreed(), "local.jpg");
                cowImageMap.put(cow.getId(), "/images/breeds/" + file);
            }
        }
        model.addAttribute("cowImageMap", cowImageMap);

        return "admin/cows/list";
    }

    // ── GET /admin/cows/add ──────────────────────────────────────────────────
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("cow", new Cow());
        return "admin/cows/add";
    }

    // ── POST /admin/cows/add ─────────────────────────────────────────────────
    @PostMapping("/add")
    public String recordCow(@ModelAttribute Cow cow,
                            @AuthenticationPrincipal UserDetails ud,
                            RedirectAttributes flash,
                            Model model) {
        try {
            Cow saved = adminService.recordCow(cow, currentUser(ud));
            flash.addFlashAttribute("successMessage",
                    "Cow " + saved.getTagNumber() + " recorded successfully.");
            return "redirect:/admin/cows/" + saved.getId();

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("cow", cow);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/cows/add";
        }
    }

    // ── GET /admin/cows/{id} ─────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String cowDetail(@PathVariable UUID id, Model model) {
        try {
            model.addAttribute("cow", adminService.getCowById(id));
            // Full assignment history for this cow
            model.addAttribute("assignments", cowAssignmentRepository.findByCowId(id));
            // Is it currently assigned?
            model.addAttribute("currentAssignment",
                    cowAssignmentRepository.findByCowIdAndActiveTrue(id).orElse(null));
            return "admin/cows/detail";

        } catch (IllegalArgumentException e) {
            return "redirect:/admin/cows?error=Cow+not+found";
        }
    }

    // ── POST /admin/cows/{id}/status ─────────────────────────────────────────
    @PostMapping("/{id}/status")
    public String updateCowStatus(@PathVariable UUID id,
                                  @RequestParam String status,
                                  @AuthenticationPrincipal UserDetails ud,
                                  RedirectAttributes flash) {
        try {
            adminService.updateCowStatus(id, status.toUpperCase(), currentUser(ud));
            flash.addFlashAttribute("successMessage",
                    "Cow status updated to " + status + ".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/cows/" + id;
    }

    // ── POST /admin/cows/{id}/portrait ───────────────────────────────────────
    @PostMapping("/{id}/portrait")
    public String uploadPortrait(@PathVariable UUID id,
                                 @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                 @RequestParam(value = "redirect", required = false) String redirectTo,
                                 @AuthenticationPrincipal UserDetails ud,
                                 RedirectAttributes flash) {
        try {
            if (file.isEmpty()) {
                flash.addFlashAttribute("errorMessage", "Please select a file to upload.");
                return getRedirectUrl(id, redirectTo);
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                flash.addFlashAttribute("errorMessage", "Only image files are allowed.");
                return getRedirectUrl(id, redirectTo);
            }

            // Validate file size (5MB max)
            if (file.getSize() > 5 * 1024 * 1024) {
                flash.addFlashAttribute("errorMessage", "File size must be less than 5MB.");
                return getRedirectUrl(id, redirectTo);
            }

            // Convert to base64
            byte[] bytes = file.getBytes();
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);

            // Update cow portrait
            Cow cow = adminService.getCowById(id);
            cow.setPortrait(base64);
            adminService.updateCow(cow, currentUser(ud));

            flash.addFlashAttribute("successMessage", "Portrait uploaded successfully.");
        } catch (java.io.IOException e) {
            flash.addFlashAttribute("errorMessage", "Error reading file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return getRedirectUrl(id, redirectTo);
    }

    // ── DELETE /admin/cows/{id}/portrait ─────────────────────────────────────
    @PostMapping("/{id}/portrait/delete")
    public String deletePortrait(@PathVariable UUID id,
                                 @RequestParam(value = "redirect", required = false) String redirectTo,
                                 @AuthenticationPrincipal UserDetails ud,
                                 RedirectAttributes flash) {
        try {
            Cow cow = adminService.getCowById(id);
            cow.setPortrait(null);
            adminService.updateCow(cow, currentUser(ud));
            flash.addFlashAttribute("successMessage", "Portrait removed successfully.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return getRedirectUrl(id, redirectTo);
    }

    // Helper method to determine redirect URL
    private String getRedirectUrl(UUID cowId, String redirectTo) {
        if ("list".equals(redirectTo)) {
            return "redirect:/admin/cows";
        }
        return "redirect:/admin/cows/" + cowId;
    }
}