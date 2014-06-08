#version 330

in vec3 position;
in vec3 normal;

//in vec2 textureCoords;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

uniform vec3 dirLightColor;
uniform vec3 dirLightDirection;

out vec3 color;
//out vec2 textureCoordinates;

void main() {
	/*
	 * Perform position calculations
	 */
  mat4 mv = projectionMatrix * viewMatrix;
	gl_Position = mv * vec4(position, 1.0);

	/*
	 * Perform light calculations
	 */
	//Directional light
	float cosTheta = dot(normalize(normal), normalize(dirLightDirection));

	color = dirLightColor*cosTheta;
	
//	textureCoordinates = textureCoords;
}
