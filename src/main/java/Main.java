import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.Application;
import application.Config;
import ui.Launcher;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Launch app without launcher using default/development config
        if (args.length > 0 && args[0].equals("--noLauncher")) {
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
            System.exit(0);
        }

        logger.info("Starting launcher");

        Launcher launcher = new Launcher();
        Config config = launcher.open();

        // Check if user didn't launch the application
        if (config == null) {
            logger.info("Launcher was closed without starting the application");
            System.exit(0);
        }

        // Launch the application
        try {
            logger.info("Launching application with user config\n{}", config);
            new Application(config).run();
        } catch (Exception e) {
            logger.error("Error occured during application launch: {}", e);
            System.exit(1);
        }

        logger.info("Application closed succesfully");
        System.exit(0);
    }
}