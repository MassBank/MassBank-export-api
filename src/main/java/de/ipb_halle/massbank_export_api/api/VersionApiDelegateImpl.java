package de.ipb_halle.massbank_export_api.api;


import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
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
        Gson gson = new Gson();
        String versionInfo = String.format("export api %s, %s", artifactVersion, timestamp);
        return new ResponseEntity<>(gson.toJson(versionInfo), HttpStatus.OK);
    }

    @Value("${artifact.version}")
    private String artifactVersion;

    @Value("${timestamp}")
    private String timestamp;

}
