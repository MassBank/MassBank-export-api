package de.ipb_halle.massbank3_export_service.api;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;


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
        Gson gson = new Gson();
        System.out.println("versionGet");
        return new ResponseEntity<>(gson.toJson("export service 0.1"),  HttpStatus.OK);
    }

}
