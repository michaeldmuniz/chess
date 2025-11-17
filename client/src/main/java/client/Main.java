package client;

import client.ui.*;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String url = "http://localhost:8080";
        ServerFacade server = new ServerFacade(url);

        PreloginUI prelogin = new PreloginUI(server, scanner);

        boolean quit = false;
        boolean loggedIn = false;
        String authToken = null;
        String username = null;

        System.out.println("👑 Welcome to 240 chess. Type help to begin.");

        PostloginUI postlogin = null;

        while (!quit) {

            if (!loggedIn) {
                PreloginResult res = prelogin.runOnce();

                if (res.quit) {
                    quit = true;
                }

                if (res.loggedIn) {
                    loggedIn = true;
                    authToken = res.authToken;
                    username = res.username;


                    postlogin = new PostloginUI(server, scanner, username);
                }

            }


            else {
                PostloginResult res = postlogin.runOnce(authToken);

                if (res.quit) {
                    quit = true;
                }

                if (res.loggedOut) {
                    loggedIn = false;
                    authToken = null;
                    username = null;
                    postlogin = null;  // reset
                }

            }
        }

        System.out.println("Goodbye!");
    }
}
