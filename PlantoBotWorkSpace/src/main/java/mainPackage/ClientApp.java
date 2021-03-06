package mainPackage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import communicationResources.ClientHandler;
import models.Plant;
import models.Settings;
import models.SoilHumidityLimit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.FileWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ClientApp {

    public HashMap<Integer, Plant> plants;
    private final Object lockPlants;
    private final Object lockClose;
    private volatile boolean terminate;
    private Thread pollingThread;
    private ServerSocket serverSocket;
    public Settings settings;

    //TODO Fix so when a client connects it has 2seconds to send a request according to the protocol or it is thrown out.

    // When run from IDE
    //private static final String plants_JSON = (new File(System.getProperty("user.dir")).getParentFile().getPath()).concat("/plants.json");
    //private static final String config_JSON = (new File(System.getProperty("user.dir")).getParentFile().getPath()).concat("/config.json");

    // When run as JAR on Linux
    private static final String plants_JSON = "./plants.json";
    private static final String config_JSON = "./config.json";


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
        try {
            serverSocket = new ServerSocket(7777);
            serverSocket.setReuseAddress(true);
        } catch (Exception e) {
            e.printStackTrace();
        }


        this.pollingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                pollPlants();
            }
        });
    }

    public void startServer() {
        System.out.println("---SERVER STARTED---\n");
        try {
            //Read from files
            readPlantsFile();
            readSettingsFile();

            //Start polling thread
            pollingThread.start();

            //Print settings
            printSettings();
            //Print all plants
            printAllPlants();

            //Wait from input from android
            inputFromClients();
        } catch (Exception e) {
            e.printStackTrace();
            debugLog(e.getMessage());
        }
    }

    private void pollPlants() {
        while (!terminate) {
            int nbrOfPlants;
            int[] plantKeys;
            synchronized (lockPlants) {
                plantKeys = new int[plants.size()];
                nbrOfPlants = plantKeys.length;
                int counter = 0;
                for (int i : plants.keySet()) {
                    plantKeys[counter] = i;
                    counter++;
                }
            }

            for (int i = 0; i < nbrOfPlants; i++) {
                synchronized (lockPlants) {
                    Plant plant = plants.get(plantKeys[i]);
                    if (plant.isEnabled()) {
                        long currentMillis = System.currentTimeMillis();
                        boolean presentBefore = plant.isPresent();
                        double[] stateBefore = plant.getState();

                        //Check if gadget needs polling
                        if ((currentMillis - plant.getLastPollTime()) > (plant.getPollDelaySec() * 1000L)) {
                            try {
                                plant.poll();

                                if (plant.isPresent()) {
                                    plant.setLastPollTime(System.currentTimeMillis());
                                }

                                if (presentBefore != plant.isPresent()) {
                                    if (plant.isPresent()) {
                                        //Plant has become available again
                                        //Could notify clients here if system were full duplex

                                    } else {
                                        //Plant is no longer available
                                        //Could notify clients here if system were full duplex
                                    }
                                } else if (plant.isPresent()) {
                                    if (stateBefore != plant.getState()) {
                                        //Could notify clients here if system were full duplex
                                    }
                                    if (needsWatering(plant)) {
                                        plant.water();
                                        updateLastWateredInJson(plant.getId(), String.valueOf(plant.lastMillisToSimpleDate(plant.getLastWatered())));
                                    }
                                }
                            } catch (Exception e) {
                                debugLog("Problem polling the plant");
                            }
                            printPresentPlants();
                        }
                    }
                }
            }
            try {
                Thread.sleep(50);
            } catch (Exception e) {
                debugLog(getInstance().getPlantInformationToProto());
            }
        }
    }

    private boolean needsWatering(Plant plant) {
        boolean needsWater = false;
        if (plant.getSoilHumidLimit().equals(SoilHumidityLimit.DRY)) {
            //DRY
            if (plant.getState()[0] <= plant.getDryLimit()) {
                needsWater = true;
            }
        } else if (plant.getSoilHumidLimit().equals(SoilHumidityLimit.MOIST)) {
            //MOIST
            if (plant.getState()[0] <= plant.getMoistLimit()) {
                needsWater = true;
            }
        } else if (plant.getSoilHumidLimit().equals(SoilHumidityLimit.WET)) {
            //WET
            if (plant.getState()[0] <= plant.getWetLimit()) {
                needsWater = true;
            }
        }
        return needsWater;
    }

    private void inputFromClients() {
        while (!terminate) {
            try {
                Socket client = serverSocket.accept();
                debugLog("New Client Detected: " + client.getInetAddress().toString().split("/")[1]);

                //Launches each client on a new thread that handles the request.
                ClientHandler clientHandler = new ClientHandler(client);
                clientHandler.startThread();
            } catch (Exception e) {
                debugLog(e.getMessage());
            }
        }
    }

    public String getPlantInformationToProto() {
        StringBuilder protocolString = new StringBuilder();
        int amountOfPlants = 0;
        synchronized (lockPlants) {
            for (int plant : plants.keySet()) {
                if (plants.get(plant).isPresent() && plants.get(plant).isEnabled()) {
                    amountOfPlants++;
                    protocolString.append(plants.get(plant).toHosoProtocol());
                }
            }
        }
        return amountOfPlants + "::" + protocolString;
    }

    public void debugLog(String message) {
        if (settings.isDebugMode()) {
            System.out.println(message);
        }
    }

    public void closeServer() {
        synchronized (lockClose) {
            if (!terminate) {
                terminate = true;
            }
        }
    }

    private void updateLastWateredInJson(int plantID, String newDate) throws Exception {
        try {
            JSONParser parser = new JSONParser();
            JSONArray array = (JSONArray) parser.parse(new FileReader(plants_JSON));
            for (Object object : array) {
                JSONObject plant = (JSONObject) object;
                if ((Long) plant.get("id") == plantID) {
                    plant.put("lastWatered", newDate);
                    break;
                }
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String prettyJson = gson.toJson(array);
            try (FileWriter writer = new FileWriter(plants_JSON)) {
                writer.write(prettyJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
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
                SoilHumidityLimit soilHumidLimit = SoilHumidityLimit.valueOf(String.valueOf(plant.get("soilHumidLimit")));
                String alias = (String) plant.get("alias");
                int id = Integer.parseInt(String.valueOf(plant.get("id")));
                boolean enable = (Boolean) plant.get("enable");
                double openWaterFlowSec = Double.parseDouble(String.valueOf(plant.get("openWaterFlowSec")));
                double waterTankHeight = Double.parseDouble(String.valueOf(plant.get("waterTankHeight")));
                String lastWatered = (String) plant.get("lastWatered");
                double maxWatersPerDay = Double.parseDouble(String.valueOf(plant.get("maxWatersPerDay")));
                double wetLimit = Double.parseDouble(String.valueOf(plant.get("wetLimit")));
                double moistLimit = Double.parseDouble(String.valueOf(plant.get("moistLimit")));
                double dryLimit = Double.parseDouble(String.valueOf(plant.get("dryLimit")));

                Plant newPlant = new Plant(ip, port, pollDelaySec, soilHumidLimit, alias, id, enable, openWaterFlowSec, waterTankHeight,
                        lastWatered, maxWatersPerDay, wetLimit, moistLimit, dryLimit);
                plants.put(id, newPlant);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Problem reading plants file");
        }
    }

    private void readSettingsFile() throws Exception {
        try {
            JSONParser parser = new JSONParser();
            JSONObject plant = (JSONObject) parser.parse(new FileReader(config_JSON));

            boolean debugMode = (Boolean) (plant.get("debugMode"));
            String alias = (String) plant.get("hubAlias");

            settings = new Settings(debugMode, alias, System.currentTimeMillis());

        } catch (Exception e) {
            throw new Exception("Problems reading settings file");
        }
    }

    private void printAllPlants() {
        if (!plants.isEmpty()) {
            debugLog("\n\n=== ALL Plants ===");
            for (int key : plants.keySet()) {
                debugLog(plants.get(key).getPlantInfo());
                debugLog("\n\n");
            }
            debugLog("====================\n\n");
        }
    }

    private void printPresentPlants() {
        if (!plants.isEmpty()) {
            debugLog("\n\n=== ALL Plants ===");
            for (int key : plants.keySet()) {
                if (plants.get(key).isPresent()) {
                    debugLog(plants.get(key).getPlantInfo() + "\n");
                }
            }
            debugLog("====================\n\n");
        }
    }

    private void printSettings() {
        debugLog("\n\n=== SETTINGS ===");
        debugLog(settings.printSettingsInfo());
        debugLog("=================\n\n");
    }
}
