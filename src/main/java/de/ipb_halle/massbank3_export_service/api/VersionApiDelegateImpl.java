package de.ipb_halle.massbank3_export_service.api;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
public class VersionApiDelegateImpl implements VersionApiDelegate {
    /**
     * GET /version : Get the version string of the implementation.
     *
     * @return version string (status code 200)
     * @see VersionApi#versionGet
     */
    @Override
    public ResponseEntity<String> versionGet() {
        return new ResponseEntity<>("1.0.0", HttpStatus.OK);
    }

}
