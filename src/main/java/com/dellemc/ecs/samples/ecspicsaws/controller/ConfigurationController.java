package com.dellemc.ecs.samples.ecspicsaws.controller;

import com.amazonaws.services.s3.AmazonS3;
import com.dellemc.ecs.samples.ecspicsaws.model.EcsConfiguration;
import com.dellemc.ecs.samples.ecspicsaws.util.S3Factory;
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

        EcsConfiguration config = getConfig(session, configCookie);
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
        EcsConfiguration config = getConfig(session, "");
        config.setConfiguration(endpoint, accessKey, secretKey, bucketName);

        StringBuilder message = new StringBuilder();
        message.append("Configuration Updated.");

        AmazonS3 s3 = S3Factory.getInstance().getClient(config);

        // Test credentials. NOTE: this call is not 'free' so only do it when you first configure connections.  If
        // there is something wrong with the credentials it should throw an exception here.
        s3.listBuckets();

        if(!s3.doesBucketExist(bucketName)) {
            s3.createBucket(bucketName);
            message.append(" Created bucket " + bucketName);
        }

        attrs.addAttribute("message", message.toString());

        // Stash it into a cookie for next time the app is run.
        Cookie c = new Cookie("config-cookie", config.toCookie());
        c.setMaxAge(86400*7); // 1 week
        response.addCookie(c);
        log.debug("Set Cookie: " + c.toString());

        return "redirect:/";
    }

    private EcsConfiguration getConfig(HttpSession session, String configCookie) {
        EcsConfiguration config = (EcsConfiguration) session.getAttribute("config");

        if(config == null) {
            config = new EcsConfiguration();
            session.setAttribute("config", config);
            // Load it from a cookie if possible.
            if(!("".equals(configCookie))) {
                config.fromCookie(configCookie);
            }
        }

        return config;
    }
}
