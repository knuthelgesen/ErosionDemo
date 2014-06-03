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
				heightMap[x][z] = (float)(Math.sin(((double)x / Configuration.TERRAIN_SIZE)* Math.PI) * Math.sin(((double)z / Configuration.TERRAIN_SIZE)* Math.PI) * 2500) - 150.0f + random.nextFloat() * 100;
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
		for (int x = 0; x < Configuration.TERRAIN_SIZE; x++) {
			for (int z = 0; z < Configuration.TERRAIN_SIZE; z++) {
				int curX = x;
				int curZ = z;
				boolean finished = false;
				float amountMoved = 0.0f;
				while (!finished) {
					if (heightMap[curX][curZ] < -10.0f) {
						//If under water, don't erode
						finished = true;
						newHeightMap[curX][curZ] += amountMoved;
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
//					System.out.println("Old height: " + newHeightMap[curX][curZ]);
						newHeightMap[curX][curZ] += amountMoved;
//						System.out.println("New height: " + newHeightMap[curX][curZ]);
						break;
					case 1:
						if (((heightMap[curX][curZ] - lowestPoint) > landslideDeltaHeight) && (Math.random() < Configuration.TERRAIN_LANDSLIDE_CHANCE)) {
							newHeightMap[curX][curZ] -= (heightMap[curX][curZ] - lowestPoint) / 2;
							newHeightMap[curX][curZ-1] += (heightMap[curX][curZ] - lowestPoint) / 2;
						} else {
							newHeightMap[curX][curZ] -= Configuration.TERRAIN_EROSION_AMOUNT;
							amountMoved += Configuration.TERRAIN_EROSION_AMOUNT;
						}
						curZ = curZ - 1;
						break;
					case 2:
						if (((heightMap[curX][curZ] - lowestPoint) > landslideDeltaHeight) && (Math.random() < Configuration.TERRAIN_LANDSLIDE_CHANCE)) {
							newHeightMap[curX][curZ] -= (heightMap[curX][curZ] - lowestPoint) / 2;
							newHeightMap[curX+1][curZ] += (heightMap[curX][curZ] - lowestPoint) / 2;
						} else {
							newHeightMap[curX][curZ] -= Configuration.TERRAIN_EROSION_AMOUNT;
							amountMoved += Configuration.TERRAIN_EROSION_AMOUNT;
						}
						curX = curX + 1;
						break;
					case 3:
						if (((heightMap[curX][curZ] - lowestPoint) > landslideDeltaHeight) && (Math.random() < Configuration.TERRAIN_LANDSLIDE_CHANCE)) {
							newHeightMap[curX][curZ] -= (heightMap[curX][curZ] - lowestPoint) / 2;
							newHeightMap[curX][curZ+1] += (heightMap[curX][curZ] - lowestPoint) / 2;
						} else {
							newHeightMap[curX][curZ] -= Configuration.TERRAIN_EROSION_AMOUNT;
							amountMoved += Configuration.TERRAIN_EROSION_AMOUNT;
						}
						curZ = curZ + 1;
						break;
					case 4:
						if (((heightMap[curX][curZ] - lowestPoint) > landslideDeltaHeight) && (Math.random() < Configuration.TERRAIN_LANDSLIDE_CHANCE)) {
							newHeightMap[curX][curZ] -= (heightMap[curX][curZ] - lowestPoint) / 2;
							newHeightMap[curX-1][curZ] += (heightMap[curX][curZ] - lowestPoint) / 2;
						} else {
							newHeightMap[curX][curZ] -= Configuration.TERRAIN_EROSION_AMOUNT;
							amountMoved += Configuration.TERRAIN_EROSION_AMOUNT;
						}
						curX = curX - 1;
						break;
					default:
						finished = true;
						newHeightMap[curX][curZ] += amountMoved;
						break;
					}
				}
			}					
		}
		
		heightMap = newHeightMap.clone();
		
		/*
		 * Create mesh for the terrain
		 */
		List<Vertex> vertices = this.getVertices();
		vertices.clear();
		for (int x = 0; x < Configuration.TERRAIN_SIZE; x++) {
			for (int z = 0; z < Configuration.TERRAIN_SIZE; z++) {
				/*
				 * Find all surrounding heights.
				 * 
				 * 	     -z
				 * 
				 *    h1|h2|h3
				 * -x h4|XZ|h5 +x 
				 *    h6|h7|h8
				 *    
				 *       +z
				 *       
				 * x1z1 = Corner towards x-1,z-1
				 * x2z1 = Corner towards x+1,z-1
				 * x2z2 = Corner towards x+1,z+1
				 * x1z2 = Corner towards x-1,z+1
				 * 
				 */
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
				
				float x1z1 = average(h1, h2, h4, heightMap[x][z]);
				float x2z1 = average(h2, h3, h5, heightMap[x][z]);
				float x1z2 = average(h4, h6, h7, heightMap[x][z]);
				float x2z2 = average(h5, h7, h8, heightMap[x][z]);
				
				vertices.add(new Vertex(new Vector3f((x + 1) * Configuration.TERRAIN_TILE_SIZE, x2z1, z * Configuration.TERRAIN_TILE_SIZE)));
				vertices.add(new Vertex(new Vector3f(x * Configuration.TERRAIN_TILE_SIZE, x1z1, z * Configuration.TERRAIN_TILE_SIZE)));
				vertices.add(new Vertex(new Vector3f((x + 1) * Configuration.TERRAIN_TILE_SIZE, x2z2, (z + 1) * Configuration.TERRAIN_TILE_SIZE)));
				vertices.add(new Vertex(new Vector3f(x * Configuration.TERRAIN_TILE_SIZE, x1z2, (z + 1) * Configuration.TERRAIN_TILE_SIZE)));
			}
		}
		
		renderer.registerRenderable(this);
	}

	private float average(float... floats) {
		float sum = 0.0f;
		for (float f : floats) {
			sum += f;
		}
		return sum / floats.length;
	}
	
}
