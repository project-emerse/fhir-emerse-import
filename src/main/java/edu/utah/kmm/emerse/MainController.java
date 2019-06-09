package edu.utah.kmm.emerse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for all REST services.
 *
 */
@Controller
public class MainController {

    private static final Log log = LogFactory.getLog(MainController.class);

    /**
     * The logging endpoint.  Formats the payload according to the "logging.format" property.  Then outputs
     * to the logger service.
     *
     */
    @RequestMapping(path = "/documents", method = RequestMethod.GET)
    public ResponseEntity log(@RequestBody String payload) {
        return new ResponseEntity(HttpStatus.OK);
    }

}
