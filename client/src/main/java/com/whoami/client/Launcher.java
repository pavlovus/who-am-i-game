package com.whoami.client;

/**
 * The Launcher hack for JavaFX.
 * By having a separate class with a main method that calls Application.launch,
 * we bypass the strict module-path requirements for JavaFX in Java 11+.
 */
public class Launcher {
    public static void main(String[] args) {
        MainClient.main(args);
    }
}
