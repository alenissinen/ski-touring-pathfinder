package application;

/**
 * Stores application configuration settings such as window properties and
 * OpenGL version.
 */
public class Config {
    /** Window title */
    private String title = "Example";

    /** Window width */
    private int width = 1280;

    /** Window height */
    private int height = 720;

    /** Application target fps */
    private int targetFps = 60;

    /** OpenGL major version (4 in 4.6) */
    private int openGlMajor = 3;

    /** OpenGL minor version (6 in 4.6) */
    private int openGlMinor = 3;

    /** Render distanche in chunk units */
    private int renderDistance = 8;

    /** Movement speed */
    private float movementSpeed = 100.0f;

    /**
     * Private constructor, use {@link Builder} to create a configuration
     */
    private Config() {
    }

    /**
     * @return Window title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return Window width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return Window height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return Desired frame rate
     */
    public int getTargetFps() {
        return targetFps;
    }

    /**
     * @return OpenGL major version
     */
    public int getOpenGlMajor() {
        return openGlMajor;
    }

    /**
     * @return OpenGL minor version
     */
    public int getOpenGlMinor() {
        return openGlMinor;
    }

    /**
     * @return Render distance
     */
    public int getRenderDistance() {
        return renderDistance;
    }

    /**
     * @return Movement speed
     */
    public float getMovementSpeed() {
        return movementSpeed;
    }

    /**
     * Builder for constructing a {@link Config} instance
     *
     * <pre>{@code
     * Config config = new Config.Builder()
     *         .title("Example app")
     *         .width(1920)
     *         .height(1080)
     *         .build();
     * }</pre>
     */
    public static class Builder {
        private String title;
        private int width;
        private int height;
        private int targetFps;
        private int openGlMajor;
        private int openGlMinor;
        private int renderDistance;
        private float movementSpeed;

        /**
         * Sets the window title
         * 
         * @param title window title
         * @return Current {@link Builder} instance
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the window width
         * 
         * @param width window width in pixels
         * @return Current {@link Builder} instance
         */
        public Builder width(int width) {
            this.width = width;
            return this;
        }

        /**
         * Sets the window height
         * 
         * @param height window height in pixels
         * @return Current {@link Builder} instance
         */
        public Builder height(int height) {
            this.height = height;
            return this;
        }

        /**
         * Sets the target fps
         * 
         * @param fps target frame rate
         * @return Current {@link Builder} instance
         */
        public Builder targetFps(int fps) {
            this.targetFps = fps;
            return this;
        }

        /**
         * Sets the OpenGL major version
         * 
         * @param major OpenGL major version number (4 in 4.6)
         * @return Current {@link Builder} instance
         */
        public Builder major(int major) {
            this.openGlMajor = major;
            return this;
        }

        /**
         * Sets the OpenGL minor version
         * 
         * @param minor OpenGL minor version number (6 in 4.6)
         * @return Current {@link Builder} instance
         */
        public Builder minor(int minor) {
            this.openGlMinor = minor;
            return this;
        }

        /**
         * Sets the render distance
         * 
         * @param renderDistance Render distanche in chunk units
         * @return Current {@link Builder} instance
         */
        public Builder renderDistance(int renderDistance) {
            this.renderDistance = renderDistance;
            return this;
        }

        /**
         * Sets the movement speed
         * 
         * @param movementSpeed Movoment speed
         * @return Current {@link Builder} instance
         */
        public Builder movementSpeed(float movementSpeed) {
            this.movementSpeed = movementSpeed;
            return this;
        }

        /**
         * Builds and returns a {@link Config} instance
         * 
         * @return Configured {@link Config} object
         */
        public Config build() {
            Config config = new Config();
            config.title = this.title;
            config.width = this.width;
            config.height = this.height;
            config.targetFps = this.targetFps;
            config.openGlMajor = this.openGlMajor;
            config.openGlMinor = this.openGlMinor;
            config.renderDistance = this.renderDistance;
            config.movementSpeed = this.movementSpeed;

            return config;
        }
    }

    @Override
    public String toString() {
        return "Config {" +
                "\n  title          = '" + title + '\'' +
                "\n  resolution     = " + width + "x" + height +
                "\n  targetFps      = " + targetFps +
                "\n  openGlVersion  = " + openGlMajor + "." + openGlMinor +
                "\n  renderDistance = " + renderDistance + " chunks" +
                "\n  movementSpeed  = " + movementSpeed + "m/s" +
                "\n}";
    }
}
