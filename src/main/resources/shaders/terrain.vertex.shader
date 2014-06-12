#version 330

in vec3 position;
in vec3 normal;
in vec3 vertexColor;

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
  mat4 vp = projectionMatrix * viewMatrix;
	gl_Position = vp * vec4(position, 1.0);

	/*
	 * Perform light calculations
	 */
	//Directional light
	float cosTheta = dot(normalize(normal), normalize(dirLightDirection));

	color = vertexColor*dirLightColor*cosTheta;
	
	//Ambient light
	color = clamp(color, 0.1, 1.0);
//	textureCoordinates = textureCoords;
}
