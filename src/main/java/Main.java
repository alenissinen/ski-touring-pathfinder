import application.Application;
import application.Config;

public class Main {
    public static void main(String[] args) {
        // Create application config
        Config config = new Config.Builder()
                .title("Ski Touring Pathfinder")
                .width(1280)
                .height(720)
                .targetFps(60)
                .major(4)
                .minor(1) // macOS doesn't officially support 4.6
                .build();

        // Start application
        new Application(config).run();
    }
}