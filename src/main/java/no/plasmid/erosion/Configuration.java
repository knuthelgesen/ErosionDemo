package no.plasmid.erosion;

public class Configuration {
	public static final String WINDOW_TITLE	= "Erosion";
	public static final int WINDOW_WIDTH		= 900;
	public static final int WINDOW_HEIGTH		= 900;
	public static final float Z_NEAR				= 0.1f;
	public static final float Z_FAR					= 20000.0f;
	public static final float FIELD_OF_VIEW	= 60.0f;	//Degrees

	public static final int TERRAIN_SIZE		= 64;	//Unit is terrain tiles
	public static final int TERRAIN_TILE_SIZE	= 256;	//Unit is meters
	
	public static final double TERRAIN_NOISE_PERSISTENCE	= 0.42;
	public static final double TERRAIN_NOISE_FREQUENCY	= 0.05;
	public static final double TERRAIN_NOISE_AMPLITUDE	= 70.0;
	public static final int TERRAIN_NOISE_OCTAVES			= 5;
//	public static final int TERRAIN_NOISE_RANDOM_SEED		= (int)(System.currentTimeMillis() % 46340);
	public static final int TERRAIN_NOISE_RANDOM_SEED		= 1;
	public static final float TERRAIN_EROSION_AMOUNT		= 0.0001f;
	public static final float TERRAIN_LANDSLIDE_ANGLE		= 60.0f;	//Angle at which the terrain is considered for a landslide
	public static final float TERRAIN_LANDSLIDE_CHANCE	= 0.0001f;	//Chance of landslide occuring (considered each tick, so should be low)
}
