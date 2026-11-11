package in.neonjava.erp.web;

import in.neonjava.erp.model.AppUser;
import in.neonjava.erp.security.CurrentUserService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes the logged-in user available to every template as ${me}. */
@ControllerAdvice
public class GlobalModel {

    private final CurrentUserService currentUser;

    public GlobalModel(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @ModelAttribute("me")
    public AppUser me() {
        return currentUser.get();
    }
}
