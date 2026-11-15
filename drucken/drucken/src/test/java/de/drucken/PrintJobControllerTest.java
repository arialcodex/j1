package de.drucken;

import de.drucken.service.PdfPrintService;
import de.drucken.service.AsyncPrintJobSubmissionService;
import de.drucken.persistence.PrintJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;

@SpringBootTest
@AutoConfigureMockMvc
class PrintJobControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    PrintJobRepository jobRepository;

    @MockitoBean
    PdfPrintService pdfPrintService;

    @MockitoBean
    AsyncPrintJobSubmissionService submissionService;

    @BeforeEach
    void leertAuftraege() {
        jobRepository.deleteAll();
    }

    @Test
    void erstelltDruckauftrag() throws Exception {
        when(pdfPrintService.pdfDateiFinden("test.pdf")).thenReturn(Path.of("test.pdf"));

        var result = mvc.perform(post("/api/druckauftraege")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dokument\":\"test.pdf\",\"kopien\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WARTEND"))
                .andReturn();

        String id = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.id").toString();
        mvc.perform(get("/api/druckauftraege/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WARTEND"));

        mvc.perform(get("/api/druckauftraege/{id}/status", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Integer.parseInt(id)))
                .andExpect(jsonPath("$.status").value("WARTEND"))
                .andExpect(jsonPath("$.aktualisiertAm").exists())
                .andExpect(jsonPath("$.fehlermeldung").doesNotExist());
    }

    @Test
    void lehntUngueltigeKopienAb() throws Exception {
        mvc.perform(post("/api/druckauftraege")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dokument\":\"test.pdf\",\"kopien\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void liefertNotFoundFuerUnbekanntenStatus() throws Exception {
        mvc.perform(get("/api/druckauftraege/{id}/status", 999999))
                .andExpect(status().isNotFound());
    }
}
