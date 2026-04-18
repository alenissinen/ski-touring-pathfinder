#version 410 core
out vec4 FragColor;
in vec3 vWorldPos;
in vec3 vNormal;

uniform float uElevationMin;
uniform float uElevationMax;
uniform int uRenderMode;
uniform vec2 uSelectedGrid;
uniform sampler2D uVisitedTex;
uniform sampler2D uHeatmapTex;
uniform vec3 uLightPos;
uniform vec3 uCameraPos;

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
    // TODO: test different values to make water lighting smoother
    } else if(uRenderMode == 0 && diff < 0.0005 && vWorldPos.y < (uElevationMax - (span * 0.1))) {
        // Create artificial ripple effect to make smoother lighting
        float ripple = sin(vWorldPos.x * 100.0) * cos(vWorldPos.z * 100.0) * 0.02;

        // Create "fake" normal vector
        vec3 n = normalize(vec3(ripple, 1.0, ripple));

        // Calculate lighting direction
        vec3 vDir = normalize(uCameraPos - vWorldPos);
        vec3 hDir = normalize(uLightPos + vDir);

        // Fresnel: makes water darker on top of it and reflective from distance
        float fresnel = pow(1.0 - max(dot(n, vDir), 0.0), 3.0);
        vec3 baseWater = mix(WATER_COLOR * 0.4, WATER_COLOR, fresnel);

        // Specular lighting with glint effect
        float specBase = pow(max(dot(n, hDir), 0.0), 4.0);
        float specGlint = pow(max(dot(n, hDir), 0.0), 64.0);
        vec3 glint = vec3(0.8, 0.9, 1.0) * ((specBase * 0.3) + (specGlint * 0.7)) * 2.0;

        finalColor = baseWater + glint;
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

    // Calculate diffuse + specular lighting
    vec3 normal = normalize(vNormal);

    // Calculate directions
    vec3 lDir = normalize(uLightPos);
    vec3 reflectDir = reflect(-lDir, normal);
    vec3 vDir = normalize(uCameraPos - vWorldPos);

    // Cap diffuse lighting to 0.25 so no completely dark shadows get rendered
    float diffuse = max(0.25, dot(normal, lDir));

    // Calculate specular lighting
    float spec = pow(max(dot(reflectDir, vDir), 0.0), 32.0);
    vec3 specular = vec3(spec * 0.5);

    // Diffuse + specular
    vec3 lighting = vec3(diffuse) + specular;

    // Set final color
    FragColor = vec4(finalColor * lighting, 1.0);
}
