package no.plasmid.erosion;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import no.plasmid.erosion.im.Camera;
import no.plasmid.erosion.im.Renderable;
import no.plasmid.erosion.im.Terrain;
import no.plasmid.erosion.im.Water;
import no.plasmid.lib.AbstractApp;

/**
 * Hello world!
 *
 */
public class App extends AbstractApp
{

	public static void main(String[] args) {
  	App app = new App();
  	app.loadNatives(SupportedPlatform.getPlatformForOS());

  	app.initializeApplication();
  	app.runApplication();
  	app.cleanupApplication();
  }
	
	//Input handler instance
	private InputHandler inputHandler;
	
	//Renderer instance
	private Renderer renderer;

	/*
   * Perform initialization to prepare for running
   */
  private void initializeApplication() {
  	//Open the program window
    try {
    	PixelFormat pf = new PixelFormat(32, 8, 16, 0, 16);
    	Display.setDisplayMode(new DisplayMode(Configuration.WINDOW_WIDTH, Configuration.WINDOW_HEIGTH));
    	Display.setTitle(Configuration.WINDOW_TITLE);
    	Display.setVSyncEnabled(false);
    	Display.create(pf);
		} catch (LWJGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); 
		}
    
    //Create the input handler
    inputHandler = new InputHandler();
    
    //Create the renderer
    renderer = new Renderer();
    renderer.initializeRenderer();
  }

  /**
   * Run the application
   */
  private void runApplication() {
  	Camera camera = new Camera();
  	
  	List<Renderable> renderables = new ArrayList<Renderable>();
  	Terrain terrain = new Terrain();
  	terrain.createInitialTerrain();
  	renderables.add(terrain);
  	Water water = new Water();
  	water.createMesh(renderer);
  	renderables.add(water);
  	
  	
  	//Create the projection matrix that is used for rendering
  	Matrix4f projectionMatrix = renderer.calculateProjectionMatrix(Configuration.WINDOW_WIDTH, Configuration.WINDOW_HEIGTH,
  			Configuration.Z_NEAR, Configuration.Z_FAR, Configuration.FIELD_OF_VIEW);
  	FloatBuffer pmBuffer = BufferUtils.createFloatBuffer(16);
  	projectionMatrix.store(pmBuffer);
  	pmBuffer.flip();
  	//Create the view matrix that is used for rendering
  	Matrix4f viewMatrix = new Matrix4f();
  	FloatBuffer vmBuffer = BufferUtils.createFloatBuffer(16);
  	
  	while (!inputHandler.isCloseRequested()) {
  		//Run erosion "simulation" (one step)
  		terrain.runErsionStep(renderer);
  		if (inputHandler.getKeyStatus()[Keyboard.KEY_LSHIFT] || inputHandler.getKeyStatus()[Keyboard.KEY_RSHIFT]) {
    		if (inputHandler.getKeyStatus()[Keyboard.KEY_LEFT]) {
    			camera.rotateCamera(new Vector3f(0.0f, -Configuration.CAMERA_ROTATION_SPEED, 0.0f));
    		}
    		if (inputHandler.getKeyStatus()[Keyboard.KEY_RIGHT]) {
    			camera.rotateCamera(new Vector3f(0.0f, Configuration.CAMERA_ROTATION_SPEED, 0.0f));
    		}
    		if (inputHandler.getKeyStatus()[Keyboard.KEY_UP]) {
    			camera.rotateCamera(new Vector3f(Configuration.CAMERA_ROTATION_SPEED, 0.0f, 0.0f));
    		}
    		if (inputHandler.getKeyStatus()[Keyboard.KEY_DOWN]) {
    			camera.rotateCamera(new Vector3f(-Configuration.CAMERA_ROTATION_SPEED, 0.0f, 0.0f));
    		}
  		} else {
    		if (inputHandler.getKeyStatus()[Keyboard.KEY_LEFT]) {
    			camera.moveCamera(new Vector3f(Configuration.CAMERA_MOVEMENT_SPEED, 0.0f, 0.0f));
    		}
    		if (inputHandler.getKeyStatus()[Keyboard.KEY_RIGHT]) {
    			camera.moveCamera(new Vector3f(-Configuration.CAMERA_MOVEMENT_SPEED, 0.0f, 0.0f));
    		}
    		if (inputHandler.getKeyStatus()[Keyboard.KEY_UP]) {
    			camera.moveCamera(new Vector3f(0.0f, 0.0f, Configuration.CAMERA_MOVEMENT_SPEED));
    		}
    		if (inputHandler.getKeyStatus()[Keyboard.KEY_DOWN]) {
    			camera.moveCamera(new Vector3f(0.0f, 0.0f, -Configuration.CAMERA_MOVEMENT_SPEED));
    		}
    		if (inputHandler.getKeyStatus()[Keyboard.KEY_NEXT]) {
    			camera.moveCamera(new Vector3f(0.0f, Configuration.CAMERA_MOVEMENT_SPEED, 0.0f));
    		}
    		if (inputHandler.getKeyStatus()[Keyboard.KEY_PRIOR]) {
    			camera.moveCamera(new Vector3f(0.0f, -Configuration.CAMERA_MOVEMENT_SPEED, 0.0f));
    		}
  		}
  		
    	viewMatrix = camera.createViewMatrix();
    	viewMatrix.store(vmBuffer);
    	vmBuffer.flip();
  		renderer.render(pmBuffer, vmBuffer, renderables);

  		inputHandler.handleInput();
  		
  		Display.update();
  		
  		Display.sync(60);
  	}
  }

  /**
   * Clean up after the application
   */
  private void cleanupApplication() {
  	//Destroy the program window
  	Display.destroy();
  }

  @Override
	protected String getCodeSourcePathString() {
  	return App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	}
	
}
