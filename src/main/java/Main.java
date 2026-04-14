import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.Application;
import application.Config;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Create application config
        Config config = new Config.Builder()
                .title("Ski Touring Pathfinder")
                .width(1280)
                .height(720)
                .targetFps(240)
                .major(4)
                .minor(1) // macOS doesn't officially support 4.6
                .renderDistance(24)
                .movementSpeed(250)
                .build();

        logger.info(config.toString());

        // Start application
        new Application(config).run();
    }
}