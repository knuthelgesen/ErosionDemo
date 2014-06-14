package no.plasmid.erosion;

import java.nio.FloatBuffer;
import java.util.List;

import no.plasmid.erosion.im.Renderable;
import no.plasmid.erosion.im.ShaderDescriptor;
import no.plasmid.erosion.im.Skybox;
import no.plasmid.erosion.im.Vertex;
import no.plasmid.lib.AbstractRenderer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.NVMultisampleFilterHint;

public class Renderer extends AbstractRenderer {

	private ShaderDescriptor skyboxShaderDescriptor;
	private ShaderDescriptor terrainShaderDescriptor;

	/**
	 * Initialize the renderer, including all shaders
	 */
	public void initializeRenderer() {
		/*
		 * GL context
		 */
		GL11.glViewport(0, 0, Configuration.WINDOW_WIDTH, Configuration.WINDOW_HEIGTH);
//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		GL11.glClearColor(Configuration.FOG_COLOR[0], Configuration.FOG_COLOR[1], Configuration.FOG_COLOR[2], 1.0f);
		
		GL11.glCullFace(GL11.GL_BACK);
		GL11.glEnable(GL11.GL_CULL_FACE);

		//Enable multisample anti aliasing
		GL11.glEnable(GL13.GL_MULTISAMPLE);
		GL11.glHint(NVMultisampleFilterHint.GL_MULTISAMPLE_FILTER_HINT_NV, GL11.GL_NICEST);
		
		//Enable texturing
		GL11.glEnable(GL11.GL_TEXTURE_2D);

		GL11.glEnable(GL11.GL_DEPTH_TEST);

		checkGL();
		
		/*
		 * Shaders
		 */
		//Skybox shader
		skyboxShaderDescriptor = new ShaderDescriptor("/shaders/skybox.vertex.shader", "/shaders/skybox.fragment.shader") {

			@Override
			protected void checkValues() {
				if (-1 == getPositionAttributeId()) {
					throw new IllegalStateException("Could not find ID of the position vertex attribute");
				}
				if (-1 == getColorAttributeId()) {
					throw new IllegalStateException("Could not find ID of the color vertex attribute");
				}
				if (-1 == getProjectionMatrixUniformId()) {
					throw new IllegalStateException("Could not find ID of the projection matrix uniform");
				}
			}
			
		};
		
		//Terrain shader
		terrainShaderDescriptor = new ShaderDescriptor("/shaders/terrain.vertex.shader", "/shaders/terrain.fragment.shader") {

			@Override
			protected void checkValues() {
				if (-1 == getPositionAttributeId()) {
					throw new IllegalStateException("Could not find ID of the position vertex attribute");
				}
				if (-1 == getNormalAttributeId()) {
					throw new IllegalStateException("Could not find ID of the normal vertex attribute");
				}
				if (-1 == getColorAttributeId()) {
					throw new IllegalStateException("Could not find ID of the color vertex attribute");
				}
				if (-1 == getProjectionMatrixUniformId()) {
					throw new IllegalStateException("Could not find ID of the projection matrix uniform");
				}
				if (-1 == getViewMatrixUniformId()) {
					throw new IllegalStateException("Could not find ID of the view matrix uniform");
				}
				if (-1 == getDirLightColorUniformId()) {
					throw new IllegalStateException("Could not find ID of the directional light color uniform");
				}
				if (-1 == getDirLightDirectionUniformId()) {
					throw new IllegalStateException("Could not find ID of the directional light direction");
				}
				if (-1 == getFogColorUniformId()) {
					throw new IllegalStateException("Could not find ID of the fog color uniform");
				}
				if (-1 == getFogStartUniformId()) {
					throw new IllegalStateException("Could not find ID of the fog start uniform");
				}
				if (-1 == getTransparencyStartUniformId()) {
					throw new IllegalStateException("Could not find ID of the transparency start uniform");
				}
				if (-1 == getzFarUniformId()) {
					throw new IllegalStateException("Could not find ID of the Z far uniform");
				}
			}
			
		};
	}
	
	/**
	 * Render the scene
	 */
	public void render(FloatBuffer projectionMatrixBuffer, FloatBuffer viewMatrixBuffer, Skybox skybox, List<Renderable> renderables) {
		//Clear the display
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

		/*
		 * Skybox
		 */
		//Use the shader
    GL20.glUseProgram(skyboxShaderDescriptor.getShaderProgramId());

    //Enable vertex attributes "position" 
    GL20.glEnableVertexAttribArray(skyboxShaderDescriptor.getPositionAttributeId());
    //Enable vertex attributes "color" 
    GL20.glEnableVertexAttribArray(skyboxShaderDescriptor.getColorAttributeId());
    //Set projection matrix
    GL20.glUniformMatrix4(skyboxShaderDescriptor.getProjectionMatrixUniformId(), false, projectionMatrixBuffer);
    //Set view matrix
    GL20.glUniformMatrix4(skyboxShaderDescriptor.getViewMatrixUniformId(), false, viewMatrixBuffer);

    //Bind data buffer
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, skybox.getBufferId());
		//Point to position. Size is 3 * 4 bytes. Stride is position (3 floats) + normal (3 floats) + color (3 floats) * 4 bytes. Offset is 0
    GL20.glVertexAttribPointer(skyboxShaderDescriptor.getPositionAttributeId(), 3, GL11.GL_FLOAT, true, 6 * 4, 0);
		//Point to color. Size is 3 * 4 bytes. Stride is position (3 floats) + color (3 floats) * 4 bytes. Offset is position (3 floats) * 4 bytes
    GL20.glVertexAttribPointer(skyboxShaderDescriptor.getColorAttributeId(), 3, GL11.GL_FLOAT, false, 6 * 4, 3 * 4);
		
		//Draw
		GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, skybox.getVertices().size());

		//Disable vertex attribute arrays
		GL20.glDisableVertexAttribArray(skyboxShaderDescriptor.getPositionAttributeId());
		GL20.glDisableVertexAttribArray(skyboxShaderDescriptor.getColorAttributeId());
    
		//Clear the Z buffer
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    
		/*
		 * Terrain
		 */
		//Use the shader
    GL20.glUseProgram(terrainShaderDescriptor.getShaderProgramId());

    //Enable vertex attributes "position" 
    GL20.glEnableVertexAttribArray(terrainShaderDescriptor.getPositionAttributeId());
    //Enable vertex attributes "normal" 
    GL20.glEnableVertexAttribArray(terrainShaderDescriptor.getNormalAttributeId());
    //Enable vertex attributes "color" 
    GL20.glEnableVertexAttribArray(terrainShaderDescriptor.getColorAttributeId());
    
    //Set projection matrix
    GL20.glUniformMatrix4(terrainShaderDescriptor.getProjectionMatrixUniformId(), false, projectionMatrixBuffer);
    //Set view matrix
    GL20.glUniformMatrix4(terrainShaderDescriptor.getViewMatrixUniformId(), false, viewMatrixBuffer);
    
    //Set directional light color
    GL20.glUniform3f(terrainShaderDescriptor.getDirLightColorUniformId(), 1.0f, 1.0f, 1.0f);
    //Set directional light direction
    GL20.glUniform3f(terrainShaderDescriptor.getDirLightDirectionUniformId(), 0.5f, 1.0f, 0.5f);
    
    //Set fog color
    GL20.glUniform3f(terrainShaderDescriptor.getFogColorUniformId(), Configuration.FOG_COLOR[0], Configuration.FOG_COLOR[1], Configuration.FOG_COLOR[2]);
    //Set fog start
    GL20.glUniform1f(terrainShaderDescriptor.getFogStartUniformId(), Configuration.FOG_START);
    //Set transparency start
    GL20.glUniform1f(terrainShaderDescriptor.getTransparencyStartUniformId(), Configuration.FOG_TRANSPARENCY_START);
    //Set Z far
    GL20.glUniform1f(terrainShaderDescriptor.getzFarUniformId(), Configuration.Z_FAR);
    
    for (Renderable renderable : renderables) {
      //Bind data buffer
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderable.getBufferId());
  		//Point to position. Size is 3 * 4 bytes. Stride is position (3 floats) + normal (3 floats) + color (3 floats) * 4 bytes. Offset is 0
      GL20.glVertexAttribPointer(terrainShaderDescriptor.getPositionAttributeId(), 3, GL11.GL_FLOAT, true, 9 * 4, 0);
  		//Point to normal. Size is 3 * 4 bytes. Stride is position (3 floats) + normal (3 floats) + color (3 floats) * 4 bytes. Offset is position (3 floats) * 4 bytes
      GL20.glVertexAttribPointer(terrainShaderDescriptor.getNormalAttributeId(), 3, GL11.GL_FLOAT, false, 9 * 4, 3 * 4);
  		//Point to color. Size is 3 * 4 bytes. Stride is position (3 floats) + normal (3 floats) + color (3 floats) * 4 bytes. Offset is position (3 floats) + normal (3 floats) * 4 bytes
      GL20.glVertexAttribPointer(terrainShaderDescriptor.getColorAttributeId(), 3, GL11.GL_FLOAT, false, 9 * 4, 6 * 4);
  		
  		//Draw
  		GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, renderable.getVertices().size());
    }
    
		//Disable vertex attribute arrays
		GL20.glDisableVertexAttribArray(terrainShaderDescriptor.getPositionAttributeId());
		GL20.glDisableVertexAttribArray(terrainShaderDescriptor.getNormalAttributeId());
		GL20.glDisableVertexAttribArray(terrainShaderDescriptor.getColorAttributeId());
	}
	
	public void registerRenderable(Renderable renderable) {
		List<Vertex> vertices = renderable.getVertices();
		if (0 != renderable.getBufferId()) {
			//Delete the old buffer first
			GL15.glDeleteBuffers(renderable.getBufferId());
		}
		renderable.setBufferId(GL15.glGenBuffers());
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, renderable.getBufferId());
		//Reserve enough space for position.
		FloatBuffer data = BufferUtils.createFloatBuffer((vertices.size() * 9));	// 3 floats for position + 3 floats for normal + 3 floats for color
		for (Vertex vertex : vertices) {
			vertex.store(data);
		}
		data.rewind();
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);

		checkGL();
	}
		
}
