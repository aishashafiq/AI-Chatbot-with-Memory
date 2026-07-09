import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;

public class ChatBot {
    private final ArrayList<Message> history = new ArrayList<>();
    private final String apiKey;
    private final HttpClient client = HttpClient.newHttpClient();

    public ChatBot(String apiKey) {
        this.apiKey = apiKey;
    }
    private static final int MAX_HISTORY_SIZE = 20; // last 10 user+AI exchanges
    public void clearHistory() {
        history.clear();
    }
    private void trimHistoryIfNeeded() {
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0); // remove oldest message first
        }
    }
    public String sendMessage(String userInput) {
        // 1. Save the user's message into memory
        history.add(new Message("user", userInput));
        trimHistoryIfNeeded();

        try {
            // 2. Build JSON from the FULL history
            JsonObject requestBody = buildRequestJson();

            // 3. Send it to Gemini
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 4. Extract the AI's reply text
            String reply = extractReply(response.body());

            // 5. Save AI's reply into memory
            history.add(new Message("model", reply));

            return reply;

        } catch (java.net.ConnectException e) {
            return "⚠️ Could not connect to the internet. Please check your connection and try again.";
        } catch (java.net.http.HttpTimeoutException e) {
            return "⚠️ The request timed out. Please try again.";
        } catch (Exception e) {
            return "⚠️ Something went wrong: " + e.getMessage();
        }
    }

    private JsonObject buildRequestJson() {
        JsonObject root = new JsonObject();
        JsonArray contents = new JsonArray();

        for (Message m : history) {
            JsonObject turn = new JsonObject();
            turn.addProperty("role", m.getRole());

            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", m.getText());
            parts.add(part);

            turn.add("parts", parts);
            contents.add(turn);
        }

        root.add("contents", contents);
        return root;
    }

    private String extractReply(String jsonResponse) {
        JsonObject obj = JsonParser.parseString(jsonResponse).getAsJsonObject();

        if (obj.has("error")) {
            return "API Error: " + obj.getAsJsonObject("error").get("message").getAsString();
        }

        return obj.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();
    }
}