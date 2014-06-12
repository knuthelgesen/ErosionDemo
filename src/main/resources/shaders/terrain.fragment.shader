#version 330

in vec3 color;
//in vec2 textureCoordinates;

//uniform sampler2D sampler;

uniform vec3 fogColor;
uniform float fogStart;
uniform float transparencyStart;
uniform float zFar;

out vec4 fragColor;

void main() {
//	fragColor = texture2D(sampler, textureCoordinates.st);
	fragColor = vec4(color, 1.0);
	
	float fogFactor = 1.0 - ((gl_FragCoord.z / gl_FragCoord.w) - fogStart) / (zFar - fogStart);
	fogFactor = clamp(fogFactor, 0.0, 1.0);	
	
	vec3 c = vec3(color * fogFactor + fogColor * (1.0 - fogFactor));
	c = clamp(c, 0.0, 1.0);
	
	float aFactor = ((gl_FragCoord.z / gl_FragCoord.w) - transparencyStart) / (zFar - transparencyStart);
	
	fragColor = vec4(c, 1.0 - aFactor);
	
//	fragColor = vec4(color[0], 1.0, 1.0, 1.0);
}
