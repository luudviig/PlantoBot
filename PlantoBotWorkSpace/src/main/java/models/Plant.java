package models;

import mainPackage.ClientApp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Plant {

    private BufferedWriter output;
    private BufferedReader input;
    private Socket socket;
    private String alias;
    private int id;
    private String ip;
    private int port;
    private int pollDelaySec;
    private double openWaterFlowSec;
    private SoilHumidityLimit soilHumidLimit;
    private long lastWatered;
    private double[] state;
    private long lastPollTime;
    private volatile boolean isPresent;
    private volatile boolean enabled;
    private volatile boolean waterTankEmpty;
    private double waterTankHeight;
    private double distanceToWater;


    public Plant(String ip, int port, int pollDelaySec, SoilHumidityLimit soilHumidLimit, String alias, int id, boolean enabled, double openWaterFlowSec, double waterTankHeight) {
        this.ip = ip;
        this.port = port;
        this.pollDelaySec = pollDelaySec;
        this.soilHumidLimit = soilHumidLimit;
        this.alias = alias;
        this.id = id;
        this.lastPollTime = 0;
        this.lastWatered = 0;
        this.isPresent = false;
        this.enabled = enabled;
        this.openWaterFlowSec = openWaterFlowSec;
        this.state = new double[4]; //In the following order; {soilHumidity, airHumidity, airTemp, lightExposure}
        this.waterTankEmpty = false;
        this.waterTankHeight = waterTankHeight;
        this.distanceToWater = 0;
    }

    public void poll() {
        try {
            String response = sendCommand("{\"command\": 1003}");

            String[] splittedRequest = response.split("::");
            if (splittedRequest[0].equalsIgnoreCase("1004")) {
                setState(new double[]{Double.parseDouble(splittedRequest[1]), Double.parseDouble(splittedRequest[2]),
                        Double.parseDouble(splittedRequest[3]), Double.parseDouble(splittedRequest[4])});
                distanceToWater = Double.parseDouble(splittedRequest[5]);

                //Calculate If water tank empty
                calculateWaterTankEmpty();

                setPresent(true);
            }
        } catch (Exception e) {
            setPresent(false);
        }
    }

    private String sendCommand(String command) throws Exception {
        try {
            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(this.ip, this.port), 2500);
            this.socket.setSoTimeout(4000);
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            this.output.write(command.concat("\n"));
            this.output.flush();

            return input.readLine();
        } catch (Exception e) {
            throw new Exception("Unable to communicate with plant.");
        } finally {
            if (this.socket != null) {
                this.socket.close();
            }
        }
    }

    public void water() throws Exception {
        //Different limits for testing.
        //long limitDay = 86_400 * 1000;
        //long halfDay = limitDay / 2;
        //long limit5Minutes =300*1000;
        //long limit10Minutes = 600*1000;
        long limit10Seconds = 10*1000;
        try {
            if (!calculateWaterTankEmpty() && (System.currentTimeMillis() - lastWatered >= limit10Seconds)) { //This makes so the plant is only allowed to be watered once in 24h.
                ClientApp.getInstance().debugLog("Watering plant. " + lastMillisToSimpleDate(System.currentTimeMillis()));

                String[] response = sendCommand("{\"command\":1005, \"openWaterFlowSec\":" + this.openWaterFlowSec + "}").split("::");

                if (response[0].equalsIgnoreCase("1004")) {
                    setState(new double[]{Double.parseDouble(response[1]), Double.parseDouble(response[2]),
                            Double.parseDouble(response[3]), Double.parseDouble(response[4])});
                    distanceToWater = Double.parseDouble(response[5]);
                    setPresent(true);
                }
                lastWatered = System.currentTimeMillis();
            }
        } catch (Exception e) {
            setPresent(false);
        }
    }

    public boolean calculateWaterTankEmpty() {
        if (distanceToWater >= (waterTankHeight - 4)) { //4cm of marginal to not destroy the water pump
            waterTankEmpty = true;
            return true;
        } else {
            waterTankEmpty = false;
            return false;
        }
    }

    public String lastMillisToSimpleDate(long millis) {
        // Millis to date
        Date resultDate = new Date(millis);
        String pattern = "yyyy-MM-dd HH:mm";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(resultDate);
    }

    public boolean isWaterTankEmpty() {
        return waterTankEmpty;
    }

    public void setWaterTankEmpty(boolean waterTankEmpty) {
        this.waterTankEmpty = waterTankEmpty;
    }

    public double getWaterTankHeight() {
        return waterTankHeight;
    }

    public void setWaterTankHeight(double waterTankHeight) {
        this.waterTankHeight = waterTankHeight;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPollDelaySec() {
        return pollDelaySec;
    }

    public void setPollDelaySec(int pollDelaySec) {
        this.pollDelaySec = pollDelaySec;
    }

    public SoilHumidityLimit getSoilHumidLimit() {
        return soilHumidLimit;
    }

    public void setSoilHumidLimit(SoilHumidityLimit soilHumidLimit) {
        this.soilHumidLimit = soilHumidLimit;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getLastPollTime() {
        return lastPollTime;
    }

    public void setLastPollTime(long lastPollTime) {
        this.lastPollTime = lastPollTime;
    }

    public long getLastWatered() {
        return lastWatered;
    }

    public void setLastWatered(long lastWatered) {
        this.lastWatered = lastWatered;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public void setPresent(boolean present) {
        isPresent = present;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double[] getState() {
        return state;
    }

    public void setState(double[] state) {
        this.state = state;
    }

    public double getOpenWaterFlowSec() {
        return openWaterFlowSec;
    }

    public void setOpenWaterFlowSec(double openWaterFlowSec) {
        this.openWaterFlowSec = openWaterFlowSec;
    }

    public String getPlantInfo() {
        return "SoilHumidity: " + state[0] + "\n" +
                "AirHumidity: " + state[1] + "\n" +
                "AirTemp: " + state[2] + "\n" +
                "LightExposure: " + state[3] + "\n" +
                "IP: " + this.ip + "\n" +
                "ID: " + this.id + "\n" +
                "PORT: " + this.port + "\n" +
                "Alias: " + this.alias + "\n" +
                "Last watered: " + lastMillisToSimpleDate(lastWatered) + "\n" +
                "Last polled: " + lastMillisToSimpleDate(lastPollTime) + "\n" +
                "OpenWaterFlowSec: " + this.openWaterFlowSec + "\n" +
                "PollDelaySec: " + this.pollDelaySec + "\n" +
                "Is present: " + this.isPresent + "\n" +
                "Enabled: " + this.enabled + "\n" +
                "SoilHumidLimit: " + this.soilHumidLimit + "\n" +
                "Empty Water Tank: " + this.waterTankEmpty + "\n" +
                "Water Water Height: " + this.waterTankHeight + " cm";
    }

    public String toHosoProtocol() {
        return id + "::" + alias + "::" + lastMillisToSimpleDate(lastWatered) + "::" + lastMillisToSimpleDate(lastPollTime) + "::" + stateToProto() + "::" + ClientApp.getInstance().settings.getAlias() + "::" + waterTankEmpty;
    }

    public String stateToProto() {
        return state[0] + "::" + state[1] + "::" + state[2] + "::" + state[3];
    }
}
