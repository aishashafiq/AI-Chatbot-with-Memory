import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        String apiKey = loadApiKey();
        ChatBot bot = new ChatBot(apiKey);
        Scanner scanner = new Scanner(System.in);

        System.out.println("Chatbot ready. Type 'exit' to quit.");

        while (true) {
            System.out.print("You: ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                break;
            }

            String reply = bot.sendMessage(input);
            System.out.println("AI: " + reply);
        }
    }

    private static String loadApiKey() throws IOException {
        Properties props = new Properties();
        props.load(Files.newInputStream(Paths.get("src/config.properties")));
        return props.getProperty("GEMINI_API_KEY");
    }
}