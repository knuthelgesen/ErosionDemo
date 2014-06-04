package no.plasmid.erosion.im;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class Camera {

	private Vector3f position;	//World space
	private Vector3f orientation;	//In degrees, around each axis
	
	public Camera() {
		position = new Vector3f();
		orientation = new Vector3f();
	}
	
	public void setPosition(Vector3f position) {
		this.position = position;
	}

	public void setOrientation(Vector3f orientation) {
		this.orientation = orientation;
	}

	public void moveCamera(Vector3f movement) {
		position.x += movement.x * Math.sin((Math.toRadians(orientation.y + 90)));
		position.z += movement.x * Math.cos((Math.toRadians(orientation.y - 90)));
		
		position.y += movement.y;

		position.x += movement.z * Math.sin((Math.toRadians(orientation.y - 180)));
		position.z += movement.z * Math.cos((Math.toRadians(orientation.y)));
		position.y += movement.z * Math.sin((Math.toRadians(orientation.x)));
	}
	
	public void rotateCamera(Vector3f rotation) {
		orientation.x += rotation.x;
		orientation.y += rotation.y;
		orientation.z += rotation.z;
	}
	
	public Matrix4f createViewMatrix() {
		Matrix4f rc = new Matrix4f();
		
		rc.rotate((float)Math.toRadians(orientation.x), new Vector3f(1.0f, 0.0f, 0.0f));
		rc.rotate((float)Math.toRadians(orientation.y), new Vector3f(0.0f, 1.0f, 0.0f));
		rc.rotate((float)Math.toRadians(orientation.z), new Vector3f(0.0f, 0.0f, 1.0f));
		
		rc.translate(position);

		return rc;
	}
		
}
