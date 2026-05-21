package nl.tue.pdc.sampling;

import uk.ac.manchester.tornado.api.KernelContext;

public class RayBounce {
	
	// This facade can be used to quickly switch between implementations
	public static float bounce(KernelContext context, GPURandom random) {
		return bounceTriangleRejection(context, random);
	}
	
	public static float bounceTriangleRejection(KernelContext context, GPURandom random) {
		float x = 0.0f; 
		float y = 0.0f;

		Triangle t = new Triangle(0.5f, 0.0f, -0.5f, -0.5f, -0.5f, 0.5f);
		// TODO: Generate random samples, and reject if sample is outside of triangle
		// TIP: use t.is_in_triangle(x, y)

		return Sampler.sample(context, x, y);
	}

	public static float bounceTriangleFast(KernelContext context, GPURandom random) {
		float x = 0.0f; 
		float y = 0.0f;

		// TODO: Faster method of generating samples inside a triangle
		// TIP: A linear transformation can be a useful step in converting a square into a triangle

		return Sampler.sample(context, x, y);
	}

	public static float bounceCircleRejection(KernelContext context, GPURandom random) {
		float x = 0.0f; 
		float y = 0.0f;

		// TODO: Generate random samples, and reject if sample is outside of circle

		return Sampler.sample(context, x, y);
	}
	
	public static float bounceCircleFast(KernelContext context, GPURandom random) {
		float x = 0.0f; 
		float y = 0.0f;

		// TODO: Find more efficient approach to uniform sampling
		// TIP: use the methods in TornadoMath to get access to special float32 functions

		return Sampler.sample(context, x, y);
	}
}
