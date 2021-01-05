package mainPackage;

public class Main {

    public static void main(String[] args) {
        //Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            //Cleans up in case of external shutdown/error
            @Override
            public void run() {
                System.out.println("ShutDown Hook Executing");
                ClientApp.getInstance().closeServer();
            }
        }));
        //Launch Hub
        ClientApp.getInstance().startServer();
    }
}
