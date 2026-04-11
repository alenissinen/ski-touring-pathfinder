#version 410 core

in float vElevation;

uniform float uElevationMin;
uniform float uElevationMax;

out vec4 fragColor;

void main()
{
    float span = max(uElevationMax - uElevationMin, 1.0);
    float t = clamp((vElevation - uElevationMin) / span, 0.0, 1.0);
    fragColor = vec4(vec3(t), 1.0);
}
