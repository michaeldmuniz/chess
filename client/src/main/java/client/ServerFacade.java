package client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServerFacade {

    private final String baseUrl;
    private String authToken;

    public ServerFacade(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    public void clear() {
    }

    private String request(String method, String path) {
        HttpURLConnection conn = null;
        try {
            var url = new URL(baseUrl + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Accept", "application/json");

            // Add auth header later when we log in
            if (authToken != null) {
                conn.setRequestProperty("Authorization", authToken);
            }

            conn.connect();

            int code = conn.getResponseCode();
            if (code / 100 != 2) {
                throw new RuntimeException("Request failed with code " + code);
            }

            return readAll(conn.getInputStream());

        } catch (IOException e) {
            throw new RuntimeException("HTTP problem: " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        var sb = new StringBuilder();
        var buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) > 0) {
            sb.append(new String(buf, 0, n));
        }
        return sb.toString();
    }
}
