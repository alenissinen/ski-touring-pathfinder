package application;

/** Defines how terrain is rendered */
public enum RenderMode {
    /** Render normally with terrain color based on elevation */
    NORMAL,
    /** Render heatmap based on slope angle */
    HEATMAP
}
