package de.ipb_halle.massbank3_export_service.api;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import de.ipb_halle.massbank3_export_service.model.Conversion;



@Service
public class ConvertApiDelegateImpl implements ConvertApiDelegate {

    /**
     * POST /convert : Create a conversion task.
     *
     * @param conversion  (required)
     * @return Conversion successfully completed. (status code 200)
     * @see ConvertApi#convertPost
     */
    @Override
    public ResponseEntity<String> convertPost(Conversion conversion) {
        System.out.println("Conversion task received: " + conversion);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
