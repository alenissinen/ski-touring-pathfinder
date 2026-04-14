#version 410 core
out vec4 FragColor;
in vec3 vWorldPos;

uniform float uElevationMin;
uniform float uElevationMax;
uniform vec2 uSelectedGrid;
uniform sampler2D uVisitedTex;

const vec3 SELECTED_COLOR = vec3(1.0, 0.91, 0.56);
const vec3 VISITED_COLOR = vec3(1.0, 0.0, 0.0);
const vec3 LOW_COLOR = vec3(0.2, 0.16, 0.13);
const vec3 HIGH_COLOR = vec3(0.95, 0.98, 1.0);
const vec3 WATER_COLOR = vec3(0.16, 0.56, 0.79);
const vec3 FOUND_COLOR = vec3(0.0, 1.0, 0.0);

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

    // water detection
    float diff = fwidth(vWorldPos.y);

    vec3 finalColor;
    if(dist < 1.0) {
        finalColor = SELECTED_COLOR;
    // Search is still ongoing
    } else if(visited > 0.9) {
        finalColor = VISITED_COLOR;
    // Path has been found
    } else if(visited > 0.4) {
        finalColor = FOUND_COLOR;
    // If area is almost flat and fragment isn't in the highest 10% of the scene -> assume it is water
    } else if(diff < 0.0005 && vWorldPos.y < (uElevationMax - (span * 0.1))) {
        finalColor = WATER_COLOR;
    } else {
        finalColor = terrainColor;
    }

    // Set final color
    FragColor = vec4(finalColor, 1.0);
}
