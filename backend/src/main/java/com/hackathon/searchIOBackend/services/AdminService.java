package com.hackathon.searchIOBackend.services;

import com.hackathon.searchIOBackend.model.FormData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AdminService {

    @Value("${pdf.storage.dir:pdf-storage}")
    private String pdfStorageDir;

    @Value("${api.upload.url}")
    private String apiUploadUrl;

    @Value("${api.host}")
    private String host;

    @Value("${api.organizationId}")
    private String organizationId;

    @Value("${api.token}")
    private String token;

    private static final String BASE_PDF_CONTENT = "This is the default content for base.pdf.";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    private Path getJsonFilePath() {
        return Paths.get(pdfStorageDir, "form_data.json");
    }

    private Path getPdfFilePath() {
        return Paths.get(pdfStorageDir, "form_data.pdf");
    }

    public void appendDataToJsonFile(FormData formData) throws IOException {
        Path jsonFilePath = getJsonFilePath();
        List<FormData> formDataList;

        String formDataJson = objectMapper.writeValueAsString(formData);

        if (Files.exists(jsonFilePath)) {
            try (BufferedReader reader = Files.newBufferedReader(jsonFilePath)) {
                try {
                    formDataList = objectMapper.readValue(reader, new TypeReference<List<FormData>>() {});
                } catch (EOFException e) {
                    formDataList = new ArrayList<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
                formDataList = new ArrayList<>();
            }

            formDataList.add(formData);

            try (BufferedWriter writer = Files.newBufferedWriter(jsonFilePath, StandardOpenOption.TRUNCATE_EXISTING)) {
                objectMapper.writeValue(writer, formDataList);
            }
        } else {
            formDataList = new ArrayList<>();
            formDataList.add(formData);

            try (BufferedWriter writer = Files.newBufferedWriter(jsonFilePath, StandardOpenOption.CREATE)) {
                objectMapper.writeValue(writer, formDataList);
            }
        }

        createPdfFromJson(formDataList);
    }

    private void createPdfFromJson(List<FormData> formDataList) throws IOException {
        Path pdfPath = getPdfFilePath();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(25, 750);

                for (FormData formData : formDataList) {
                    String text = objectMapper.writeValueAsString(formData);
                    contentStream.showText(text);
                    contentStream.newLineAtOffset(0, -15); // Move to next line
                }

                contentStream.endText();
            }
            document.save(pdfPath.toFile());
        }

        sendPdfToApi(pdfPath.toString());
    }

    public void sendPdfToApi(String pdfPath) {
        String url = String.format("https://%s/api/v1/documents/organizations/%s/documents", host, organizationId);

        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("api-key", token);

        // Create file resource
        File file = new File(pdfPath);
        Resource fileResource = new FileSystemResource(file);

        // Create request entity
        HttpEntity<Resource> requestEntity = new HttpEntity<>(fileResource, headers);

        // Create RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        try {
            // Make the request
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // Handle response
            System.out.println("API response: " + response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Handle HTTP errors (4xx and 5xx)
            System.err.println("HTTP error occurred: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception e) {
            // Handle any other exceptions
            System.err.println("An error occurred: " + e.getMessage());
        }
    }

}
