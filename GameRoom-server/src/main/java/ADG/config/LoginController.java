package ADG.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class LoginController {

    /** Redirect GET /login → /login.html.  POST /login is handled by Spring Security before this runs. */
    @GetMapping("/login")
    public RedirectView loginPage() {
        return new RedirectView("/login.html");
    }
}