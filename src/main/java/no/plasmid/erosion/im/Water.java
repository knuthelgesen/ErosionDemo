package no.plasmid.erosion.im;

import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import no.plasmid.erosion.Configuration;
import no.plasmid.erosion.Renderer;

public class Water extends Renderable {
	
	public void createMesh(Renderer renderer) {
		List<Vertex> vertexList = getVertices();
		
		int extraWater = Configuration.TERRAIN_TILE_SIZE * 50;
		
		vertexList.add(new Vertex(new Vector3f(Configuration.TERRAIN_SIZE * Configuration.TERRAIN_TILE_SIZE + extraWater, 0.0f, -extraWater), new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f)));
		vertexList.add(new Vertex(new Vector3f(-extraWater, 0.0f, -extraWater), new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f)));
		vertexList.add(new Vertex(new Vector3f(Configuration.TERRAIN_SIZE * Configuration.TERRAIN_TILE_SIZE + extraWater, 0.0f, Configuration.TERRAIN_SIZE * Configuration.TERRAIN_TILE_SIZE + extraWater), new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f)));
		vertexList.add(new Vertex(new Vector3f(-extraWater, 0.0f, Configuration.TERRAIN_SIZE * Configuration.TERRAIN_TILE_SIZE + extraWater), new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f)));
		
		renderer.registerRenderable(this);
	}

}
