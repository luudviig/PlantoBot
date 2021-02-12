package models;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Settings {
    private boolean debugMode;
    private String alias;
    private String activeSince;


    public Settings(boolean debugMode, String alias, long activeSince) {
        this.debugMode = debugMode;
        this.alias = alias;
        this.activeSince = lastMillisToSimpleDate(activeSince);
    }

    public String lastMillisToSimpleDate(long millis) {
        // Millis to date
        Date resultDate = new Date(millis);
        String pattern = "yyyy-MM-dd HH:mm";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(resultDate);
    }

    public String getActiveSince() {
        return activeSince;
    }

    public void setActiveSince(String activeSince) {
        this.activeSince = activeSince;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String printSettingsInfo(){
        return "DEBUGMODE: " + this.debugMode + "\n" + "ALIAS: " + this.alias + "\n" + "ACTTIVE SINCE: " + this.activeSince;
    }
}
