package nl.tue.pdc.sampling;

import uk.ac.manchester.tornado.api.KernelContext;
import uk.ac.manchester.tornado.api.math.TornadoMath;

public class RayBounce {
	
	// This facade can be used to quickly switch between implementations
	public static float bounce(KernelContext context, GPURandom random) {
		return bounceTriangleRejection(context, random);
	}
	
	public static float bounceTriangleRejection(KernelContext context, GPURandom random) {
		float x = 0.0f; 
		float y = 0.0f;

		Triangle t = new Triangle(0.5f, 0.0f, -0.5f, -0.5f, -0.5f, 0.5f);
		do {
			x = random.random(-0.5f, 0.5f);
			y = random.random(-0.5f, 0.5f);
		} while (!t.isInTriangle(x, y));

		return Sampler.sample(context, x, y);
	}

	public static float bounceTriangleFast(KernelContext context, GPURandom random) {
		float x = 0.0f; 
		float y = 0.0f;

		float u = random.random(0.0f, 1.0f);
		float v = random.random(0.0f, 1.0f);

		if (u + v > 1.0f) {
			u = 1.0f - u;
			v = 1.0f - v;
		}

		x = 0.5f - u - v;
		y = 0.5f * (v - u);

		return Sampler.sample(context, x, y);
	}

	public static float bounceCircleRejection(KernelContext context, GPURandom random) {
		float x = 0.0f; 
		float y = 0.0f;

		do {
			x = random.random(-1.0f, 1.0f);
			y = random.random(-1.0f, 1.0f);
		} while (x * x + y * y >= 1.0f);

		return Sampler.sample(context, x, y);
	}
	
	public static float bounceCircleFast(KernelContext context, GPURandom random) {
		float x = 0.0f; 
		float y = 0.0f;

		float radius = TornadoMath.sqrt(random.random(0.0f, 1.0f));
		float angle = random.random(0.0f, 2.0f * TornadoMath.floatPI());

		x = radius * TornadoMath.cos(angle);
		y = radius * TornadoMath.sin(angle);

		return Sampler.sample(context, x, y);
	}
}
