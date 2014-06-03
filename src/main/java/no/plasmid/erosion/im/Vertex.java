package no.plasmid.erosion.im;

import java.nio.FloatBuffer;

import org.lwjgl.util.vector.Vector3f;

public class Vertex {

	public Vector3f positionCoords;
	
	public Vertex(Vector3f positionCoords) {
		this.positionCoords = positionCoords;
	}
	
	public void store(FloatBuffer buf) {
		positionCoords.store(buf);
	}
	
}
