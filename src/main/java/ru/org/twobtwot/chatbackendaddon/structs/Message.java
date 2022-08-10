package ru.org.twobtwot.chatbackendaddon.structs;

public class Message {
    public String username;
    public String message;
    public String receiver;

    public Message(String username, String message, String receiver) {
        this.username = username;
        this.message = message;
        this.receiver = receiver;
    }

    public Message(String username, String message) {
        this(username, message, null);
    }
}
