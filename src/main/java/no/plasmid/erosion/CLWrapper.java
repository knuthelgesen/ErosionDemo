package no.plasmid.erosion;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Scanner;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLPlatform;

/**
 * Class that handles OpenCL platform, device and context. Does not manage individual programs or kernels.
 * 
 * @author helgesk
 *
 */
public class CLWrapper {

	private IntBuffer errorCodeBuffer;
	
	private CLPlatform platform = null;
	private List<CLDevice> deviceList = null;
	private CLContext context = null;
	private CLCommandQueue commandQueue = null;
	
	public void initializeOpenCL() throws LWJGLException {
		errorCodeBuffer = BufferUtils.createIntBuffer(1);

		CL.create();
		//Prepare platform
		platform = CLPlatform.getPlatforms().get(0);
		//Get list of devices
		deviceList = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);
		//Get context
		context = CLContext.create(platform, deviceList, null, null, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
		//Create command queue
		commandQueue = CL10.clCreateCommandQueue(context, deviceList.get(0), CL10.CL_QUEUE_PROFILING_ENABLE, errorCodeBuffer);
		checkErrorCodeBuffer(errorCodeBuffer);
	}
	
	public void cleanupOpenCL() {
		//Clean up OpenCL resources
		if (null != commandQueue) {
			CL10.clReleaseCommandQueue(commandQueue);
		}
		if (null != context) {
			CL10.clReleaseContext(context);
		}
		CL.destroy();
	}

	public void checkErrorCodeBuffer(IntBuffer errorCodeBuffer) {
		if (errorCodeBuffer.get(0) != 0) {
			System.out.println("Got error code " + errorCodeBuffer.get(0) + " from OpenCL");
		}
	}
	
	public String loadTextFile(String fileName) throws FileNotFoundException {
		URL fileURL = CLWrapper.class.getResource(fileName);
		if (fileURL == null) {
			throw new FileNotFoundException("Could not find shader source file " + fileName);
		}
		
		StringBuilder text = new StringBuilder();
		String NL = System.getProperty("line.separator");
		FileInputStream fis;
		Scanner scanner = null;
		try {
			fis = new FileInputStream(fileURL.getFile());
			scanner = new Scanner(fis);
			
			while (scanner.hasNextLine()) {
				text.append(scanner.nextLine() + NL);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
		
		return text.toString();
	}
	
	public void printChars(ByteBuffer buffer) {
		for (int i = 0; i < buffer.capacity(); i++) {
			System.out.print(((char)buffer.get(i)));
		}
		System.out.println("");
	}

	public IntBuffer geErrorCodeBuffer() {
		return errorCodeBuffer;
	}
	
	public List<CLDevice> getDeviceList() {
		return deviceList;
	}
	
	public CLContext getContext() {
		return context;
	}
	
	public CLCommandQueue getCommandQueue() {
		return commandQueue;
	}

}
