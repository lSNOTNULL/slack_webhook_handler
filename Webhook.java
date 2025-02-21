import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Webhook {
    public static void main(String[] args) {
        String prompt = System.getenv("LLM_PROMPT");
        String llmResult = useLLM(prompt);
        System.out.println("llmResult = " + llmResult);
        String template = System.getenv("LLM2_IMAGE_TEMPLATE");
        String imagePrompt = template.formatted(llmResult);
        System.out.println("imagePrompt = " + imagePrompt);
        String llmImageResult = useLLMForImage(imagePrompt);
        System.out.println("llmImageResult = " + llmImageResult);
        String title = System.getenv("SLACK_WEBHOOK_TITLE");
        sendSlackMessage(title, llmResult, llmImageResult);
    }

    public static String useLLMForImage(String prompt) {
        String apiUrl = System.getenv("LLM2_API_URL");
        String apiKey = System.getenv("LLM2_API_KEY");
        String model = System.getenv("LLM2_MODEL");
        String payload = """
                {
                  "prompt": "%s",
                  "model": "%s",
                  "width": 1440,
                  "height": 1440,
                  "steps": 4,
                  "n": 1
                }
                """.formatted(prompt, model);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        String result = null;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());

            if(response.statusCode() >= 200 && response.statusCode()<300) {
                String[] parts1 = response.body.split("url\" : \"");
                if (parts1.length >1) {
                    String resultUrlPart = parts1[1];
                    String[] parts2 = resultUrlPart.split("\",");
                    result = parts2[0];
                } else {
                    System.err.println("Error: 'url\": \"' not found in response body. Response body: " + response.body());
                result = null; // 또는 오류 처리
                }
            }else {
            System.err.println("HTTP request failed with status code: " + response.statusCode());
            System.err.println("Response body: " + response.body());
            result = null; // 또는 오류 처리
           // result = response.body().split("url\": \"")[1].split("\",")[0];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static String useLLM(String prompt) {
        String apiUrl = System.getenv("LLM_API_URL");
        String apiKey = System.getenv("LLM_API_KEY");
        String model = System.getenv("LLM_MODEL");
        String payload = """
                {
                  "messages": [
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "model": "%s"
                }
                """.formatted(prompt, model);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        String result = null;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) { // HTTP 성공 상태 코드 검사
            String[] parts1 = response.body().split("\"content\":\"");
            if (parts1.length > 1) { // 배열 길이 검사: 길이가 2 이상일 때만 인덱스 1 접근
                String resultContentPart = parts1[1];
                String[] parts2 = resultContentPart.split("\"},\"logprobs\"");
                result = parts2[0];
            } else {
                System.err.println("Error: '\"content\":\"' not found in response body. Response body: " + response.body());
                result = null; // 또는 오류 처리
            }
        } else {
            System.err.println("HTTP request failed with status code: " + response.statusCode());
            System.err.println("Response body: " + response.body());
            result = null; // 또는 오류 처리
        }


           // result = response.body().split("\"content\":\"")[1].split("\"},\"logprobs\"")[0];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static void sendSlackMessage(String title, String text, String imageUrl) {
        String slackUrl = System.getenv("SLACK_WEBHOOK_URL");
        String payload = """
                    {"attachments": [{
                        "title": "%s",
                        "text": "%s",
                        "image_url": "%s"
                    }]}
                """.formatted(title, text, imageUrl);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(slackUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}