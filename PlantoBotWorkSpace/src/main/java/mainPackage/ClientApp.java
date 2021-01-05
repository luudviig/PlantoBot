package mainPackage;


import models.Plant;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

public class ClientApp {

    private HashMap<Integer, Plant> plants;
    private final Object lockPlants;
    private final Object lockClose;
    private volatile boolean terminate;
    private Thread pollingThread;

    // When run from IDE
    private static final String plants_JSON = (new File(System.getProperty("user.dir")).getParentFile().getPath()).concat("/plants.json");

    // When run as JAR on Linux
    //private static final String plants_JSON = "./plants.json";

    private static ClientApp instance = null;

    public static ClientApp getInstance() {
        if (instance == null) {
            instance = new ClientApp();
        }
        return instance;
    }

    private ClientApp() {
        this.plants = new HashMap<>();
        this.lockPlants = new Object();
        this.lockClose = new Object();
        this.terminate = false;

        this.pollingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                pollPlants();
            }
        });
    }

    public void startServer() {
        System.out.println("Server Started \n");
        try {
            //Read from file
            readPlantsFile();

            //Start polling thread
            //pollingThread.start();

            //Wait from input from android

            printAllPlants();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private void pollPlants() {

    }

    private void inputFromClients() {

    }

    public void closeServer() {
        synchronized (lockClose) {
            if (!terminate) {
                terminate = true;
                //TODO close communication with all clients. Close polling thread?
            }
        }
    }

    private void readPlantsFile() throws Exception {
        try {
            JSONParser parser = new JSONParser();
            JSONArray array = (JSONArray) parser.parse(new FileReader(plants_JSON));
            for (Object object : array) {
                JSONObject plant = (JSONObject) object;

                String ip = (String) plant.get("ip");
                int port = Integer.parseInt(String.valueOf(plant.get("port")));
                int pollDelaySec = Integer.parseInt(String.valueOf(plant.get("pollDelaySec")));
                int soilHumidLimit= Integer.parseInt(String.valueOf(plant.get("soilHumidLimit")));
                String alias = (String) plant.get("alias");
                int id = Integer.parseInt(String.valueOf(plant.get("id")));

                Plant newPlant = new Plant(ip, port, pollDelaySec, soilHumidLimit, alias, id);
                plants.put(id, newPlant);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }

    private void printAllPlants(){
        if (!plants.isEmpty()) {
            System.out.println("=== ALL Plants ===");
            for (int key : plants.keySet()) {
                System.out.println("Alias: " + plants.get(key).getAlias() + "\n" +
                        "State: " + plants.get(key).getSoilHumidLimit() + "\n" + "Present: " + plants.get(key).isPresent());
            }
            System.out.println("====================");
        }
    }
}
