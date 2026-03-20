package application;

/**
 * Stores application configuration settings such as window properties and OpenGL version
 */
public class Config {
    /** Window title */
    private String title;
    /** Window width */
    private int width;
    /** Window height */
    private int height;
    /** Application target fps */
    private int targetFps;
    /** OpenGL major version (4 in 4.6) */
    private int openGlMajor;
    /** OpenGL minor version (6 in 4.6) */
    private int openGlMinor;

    /**
     * Private constructor, use {@link Builder} to create a configuration
     */
    private Config() {}

    /**
     * @return Window title
     */
    public String getTitle() { return title; }

    /**
     * @return Window width in pixels
     */
    public int getWidth() { return width; }

    /**
     * @return Window height in pixels
     */
    public int getHeight() { return height; }

    /**
     * @return Desired frame rate
     */
    public int getTargetFps() { return targetFps; }

    /**
     * @return OpenGL major version
     */
    public int getOpenGlMajor() { return openGlMajor; }

    /**
     * @return OpenGL minor version
     */
    public int getOpenGlMinor() { return openGlMinor; }

    /**
     * Builder for constructing a {@link Config} instance
     *
     * <pre>{@code
     * Config config = new Config.Builder()
     *     .title("Example app")
     *     .width(1920)
     *     .height(1080)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private String title;
        private int width;
        private int height;
        private int targetFps;
        private int openGlMajor;
        private int openGlMinor;

        /**
         * Sets the window title
         * @param title window title
         * @return Current {@link Builder} instance
         */
        public Builder title(String title) { this.title = title; return this; }

        /**
         * Sets the window width
         * @param width window width in pixels
         * @return Current {@link Builder} instance
         */
        public Builder width(int width) { this.width = width; return this; }

        /**
         * Sets the window height
         * @param height window height in pixels
         * @return Current {@link Builder} instance
         */
        public Builder height(int height) { this.height = height; return this; }

        /**
         * Sets the target fps
         * @param fps target frame rate
         * @return Current {@link Builder} instance
         */
        public Builder targetFps(int fps) { this.targetFps = fps; return this; }

        /**
         * Sets the OpenGL major version
         * @param major OpenGL major version number (4 in 4.6)
         * @return Current {@link Builder} instance
         */
        public Builder major(int major) { this.openGlMajor = major; return this; }

        /**
         * Sets the OpenGL minor version
         * @param minor OpenGL minor version number (6 in 4.6)
         * @return Current {@link Builder} instance
         */
        public Builder minor(int minor) { this.openGlMinor = minor; return this; }

        /**
         * Builds and returns a {@link Config} instance
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

            return config;
        }
    }
}
