import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

public class ChatWindow extends JFrame {
    private final ChatBot bot;
    private final JTextPane chatArea;
    private final JTextField inputField;
    private final JButton sendButton;
    private final JButton newChatButton;
    private final JLabel statusLabel;

    public ChatWindow(ChatBot bot) {
        this.bot = bot;

        setTitle("AI Chatbot");
        setSize(550, 650);
        setMinimumSize(new Dimension(400, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ---------- Menu Bar ----------
        setJMenuBar(buildMenuBar());

        // ---------- Header ----------
        JLabel title = new JLabel("  🤖 AI Chatbot", SwingConstants.LEFT);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        title.setOpaque(true);
        title.setBackground(new Color(41, 98, 255));
        title.setForeground(Color.WHITE);
        add(title, BorderLayout.NORTH);

        // ---------- Chat Display ----------
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // ---------- Bottom Panel (status + input row) ----------
        JPanel bottomContainer = new JPanel(new BorderLayout());

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        bottomContainer.add(statusLabel, BorderLayout.NORTH);

        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));

        sendButton = new JButton("Send ➤");
        newChatButton = new JButton("🗑 New Chat");

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.add(sendButton);
        buttonPanel.add(newChatButton);

        JPanel inputRow = new JPanel(new BorderLayout(5, 0));
        inputRow.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        inputRow.add(inputField, BorderLayout.CENTER);
        inputRow.add(buttonPanel, BorderLayout.EAST);

        bottomContainer.add(inputRow, BorderLayout.CENTER);
        add(bottomContainer, BorderLayout.SOUTH);

        // ---------- Actions ----------
        sendButton.addActionListener(e -> handleSend());
        inputField.addActionListener(e -> handleSend());
        newChatButton.addActionListener(e -> handleNewChat());

        appendMessage("System", "Chatbot ready. Ask me anything!", new Color(100, 100, 100));
        inputField.requestFocusInWindow();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem newChatItem = new JMenuItem("New Chat");
        JMenuItem exitItem = new JMenuItem("Exit");
        newChatItem.addActionListener(e -> handleNewChat());
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(newChatItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "AI Chatbot with Memory\nBuilt in Java using the Gemini API.",
                "About",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void handleSend() {
        String userText = inputField.getText().trim();
        if (userText.isEmpty()) return;

        appendMessage("You", userText, new Color(20, 90, 200));
        inputField.setText("");
        setInputEnabled(false);
        statusLabel.setText("AI is typing...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return bot.sendMessage(userText);
            }

            @Override
            protected void done() {
                try {
                    String reply = get();
                    appendMessage("AI", reply, new Color(20, 140, 80));
                } catch (Exception ex) {
                    appendMessage("AI", "⚠️ Unexpected error occurred.", Color.RED);
                } finally {
                    statusLabel.setText(" ");
                    setInputEnabled(true);
                    inputField.requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void handleNewChat() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Start a new chat? This will clear the current conversation.",
                "New Chat",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            bot.clearHistory();
            chatArea.setText("");
            appendMessage("System", "New chat started.", new Color(100, 100, 100));
        }
    }

    private void setInputEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
    }

    private void appendMessage(String sender, String message, Color color) {
        StyledDocument doc = chatArea.getStyledDocument();

        SimpleAttributeSet boldStyle = new SimpleAttributeSet();
        StyleConstants.setBold(boldStyle, true);
        StyleConstants.setForeground(boldStyle, color);

        SimpleAttributeSet normalStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(normalStyle, Color.BLACK);

        try {
            doc.insertString(doc.getLength(), sender + ": ", boldStyle);
            doc.insertString(doc.getLength(), message + "\n\n", normalStyle);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        chatArea.setCaretPosition(doc.getLength()); // auto-scroll to bottom
    }

    public static void main(String[] args) throws IOException {
        String apiKey = loadApiKey();
        ChatBot bot = new ChatBot(apiKey);

        SwingUtilities.invokeLater(() -> {
            ChatWindow window = new ChatWindow(bot);
            window.setVisible(true);
        });
    }

    private static String loadApiKey() throws IOException {
        Properties props = new Properties();
        props.load(Files.newInputStream(Paths.get("src/config.properties")));
        return props.getProperty("GEMINI_API_KEY");
    }
}