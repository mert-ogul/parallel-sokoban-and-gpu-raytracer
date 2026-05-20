package nl.tue.pdc.sampling;

import uk.ac.manchester.tornado.api.KernelContext;

public class Sampler {

	public static float sample(KernelContext context, float x, float y) {
		float center_x = (context.globalIdx - context.globalGroupSizeX / 2) * 0.02f;
		float center_y = (context.globalIdy - context.globalGroupSizeY / 2) * 0.02f;
		float d_x = center_x - x;
		float d_y = center_y - y;
		
		float result = 0;
		
		if ( (d_x * d_x + d_y * d_y) < 0.005) {
			result = 100.0f;
		}

		result += x * x;
		result += y * y;
		return result;
	}
}
