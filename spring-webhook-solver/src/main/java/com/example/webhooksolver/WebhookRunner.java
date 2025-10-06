
package com.example.webhooksolver;

import com.example.webhooksolver.dto.GenerateWebhookRequest;
import com.example.webhooksolver.dto.TestWebhookRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import java.util.Map;

@Component
public class WebhookRunner implements CommandLineRunner {

    private final RestTemplate restTemplate;

    @Value("${solver.request.name}")
    private String name;

    @Value("${solver.request.regNo}")
    private String regNo;

    @Value("${solver.request.email}")
    private String email;

    // endpoint to request webhook (from task spec). If firewall blocks, the returned webhook field may be used instead.
    private static final String GENERATE_WEBHOOK_URL =
            "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

    public WebhookRunner() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting webhook flow...");

        // 1) Prepare request body
        GenerateWebhookRequest requestBody = new GenerateWebhookRequest();
        requestBody.setName(name);
        requestBody.setRegNo(regNo);
        requestBody.setEmail(email);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<GenerateWebhookRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            System.out.println("Calling generateWebhook endpoint: " + GENERATE_WEBHOOK_URL);
            ResponseEntity<Map> response = restTemplate.postForEntity(GENERATE_WEBHOOK_URL, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                Object webhookObj = body.get("webhook");
                Object accessTokenObj = body.get("accessToken");

                if (webhookObj == null || accessTokenObj == null) {
                    System.err.println("generateWebhook response missing required fields. Full response: " + body);
                    return;
                }

                String webhookUrl = webhookObj.toString();
                String accessToken = accessTokenObj.toString();

                System.out.println("Received webhook URL: " + webhookUrl);
                System.out.println("Received accessToken (truncated): " + (accessToken.length() > 12 ? accessToken.substring(0, 12) + "..." : accessToken));

                // 2) Decide which question based on regNo last digits (odd=>Q1, even=>Q2)
                boolean isOdd = isRegNoOdd(regNo);
                System.out.println("RegNo '" + regNo + "' considered " + (isOdd ? "ODD (Question 1)" : "EVEN (Question 2)"));

                // 3) Build final SQL query for Question 1 (we use the query for Question 1 here)
                String finalQuery = buildFinalSqlForQuestion1();

                // 4) Submit the final query to the webhook URL using Authorization header with accessToken
                HttpHeaders submitHeaders = new HttpHeaders();
                submitHeaders.setContentType(MediaType.APPLICATION_JSON);
                // The task expects the JWT in the Authorization header
                submitHeaders.set(HttpHeaders.AUTHORIZATION, accessToken);

                TestWebhookRequest testRequest = new TestWebhookRequest();
                testRequest.setFinalQuery(finalQuery);

                HttpEntity<TestWebhookRequest> submitEntity = new HttpEntity<>(testRequest, submitHeaders);

                try {
                    System.out.println("Submitting final query to webhook...");
                    ResponseEntity<String> submitResponse = restTemplate.postForEntity(webhookUrl, submitEntity, String.class);
                    System.out.println("Submit response status: " + submitResponse.getStatusCode());
                    System.out.println("Submit response body: " + submitResponse.getBody());
                } catch (HttpStatusCodeException ex) {
                    System.err.println("Error when submitting finalQuery. Status: " + ex.getStatusCode());
                    System.err.println("Response body: " + ex.getResponseBodyAsString());
                } catch (Exception ex) {
                    System.err.println("Exception while submitting finalQuery: " + ex.getMessage());
                }

            } else {
                System.err.println("generateWebhook call did not return 2xx. Status: " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException ex) {
            System.err.println("generateWebhook returned non-2xx status: " + ex.getStatusCode());
            System.err.println("Response body: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            System.err.println("Exception calling generateWebhook: " + ex.getMessage());
            ex.printStackTrace();
        }

        System.out.println("Webhook flow completed.");
    }

    private boolean isRegNoOdd(String regNo) {
        if (regNo == null) return true;
        // extract digits
        String digits = regNo.replaceAll("\\D+", "");
        if (digits.isEmpty()) return true;
        String lastTwo;
        if (digits.length() == 1) lastTwo = digits;
        else lastTwo = digits.substring(digits.length() - 2);
        int value = Integer.parseInt(lastTwo);
        return (value % 2) != 0;
    }

    private String buildFinalSqlForQuestion1() {
        // final SQL query (MySQL style) - the same one shown in the documentation
        return "SELECT p.amount AS SALARY,\n" +
               "       CONCAT(e.first_name, ' ', e.last_name) AS NAME,\n" +
               "       TIMESTAMPDIFF(YEAR, e.dob, CURDATE()) AS AGE,\n" +
               "       d.department_name AS DEPARTMENT_NAME\n" +
               "FROM payments p\n" +
               "JOIN employee e ON p.emp_id = e.emp_id\n" +
               "JOIN department d ON e.department = d.department_id\n" +
               "WHERE DAY(p.payment_time) <> 1\n" +
               "  AND p.amount = (\n" +
               "      SELECT MAX(amount) FROM payments WHERE DAY(payment_time) <> 1\n" +
               "  );";
    }
}
