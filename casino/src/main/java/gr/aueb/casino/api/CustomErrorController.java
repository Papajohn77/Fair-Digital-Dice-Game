package gr.aueb.casino.api;

import jakarta.servlet.http.HttpSession;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String errorPage(HttpSession session, Model model) {
        Integer status = (Integer) session.getAttribute("errorStatus");
        String message = (String) session.getAttribute("errorMessage");

        session.removeAttribute("errorStatus");
        session.removeAttribute("errorMessage");

        model.addAttribute("status", status != null ? status : 500);
        model.addAttribute("error", status != null && status == 403 ? "Forbidden" : "Internal Server Error");
        model.addAttribute("message", message != null ? message : "An unexpected error occurred");

        return "error";
    }
}
