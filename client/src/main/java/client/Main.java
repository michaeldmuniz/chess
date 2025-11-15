package client;

import client.ui.PreloginUI;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String serverUrl = "http://localhost:8080";
        ServerFacade server = new ServerFacade(serverUrl);

        PreloginUI prelogin = new PreloginUI(server, scanner);

        System.out.println("👑 Welcome to 240 chess. Type Help to get started. 👑");

        boolean quit = false;
        while (!quit) {
            quit = prelogin.runOnce();
        }

        System.out.println("Goodbye!");
    }
}
