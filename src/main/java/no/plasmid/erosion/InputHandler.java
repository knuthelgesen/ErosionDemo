package no.plasmid.erosion;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

public class InputHandler {

	private boolean closeRequested;
	private boolean[] keyStatus;
	
	public InputHandler() {
		keyStatus = new boolean[256];
	}
	
	public void handleInput() {
		if (Display.isCloseRequested()) {
			closeRequested = true;
		}

		//Handle keyboard input
		while (Keyboard.next()) {
			if (Keyboard.getEventKeyState()) {
				//Key down
				int key = Keyboard.getEventKey();
				switch (key) {
					case Keyboard.KEY_ESCAPE: {
						closeRequested = true;
					}
					default: {
						keyStatus[key] = true;
					}
				}
			} else {
				//Key up
				int key = Keyboard.getEventKey();
				switch (key) {
					case Keyboard.KEY_ESCAPE: {
						closeRequested = true;
					}
					default: {
						keyStatus[key] = false;
					}
				}
			}
		}
	}
	
	public boolean isCloseRequested() {
		return closeRequested;
	}
	
	public boolean[] getKeyStatus() {
		return keyStatus;
	}
	
}
