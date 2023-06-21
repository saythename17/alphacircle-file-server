import com.sun.net.httpserver.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class HTTPServer {
    private HttpServer server = null;

    public HTTPServer(String host, int port) {
        try {
            this.server = HttpServer.create(new InetSocketAddress(host, port), 0);

            // API: /files
            this.server.createContext("/files", exchange -> {
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append("\"data\": [");

                File[] images = FileManager.getFilesInDirectory("images");
                File[] texts = FileManager.getFilesInDirectory("texts");
                File[] zips = FileManager.getFilesInDirectory("zips");

                generateJSONStringBy(images, "image", sb, false);
                generateJSONStringBy(texts, "text", sb, false);
                generateJSONStringBy(zips, "zip", sb, true);

                sb.append("]");
                sb.append("}");

                byte[] result = sb.toString().getBytes(StandardCharsets.UTF_8);

                OutputStream res = exchange.getResponseBody();
                Headers responseHeader = exchange.getResponseHeaders();
                responseHeader.add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, result.length);
                res.write(result);

                res.flush();
                res.close();
                exchange.close();
            });

            // API: /file/{ file-name }?type={ image | text | zip }
            this.server.createContext("/file", exchange -> {
                String[] paths = exchange.getRequestURI().getPath().split("/");
                String fileName = paths[paths.length - 1];

                String[] typeAndValue = exchange.getRequestURI().getQuery().split("=");
                String type = typeAndValue[typeAndValue.length - 1];

                switch (type) {
                    case "image" -> this.getImageFileAndResponse(fileName, exchange);
                    case "text" -> this.getTextFileAndResponse(fileName, exchange);
                    case "zip" -> this.getZipFileAndResponse(fileName, exchange);
                }
            });
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private void generateJSONStringBy(File[] files, String type, StringBuilder sb, boolean isLastArray) {
        for(int i = 0; i < files.length; i++) {
            File file = files[i];
            String[] extList = file.toString().split("\\.");
            String ext = extList[extList.length - 1];

            sb.append("    {\n");
            sb.append("      \"name\": \"").append(file.getName()).append("\",\n");
            sb.append("      \"size\": ").append(file.length()).append(",\n");
            sb.append("      \"ext\": \"").append(ext).append("\",\n");
            sb.append("      \"type\": \"").append(type).append("\"\n");
            if(!isLastArray) sb.append("},");
            else if(i < files.length - 1) sb.append("},");
            else sb.append("}");
        }
    }

    private void getImageFileAndResponse(String fileName, HttpExchange exchange) throws IOException {
        File file = FileManager.getFileInDirectory("images/" + fileName);
        String contentType = "";
        if (file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg"))
            contentType = "image/jpeg";
        else if (fileName.endsWith(".png")) contentType = "image/png";

        this.response(exchange, file, contentType);
    }

    private void getTextFileAndResponse(String fileName, HttpExchange exchange) throws IOException {
        File file = FileManager.getFileInDirectory("texts/" + fileName);
        String contentType = "";
        if (file.getName().endsWith(".txt"))
            contentType = "text/plain; charset=UTF-8";

        this.response(exchange, file, contentType);
    }

    private void getZipFileAndResponse(String fileName, HttpExchange exchange) throws IOException {
        File file = FileManager.getFileInDirectory("zips/" + fileName);
        String contentType = "";
        if (file.getName().endsWith(".zip"))
            contentType = "application/zip";

        this.response(exchange, file, contentType);
    }

    private void response(HttpExchange exchange, File file, String contentType) throws IOException {
        Headers responseHeader = exchange.getResponseHeaders();
        responseHeader.set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, file.length());

        OutputStream res = exchange.getResponseBody();
        Files.copy(file.toPath(), res);
        res.flush();
        res.close();
        exchange.close();
    }

    public void run() {
        this.server.start();
    }
}
