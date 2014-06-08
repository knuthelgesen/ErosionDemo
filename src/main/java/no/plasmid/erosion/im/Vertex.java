package no.plasmid.erosion.im;

import java.nio.FloatBuffer;

import org.lwjgl.util.vector.Vector3f;

public class Vertex {

	public Vector3f positionCoords;

	public Vector3f normal;
	
	public Vertex(Vector3f positionCoords, Vector3f normal) {
		this.positionCoords = positionCoords;
		this.normal = normal;
	}
	
	public void store(FloatBuffer buf) {
		positionCoords.store(buf);
		normal.store(buf);
	}
	
}
