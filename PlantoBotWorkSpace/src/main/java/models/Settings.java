package models;

public class Settings {
    private boolean debugMode;
    private String alias;

    public Settings(boolean debugMode, String alias) {
        this.debugMode = debugMode;
        this.alias = alias;
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
        return "DEBUGMODE: " + this.debugMode + "\n" + "ALIAS: " + this.alias;
    }
}
