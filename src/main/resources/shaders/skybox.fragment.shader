#version 330

in vec3 color;
//in vec2 textureCoordinates;

//uniform sampler2D sampler;

out vec4 fragColor;

void main() {
//	fragColor = texture2D(sampler, textureCoordinates.st);
	fragColor = vec4(color, 1.0);
}
