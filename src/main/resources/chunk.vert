#version 410 core
layout (location = 0) in vec3 pos;

out vec4 color;

uniform mat4 MVP;

void main()
{
    gl_Position = MVP * vec4(pos, 1.0);

    color = vec4(pos.y / 550);
}