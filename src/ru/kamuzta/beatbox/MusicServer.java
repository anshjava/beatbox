package ru.kamuzta.beatbox;

import java.io.*;
import java.net.*;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class MusicServer {
    private ServerSocket serverSocket;
    private ArrayList<ObjectOutputStream> clientOutputStreams;

    public MusicServer(int port) {
        this.clientOutputStreams = new ArrayList<>();
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " Have no port argument, used default port parametr");
            port = 4242;
        }
        MusicServer ms = new MusicServer(port);
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " Server started on port: " + port);
        ms.setupNetwork();
    }

    private void setupNetwork() {
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " waiting for connections...");
        while (true) {
            try {
                //замираем до момента подключения нового клиента
                Socket clientSocket = serverSocket.accept();
                //для обработки потоков для каждого клиента создаем параллельный Thread с заданием ClientHandler
                Thread t = new Thread(new ClientHandler(clientSocket));
                t.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //метод транслируем сообщение(два объекта) на исходящий поток каждого подключенного клиента
    private void tellEveryone(Message msg) {
        try {
            for (ObjectOutputStream clientOut : clientOutputStreams) {
                clientOut.writeObject(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    class ClientHandler implements Runnable {
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            try {
                this.clientSocket = socket;
                this.in = new ObjectInputStream(clientSocket.getInputStream());
                //создаем исходящий поток на нового клиента и помещаем его в лист исх.потоков
                this.out = new ObjectOutputStream(clientSocket.getOutputStream());
                clientOutputStreams.add(out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            Message msg;
            String clientName = "";
            try {
                clientName = (String) in.readObject();
                System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " " + clientName + " connected");

                while ((msg = (Message) in.readObject()) != null) {
                    System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " got message from " + msg.getSenderName());
                    tellEveryone(msg);
                }
            } catch (SocketException | EOFException se) {
                System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " " + clientName + " disconnected");
                clientOutputStreams.remove(out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}