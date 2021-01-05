package models;

public class Plant {
    private String ip;
    private int port;
    private int pollDelaySec;
    private int soilHumidLimit;
    private String alias;
    private int id;
    private long lastPollTime;
    private String lastWatered;
    private volatile boolean isPresent;

    public Plant(String ip, int port, int pollDelaySec, int soilHumidity, String alias, int id) {
        this.ip = ip;
        this.port = port;
        this.pollDelaySec = pollDelaySec;
        this.soilHumidLimit = soilHumidity;
        this.alias = alias;
        this.id = id;
        this.lastPollTime = 0;
        this.lastWatered = null;
        this.isPresent = false;
    }

    public void poll(){
        //TODO should poll the plant and expects an answer back.
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

    public int getSoilHumidLimit() {
        return soilHumidLimit;
    }

    public void setSoilHumidLimit(int soilHumidLimit) {
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

    public String getLastWatered() {
        return lastWatered;
    }

    public void setLastWatered(String lastWatered) {
        this.lastWatered = lastWatered;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public void setPresent(boolean present) {
        isPresent = present;
    }
}
