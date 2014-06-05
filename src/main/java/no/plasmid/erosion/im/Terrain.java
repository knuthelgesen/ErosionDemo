package no.plasmid.erosion.im;

import java.util.List;
import java.util.Random;

import org.lwjgl.util.vector.Vector3f;

import no.plasmid.erosion.Configuration;
import no.plasmid.erosion.Renderer;

public class Terrain extends Renderable {

	private float[][] heightMap;

	public void createInitialTerrain() {
		heightMap = new float[Configuration.TERRAIN_SIZE][Configuration.TERRAIN_SIZE];

		//To seed the erosion
		Random random = new Random(Configuration.TERRAIN_NOISE_RANDOM_SEED);
		
		for (int x = 0; x < Configuration.TERRAIN_SIZE; x++) {
			for (int z = 0; z < Configuration.TERRAIN_SIZE; z++) {
				heightMap[x][z] = (float)(Math.sin(((double)x / Configuration.TERRAIN_SIZE)* Math.PI) * Math.sin(((double)z / Configuration.TERRAIN_SIZE)* Math.PI) * 4500) - 250.0f + random.nextFloat() * 100;
			}
		}
	}

	public void runErsionStep(Renderer renderer) {
		/*
		 * Do the erosion
		 */
		float[][] newHeightMap = heightMap.clone();
		float landslideDeltaHeight = (float)((float)Configuration.TERRAIN_TILE_SIZE / Math.cos(Math.toRadians(Configuration.TERRAIN_LANDSLIDE_ANGLE)));
		
		/*
		 * 0 = none
		 * 1 = north (z -1)
		 * 2 = east (x + 1)
		 * 3 = south (z + 1)
		 * 4 = west (x - 1)
		 */
		for (int i = 0; i < Configuration.TERRAIN_EROSION_STEPS; i++) {
			for (int x = 0; x < Configuration.TERRAIN_SIZE; x++) {
				for (int z = 0; z < Configuration.TERRAIN_SIZE; z++) {
					int curX = x;
					int curZ = z;
					boolean finished = false;
					float amountMoved = 0.0f;
					while (!finished) {
						if (heightMap[curX][curZ] < 0.0f) {
							//If under water, don't erode
							finished = true;
							
							distributeMovedMaterial(curX, curZ, newHeightMap, amountMoved);
							continue;
						}
						
						//Find lower neighbor
						float lowestPoint = heightMap[curX][curZ];
						int lowestNeighbor = 0;
						if (curZ != 0 && heightMap[curX][curZ-1] < lowestPoint) {
							lowestPoint = heightMap[curX][curZ-1];
							lowestNeighbor = 1;
						}
						if (curX != Configuration.TERRAIN_SIZE - 1 && heightMap[curX+1][curZ] < lowestPoint) {
							lowestPoint = heightMap[curX+1][curZ];
							lowestNeighbor = 2;
						}
						if (curZ != Configuration.TERRAIN_SIZE - 1 && heightMap[curX][curZ+1] < lowestPoint) {
							lowestPoint = heightMap[curX][curZ+1];
							lowestNeighbor = 3;
						}
						if (curX != 0 && heightMap[curX-1][curZ] < lowestPoint) {
							lowestPoint = heightMap[curX-1][curZ];
							lowestNeighbor = 4;
						}
						
						switch (lowestNeighbor) {
						case 0:
							finished = true;
							distributeMovedMaterial(curX, curZ, newHeightMap, amountMoved);
							break;
						case 1:
							if (((heightMap[curX][curZ] - lowestPoint) > landslideDeltaHeight) && (Math.random() < Configuration.TERRAIN_LANDSLIDE_CHANCE)) {
								float deltaY = (heightMap[curX][curZ] - lowestPoint) / 2;
								newHeightMap[curX][curZ] -= deltaY;
								newHeightMap[curX][curZ-1] += deltaY;
							} else {
								newHeightMap[curX][curZ] -= Configuration.TERRAIN_EROSION_AMOUNT;
								amountMoved += Configuration.TERRAIN_EROSION_AMOUNT;
							}
							curZ = curZ - 1;
							break;
						case 2:
							if (((heightMap[curX][curZ] - lowestPoint) > landslideDeltaHeight) && (Math.random() < Configuration.TERRAIN_LANDSLIDE_CHANCE)) {
								float deltaY = (heightMap[curX][curZ] - lowestPoint) / 2;
								newHeightMap[curX][curZ] -= deltaY;
								newHeightMap[curX+1][curZ] += deltaY;
							} else {
								newHeightMap[curX][curZ] -= Configuration.TERRAIN_EROSION_AMOUNT;
								amountMoved += Configuration.TERRAIN_EROSION_AMOUNT;
							}
							curX = curX + 1;
							break;
						case 3:
							if (((heightMap[curX][curZ] - lowestPoint) > landslideDeltaHeight) && (Math.random() < Configuration.TERRAIN_LANDSLIDE_CHANCE)) {
								float deltaY = (heightMap[curX][curZ] - lowestPoint) / 2;
								newHeightMap[curX][curZ] -= deltaY;
								newHeightMap[curX][curZ+1] += deltaY;
							} else {
								newHeightMap[curX][curZ] -= Configuration.TERRAIN_EROSION_AMOUNT;
								amountMoved += Configuration.TERRAIN_EROSION_AMOUNT;
							}
							curZ = curZ + 1;
							break;
						case 4:
							if (((heightMap[curX][curZ] - lowestPoint) > landslideDeltaHeight) && (Math.random() < Configuration.TERRAIN_LANDSLIDE_CHANCE)) {
								float deltaY = (heightMap[curX][curZ] - lowestPoint) / 2;
								newHeightMap[curX][curZ] -= deltaY;
								newHeightMap[curX-1][curZ] += deltaY;
							} else {
								newHeightMap[curX][curZ] -= Configuration.TERRAIN_EROSION_AMOUNT;
								amountMoved += Configuration.TERRAIN_EROSION_AMOUNT;
							}
							curX = curX - 1;
							break;
						default:
							finished = true;
							distributeMovedMaterial(curX, curZ, newHeightMap, amountMoved);
							break;
						}
					}
				}					
			}
			
			heightMap = newHeightMap.clone();
		}
				
		createMesh(renderer);
	}
	
	private void createMesh(Renderer renderer) {
		List<Vertex> vertices = this.getVertices();
		vertices.clear();
		
		/*
		 * To make a triangle strip, we need to go back and forth, adding one strip each way.
		 * Begin:
		 * x = 0; {
		 *   x % 2 = 0;
		 *   Add two points to begin.
		 *   z = 0 then loop until z = TERRAIN_SIZE - 1. Add two points for each square (two triangles)
		 *   x++ (sp x % 2 = 1)
		 *   Add two point to begin the other way
		 *   z = TERRAIN SIZE - 1, loop until z = 0. Add two points for each square (two triangles)
		 *   x++ (so x % 2 = 0 again)
		 * }
		 * Repeat until x = TERRAIN_SIZE - 1
		 */

		/*
		 * Map of all surrounding heights.
		 * 
		 *        North
		 * 	       -z
		 * 
		 * E    h1|h2|h3    W
		 * a -x h4|XZ|h5 +x e
		 * s    h6|h7|h8    s
		 * t                t
		 *         +z
		 *        South
		 *       
		 * x1z1 = Corner towards x-1,z-1
		 * x2z1 = Corner towards x+1,z-1
		 * x2z2 = Corner towards x+1,z+1
		 * x1z2 = Corner towards x-1,z+1
		 * 
		 */
		float x1z1, x2z1, x1z2, x2z2;
		float[] cornerValues;
		for (int x = 0; x < Configuration.TERRAIN_SIZE;) {
			int z = 0;
			//First two initial points
			cornerValues = generateCornerValues(x, z);
			//TODO
			x1z1 = cornerValues[0];
			x2z1 = cornerValues[1];
			vertices.add(new Vertex(new Vector3f((x + 1) * Configuration.TERRAIN_TILE_SIZE, x2z1, z * Configuration.TERRAIN_TILE_SIZE)));
			vertices.add(new Vertex(new Vector3f(x * Configuration.TERRAIN_TILE_SIZE, x1z1, z * Configuration.TERRAIN_TILE_SIZE)));
			
			//Go one way with z
			//TODO
			while (z < Configuration.TERRAIN_SIZE) {
				cornerValues = generateCornerValues(x, z);
				x1z2 = cornerValues[2];
				x2z2 = cornerValues[3];
				vertices.add(new Vertex(new Vector3f((x + 1) * Configuration.TERRAIN_TILE_SIZE, x2z2, (z + 1) * Configuration.TERRAIN_TILE_SIZE)));
				vertices.add(new Vertex(new Vector3f(x * Configuration.TERRAIN_TILE_SIZE, x1z2, (z + 1) * Configuration.TERRAIN_TILE_SIZE)));
				z++;
			}
			z--;
			
			//Increase x to next row
			x++;
			
			//Two points to start the second row
			//TODO
			cornerValues = generateCornerValues(x, z);
			x2z2 = cornerValues[3];
			x1z2 = cornerValues[2];
			vertices.add(new Vertex(new Vector3f(x * Configuration.TERRAIN_TILE_SIZE, x1z2, (z + 1) * Configuration.TERRAIN_TILE_SIZE)));
			vertices.add(new Vertex(new Vector3f((x + 1) * Configuration.TERRAIN_TILE_SIZE, x2z2, (z + 1) * Configuration.TERRAIN_TILE_SIZE)));
			
			//Go the other way
			//TODO
			while (z > -1) {
				cornerValues = generateCornerValues(x, z);
				x2z1 = cornerValues[1];
				x1z1 = cornerValues[0];
				vertices.add(new Vertex(new Vector3f(x * Configuration.TERRAIN_TILE_SIZE, x1z1, z * Configuration.TERRAIN_TILE_SIZE)));
				vertices.add(new Vertex(new Vector3f((x + 1) * Configuration.TERRAIN_TILE_SIZE, x2z1, z * Configuration.TERRAIN_TILE_SIZE)));
				z--;
			}
			
			//Finished with this row, increase again
			x++;
		}		

		//Register with the renderer
		renderer.registerRenderable(this);
	}

	private float average(float... floats) {
		float sum = 0.0f;
		for (float f : floats) {
			sum += f;
		}
		return sum / floats.length;
	}
	
	/**
	 * 
	 * rc[0] = x1z1 = Corner towards x-1,z-1
	 * rc[1] = x2z1 = Corner towards x+1,z-1
	 * rc[2] = x2z2 = Corner towards x+1,z+1
	 * rc[3] = x1z2 = Corner towards x-1,z+1
	 * 	
	 * @param heightMap
	 * @param x
	 * @param z
	 * @return and array containing the corner values as described above
	 */
	private float[] generateCornerValues(int x, int z) {
		/*
		 * Map of all surrounding heights.
		 * 
		 *        North
		 * 	       -z
		 * 
		 * E    h1|h2|h3    W
		 * a -x h4|XZ|h5 +x e
		 * s    h6|h7|h8    s
		 * t                t
		 *         +z
		 *        South
		 *       
		 * rc[0] = x1z1 = Corner towards x-1,z-1
		 * rc[1] = x2z1 = Corner towards x+1,z-1
		 * rc[2] = x2z2 = Corner towards x+1,z+1
		 * rc[3] = x1z2 = Corner towards x-1,z+1
		 * 
		 */
		float[] rc = new float[4];
		
		float h1, h2, h3, h4, h5, h6, h7, h8;
		//H1
		if (x == 0) {
			if (z == 0) {
				h1 = heightMap[x][z];
			} else {
				h1 = heightMap[x][z-1];
			}
		} else {
			if (z == 0) {
				h1 = heightMap[x-1][z];
			} else {
				h1 = heightMap[x-1][z-1];
			}
		}
		//H2
		if (z == 0) {
			h2 = heightMap[x][z];
		} else {
			h2 = heightMap[x][z-1];
		}
		//H3
		if (x == Configuration.TERRAIN_SIZE - 1) {
			if (z == 0) {
				h3 = heightMap[x][z];
			} else {
				h3 = heightMap[x][z-1];
			}
		} else {
			if (z == 0) {
				h3 = heightMap[x+1][z];
			} else {
				h3 = heightMap[x+1][z-1];
			}
		}
		//H4
		if (x == 0) {
			h4 = heightMap[x][z];
		} else {
			h4 = heightMap[x-1][z];
		}
		//H5
		if (x == Configuration.TERRAIN_SIZE - 1) {
			h5 = heightMap[x][z];
		} else {
			h5 = heightMap[x+1][z];
		}
		//H6
		if (x == 0) {
			if (z == Configuration.TERRAIN_SIZE - 1) {
				h6 = heightMap[x][z];
			} else {
				h6 = heightMap[x][z+1];
			}
		} else {
			if (z == Configuration.TERRAIN_SIZE - 1) {
				h6 = heightMap[x-1][z];
			} else {
				h6 = heightMap[x-1][z+1];
			}
		}
		//H7
		if (z == Configuration.TERRAIN_SIZE - 1) {
			h7 = heightMap[x][z];
		} else {
			h7 = heightMap[x][z+1];
		}
		//H8
		if (x == Configuration.TERRAIN_SIZE - 1) {
			if (z == Configuration.TERRAIN_SIZE - 1) {
				h8 = heightMap[x][z];
			} else {
				h8 = heightMap[x][z+1];
			}
		} else {
			if (z == Configuration.TERRAIN_SIZE - 1) {
				h8 = heightMap[x+1][z];
			} else {
				h8 = heightMap[x+1][z+1];
			}
		}
		rc[0] = average(h1, h2, h4, heightMap[x][z]);
		rc[1] = average(h2, h3, h5, heightMap[x][z]);
		rc[2] = average(h4, h6, h7, heightMap[x][z]);
		rc[3] = average(h5, h7, h8, heightMap[x][z]);
		
		return rc;
	}
	
	private void distributeMovedMaterial(int curX, int curZ, float[][] newHeightMap, float amountMoved) {
		float amountPart = amountMoved / 8;
		
		if (curX != 0) {
			newHeightMap[curX-1][curZ] += amountPart;
		}
		if (curX != Configuration.TERRAIN_SIZE - 1) {
			newHeightMap[curX+1][curZ] += amountPart;
		}
		if (curZ != 0) {
			newHeightMap[curX][curZ-1] += amountPart;
		}
		if (curZ != Configuration.TERRAIN_SIZE - 1) {
			newHeightMap[curX][curZ+1] += amountPart;
		}
		newHeightMap[curX][curZ] += amountPart * 4;
	}
	
}
