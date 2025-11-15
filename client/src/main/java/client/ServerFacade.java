package client;

import client.dto.*;
import model.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ServerFacade {
    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public RegisterResponse register(RegisterRequest req) throws IOException {
        var conn = makeConnection("/user", "POST");
        conn.setRequestProperty("Content-Type", "application/json");

        // Sends the RegisterRequest DTO as JSON
        writeJsonBody(conn, req);

        var status = conn.getResponseCode();
        var body = readBody(conn);

        if (status >= 200 && status < 300) {
            // Converts the JSON response from the server into a RegisterResponse DTO
            return gson.fromJson(body, RegisterResponse.class);
        } else {
            throw new IOException("register failed: " + status + " -> " + body);
        }
    }
    public LoginResponse login(LoginRequest req) throws IOException {
        var conn = makeConnection("/session", "POST");

        try (var out = conn.getOutputStream()) {
            out.write(gson.toJson(req).getBytes());
        }

        conn.connect();
        int status = conn.getResponseCode();

        // Read whatever the server sent back. If the status was OK, read the normal stream;
        // otherwise read the error stream. Turn the bytes into a string.
        String raw;
        try (var in = (status == HttpURLConnection.HTTP_OK)
                ? conn.getInputStream()
                : conn.getErrorStream()) {
            raw = new String(in.readAllBytes());
        }

        if (status == HttpURLConnection.HTTP_OK) {
            return gson.fromJson(raw, LoginResponse.class);
        }

        var msg = gson.fromJson(raw, Map.class).get("message").toString();

        throw new IOException(msg);
    }



    public LogoutResponse logout(String authToken) throws IOException {
        if (authToken == null || authToken.isBlank()) {
            throw new IOException("Error: bad request");
        }

        var conn = makeConnection("/session", "DELETE");
        conn.addRequestProperty("Authorization", authToken);

        try {
            conn.connect();
            int status = conn.getResponseCode();

            if (status == HttpURLConnection.HTTP_OK) {
                return new LogoutResponse("ok");
            }

            String body = new String(conn.getErrorStream().readAllBytes());
            var msg = gson.fromJson(body, Map.class).get("message");

            throw new IOException(msg.toString());

        } finally {
            conn.disconnect();
        }
    }

    public CreateGameResponse createGame(CreateGameRequest req) throws IOException {

        // Pull out the auth token and game name from the DTO
        String auth = req.authToken();
        String gameName = req.gameName();

        // Make sure both values exist and aren't empty
        if (auth == null || auth.isBlank() ||
                gameName == null || gameName.isBlank()) {
            throw new IOException("Error: bad request");
        }

        // Make a POST request to /game
        var conn = makeConnection("/game", "POST");

        // Tell the server we are sending JSON and include the auth token
        conn.addRequestProperty("Content-Type", "application/json");
        conn.addRequestProperty("Authorization", auth);

        // Send the gameName in the request body as JSON
        try (var out = conn.getOutputStream()) {
            var json = gson.toJson(Map.of("gameName", gameName));
            out.write(json.getBytes());
        }

        // Ask the server for the status code after sending the request
        int status = conn.getResponseCode();

        // If the server says OK (200), read its response and turn it into a DTO
        if (status == HttpURLConnection.HTTP_OK) {
            try (var in = conn.getInputStream()) {
                var body = new String(in.readAllBytes());
                return gson.fromJson(body, CreateGameResponse.class);
            }
        }

        // If not OK, read the error message and throw it as an exception
        try (var err = conn.getErrorStream()) {
            if (err != null) {
                var body = new String(err.readAllBytes());
                var error = gson.fromJson(body, Map.class);
                throw new IOException(error.get("message").toString());
            }
        }

        // Should never happen but catches unexpected cases
        throw new IOException("Error: unexpected failure");
    }

    public ListGamesResponse listGames(ListGamesRequest req) throws IOException {
        // Pull out the authToken from the DTO
        String authToken = req.authToken();

        // Make GET /game call
        var conn = makeConnection("/game", "GET");
        conn.addRequestProperty("Authorization", authToken);

        int status = conn.getResponseCode();

        // Success: parse the JSON response directly into ListGamesResponse
        if (status == HttpURLConnection.HTTP_OK) {
            try (var in = conn.getInputStream()) {
                var body = new String(in.readAllBytes());
                return gson.fromJson(body, ListGamesResponse.class);
            }
        }

        // Error case: read the message and throw it
        throwErrorFromConnection(conn);

        throw new IOException("Error: unexpected failure");
    }

    public JoinGameResponse joinGame(JoinGameRequest req, String authToken) throws IOException {

        // Make sure auth token was provided
        if (authToken == null || authToken.isBlank()) {
            throw new IOException("Error: bad request");
        }

        // Open PUT /game connection
        var conn = makeConnection("/game", "PUT");
        conn.addRequestProperty("Content-Type", "application/json");
        conn.addRequestProperty("Authorization", authToken);

        // Write the JoinGameRequest as JSON
        try (var out = conn.getOutputStream()) {
            var json = gson.toJson(req);
            out.write(json.getBytes());
        }

        // Send request and get the status
        int status = conn.getResponseCode();

        // If success, return an empty JoinGameResponse DTO
        if (status == HttpURLConnection.HTTP_OK) {
            return new JoinGameResponse();
        }

        // If failure, read the error message and throw it
        throwErrorFromConnection(conn);

        throw new IOException("Error: unexpected failure");
    }

    public void clear() throws IOException {
        var conn = makeConnection("/db", "DELETE");
        try {
            conn.connect();
            var status = conn.getResponseCode();

            if (status != HttpURLConnection.HTTP_OK) {
                System.out.println("Clear failed. Status: " + status);
            } else {
                System.out.println("Database cleared successfully.");
            }
        } finally {
            conn.disconnect();
        }
    }

    private void writeJsonBody(HttpURLConnection conn, Object payload) throws IOException {
        byte[] jsonBytes = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);

        conn.setFixedLengthStreamingMode(jsonBytes.length);

        try (var out = new BufferedOutputStream(conn.getOutputStream())) {
            out.write(jsonBytes);
        }
    }

    private String readBody(HttpURLConnection conn) throws IOException {
        InputStream stream = null;

        try {
            if (conn.getResponseCode() < 400) {
                stream = conn.getInputStream();
            } else {
                stream = conn.getErrorStream();
            }
        } catch (Exception ignored) {
            stream = conn.getErrorStream();
        }

        if (stream == null) {
            return "";
        }

        try (var in = new BufferedInputStream(stream);
             var reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             var buffer = new StringWriter()) {

            reader.transferTo(buffer);
            return buffer.toString();
        }
    }

    private HttpURLConnection makeConnection(String path, String method) throws IOException {
        var url = new URL(serverUrl + path);
        var conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoOutput(true);
        conn.addRequestProperty("Accept", "application/json");
        return conn;
    }

    private void throwErrorFromConnection(HttpURLConnection conn) throws IOException {
        try (var err = conn.getErrorStream()) {
            if (err != null) {
                var body = new String(err.readAllBytes());
                var error = gson.fromJson(body, Map.class);
                throw new IOException(error.get("message").toString());
            }
        }
    }

}
