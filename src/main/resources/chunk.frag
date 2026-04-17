#version 410 core
out vec4 FragColor;
in vec3 vWorldPos;

uniform float uElevationMin;
uniform float uElevationMax;
uniform int uRenderMode;
uniform vec2 uSelectedGrid;
uniform sampler2D uVisitedTex;
uniform sampler2D uHeatmapTex;

const vec3 SELECTED_COLOR = vec3(1.0, 0.91, 0.56);
const vec3 LOW_COLOR = vec3(0.2, 0.16, 0.13);
const vec3 HIGH_COLOR = vec3(0.95, 0.98, 1.0);
const vec3 WATER_COLOR = vec3(0.16, 0.56, 0.79);
const vec3 PATH_COLOR = vec3(0.87, 0.0, 1.0);
const vec3 HEATMAP_GREEN_COLOR = vec3(0.0, 0.8, 0.0);
const vec3 HEATMAP_YELLOW_COLOR = vec3(1.0, 0.9, 0.0);
const vec3 HEATMAP_RED_COLOR = vec3(0.9, 0.0, 0.0);
const vec3 HEATMAP_BLACK_COLOR = vec3(0.1, 0.1, 0.1);

void main() {
    // Terrain height difference
    float span = uElevationMax - uElevationMin;

    // Calculate relative height, normalize it and clamp between [0.0, 1.0]
    // Clamping shouldn't be needed but better safe than sorry
    float t = clamp((vWorldPos.y - uElevationMin) / span, 0.0, 1.0);

    // Choose color by interpolation
    vec3 terrainColor = mix(LOW_COLOR, HIGH_COLOR, t);

    // Scale coordinates with world scale
    vec2 gridPos = vWorldPos.xz / 2.0;

    // Get texture resolution
    ivec2 texSize = textureSize(uVisitedTex, 0);

    // Convert grid coordinates to texture coordinates ([0.0, 1.0])
    vec2 texCoord = (gridPos + vec2(texSize) * 0.5) / vec2(texSize);

    // Read info from texture red channel, 1 ((byte) 255 in java code) means A* has visited that cell
    float visited = texture(uVisitedTex, texCoord).r;

    // Calculate distance between current position and selected position
    float dist = length(gridPos - uSelectedGrid);

    // Water detection
    float diff = fwidth(vWorldPos.y);

    vec3 finalColor;
    if(dist < 1.0) {
        finalColor = SELECTED_COLOR;
    // Use only 1 color for pathfinding to give better visibility in heatmap mode
    } else if(visited > 0.05) {
        finalColor = PATH_COLOR;
    // If area is almost flat and fragment isn't in the highest 10% of the scene -> assume it is water
    } else if(uRenderMode == 0 && diff < 0.0005 && vWorldPos.y < (uElevationMax - (span * 0.1))) {
        finalColor = WATER_COLOR;
    } else {
        if(uRenderMode == 0) {
            finalColor = terrainColor;
        } else if(uRenderMode == 1) {
            // Fetch the slope angle from the texture
            float slope = texture(uHeatmapTex, texCoord).r;
            vec3 result;

            // Mixes colors between stages to achieve smoother look
            if(slope < 0.4) {
                result = mix(HEATMAP_GREEN_COLOR, HEATMAP_YELLOW_COLOR, slope / 0.4);
            } else if(slope < 0.8) {
                result = mix(HEATMAP_YELLOW_COLOR, HEATMAP_RED_COLOR, (slope - 0.4) / 0.4);
            } else {
                result = mix(HEATMAP_RED_COLOR, HEATMAP_BLACK_COLOR, (slope - 0.8) / 0.2);
            }

            finalColor = result;
        }
    }

    // Set final color
    FragColor = vec4(finalColor, 1.0);
}
