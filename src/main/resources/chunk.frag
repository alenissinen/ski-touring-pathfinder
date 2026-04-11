#version 410 core
out vec4 FragColor;
in vec3 vWorldPos;

uniform float uElevationMin;
uniform float uElevationMax;
uniform vec2 uSelectedGrid;

void main() {
    float vElevation = vWorldPos.y;
    float span = max(uElevationMax - uElevationMin, 1.0);
    float t = clamp((vElevation - uElevationMin) / span, 0.0, 1.0);
    float dist = length(vec2(vWorldPos.x / 2.0, vWorldPos.z / 2.0) - uSelectedGrid);
    if(dist < 2.0) {
        FragColor = vec4(1.0, 0.91, 0.56, 1.0);
    } else {
        FragColor = vec4(vec3(t), 1.0);
    }
}
