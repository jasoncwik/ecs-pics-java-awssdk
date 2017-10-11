package com.dellemc.ecs.samples.ecspicsaws.controller;

import com.dellemc.ecs.samples.ecspicsaws.model.EcsConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This controller is used to manage the ECS S3 connection configuration information on the '/configure' path of the
 * application.  It will update and store an EcsConfiguration object in the HttpSession.  It will also read/write the
 * configuration information in a cookie so if you stop and restart the app or your browser that the configuration will
 * persist for a week.
 */
@Controller
@EnableAutoConfiguration
public class ConfigurationController {
    private static final Log log = LogFactory.getLog(ConfigurationController.class);


    /**
     * Load the configuration (if any) and redirect to the configuration page.
     */
    @RequestMapping(path = "/configure")
    public String loadConfiguration(Model model, HttpSession session,
                                    @CookieValue(value = "config-cookie", defaultValue = "") String configCookie,
                                    @ModelAttribute("message") String message,
                                    @ModelAttribute("error") String error) {

        EcsConfiguration config = (EcsConfiguration) session.getAttribute("config");
        if(config == null) {
            config = new EcsConfiguration();
            session.setAttribute("config", config);
            // Load it from a cookie if possible.
            if(!("".equals(configCookie))) {
                config.fromCookie(configCookie);
            }
        }

        model.addAttribute("config", config);

        return "config";
    }

    /**
     * Updates the EcsConfiguration object.
     */
    @PostMapping(path = "/configure")
    public String updateConfiguration(@RequestParam("endpoint") String endpoint,
                                      @RequestParam("access-key") String accessKey,
                                      @RequestParam("secret-key") String secretKey,
                                      @RequestParam("bucket-name") String bucketName,
                                      HttpSession session,
                                      RedirectAttributes attrs,
                                      HttpServletResponse response) {
        EcsConfiguration config = (EcsConfiguration) session.getAttribute("config");
        config.setConfiguration(endpoint, accessKey, secretKey, bucketName);

        attrs.addAttribute("message", "Configuration Updated.");

        // Stash it into a cookie for next time the app is run.
        Cookie c = new Cookie("config-cookie", config.toCookie());
        c.setMaxAge(86400*7); // 1 week
        response.addCookie(c);
        log.debug("Set Cookie: " + c.toString());

        return "redirect:/";
    }
}
