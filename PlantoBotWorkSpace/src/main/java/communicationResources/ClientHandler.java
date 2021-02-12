package communicationResources;

import mainPackage.ClientApp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ClientHandler {

    private Socket client;
    private Thread thread;
    private BufferedReader input;
    private BufferedWriter output;

    public ClientHandler(Socket client) {
        this.client = client;
        this.thread = new Thread(new Runnable() {
            @Override
            public void run() {
                handleCommunication();
            }
        });
    }

    private void handleCommunication() {
        try {
            input = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            output = new BufferedWriter(new OutputStreamWriter(this.client.getOutputStream()));

            String messageFromClient = input.readLine();
            String[] command = messageFromClient.split("::");

            switch (command[0]) {
                case "1000":
                    //Client requests information about plants
                    String messageToClient = "1001::" + ClientApp.getInstance().getPlantInformationToProto();
                    output.write(messageToClient.concat("\n"));
                    output.flush();
                    break;
                default:
                    //Not know message from client, do nothing
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startThread() {
        thread.start();
    }
}
