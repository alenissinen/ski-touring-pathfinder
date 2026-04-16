package application;

import java.util.ArrayList;
import java.util.List;

import terrain.HeightMap;

/**
 * Stores application configuration settings such as window properties and
 * OpenGL version.
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

    /** Render distanche in chunk units */
    private int renderDistance;

    /** Movement speed */
    private float movementSpeed;

    /** Fov */
    private float fov;

    /** Height maps */
    private HeightMap heightMap;

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
     * @return Height map
     */
    public HeightMap getHeightMap() {
        return heightMap;
    }

    /**
     * @return Field of view
     */
    public float getFov() {
        return fov;
    }

    // Setters for builder
    private void setTitle(String title) {
        this.title = title;
    }

    private void setWidth(int width) {
        this.width = width;
    }

    private void setHeight(int height) {
        this.height = height;
    }

    private void setTargetFps(int targetFps) {
        this.targetFps = targetFps;
    }

    private void setOpenGlMajor(int openGlMajor) {
        this.openGlMajor = openGlMajor;
    }

    private void setOpenGlMinor(int openGlMinor) {
        this.openGlMinor = openGlMinor;
    }

    private void setRenderDistance(int renderDistance) {
        this.renderDistance = renderDistance;
    }

    private void setMovementSpeed(float movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    private void setFov(float fov) {
        this.fov = fov;
    }

    private void setHeightMap(HeightMap heightMap) {
        this.heightMap = heightMap;
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
        private String title = "Example";
        private int width = 1280;
        private int height = 720;
        private int targetFps = 60;
        private int openGlMajor = 4;
        private int openGlMinor = 1;
        private int renderDistance = 8;
        private float movementSpeed = 100.0f;
        private float fov = 70.0f;
        private HeightMap heightMap = null;

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
         * Sets the field of view
         * 
         * @param fov Field of view in degrees
         * @return Current {@link Builder} instance
         */
        public Builder fov(float fov) {
            this.fov = fov;
            return this;
        }

        /**
         * Sets the height map
         * 
         * @param heightMap List of height maps to use
         * @return Current {@link Builder} instance
         */
        public Builder heightMap(HeightMap heightMap) {
            this.heightMap = heightMap;
            return this;
        }

        /**
         * Builds and returns a {@link Config} instance
         * 
         * @return Configured {@link Config} object
         */
        public Config build() {
            Config config = new Config();
            config.setTitle(this.title);
            config.setWidth(this.width);
            config.setHeight(this.height);
            config.setTargetFps(this.targetFps);
            config.setOpenGlMajor(this.openGlMajor);
            config.setOpenGlMinor(this.openGlMinor);
            config.setRenderDistance(this.renderDistance);
            config.setMovementSpeed(this.movementSpeed);
            config.setFov(this.fov);
            config.setHeightMap(this.heightMap);

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
                "\n  fov            = " + fov + " degrees" +
                "\n  heightMap      = " + heightMap.toString() +
                "\n}";
    }
}
