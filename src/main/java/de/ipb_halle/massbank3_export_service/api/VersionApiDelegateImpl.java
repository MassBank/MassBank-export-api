package de.ipb_halle.massbank3_export_service.api;

import jakarta.annotation.Generated;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;

/**
 * A delegate to be called by the {@link VersionApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-09-02T14:32:39.719208510+02:00[Europe/Oslo]", comments = "Generator version: 7.8.0")
public interface VersionApiDelegateImpl {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * GET /version : Get the version string of the implementation.
     *
     * @return version string (status code 200)
     * @see VersionApi#versionGet
     */
    default ResponseEntity<String> versionGet() {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

}
