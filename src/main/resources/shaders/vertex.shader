#version 330

in vec3 position;
//in vec2 textureCoords;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

//out vec2 textureCoordinates;

void main() {

  mat4 mv = projectionMatrix * viewMatrix;
	gl_Position = mv * vec4(position, 1.0);

//	textureCoordinates = textureCoords;
}
