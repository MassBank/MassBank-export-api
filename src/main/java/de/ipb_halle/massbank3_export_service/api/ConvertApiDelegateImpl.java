package de.ipb_halle.massbank3_export_service.api;

import de.ipb_halle.massbank3_export_service.model.Conversion;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConvertApiDelegateImpl implements ConvertApiDelegate {

     /**
     * POST /convert : Create a conversion task.
     *
     * @param conversion  (required)
     * @return Conversion successfully completed. (status code 200)
     *         or The server can not process the input. (status code 400)
     *         or The server encountered an unexpected condition. (status code 500)
     * @see ConvertApi#convertPost
     */
     @Override
     public ResponseEntity<String> convertPost(Conversion conversion) {
        getRequest().ifPresent(request -> {
            for (MediaType mediaType: MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
                    String exampleString = "Custom MIME type example not yet supported: application/problem+json";
                    ApiUtil.setExampleResponse(request, "application/problem+json", exampleString);
                    break;
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
                    String exampleString = "Custom MIME type example not yet supported: application/problem+json";
                    ApiUtil.setExampleResponse(request, "application/problem+json", exampleString);
                    break;
                }
            }
        });
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
