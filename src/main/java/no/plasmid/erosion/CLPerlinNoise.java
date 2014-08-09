package no.plasmid.erosion;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.OpenCLException;
import org.lwjgl.opencl.Util;

public class CLPerlinNoise {

	private CLWrapper clWrapper;
	
	private IntBuffer xPosBuffer = null;
	private IntBuffer yPosBuffer = null;
	private FloatBuffer answerBuffer = null;
	
	private CLMem xPosMem = null;
	private CLMem yPosMem = null;
	private CLMem answerMem = null;
	
	private CLProgram program = null;
	private CLKernel kernel = null;
	
	public CLPerlinNoise(CLWrapper clWrapper) {
		this.clWrapper = clWrapper;
	}
	
	public void prepareDataAndProgram() throws LWJGLException, FileNotFoundException {
		xPosBuffer = BufferUtils.createIntBuffer(Configuration.TERRAIN_SIZE * Configuration.TERRAIN_SIZE);
		yPosBuffer = BufferUtils.createIntBuffer(Configuration.TERRAIN_SIZE * Configuration.TERRAIN_SIZE);
		for (int i = 0; i < Configuration.TERRAIN_SIZE * Configuration.TERRAIN_SIZE; i++) {
			xPosBuffer.put(i, i % Configuration.TERRAIN_SIZE);
			yPosBuffer.put(i, i / Configuration.TERRAIN_SIZE);
		}
		xPosBuffer.rewind();
		yPosBuffer.rewind();
		answerBuffer = BufferUtils.createFloatBuffer(Configuration.TERRAIN_SIZE * Configuration.TERRAIN_SIZE);
		System.out.println("Data ready");
		
		//Allocate memory and copy data
		xPosMem = CL10.clCreateBuffer(clWrapper.getContext(), CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, xPosBuffer, clWrapper.geErrorCodeBuffer());
		clWrapper.checkErrorCodeBuffer(clWrapper.geErrorCodeBuffer());
		CL10.clEnqueueWriteBuffer(clWrapper.getCommandQueue(), xPosMem, CL10.CL_TRUE, 0, xPosBuffer, null, null);
		yPosMem = CL10.clCreateBuffer(clWrapper.getContext(), CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, yPosBuffer, clWrapper.geErrorCodeBuffer());
		clWrapper.checkErrorCodeBuffer(clWrapper.geErrorCodeBuffer());
		CL10.clEnqueueWriteBuffer(clWrapper.getCommandQueue(), yPosMem, CL10.CL_TRUE, 0, yPosBuffer, null, null);
		answerMem = CL10.clCreateBuffer(clWrapper.getContext(), CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, answerBuffer, clWrapper.geErrorCodeBuffer());
		clWrapper.checkErrorCodeBuffer(clWrapper.geErrorCodeBuffer());
		CL10.clEnqueueWriteBuffer(clWrapper.getCommandQueue(), answerMem, CL10.CL_TRUE, 0, answerBuffer, null, null);
		CL10.clFinish(clWrapper.getCommandQueue());
		System.out.println("Data copied to OpenCL");
		
		//Load program source
		String source = clWrapper.loadTextFile("/cl/noise.cl");
		
		//Create the program
		program = CL10.clCreateProgramWithSource(clWrapper.getContext(), source, clWrapper.geErrorCodeBuffer());
		clWrapper.checkErrorCodeBuffer(clWrapper.geErrorCodeBuffer());
		try {
			Util.checkCLError(CL10.clBuildProgram(program, clWrapper.getDeviceList().get(0), "", null));
		} catch (OpenCLException e) {
			ByteBuffer buffer = BufferUtils.createByteBuffer(1000);
			CL10.clGetProgramBuildInfo(program, clWrapper.getDeviceList().get(0), CL10.CL_PROGRAM_BUILD_LOG, buffer, null);
			clWrapper.printChars(buffer);
			
			System.out.println(program.getBuildInfoString(clWrapper.getDeviceList().get(0), CL10.CL_PROGRAM_BUILD_LOG));
			throw e;
		}
		//Sum has to match a kernel method name in the OpenCL source
		kernel = CL10.clCreateKernel(program, "getheight", clWrapper.geErrorCodeBuffer());
		clWrapper.checkErrorCodeBuffer(clWrapper.geErrorCodeBuffer());
		System.out.println("OpenCL program ready");
	}
	
	public FloatBuffer calculateOpenCL() throws LWJGLException {
		//Execute the kernel
		PointerBuffer kernel1DGlobalWorkSize = BufferUtils.createPointerBuffer(1);
		kernel1DGlobalWorkSize.put(0, Math.max(Configuration.TERRAIN_SIZE * Configuration.TERRAIN_SIZE, 1));
		kernel.setArg(0, Configuration.TERRAIN_NOISE_PERSISTENCE);
		kernel.setArg(1, Configuration.TERRAIN_NOISE_FREQUENCY);
		kernel.setArg(2, Configuration.TERRAIN_NOISE_AMPLITUDE);
		kernel.setArg(3, Configuration.TERRAIN_NOISE_OCTAVES);
		kernel.setArg(4, Configuration.TERRAIN_NOISE_RANDOM_SEED);
		kernel.setArg(5, xPosMem);
		kernel.setArg(6, yPosMem);
		kernel.setArg(7, answerMem);
//		kernel.setArg(0, xPosMem);
//		kernel.setArg(1, yPosMem);
//		kernel.setArg(2, answerMem);
		CL10.clEnqueueNDRangeKernel(clWrapper.getCommandQueue(), kernel, 1, null, kernel1DGlobalWorkSize, null, null, null);
		
		//Read back results
		CL10.clEnqueueReadBuffer(clWrapper.getCommandQueue(), answerMem, CL10.CL_TRUE, 0, answerBuffer, null, null);
		CL10.clFinish(clWrapper.getCommandQueue());

		return answerBuffer;
	}
	
	public void cleanupDataAndProgram() {
		//Clean up OpenCL resources
		if (null != kernel) {
			CL10.clReleaseKernel(kernel);
		}
		if (null != program) {
			CL10.clReleaseProgram(program);
		}
		if (null != xPosMem) {
			CL10.clReleaseMemObject(xPosMem);
		}
		if (null != yPosMem) {
			CL10.clReleaseMemObject(yPosMem);
		}
		if (null != answerMem) {
			CL10.clReleaseMemObject(answerMem);
		}
	}

}
