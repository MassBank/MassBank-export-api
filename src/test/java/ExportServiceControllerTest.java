import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = org.openapitools.OpenApiGeneratorApplication.class)
@AutoConfigureMockMvc
public class ExportServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetVersion() throws Exception {
        mockMvc.perform(get("/version"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$").value(startsWith("export service ")));
    }

    @Test
    public void testCreateConversionTask() throws Exception {
        String requestBody = "{ \"record_list\": [\"MSBNK-IPB_Halle-PB001341\"], \"format\": \"nist_msp\" }";

        mockMvc.perform(post("/convert")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/octet-stream"));
    }
}