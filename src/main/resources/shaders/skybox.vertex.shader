#version 330

in vec3 position;
in vec3 vertexColor;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

out vec3 color;

void main() {
	/*
	 * Perform position calculations
	 */
// 	gl_Position = projectionMatrix * vec4(position, 1.0);
// 	gl_Position = vec4(position, 1.0);
  mat4 vp = projectionMatrix * viewMatrix;
	gl_Position = projectionMatrix * vec4(position, 1.0);

	//Set color
	color = vertexColor;
//	color = clamp(vertexColor, 0.1, 1.0);
}
