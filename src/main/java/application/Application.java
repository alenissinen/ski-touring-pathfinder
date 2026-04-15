package application;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;

import java.nio.IntBuffer;

import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import exceptions.HeightMapParseException;
import input.InputHandler;
import pathfinding.AStar;
import rendering.Camera;
import rendering.Renderer;
import terrain.ChunkManager;
import terrain.HeightMap;
import ui.ImGuiLayer;

/**
 * Main application class responsible for the program lifecycle. Manages
 * initialization,
 * main loop and cleanup.
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    /** Store start time to calculate how long intialization took */
    private static final long STARTUP_TIME = System.currentTimeMillis();

    /** Window object */
    private final Window window;

    /** Application config */
    private final Config config;

    /** Renderer object */
    private Renderer renderer;

    /** Input handler */
    private InputHandler inputHandler;

    /** A* instance */
    private AStar aStar;

    /** ImGui instance */
    private ImGuiLayer imGuiLayer;

    /**
     * Constructor to create new application
     * 
     * @param config Application config (see {@link Config})
     */
    public Application(Config config) {
        this.config = config;
        this.window = new Window(config);
    }

    /**
     * Starting point of the application
     */
    public void run() {
        // Initialize GLFW and start render loop
        try {
            this.init();
        } catch (HeightMapParseException | IllegalStateException e) {
            e.printStackTrace();
        }

        this.loop();

        // Clean resources before shutting down
        this.cleanUp();
    }

    /**
     * Setups necessary callbacks, initializes GLFW, and creates the window
     */
    public void init() throws HeightMapParseException, IllegalStateException {
        // Create an error callback, prints error message in stderr
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) {
            logger.error("Failed to initialize GLFW");
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        logger.info("GLFW initialized");

        // Create GLFW window
        this.window.create();

        GL.createCapabilities();

        // Create north and south height maps and merge them
        HeightMap north = HeightMap.fromAsciiFile("/T5311B.asc");
        HeightMap south = HeightMap.fromAsciiFile("/T5311A.asc");
        HeightMap merged = HeightMap.merge(north, south);

        logger.info("Height maps created and merged");

        float spawnY = merged.getElevation(0, 0) + 40.0f;

        Camera camera = new Camera(new Vector3f(0, spawnY, 0), 60f,
                (float) this.config.getWidth() / this.config.getHeight(), this.config.getMovementSpeed());

        // Set initial viewport size
        IntBuffer fbW = BufferUtils.createIntBuffer(1);
        IntBuffer fbH = BufferUtils.createIntBuffer(1);

        glfwGetFramebufferSize(this.window.getHandle(), fbW, fbH);

        int iw = fbW.get(0);
        int ih = fbH.get(0);

        if (iw > 0 && ih > 0) {
            camera.onResize(iw, ih);
            glViewport(0, 0, iw, ih);
        }

        ChunkManager chunkManager = new ChunkManager(merged, config.getRenderDistance());

        this.aStar = new AStar(merged);
        this.renderer = new Renderer(camera, chunkManager, this.aStar, this.window, merged);
        this.inputHandler = new InputHandler(this.window.getHandle(), camera, merged, this.aStar,
                this.renderer.getShader());

        logger.info("Modules instantiated");
    }

    /**
     * Main loop of the application
     */
    public void loop() {
        logger.info("Application loop started, initialization took {}ms", System.currentTimeMillis() - STARTUP_TIME);

        // LWJGl detects the current context and creates GLCapabilities instance
        // and makes the OpenGL bindings available
        GL.createCapabilities();

        // Set background color to light blue
        glClearColor(0.58f, 0.88f, 1.0f, 0.9f);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CW);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        // Convert target fps to target frametime
        float targetFrameTime = 1.0f / config.getTargetFps();

        // Keep track of the last time when something was rendered
        double lastTime = glfwGetTime();

        // Run rendering loop until user closes the window
        while (!glfwWindowShouldClose(window.getHandle())) {
            // Always poll so input stays responsive while waiting for the next tick
            glfwPollEvents();

            double now = glfwGetTime();
            double deltaTime = now - lastTime;

            if (deltaTime >= targetFrameTime) {
                lastTime = now;

                // Run multiple iterations each frame
                for (int n = 0; n < Constants.ASTAR_ITERATIONS; n++) {
                    if (aStar.isRunning()) {
                        aStar.step();
                    }
                }

                this.renderer.getCamera().update(this.inputHandler.getPressedKeys(), (float) deltaTime);
                this.renderer.render();
                glfwSwapBuffers(window.getHandle());
            }
        }
    }

    /**
     * Releases all resources on application shutdown.
     * Disposes OpenGL resources, destroys the GLFW window and terminates GLFW.
     */
    public void cleanUp() {
        this.window.cleanUp();
        this.renderer.cleanUp();

        logger.info("Application resources removed from RAM and VRAM");
    }
}
