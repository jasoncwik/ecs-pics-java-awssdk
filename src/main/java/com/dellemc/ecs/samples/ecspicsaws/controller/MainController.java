package com.dellemc.ecs.samples.ecspicsaws.controller;

import com.dellemc.ecs.samples.ecspicsaws.model.SamplePage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the main controller for the samples.  You generally won't need to edit this Controller class unless you are
 * adding another sample.
 */
@SpringBootApplication
@Controller
public class MainController {
    private static final Log log = LogFactory.getLog(MainController.class);

    /**
     * Displays the index page when the app starts.  The model will contain a list of sample pages, their descriptions,
     * and the path to the sample controller.
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(Model model,
                        @ModelAttribute("message") String message,
                        @ModelAttribute("error") String error) {

        List<SamplePage> samples = new ArrayList<>();
        samples.add(new SamplePage("Configure ECS Connection",
                "Configure your connection to ECS. Run me first!", "/configure"));
        samples.add(new SamplePage("Sample 1", "Uploads a file to ECS", "/sample1"));
        samples.add(new SamplePage("Sample 1 Solution", "Uploads a file to ECS", "/sample1sol"));

        model.addAttribute("samples", samples);

        log.info("model: " + model);

        return "index";
    }

    /**
     * Spring... boot!
     */
    public static void main(String[] args) throws Exception {
        SpringApplication.run(MainController.class, args);
    }
}
