package no.plasmid.erosion.im;

import java.io.FileNotFoundException;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;

import org.lwjgl.LWJGLException;
import org.lwjgl.util.vector.Vector3f;

import no.plasmid.erosion.CLPerlinNoise;
import no.plasmid.erosion.CLWrapper;
import no.plasmid.erosion.Configuration;
import no.plasmid.erosion.PerlinNoise;
import no.plasmid.erosion.Renderer;

public class Terrain extends Renderable {

	private static final Vector3f COLOR_HIGH_GRAS = new Vector3f(0.0f, 0.8f, 0.1f);
	private static final Vector3f COLOR_LOW_GRAS = new Vector3f(0.0f, 0.6f, 0.1f);
	private static final Vector3f COLOR_SAND = new Vector3f(0.85f, 0.6f, 0.0f);
	private static final Vector3f COLOR_STONE = new Vector3f(0.65f, 0.65f, 0.65f);
	private static final Vector3f COLOR_SNOW = new Vector3f(0.95f, 0.95f, 1.0f);
	private static final Vector3f COLOR_WATER = new Vector3f(0.2f, 0.2f, 1.0f);
	
	private float[][] heightMap;
	private int[][] waterMap;
	private boolean erosionFinished;

	private boolean applicationExiting;
	private boolean erosionRunning;
	
	public void createInitialTerrain(CLWrapper clWrapper) {
		heightMap = new float[Configuration.TERRAIN_SIZE][Configuration.TERRAIN_SIZE];
		waterMap = new int[Configuration.TERRAIN_SIZE][Configuration.TERRAIN_SIZE];

		//To seed the erosion
		Random random = new Random(Configuration.TERRAIN_NOISE_RANDOM_SEED);

		long startTime = System.currentTimeMillis();
		if (Configuration.USE_OPEN_CL) {
			generateNoiseHeightMapOpenCL(clWrapper);
		} else {
			generateNoiseHeightMapJava();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Terrain generation time: " + (endTime - startTime));
		
		for (int x = 0; x < Configuration.TERRAIN_SIZE; x++) {
			for (int z = 0; z < Configuration.TERRAIN_SIZE; z++) {
				heightMap[x][z] = (float)((heightMap[x][z] * 190 * Math.sin(((double)x / Configuration.TERRAIN_SIZE)* Math.PI) * Math.sin(((double)z / Configuration.TERRAIN_SIZE)* Math.PI))
						+ (Math.sin(((double)x / Configuration.TERRAIN_SIZE)* Math.PI) * Math.sin(((double)z / Configuration.TERRAIN_SIZE)* Math.PI) * Configuration.TERRAIN_SINE_AMPLITUDE)
						+ Configuration.TERRAIN_INITIAL_HEIGHT + random.nextFloat() * Configuration.TERRAIN_RANDOM_AMPLITUDE);
			}
		}
	}

	private void generateNoiseHeightMapJava() {
		PerlinNoise perlinNoise = new PerlinNoise(Configuration.TERRAIN_NOISE_PERSISTENCE, Configuration.TERRAIN_NOISE_FREQUENCY,
				Configuration.TERRAIN_NOISE_AMPLITUDE, Configuration.TERRAIN_NOISE_OCTAVES, Configuration.TERRAIN_NOISE_RANDOM_SEED);

		for (int x = 0; x < Configuration.TERRAIN_SIZE; x++) {
			for (int z = 0; z < Configuration.TERRAIN_SIZE; z++) {
				heightMap[x][z] = perlinNoise.getHeight(x, z);
			}
		}
	}
	private void generateNoiseHeightMapOpenCL(CLWrapper clWrapper) {
		CLPerlinNoise clNoise = new CLPerlinNoise(clWrapper);

		try {
			clNoise.prepareDataAndProgram();
		
			FloatBuffer heightValues = clNoise.calculateOpenCL();
			
			for (int x = 0; x < Configuration.TERRAIN_SIZE; x++) {
				for (int z = 0; z < Configuration.TERRAIN_SIZE; z++) {
					heightMap[x][z] = heightValues.get(x + z * Configuration.TERRAIN_SIZE);
				}
			}
			
			clNoise.cleanupDataAndProgram();
		} catch (FileNotFoundException | LWJGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void triggerErosions() {
		Thread t = new Thread() {

			private String LOCK = "";
			
			@Override
			public void run() {
				runErsions(LOCK);
			}
					
		};
		t.start();
	}
	
	private void runErsions(String LOCK) {
		/*
		 * Do the erosion
		 */
		float[][][] newHeightMaps = new float[2][Configuration.TERRAIN_SIZE][Configuration.TERRAIN_SIZE];
		for (int i = 0; i < Configuration.TERRAIN_SIZE; i++) {
			newHeightMaps[0][i] = heightMap[i].clone();
			newHeightMaps[1][i] = heightMap[i].clone();
		}
		
		float landslideDeltaHeight = (float)((float)Configuration.TERRAIN_TILE_SIZE / Math.cos(Math.toRadians(Configuration.TERRAIN_LANDSLIDE_ANGLE)));
		
		/*
		 * 0 = none
		 * 1 = north (z -1)
		 * 2 = east (x + 1)
		 * 3 = south (z + 1)
		 * 4 = west (x - 1)
		 * 
		 * 5 mean edge of map
		 */
		int completedErosionSteps = 0;
		while (!applicationExiting) {
			if (!isErosionRunning()) {
				//Just wait
				synchronized (LOCK) {
					try {
						LOCK.wait(100);
					} catch (InterruptedException e) {
						//This is ok
					}
				}
			} else {
				//Do erosion
				int[][] newWaterMap = new int[Configuration.TERRAIN_SIZE][Configuration.TERRAIN_SIZE];
				for (int x = 0; x < Configuration.TERRAIN_SIZE; x++) {
					for (int z = 0; z < Configuration.TERRAIN_SIZE; z++) {
						int curX = x;
						int curZ = z;
						boolean finished = false;
						float amountMoved = 0.0f;
						while (!finished) {
							if (newHeightMaps[completedErosionSteps%2][curX][curZ] < -50.0f) {
								//If under water, don't erode
								finished = true;
								
								distributeMovedMaterial(curX, curZ, newHeightMaps[(completedErosionSteps+1)%2], amountMoved);
								continue;
							}
							
							//Find lower neighbor
							float lowestPoint = newHeightMaps[completedErosionSteps%2][curX][curZ];
							int lowestNeighbor = 0;
							if (curX == 0 || curX == Configuration.TERRAIN_SIZE - 1 || curZ == 0 || curZ == Configuration.TERRAIN_SIZE - 1) {
								lowestNeighbor = 5;
								lowestPoint = Float.NEGATIVE_INFINITY;
							}
							if (curZ != 0 && newHeightMaps[completedErosionSteps%2][curX][curZ-1] < lowestPoint) {
								lowestPoint = newHeightMaps[completedErosionSteps%2][curX][curZ-1];
								lowestNeighbor = 1;
								newWaterMap[curX][curZ-1]++;
							}
							if (curX != Configuration.TERRAIN_SIZE - 1 && newHeightMaps[completedErosionSteps%2][curX+1][curZ] < lowestPoint) {
								lowestPoint = newHeightMaps[completedErosionSteps%2][curX+1][curZ];
								lowestNeighbor = 2;
								newWaterMap[curX+1][curZ]++;
							}
							if (curZ != Configuration.TERRAIN_SIZE - 1 && newHeightMaps[completedErosionSteps%2][curX][curZ+1] < lowestPoint) {
								lowestPoint = newHeightMaps[completedErosionSteps%2][curX][curZ+1];
								lowestNeighbor = 3;
								newWaterMap[curX][curZ+1]++;
							}
							if (curX != 0 && newHeightMaps[completedErosionSteps%2][curX-1][curZ] < lowestPoint) {
								lowestPoint = newHeightMaps[completedErosionSteps%2][curX-1][curZ];
								lowestNeighbor = 4;
								newWaterMap[curX-1][curZ]++;
							}
							
							switch (lowestNeighbor) {
							case 0:
								finished = true;
								distributeMovedMaterial(curX, curZ, newHeightMaps[(completedErosionSteps+1)%2], amountMoved);
								break;
							case 1:
								if (((newHeightMaps[completedErosionSteps%2][curX][curZ] - lowestPoint) > landslideDeltaHeight) && (Math.random() < Configuration.TERRAIN_LANDSLIDE_CHANCE)) {
									float deltaY = (newHeightMaps[completedErosionSteps%2][curX][curZ] - lowestPoint) / 2;
									newHeightMaps[(completedErosionSteps+1)%2][curX][curZ] -= deltaY;
									distributeMovedMaterial(curX, curZ - 1, newHeightMaps[(completedErosionSteps+1)%2], deltaY);
								} else {
									newHeightMaps[(completedErosionSteps+1)%2][curX][curZ] -= Configuration.TERRAIN_EROSION_AMOUNT;
									amountMoved += Configuration.TERRAIN_EROSION_AMOUNT;
								}
								curZ = curZ - 1;
								break;
							case 2:
								if (((newHeightMaps[completedErosionSteps%2][curX][curZ] - lowestPoint) > landslideDeltaHeight) && (Math.random() < Configuration.TERRAIN_LANDSLIDE_CHANCE)) {
									float deltaY = (newHeightMaps[completedErosionSteps%2][curX][curZ] - lowestPoint) / 2;
									newHeightMaps[(completedErosionSteps+1)%2][curX][curZ] -= deltaY;
									distributeMovedMaterial(curX + 1, curZ, newHeightMaps[(completedErosionSteps+1)%2], deltaY);
								} else {
									newHeightMaps[(completedErosionSteps+1)%2][curX][curZ] -= Configuration.TERRAIN_EROSION_AMOUNT;
									amountMoved += Configuration.TERRAIN_EROSION_AMOUNT;
								}
								curX = curX + 1;
								break;
							case 3:
								if (((newHeightMaps[completedErosionSteps%2][curX][curZ] - lowestPoint) > landslideDeltaHeight) && (Math.random() < Configuration.TERRAIN_LANDSLIDE_CHANCE)) {
									float deltaY = (newHeightMaps[completedErosionSteps%2][curX][curZ] - lowestPoint) / 2;
									newHeightMaps[(completedErosionSteps+1)%2][curX][curZ] -= deltaY;
									distributeMovedMaterial(curX, curZ + 1, newHeightMaps[(completedErosionSteps+1)%2], deltaY);
								} else {
									newHeightMaps[(completedErosionSteps+1)%2][curX][curZ] -= Configuration.TERRAIN_EROSION_AMOUNT;
									amountMoved += Configuration.TERRAIN_EROSION_AMOUNT;
								}
								curZ = curZ + 1;
								break;
							case 4:
								if (((newHeightMaps[completedErosionSteps%2][curX][curZ] - lowestPoint) > landslideDeltaHeight) && (Math.random() < Configuration.TERRAIN_LANDSLIDE_CHANCE)) {
									float deltaY = (newHeightMaps[completedErosionSteps%2][curX][curZ] - lowestPoint) / 2;
									newHeightMaps[(completedErosionSteps+1)%2][curX][curZ] -= deltaY;
									distributeMovedMaterial(curX - 1, curZ, newHeightMaps[(completedErosionSteps+1)%2], deltaY);
								} else {
									newHeightMaps[(completedErosionSteps+1)%2][curX][curZ] -= Configuration.TERRAIN_EROSION_AMOUNT;
									amountMoved += Configuration.TERRAIN_EROSION_AMOUNT;
								}
								curX = curX - 1;
								break;
							case 5:
								//Edge of map, so discard the material
								finished = true;
								break;
							default:
								finished = true;
								distributeMovedMaterial(curX, curZ, newHeightMaps[(completedErosionSteps+1)%2], amountMoved);
								break;
							}
						}
					}					
				}
				
				for (int j = 0; j < Configuration.TERRAIN_SIZE; j++) {
					newHeightMaps[completedErosionSteps%2][j] = newHeightMaps[(completedErosionSteps+1)%2][j].clone();
				}
			
				if (completedErosionSteps % Configuration.TERRAIN_EROSION_STEPS == 0) {
					/*
					 * Prepare for mesh generation
					 */
					for (int j = 0; j < Configuration.TERRAIN_SIZE; j++) {
						heightMap[j] = newHeightMaps[(completedErosionSteps+1)%2][j].clone();
						waterMap[j] = newWaterMap[j].clone();
					}
					//Clean the edge
					for (int x = 0; x < Configuration.TERRAIN_SIZE; x++) {
						heightMap[x][0] = -550.0f;
						heightMap[x][Configuration.TERRAIN_SIZE-1] = -550.0f;
					}
					for (int z = 0; z < Configuration.TERRAIN_SIZE; z++) {
						heightMap[0][z] = -550.0f;
						heightMap[Configuration.TERRAIN_SIZE-1][z] = -550.0f;
					}
					System.out.println("Total erosions steps: " + completedErosionSteps);
					erosionFinished = true;
				}
				
				completedErosionSteps++;
			}
		}
	}
	
	public void createMesh(Renderer renderer) {
		Vector3f[][] vertices = new Vector3f[Configuration.TERRAIN_SIZE+1][Configuration.TERRAIN_SIZE+1];
		int[][] waters = new int[Configuration.TERRAIN_SIZE+1][Configuration.TERRAIN_SIZE+1];
		
		/*
		 * First we must generate the array of vertices and normals. Later we will pick from these when we generate the vertex list.
		 * 
		 * To generate each vertex height, we average the heights of the surrounding squares, in this pattern:
		 * 
		 * H1|H2
		 * --+--		We are interested in the height in the middle, at x,z
		 * H3|h4
		 * 
		 * The squares are:
		 * H1: Square at x-1,z-1
		 * H2: Square at x,z-1
		 * H3: Square at x-1,z
		 * H4: Square at x+1,z
		 * 
		 */
		for (int x = 0; x < Configuration.TERRAIN_SIZE+1; x++) {
			for (int z = 0; z < Configuration.TERRAIN_SIZE+1; z++) {
				float h1 = heightMap[x!=0?x-1:x][z!=0?z-1:z];
				float h2 = heightMap[x!=Configuration.TERRAIN_SIZE?x:x-1][z!=0?z-1:z]; 
				float h3 = heightMap[x!=0?x-1:x][z!=Configuration.TERRAIN_SIZE?z:z-1]; 
				float h4 = heightMap[x!=Configuration.TERRAIN_SIZE?x:x-1][z!=Configuration.TERRAIN_SIZE?z:z-1]; 
				float height = average(h1, h2, h3, h4);
				vertices[x][z] = new Vector3f(x * Configuration.TERRAIN_TILE_SIZE, height, z * Configuration.TERRAIN_TILE_SIZE);
				int w1 = waterMap[x!=0?x-1:x][z!=0?z-1:z];
				int w2 = waterMap[x!=Configuration.TERRAIN_SIZE?x:x-1][z!=0?z-1:z]; 
				int w3 = waterMap[x!=0?x-1:x][z!=Configuration.TERRAIN_SIZE?z:z-1]; 
				int w4 = waterMap[x!=Configuration.TERRAIN_SIZE?x:x-1][z!=Configuration.TERRAIN_SIZE?z:z-1]; 
				int waterCount = average(w1, w2, w3, w4);
				waters[x][z] = waterCount;
			}
		}
		
		List<Vertex> vertexList = this.getVertices();
		vertexList.clear();
		
		/*
		 * Create quads for each point of terrain
		 */
		for (int x = 0; x < Configuration.TERRAIN_SIZE; x++) {
			for (int z = 0; z < Configuration.TERRAIN_SIZE; z++) {
				//Create normal for the quad
				Vector3f normal = generateNormal(x, z, vertices);
				normal = Vector3f.add(normal, generateNormal(x+1, z, vertices), normal);
				normal = Vector3f.add(normal, generateNormal(x+1, z+1, vertices), normal);
				normal = Vector3f.add(normal, generateNormal(x, z+1, vertices), normal);
				normal.normalise();
				
				//Pick color for the quad
				Vector3f color = pickColor(heightMap[x][z], normal, waterMap[x][z]);
				
				//Generate quad
				vertexList.add(new Vertex(vertices[x][z+1], normal, color));
				vertexList.add(new Vertex(vertices[x+1][z+1], normal, color));
				vertexList.add(new Vertex(vertices[x+1][z], normal, color));
				vertexList.add(new Vertex(vertices[x][z], normal, color));
			}
		}

		//Register with the renderer
		renderer.registerRenderable(this);
		erosionFinished = false;
	}

	private float average(float... floats) {
		float sum = 0.0f;
		for (float f : floats) {
			sum += f;
		}
		return sum / floats.length;
	}
	
	private int average(int... ints) {
		int sum = 0;
		for (int i : ints) {
			sum += i;
		}
		return sum / ints.length;
	}
	
	private Vector3f pickColor(float height, Vector3f normal, int waterCount) {
		Vector3f color = COLOR_LOW_GRAS;
		if (height > 1500.0f) {
			color = COLOR_HIGH_GRAS;
		}
		if (normal.y < 0.90) {
			color = COLOR_STONE;
		}
		if (height > 3000.0f) {
			color = COLOR_STONE;
		}
		if (height < 10.0f) {
			color = COLOR_SAND;
		}
		if (waterCount > Configuration.RIVER_THRESHOLD) {
			color = COLOR_WATER;
		}
		if (height > 4500.0f) {
			color = COLOR_SNOW;
		}
		
		return color;
	}
		
	private Vector3f generateNormal(int x, int z, Vector3f[][] vertices) {
		float height = vertices[x][z].y;
		Vector3f v1 = new Vector3f(Configuration.TERRAIN_TILE_SIZE, vertices[x!=Configuration.TERRAIN_SIZE?x+1:x][z].y - height, 0.0f);	//Eastbound vector
		Vector3f v2 = new Vector3f(0.0f, vertices[x][z!=Configuration.TERRAIN_SIZE?z+1:z].y - height, Configuration.TERRAIN_TILE_SIZE);	//Southbound vector
		Vector3f v3 = new Vector3f(-Configuration.TERRAIN_TILE_SIZE, vertices[x!=0?x-1:x][z].y - height, 0.0f);	//Westbound vector
		Vector3f v4 = new Vector3f(0.0f, vertices[x][z!=0?z-1:z].y - height, -Configuration.TERRAIN_TILE_SIZE);	//Northbound vector
		
		Vector3f v5 = Vector3f.cross(v2, v1, null);
		Vector3f v6 = Vector3f.cross(v4, v3, null);
		
		return new Vector3f(v5.x + v6.x, v5.y + v6.y, v5.z + v6.z).normalise(null);
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

	public boolean isErosionFinished() {
		return erosionFinished;
	}

	public void setErosionFinished(boolean erosionFinished) {
		this.erosionFinished = erosionFinished;
	}

	public boolean isErosionRunning() {
		return erosionRunning;
	}

	public void setErosionRunning(boolean erosionRunning) {
		this.erosionRunning = erosionRunning;
	}

	public boolean isApplicationExiting() {
		return applicationExiting;
	}

	public void setApplicationExiting(boolean applicationExiting) {
		this.applicationExiting = applicationExiting;
	}
	
}
