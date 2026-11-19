package in.neonjava.erp.web.admin;

import in.neonjava.erp.model.CampusResource;
import in.neonjava.erp.model.ResourceType;
import in.neonjava.erp.repo.CampusResourceRepository;
import in.neonjava.erp.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/resources")
public class ResourceAdminController {

    private final BookingService booking;
    private final CampusResourceRepository resources;

    public ResourceAdminController(BookingService booking, CampusResourceRepository resources) {
        this.booking = booking;
        this.resources = resources;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("resources", resources.findAll().stream()
                .sorted(java.util.Comparator.comparing(CampusResource::getType).thenComparing(CampusResource::getName))
                .toList());
        model.addAttribute("types", ResourceType.values());
        return "admin/resources";
    }

    @PostMapping
    public String add(@RequestParam String name, @RequestParam ResourceType type,
                      @RequestParam(required = false) String location, @RequestParam(defaultValue = "0") int capacity,
                      @RequestParam(required = false) String description, RedirectAttributes ra) {
        try {
            CampusResource r = booking.add(new CampusResource(name, type, location, capacity, description));
            ra.addFlashAttribute("success", "Added " + r.getName());
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/resources";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        CampusResource r = booking.toggleActive(id);
        ra.addFlashAttribute("success", r.getName() + (r.isActive() ? " is open for booking" : " is disabled for booking"));
        return "redirect:/admin/resources";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        booking.delete(id);
        ra.addFlashAttribute("success", "Resource deleted with its bookings");
        return "redirect:/admin/resources";
    }
}
