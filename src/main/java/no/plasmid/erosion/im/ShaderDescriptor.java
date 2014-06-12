package no.plasmid.erosion.im;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

import no.plasmid.lib.AbstractRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.glu.GLU;

public abstract class ShaderDescriptor {

	private int shaderProgramId;

	private int positionAttributeId;
	private int normalAttributeId;
	private int colorAttributeId;
	
	private int projectionMatrixUniformId;
	private int viewMatrixUniformId;
	
	private int dirLightColorUniformId;
	private int dirLightDirectionUniformId;
	
	private int fogColorUniformId;
	private int fogStartUniformId;
	private int transparencyStartUniformId;
	private int zFarUniformId;

	public ShaderDescriptor(String vertexShaderFile, String fragmentShaderFile) {
		//Load the source
		String vertexSource;
		String fragmentSource;
		try {
			vertexSource = loadTextFile(vertexShaderFile);
			fragmentSource = loadTextFile(fragmentShaderFile);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Error when opening shader file", e);
		}

		//Create the program
		shaderProgramId = GL20.glCreateProgram();
		if (0 == shaderProgramId) {
			throw new IllegalStateException("Could not create shader");
		}
		checkGL();

		//Compile the sources
		compileShader(shaderProgramId, vertexSource, GL20.GL_VERTEX_SHADER);
		compileShader(shaderProgramId, fragmentSource, GL20.GL_FRAGMENT_SHADER);
		checkGL();
		
		//Link the program
		GL20.glLinkProgram(shaderProgramId);
		int linkStatus = GL20.glGetProgrami(shaderProgramId, GL20.GL_LINK_STATUS);
		if (0 == linkStatus) {
			String error = GL20.glGetShaderInfoLog(shaderProgramId, 2048);
			throw new IllegalStateException("Error when linking shader: " + error);
		}
		checkGL();
		
		//Validate the program
		GL20.glValidateProgram(shaderProgramId);
    int validationStatus = GL20.glGetProgrami(shaderProgramId, GL20.GL_VALIDATE_STATUS);
    if (0 == validationStatus) {
        String validationError = GL20.glGetProgramInfoLog(shaderProgramId, 2048);
        throw new IllegalStateException("Error found when validating shader: " + validationError);
    }
		checkGL();

		//Find the ID of the position vertex attribute
		positionAttributeId = GL20.glGetAttribLocation(shaderProgramId, "position");

		//Find the ID of the normal vertex attribute
		normalAttributeId = GL20.glGetAttribLocation(shaderProgramId, "normal");

		//Find the ID of the color vertex attribute
		colorAttributeId = GL20.glGetAttribLocation(shaderProgramId, "vertexColor");
		
		//Find the ID for the projection matrix uniform
		projectionMatrixUniformId = GL20.glGetUniformLocation(shaderProgramId, "projectionMatrix");

		//Find the ID for the view matrix uniform
		viewMatrixUniformId = GL20.glGetUniformLocation(shaderProgramId, "viewMatrix");

		//Find the ID for the directional light color uniform
		dirLightColorUniformId = GL20.glGetUniformLocation(shaderProgramId, "dirLightColor");

		//Find the ID for the directional light direction uniform
		dirLightDirectionUniformId = GL20.glGetUniformLocation(shaderProgramId, "dirLightDirection");

		//Find the ID for the directional light direction uniform
		fogColorUniformId = GL20.glGetUniformLocation(shaderProgramId, "fogColor");
		
		//Find the ID for the directional light direction uniform
		fogStartUniformId = GL20.glGetUniformLocation(shaderProgramId, "fogStart");
		
		//Find the ID for the directional light direction uniform
		transparencyStartUniformId = GL20.glGetUniformLocation(shaderProgramId, "transparencyStart");
		
		//Find the ID for the directional light direction uniform
		zFarUniformId = GL20.glGetUniformLocation(shaderProgramId, "zFar");
				
		checkValues();
	}
	
	public int getShaderProgramId() {
		return shaderProgramId;
	}
	
	public int getPositionAttributeId() {
		return positionAttributeId;
	}

	public int getNormalAttributeId() {
		return normalAttributeId;
	}

	public int getColorAttributeId() {
		return colorAttributeId;
	}

	public int getProjectionMatrixUniformId() {
		return projectionMatrixUniformId;
	}

	public int getViewMatrixUniformId() {
		return viewMatrixUniformId;
	}

	public int getDirLightColorUniformId() {
		return dirLightColorUniformId;
	}

	public int getDirLightDirectionUniformId() {
		return dirLightDirectionUniformId;
	}

	public int getFogColorUniformId() {
		return fogColorUniformId;
	}

	public int getFogStartUniformId() {
		return fogStartUniformId;
	}

	public int getTransparencyStartUniformId() {
		return transparencyStartUniformId;
	}

	public int getzFarUniformId() {
		return zFarUniformId;
	}

	protected abstract void checkValues();

	/**
	 * Check for OpenGL error, and throw exception if any are found
	 */
	private void checkGL() {
		final int code = GL11.glGetError();
		if (code != 0) {
			final String errorString = GLU.gluErrorString(code);
			final String message = "OpenGL error (" + code + "): " + errorString;
			throw new IllegalStateException(message);
		}
	}

	private String loadTextFile(String fileName) throws FileNotFoundException {
		URL fileURL = AbstractRenderer.class.getResource(fileName);
		if (fileURL == null) {
			throw new FileNotFoundException("Could not find shader source file " + fileName);
		}
		
		StringBuilder text = new StringBuilder();
		String NL = System.getProperty("line.separator");
		FileInputStream fis;
		Scanner scanner = null;
		try {
			fis = new FileInputStream(fileURL.getFile());
			scanner = new Scanner(fis);
			
			while (scanner.hasNextLine()) {
				text.append(scanner.nextLine() + NL);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
		
		return text.toString();
	}
		
	private void compileShader(int shaderProgramId, String source, int type) {
		int shaderId = GL20.glCreateShader(type);
		if (0 == shaderId) {
			throw new IllegalStateException("Could not create shader!");
		}
		
		GL20.glShaderSource(shaderId, source);
		GL20.glCompileShader(shaderId);
		int compileStatus = GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS);
		if (0 == compileStatus) {
			String error = GL20.glGetShaderInfoLog(shaderId, 2048);
			throw new IllegalStateException("Error when compiling shader: " + error);
		}
		
		GL20.glAttachShader(shaderProgramId, shaderId);
	}
	
}
