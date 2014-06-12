package no.plasmid.erosion;

public class Configuration {
	public static final String WINDOW_TITLE	= "Erosion";
	public static final int WINDOW_WIDTH		= 900;
	public static final int WINDOW_HEIGTH		= 900;
	public static final float Z_NEAR				= 2.0f;
	public static final float Z_FAR					= 25000.0f;
	public static final float FIELD_OF_VIEW	= 60.0f;	//Degrees
	
	public static final float CAMERA_MOVEMENT_SPEED	= 200.0f;
	public static final float CAMERA_ROTATION_SPEED	= 2.0f;	//Degrees
	
	public static final int TERRAIN_SIZE		= 128;	//Unit is terrain tiles
	public static final int TERRAIN_TILE_SIZE	= 256;	//Unit is meters
	
	public static final double TERRAIN_NOISE_PERSISTENCE	= 0.42;
	public static final double TERRAIN_NOISE_FREQUENCY	= 0.035;
	public static final double TERRAIN_NOISE_AMPLITUDE	= 70.0;
	public static final int TERRAIN_NOISE_OCTAVES			= 10;
//	public static final int TERRAIN_NOISE_RANDOM_SEED		= (int)(System.currentTimeMillis() % 46340);
	public static final int TERRAIN_NOISE_RANDOM_SEED		= 1;
	public static final float TERRAIN_EROSION_AMOUNT		= 0.0001f;
	public static final int TERRAIN_EROSION_STEPS				= 100000;		//Number of erosion steps each time the user presses space
	public static final float TERRAIN_LANDSLIDE_ANGLE		= 45.0f;	//Angle at which the terrain is considered for a landslide
	public static final float TERRAIN_LANDSLIDE_CHANCE	= 0.001f;	//Chance of landslide occuring (considered each tick, so should be low)

	public static final float[] FOG_COLOR = new float[]{1.0f, 1.0f, 1.0f};
	public static final float FOG_START 	= 20000;
	public static final float FOG_TRANSPARENCY_START 	= 22500;
	
}
