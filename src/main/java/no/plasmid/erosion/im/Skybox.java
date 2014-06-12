package no.plasmid.erosion.im;

import java.util.List;

import org.lwjgl.util.vector.Vector3f;

public class Skybox extends Renderable {

	/**
	 * Vertices used to draw this skybox.
	 */
	public Skybox() {
		List<Vertex> vertices = getVertices();
		
		/*
		 * Make a "pointy hat" for the camera, with a blue top and white-grey bottom. This can be drawn as a triangle fan.
		 */
		//Add the top first
		vertices.add(new Vertex(new Vector3f(0.0f, 2.0f, 0.0f), null, new Vector3f(0.1f, 0.1f, 0.9f)));
		vertices.add(new Vertex(new Vector3f(-5.0f, 0.0f, -5.0f), null, new Vector3f(1.0f, 1.0f, 1.0f)));
		vertices.add(new Vertex(new Vector3f(5.0f, 0.0f, -5.0f), null, new Vector3f(1.0f, 1.0f, 1.0f)));
	}
	
}
