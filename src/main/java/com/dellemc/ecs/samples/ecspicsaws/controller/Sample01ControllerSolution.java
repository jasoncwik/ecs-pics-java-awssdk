package com.dellemc.ecs.samples.ecspicsaws.controller;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.internal.Mimetypes;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.dellemc.ecs.samples.ecspicsaws.model.EcsConfiguration;
import com.dellemc.ecs.samples.ecspicsaws.util.S3Factory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.embedded.MimeMappings;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.activation.MimeType;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Controller
public class Sample01ControllerSolution {
    private static final String CONTROLLER = "/sample1sol";
    private static final Log log = LogFactory.getLog(Sample01ControllerSolution.class);


    /**
     * Prepare for the upload.  Not much to do here but redirect to the upload view.
     */
    @GetMapping(CONTROLLER)
    public String loadPage(Model model,
                           @ModelAttribute("message") String message,
                           @ModelAttribute("error") String error) {
        // Put our page name in here so we can return (user vs solution)
        model.addAttribute("controller", CONTROLLER);

        return "sample1";
    }

    @PostMapping("/sample1sol")
    public String processUpload(@RequestParam("file") MultipartFile file,
                                HttpSession session,
                                RedirectAttributes attrs) throws IOException {
        EcsConfiguration config = (EcsConfiguration) session.getAttribute("config");
        if(config == null) {
            // Send to config page
            attrs.addAttribute("error", "Please set configuration before running samples.");
            return "redirect:/configure";
        }

        // Get the AmazonS3 instance.
        AmazonS3 s3 = S3Factory.getInstance().getClient(config);

        // The object metadata will contain two important facts about the object:
        // 1. The ContentType.  Since this is a web application, we need to have the correct content (mime) type so
        //    the browser can correctly render the images.
        // 2. The ContentLength.  Since we will be passing the raw InputStream to the AmazonS3 client, we need to know
        //    how many bytes to expect or otherwise the client will buffer the entire stream to determine the length.
        //    This is because the S3 PutObject API requires the size.  This is because unsized PUT requests tend to
        //    get truncated if you're not explicit about how many bytes are being sent and use chunked encoding.
        ObjectMetadata om = new ObjectMetadata();
        om.setContentType(file.getContentType());
        om.setContentLength(file.getSize());

        s3.putObject(config.getBucketName(), file.getOriginalFilename(), file.getInputStream(), om);

        String message = String.format("Successfully uploaded %s to bucket %s (%d bytes)",
                file.getOriginalFilename(), config.getBucketName(), file.getSize());
        log.info(message);
        attrs.addAttribute("message", message);

        return "redirect:" + CONTROLLER;
    }
}
