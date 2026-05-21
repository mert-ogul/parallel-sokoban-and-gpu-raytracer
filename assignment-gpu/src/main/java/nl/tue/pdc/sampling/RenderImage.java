package nl.tue.pdc.sampling;

import java.awt.image.BufferedImage;
import javax.swing.*;
import java.awt.*;

import uk.ac.manchester.tornado.api.types.arrays.FloatArray;

public class RenderImage {

    private final float[][] data;

	RenderImage(FloatArray in, int x, int y) {
		data = new float[x][y];

		for (int i = 0; i < x; i++) {
			for (int j = 0; j < y; j++) {
				data[i][j] = in.get(i + j * x);
			}
		}
	}

	public void show() {
		        int width = data.length;
        int height = data[0].length;

        BufferedImage image =
                new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        // Find min/max for normalization
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float v = data[x][y];
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }

        float range = max - min;

        // Convert float values → grayscale pixels
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                int gray = (int)(255 * (data[x][y] - min) / range);

                int rgb = (gray << 16) | (gray << 8) | gray;

                image.setRGB(x, y, rgb);
            }
        }

        // Show window
        JFrame frame = new JFrame("Array Image");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);

        JLabel label = new JLabel(new ImageIcon(image));
        frame.add(label);

        frame.pack();
        frame.setVisible(true);
	}
}
