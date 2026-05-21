package nl.tue.pdc.sampling;

public class Triangle {

	public final float x1;
	public final float y1;

	public final float x2;
	public final float y2;

	public final float x3;
	public final float y3;

	public Triangle(float x1, float y1, float x2, float y2, float x3, float y3) {
		this.x1 = x1;
		this.y1 = y1;

		this.x2 = x2;
		this.y2 = y2;

		this.x3 = x3;
		this.y3 = y3;
	}

	public static float dotProduct(float x1, float y1, float x2, float y2) {
		return x1 * x2 + y1 * y2;
	}

	public static float dotProductLine(float xstart, float ystart, float xend, float yend, float x, float y) {
		float xrel = x - xstart;
		float yrel = y - ystart;

		float xtarget = xend - xstart;
		float ytarget = yend - ystart;
		return dotProduct(xrel, yrel, -ytarget, xtarget);
	}

	public boolean isInTriangle(float x, float y) {
		float d1 = dotProductLine(x1, y1, x2, y2, x, y);
		float d2 = dotProductLine(x2, y2, x3, y3, x, y);
		float d3 = dotProductLine(x3, y3, x1, y1, x, y);
		return d1 < 0 && d2 < 0 && d3 < 0;
	}
}
