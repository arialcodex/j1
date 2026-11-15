package de.drucken;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PrintJobControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void erstelltDruckauftrag() throws Exception {
        mvc.perform(post("/api/druckauftraege")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dokument\":\"test.pdf\",\"kopien\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WARTEND"));
    }

    @Test
    void lehntUngueltigeKopienAb() throws Exception {
        mvc.perform(post("/api/druckauftraege")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dokument\":\"test.pdf\",\"kopien\":0}"))
                .andExpect(status().isBadRequest());
    }
}
