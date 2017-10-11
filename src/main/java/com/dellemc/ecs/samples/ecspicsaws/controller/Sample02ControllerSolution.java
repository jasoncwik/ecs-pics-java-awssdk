package com.dellemc.ecs.samples.ecspicsaws.controller;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.dellemc.ecs.samples.ecspicsaws.model.EcsConfiguration;
import com.dellemc.ecs.samples.ecspicsaws.util.S3Factory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * In the second sample, we'll update some metadata on the object after uploading it.
 */
@Controller
public class Sample02ControllerSolution {
    private static final String CONTROLLER = "/sample2sol";
    private static final String PHOTOGRAPHER = "photographer";
    private static final String CITY = "city";

    private static final Log log = LogFactory.getLog(Sample02ControllerSolution.class);


    /**
     * Prepare for the upload.  Not much to do here but redirect to the upload view.
     */
    @GetMapping(CONTROLLER)
    public String loadPage(Model model,
                           @ModelAttribute("message") String message,
                           @ModelAttribute("error") String error) {
        // Put our page name in here so we can return (user vs solution)
        model.addAttribute("controller", CONTROLLER);

        return "sample2-upload";
    }

    @PostMapping(CONTROLLER)
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

        // Pass the object name so we can edit it.

        return "redirect:" + CONTROLLER + "/edit/" + file.getOriginalFilename();
    }

    @GetMapping(CONTROLLER + "/edit/{key:.+}")
    public String loadEdit(Model model,
                           @PathVariable("key") String key,
                           HttpSession session,
                           RedirectAttributes attrs) {
        log.info("Editing: " + key);
        EcsConfiguration config = (EcsConfiguration) session.getAttribute("config");
        if(config == null) {
            // Send to config page
            attrs.addAttribute("error", "Please set configuration before running samples.");
            return "redirect:/configure";
        }

        // Get the AmazonS3 instance.
        AmazonS3 s3 = S3Factory.getInstance().getClient(config);

        // Get the object metadata.
        ObjectMetadata om = s3.getObjectMetadata(config.getBucketName(), key);

        model.addAttribute("contentType", om.getContentType());
        model.addAttribute(PHOTOGRAPHER, decodeMetadata(om.getUserMetaDataOf(PHOTOGRAPHER)));
        model.addAttribute(CITY, decodeMetadata(om.getUserMetaDataOf(CITY)));

        log.debug("Model: " + model);

        // Pre-signed URL.  Generate a pre-signed URL to display the image.  URL is good for 1 minute.  This will
        // allow the web page to pull the image directly from the object store without going through our application.
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, 1);
        URL imageUrl = s3.generatePresignedUrl(config.getBucketName(), key, c.getTime());

        model.addAttribute("imageUrl", imageUrl);

        // Set the form post target
        model.addAttribute("target", CONTROLLER + "/edit/" + key);

        return "sample2-edit";
    }

    @PostMapping(CONTROLLER + "/edit/{key:.+}")
    public String saveEdit(@PathVariable("key") String key,
                           @RequestParam("contentType") String contentType,
                           @RequestParam("photographer") String photographer,
                           @RequestParam("city") String city,
                           RedirectAttributes attrs,
                           HttpSession session) {

        EcsConfiguration config = (EcsConfiguration) session.getAttribute("config");
        if(config == null) {
            // Send to config page
            attrs.addAttribute("error", "Please set configuration before running samples.");
            return "redirect:/configure";
        }

        // Get the AmazonS3 instance.
        AmazonS3 s3 = S3Factory.getInstance().getClient(config);

        // Since the S3 protocol is Idempotent, there's no 'update' of an object.  To update the metadata on an object,
        // you actually copy the object to itself and use the metadata directive REPLACE to use the same object content
        // but replace the metadata.  Note that this applies to ALL metadata, including non-user metadata like
        // content-type, content-disposition, content-encoding, etc.  If you plan to edit the metadata on an object and
        // use some of those extended content headers, be sure to copy those values too.  Also note that you should
        // NOT clone() the ObjectMetadata object.  ObjectMetadata has an internal hash map of ALL the HTTP headers that
        // were passed from the response that created it.  With ECS, this includes some x-emc-* headers that are not
        // applicable to a metadata update and will cause signature errors because the AWS SDK's signer doesn't know
        // to sign x-emc-* headers but ECS expects this.
        ObjectMetadata om = new ObjectMetadata();
        om.setContentType(contentType);
        Map<String,String> userMeta = new HashMap<>();
        userMeta.put(PHOTOGRAPHER, encodeMetadata(photographer));
        userMeta.put(CITY, encodeMetadata(city));
        om.setUserMetadata(userMeta);

        try {
            CopyObjectRequest cor = new CopyObjectRequest(config.getBucketName(), key, config.getBucketName(), key)
                    .withNewObjectMetadata(om); // Under the hood, this sets x-amz-metadata-directive:REPLACE
            s3.copyObject(cor);

        } catch (RuntimeException e) {
            log.error(e);
            throw e;
        }

        log.info("Metadata update complete.");


        attrs.addAttribute("message", "Metadata updated.");

        return "redirect:" + CONTROLLER + "/edit/" + key;
    }

    /**
     * S3 passes metadata as HTTP headers.  This technically only allows for 'printable characters' to be stored (not
     * even the full ASCII space).  Therefore, we will URL-Encode the value to allow any character to be used.  This
     * of course means we'll have to decode to view.
     * @param value string value to encode
     * @return the value URL-encoded
     */
    private String encodeMetadata(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // Should never happen since UTF-8 is required by Java spec.
            return null;
        }
    }

    /**
     * S3 passes metadata as HTTP headers.  This technically only allows for 'printable characters' to be stored (not
     * even the full ASCII space).  Therefore, we will URL-Encode the value to allow any character to be used.  This
     * of course means we'll have to decode to view.
     * @param value string value to decode
     * @return the value URL-decoded
     */
    private String decodeMetadata(String value) {
        if(value == null) {
            return null;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // Should never happen since UTF-8 is required by Java spec.
            return null;
        }
    }
}
