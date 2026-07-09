public class Message {
    private String role;
    private String text;

    public Message(String role, String text) {
        this.role = role;
        this.text = text;
    }

    public String getRole() {
        return role;
    }

    public String getText() {
        return text;
    }
}