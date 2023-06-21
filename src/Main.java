public class Main {
    public static void main(String[] args) {
        HTTPServer server = new HTTPServer("0.0.0.0", 4000);
        server.run();
    }
}