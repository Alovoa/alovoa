//BSD 2-Clause "Simplified" License
//https://github.com/gbaydin/OxCaptcha/blob/master/LICENSE
// Copyright (c) 2016, Atılım Güneş Baydin

package com.nonononoki.alovoa.lib;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
//import java.security.SecureRandom;
import java.util.Random;

import javax.imageio.ImageIO;

public class OxCaptcha {
    private static final Random RAND = new SecureRandom();
//	private static final Random RAND = new Random();

	private BufferedImage _img;
	private Graphics2D _img_g;
	private int _width;
	private int _height;
	// private BufferedImage _bg;
	private Color _bg_color;
	private Color _fg_color;
	private char[] _chars = new char[] {};
	private int _length = 0;
	private boolean _hollow;
	private Font _font;
	private FontRenderContext _fontRenderContext;
	private static char[] _charSet = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'k', 'm', 'n', 'p', 'r', 'w', 'x',
			'y', '2', '3', '4', '5', '6', '7', '8' };

	public OxCaptcha(int width, int height) {
		_img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		_img_g = _img.createGraphics();
		_hollow = false;
		_font = new Font("Arial", Font.PLAIN, 40);
		_img_g.setFont(_font);
		_fontRenderContext = _img_g.getFontRenderContext();
		_bg_color = Color.WHITE;
		_fg_color = Color.BLACK;

		RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		hints.add(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
		_img_g.setRenderingHints(hints);

		_width = width;
		_height = height;

	}

	public void setCharSet(char[] charSet) {
		_charSet = charSet;

	}

	public void setFont(String name) {
		setFont(name, Font.PLAIN, 40);
	}

	public void setFont(String name, int style, int size) {
		_font = new Font(name, style, size);
		_img_g.setFont(_font);
		_fontRenderContext = _img_g.getFontRenderContext();
	}

	public void setHollow() {
		_hollow = true;
	}

	public void background() {
		background(_bg_color);
	}

	public void background(Color color) {
		_bg_color = color;
		_img_g.setPaint(color);
		_img_g.fillRect(0, 0, _width, _height);
	}

	public void foreground(Color color) {
		_fg_color = color;
	}

	public void text() {
		text(5);
	}

	public void text(int length) {
		text(genText(length));
	}

	public static char[] genText(int length) {
		char[] t = new char[length];
		for (int i = 0; i < length; i++) {
			t[i] = _charSet[RAND.nextInt(_charSet.length)];
		}
		return t;
	}

	public void text(String chars) {
		text(chars, (int) (0.05 * _width), (int) (0.75 * _height), 0);
	}

	public void text(String chars, int kerning) {
		text(chars, (int) (0.05 * _width), (int) (0.75 * _height), kerning);
	}

	public void text(char[] chars) {
		text(chars, (int) (0.05 * _width), (int) (0.75 * _height), 0);
	}

	public void text(String chars, int xOffset, int yOffset, int kerning) {
		int l = chars.length();
		char[] t = new char[l];
		chars.getChars(0, l, t, 0);
		text(t, xOffset, yOffset, kerning);
	}

	public void text(char[] chars, int xOffset, int yOffset, int kerning) {
		text(chars, xOffset, yOffset, Font.PLAIN, kerning);
	}

	public void text(String chars, int xOffset, int yOffset, int style, int kerning) {
		int l = chars.length();
		char[] t = new char[l];
		chars.getChars(0, l, t, 0);
		int styles[] = new int[t.length];
		for (int i = 0; i < t.length; i++) {
			styles[i] = style;
		}
		text(t, styles, xOffset, yOffset, kerning);
	}

	public void text(char[] chars, int xOffset, int yOffset, int style, int kerning) {
		int styles[] = new int[chars.length];
		for (int i = 0; i < chars.length; i++) {
			styles[i] = style;
		}
		text(chars, styles, xOffset, yOffset, kerning);
	}

	public void text(char[] chars, int[] styles, int xOffset, int yOffset, int kerning) {
		int xn[] = new int[chars.length];
		for (int i = 0; i < chars.length; i++) {
			xn[i] = kerning;
		}
		int yn[] = new int[chars.length];
		xn[0] = xOffset;
		yn[0] = yOffset;
		textRelative(chars, styles, xn, yn);
	}

	public void textRelative(char[] chars, int[] xOffsets, int[] yOffsets) {
		int styles[] = new int[chars.length];
		for (int i = 0; i < chars.length; i++) {
			styles[i] = Font.PLAIN;
		}
		textRelative(chars, styles, xOffsets, yOffsets);
	}

	public void textCentered(String chars, int kerning) {
		int l = chars.length();
		char[] t = new char[l];
		chars.getChars(0, l, t, 0);
		int styles[] = new int[l];
		for (int i = 0; i < l; i++) {
			styles[i] = Font.PLAIN;
		}
		textCentered(t, styles, kerning);
	}

	public void textCentered(String chars, int style, int kerning) {
		int l = chars.length();
		char[] t = new char[l];
		chars.getChars(0, l, t, 0);
		int styles[] = new int[l];
		for (int i = 0; i < l; i++) {
			styles[i] = style;
		}
		textCentered(t, styles, kerning);
	}

	// Add letters with relative per letter positioning
	// Offsets give the position of each letter relative to the top right of the
	// previous letter
	// The offsets of the first letter are relative to the top left of the image
	// (For an image 50 pixels high, it's a good idea to start the first y offset
	// around 30, so that the text is inside the image)
	// x increases from left to right
	// y increases from top to bottom
	public void textRelative(char[] chars, int[] styles, int[] xOffsets, int[] yOffsets) {
		_chars = chars;
		_length = _chars.length;

		_img_g.setColor(_fg_color);
		int x = 0;
		int y = 0;
		char[] cc = new char[1];
		GlyphVector gv;
		int gvWidth;
		for (int i = 0; i < _length; i++) {
			x = x + xOffsets[i];
			y = y + yOffsets[i];
			cc[0] = _chars[i];

			_font = _font.deriveFont(styles[i]);
			_img_g.setFont(_font);
			_fontRenderContext = _img_g.getFontRenderContext();

			renderChar(cc, x, y);

			gv = _font.createGlyphVector(_fontRenderContext, cc);
			gvWidth = (int) gv.getVisualBounds().getWidth();
			x = x + gvWidth + 1;
		}
		_font = _font.deriveFont(Font.PLAIN);
	}

	// Add letters with absolute per letter positioning.
	// xs and ys are absolute positions of letters in chars
	// The offsets of the first letter are relative to the top left of the image
	// (For an image 50 pixels high, it's a good idea to start the first y offset
	// around 30, so that the text is inside the image)
	// x increases from left to right
	// y increases from top to bottom
	public void textAbsolute(char[] chars, int[] styles, int[] xs, int[] ys) {
		_chars = chars;
		_length = _chars.length;

		_img_g.setColor(_fg_color);
		char[] cc = new char[1];
		for (int i = 0; i < _length; i++) {
			cc[0] = _chars[i];

			_font = _font.deriveFont(styles[i]);
			_img_g.setFont(_font);
			_fontRenderContext = _img_g.getFontRenderContext();

			renderChar(cc, xs[i], ys[i]);
		}
		_font = _font.deriveFont(Font.PLAIN);
	}

	public void textCentered(char[] chars, int[] styles, int kerning) {
		_chars = chars;
		_length = _chars.length;

		char[] cc = new char[1];
		GlyphVector gv;
		int[] gvWidths = new int[_length];
		int[] gvHeights = new int[_length];
		int width = 0;
		int height = 0;
		for (int i = 0; i < _length; i++) {
			cc[0] = _chars[i];
			_font = _font.deriveFont(styles[i]);
			_img_g.setFont(_font);
			_fontRenderContext = _img_g.getFontRenderContext();
			gv = _font.createGlyphVector(_fontRenderContext, cc);
			gvWidths[i] = (int) gv.getVisualBounds().getWidth();
			gvHeights[i] = (int) gv.getVisualBounds().getHeight();
			if (gvHeights[i] > height) {
				height = gvHeights[i];
			}
			width = width + gvWidths[i] + kerning + 1;
		}
		int x0 = (_width - width) / 2;
		int y0 = height + (_height - height) / 2;

		int x = x0;
		_img_g.setColor(_fg_color);
		for (int i = 0; i < _length; i++) {
			cc[0] = _chars[i];
			_font = _font.deriveFont(styles[i]);
			_img_g.setFont(_font);
			renderChar(cc, x, y0);
			x = x + gvWidths[i] + kerning + 1;
		}
		_font = _font.deriveFont(Font.PLAIN);
	}

	public void renderString(String s, int x, int y) {
		_img_g.setColor(_fg_color);
		_img_g.drawString(s, x, y);
	}

	private void renderChar(char[] cc, int x, int y) {
		if (_hollow) {
			_img_g.drawChars(cc, 0, 1, x - 1, y - 1);
			_img_g.drawChars(cc, 0, 1, x - 1, y + 1);
			_img_g.drawChars(cc, 0, 1, x + 1, y - 1);
			_img_g.drawChars(cc, 0, 1, x + 1, y + 1);
			_img_g.setColor(_bg_color);
			_img_g.drawChars(cc, 0, 1, x, y);
			_img_g.setColor(_fg_color);

		} else {
			_img_g.drawChars(cc, 0, 1, x, y);
		}
	}

	public void blur() {
		blur(3);
	}

	public void blur(int kernelSize) {

		float[] k = new float[kernelSize * kernelSize];
		for (int i = 0; i < kernelSize; i++) {
			k[i] = 1f / ((float) (kernelSize));
		}
		Kernel kernel = new Kernel(kernelSize, kernelSize, k);

		BufferedImageOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
		_img = op.filter(_img, null);
		_img_g = _img.createGraphics();
		_img_g.setFont(_font);
	}

	private ConvolveOp gbConvolve(int radius, float sigma, boolean horizontal) {
		int size = radius * 2 + 1;
		float[] vals = new float[size];
		float twoSigmaSq = 2.0f * sigma * sigma;
		float sqrtPiTwoSigmaSq = (float) Math.sqrt(twoSigmaSq * Math.PI);
		float sum = 0.0f;

		for (int i = -radius; i <= radius; i++) {
			float distance = (float) i * i;
			int index = i + radius;
			vals[index] = (float) Math.exp(-distance / twoSigmaSq) / sqrtPiTwoSigmaSq;
			sum += vals[index];
		}
		if (sum != 0) {
			for (int i = 0; i < size; i++) {
				vals[i] /= sum;
			}
		}

		Kernel kernel = null;
		if (horizontal) {
			kernel = new Kernel(size, 1, vals);
		} else {
			kernel = new Kernel(1, size, vals);
		}
		return new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
	}

	public void blurGaussian(double sigma) {
		blurGaussian(2, sigma);
	}

	public void blurGaussian(int radius, double sigma) {
		if (radius < 1) {
			throw new IllegalArgumentException("radius must be greater than 1");
		}
		BufferedImageOp op = gbConvolve(radius, (float) sigma, true);
		_img = op.filter(_img, null);

		op = gbConvolve(radius, (float) sigma, false);
		_img = op.filter(_img, null);
		_img_g = _img.createGraphics();
		_img_g.setFont(_font);
	}

	public void blurGaussian3x3() {

		float[] k = new float[] { 1f / 16f, 1f / 8f, 1f / 16f, 1f / 8f, 1f / 4f, 1f / 8f, 1f / 16f, 1f / 8f, 1f / 16f };

		Kernel kernel = new Kernel(3, 3, k);

		BufferedImageOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
		_img = op.filter(_img, null);
		_img_g = _img.createGraphics();
		_img_g.setFont(_font);
	}

	public void blurGaussian5x5s1() {

		float[] k = new float[] { 1f / 273f, 4f / 273f, 7f / 273f, 4f / 273f, 1f / 273f, 4f / 273f, 16f / 273f,
				26f / 273f, 16f / 273f, 4f / 273f, 7f / 273f, 26f / 273f, 41f / 273f, 26f / 273f, 7f / 273f, 4f / 273f,
				16f / 273f, 26f / 273f, 16f / 273f, 4f / 273f, 1f / 273f, 4f / 273f, 7f / 273f, 4f / 273f, 1f / 273f };

		Kernel kernel = new Kernel(5, 5, k);

		BufferedImageOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
		_img = op.filter(_img, null);
		_img_g = _img.createGraphics();
		_img_g.setFont(_font);

	}

	public void blurGaussian5x5s2() {

		float[] k = new float[] { 0.023528f, 0.033969f, 0.038393f, 0.033969f, 0.023528f, 0.033969f, 0.049045f,
				0.055432f, 0.049045f, 0.033969f, 0.038393f, 0.055432f, 0.062651f, 0.055432f, 0.038393f, 0.033969f,
				0.049045f, 0.055432f, 0.049045f, 0.033969f, 0.023528f, 0.033969f, 0.038393f, 0.033969f, 0.023528f };

		Kernel kernel = new Kernel(5, 5, k);

		BufferedImageOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
		_img = op.filter(_img, null);
		_img_g = _img.createGraphics();
		_img_g.setFont(_font);
	}

	public void noiseCurvedLine() {
		noiseCurvedLine(5, _width, 2.0f, _fg_color);
	}

	public void noiseCurvedLine(float thickness) {
		noiseCurvedLine(5, _width, thickness, _fg_color);
	}

	public void noiseCurvedLine(int xOffset, int xRange) {
		noiseCurvedLine(xOffset, xRange, 2.0f, _fg_color);
	}

	public void noiseCurvedLine(int xOffset, int xRange, float thickness) {
		noiseCurvedLine(xOffset, xRange, thickness, _fg_color);
	}

	public void noiseCurvedLine(int xOffset, int xRange, float thickness, Color color) {
		// the curve from where the points are taken
		CubicCurve2D cc = new CubicCurve2D.Float(xOffset + RAND.nextFloat() * _width * 0.25f,
				_height * RAND.nextFloat(), xOffset + RAND.nextFloat() * _width * 0.25f, _height * RAND.nextFloat(),
				xOffset + xRange * 0.4f, _height * RAND.nextFloat(),
				xOffset + xRange * (0.8f + RAND.nextFloat() * 0.2f), _height * RAND.nextFloat());

		// creates an iterator to define the boundary of the flattened curve
		PathIterator pi = cc.getPathIterator(null, 0.1);
		Point2D tmp[] = new Point2D[200];
		int i = 0;

		// while pi is iterating the curve, adds points to tmp array
		while (!pi.isDone()) {
			float[] coords = new float[6];
			switch (pi.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
			case PathIterator.SEG_LINETO:
				tmp[i] = new Point2D.Float(coords[0], coords[1]);
			}
			i++;
			pi.next();
		}

		// the points where the line changes the stroke and direction
		Point2D[] pts = new Point2D[i];
		// copies points from tmp to pts
		System.arraycopy(tmp, 0, pts, 0, i);

		_img_g.setColor(color);

		for (i = 0; i < pts.length - 1; i++) {
			// for the maximum 3 point change the stroke and direction
			if (i < 3) {
				_img_g.setStroke(new BasicStroke(thickness));
			}
			_img_g.drawLine((int) pts[i].getX(), (int) pts[i].getY(), (int) pts[i + 1].getX(), (int) pts[i + 1].getY());
		}
	}

	public void noiseStrokes() {
		noiseStrokes(8, 1.5f);
	}

	public void noiseStrokes(int strokes, float width) {
		_img_g.setStroke(new BasicStroke(width));
		_img_g.setColor(_fg_color);
		for (int i = 0; i < strokes; i++) {
			Path2D.Double path = new Path2D.Double();
			path.moveTo(RAND.nextInt(_width), RAND.nextInt(_height));
			path.curveTo(RAND.nextInt(_width), RAND.nextInt(_height), RAND.nextInt(_width), RAND.nextInt(_height),
					RAND.nextInt(_width), RAND.nextInt(_height));
			_img_g.draw(path);
		}
	}

	public void noiseEllipses(int ellipses, float width) {
		_img_g.setStroke(new BasicStroke(width));
		_img_g.setColor(_bg_color);
		for (int i = 0; i < ellipses; i++) {
			Ellipse2D.Double ellipse = new Ellipse2D.Double(RAND.nextInt(_width), RAND.nextInt(_height),
					RAND.nextInt(_width), RAND.nextInt(_height));
			_img_g.draw(ellipse);
		}
	}

	public void noiseStraightLine() {
		noiseStraightLine(_fg_color, 3.0f);
	}

	public void noiseStraightLine(Color color, float thickness) {
		int y1 = RAND.nextInt(_height) + 1;
		int y2 = RAND.nextInt(_height) + 1;
		int x1 = 0;
		int x2 = _width;

		// The thick line is in fact a filled polygon
		_img_g.setColor(color);
		int dX = x2 - x1;
		int dY = y2 - y1;
		// line length
		double lineLength = Math.sqrt((double) dX * dX + dY * dY);

		double scale = thickness / (2 * lineLength);

		// The x and y increments from an endpoint needed to create a
		// rectangle...
		double ddx = -scale * dY;
		double ddy = scale * dX;
		ddx += ddx > 0 ? 0.5 : -0.5;
		ddy += ddy > 0 ? 0.5 : -0.5;
		int dx = (int) ddx;
		int dy = (int) ddy;

		// Now we can compute the corner points...
		int xPoints[] = new int[4];
		int yPoints[] = new int[4];

		xPoints[0] = x1 + dx;
		yPoints[0] = y1 + dy;
		xPoints[1] = x1 - dx;
		yPoints[1] = y1 - dy;
		xPoints[2] = x2 - dx;
		yPoints[2] = y2 - dy;
		xPoints[3] = x2 + dx;
		yPoints[3] = y2 + dy;

		_img_g.fillPolygon(xPoints, yPoints, 4);
	}

	public void noiseSaltPepper() {
		noiseSaltPepper(0.01f, 0.01f);
	}

	public void noiseSaltPepper(float salt, float pepper) {
		int s = (int) (_height * _width * salt);
		int p = (int) (_height * _width * pepper);

		_img_g.setStroke(new BasicStroke(1));

		_img_g.setColor(Color.WHITE);

		for (int i = 0; i < s; i++) {
			int x = (int) (RAND.nextFloat() * _width);
			int y = (int) (RAND.nextFloat() * _height);

			_img_g.drawLine(x, y, x, y);
		}
		_img_g.setColor(Color.BLACK);
		for (int i = 0; i < p; i++) {
			int x = (int) (RAND.nextFloat() * _width);
			int y = (int) (RAND.nextFloat() * _height);
			_img_g.drawLine(x, y, x, y);
		}
	}

	public void noiseWhiteGaussian() {
		noiseWhiteGaussian(1.0);
	}

	public void noiseWhiteGaussian(double sigma) {
		for (int y = 0; y < _height; y++) {
			for (int x = 0; x < _width; x++) {
				int p = _img.getRGB(x, y) & 0xFF;
				p = (int) (((double) p) + sigma * RAND.nextGaussian());
				p = Math.max(0, Math.min(255, p));
				_img.setRGB(x, y, new Color(p, p, p).getRGB());
			}
		}
	}

	public void noiseWhiteUniform() {
		for (int y = 0; y < _height; y++) {
			for (int x = 0; x < _width; x++) {
				int p = _img.getRGB(x, y) & 0xFF;
				p = (int) (((double) p) + RAND.nextInt(256));
				p = Math.max(0, Math.min(255, p));
				_img.setRGB(x, y, new Color(p, p, p).getRGB());
			}
		}
	}

	public void distortion() {
		distortionShear();
	}

	public void distortionFishEye() {
//        Color hColor = Color.BLACK;
//        Color vColor = Color.BLACK;
		float thickness = 1.0f;

		_img_g.setStroke(new BasicStroke(thickness));

//        int hstripes = _height / 7;
//        int vstripes = _width / 7;
//
//        // Calculate space between lines
//        int hspace = _height / (hstripes + 1);
//        int vspace = _width / (vstripes + 1);
//
//        // Draw the horizontal stripes
//        for (int i = hspace; i < _height; i = i + hspace) {
//            _img_g.setColor(hColor);
//            _img_g.drawLine(0, i, _width, i);
//        }
//
//        // Draw the vertical stripes
//        for (int i = vspace; i < _width; i = i + vspace) {
//            _img_g.setColor(vColor);
//            _img_g.drawLine(i, 0, i, _height);
//        }

		// Create a pixel array of the original image.
		// we need this later to do the operations on..
		int pix[] = new int[_height * _width];
		int j = 0;

		for (int j1 = 0; j1 < _width; j1++) {
			for (int k1 = 0; k1 < _height; k1++) {
				pix[j] = _img.getRGB(j1, k1);
				j++;
			}
		}

		double distance = ranInt(_width / 4, _width / 3);

		// put the distortion in the (dead) middle
		int wMid = _width / 2;
		int hMid = _height / 2;

		// again iterate over all pixels..
		for (int x = 0; x < _width; x++) {
			for (int y = 0; y < _height; y++) {

				int relX = x - wMid;
				int relY = y - hMid;

				double d1 = Math.sqrt((double) relX * relX + relY * relY);
				if (d1 < distance) {

					int j2 = wMid + (int) (((fishEyeFormula(d1 / distance) * distance) / d1) * (x - wMid));
					int k2 = hMid + (int) (((fishEyeFormula(d1 / distance) * distance) / d1) * (y - hMid));
					_img.setRGB(x, y, pix[j2 * _height + k2]);
				}
			}
		}
	}

	public void distortionStretch() {
		double xScale = RAND.nextDouble() * 2;
		double yScale = RAND.nextDouble() * 2;
		distortionStretch(xScale, yScale);
	}

	public void distortionStretch(double xScale, double yScale) {
		AffineTransform at = new AffineTransform();
		at.scale(xScale, yScale);
		_img_g.drawRenderedImage(_img, at);
	}

	public void distortionElectric() {
		distortionElectric(4000, 2, 3);
	}

	public void distortionElectric(int amount, int shift, int size) {
		for (int i = 0; i < amount / 2; i++) {
			int x = RAND.nextInt(_width);
			int y = RAND.nextInt(_height);
			int s = RAND.nextInt(shift);
			_img_g.copyArea(x + s, y, size, 1, s, 0);
			_img_g.copyArea(x, y, size, 1, s, 0);
		}
		for (int i = 0; i < amount / 2; i++) {
			int x = RAND.nextInt(_width);
			int y = RAND.nextInt(_height);
			int s = RAND.nextInt(shift);
			_img_g.copyArea(x, y + s, 1, size, 0, s);
			_img_g.copyArea(x, y, 1, size, 0, s);
		}
	}

	public void distortionElectric2() {
		distortionElectric2(24);
	}

	public void distortionElectric2(double alpha) {
		int s[][] = getImageArray2D();
		double source[][] = new double[_height][_width];
		double dxField[][] = new double[_height][_width];
		double dyField[][] = new double[_height][_width];
		for (int y = 0; y < _height; y++) {
			for (int x = 0; x < _width; x++) {
				dxField[y][x] = 2 * (RAND.nextDouble() - 0.5);
				dyField[y][x] = 2 * (RAND.nextDouble() - 0.5);
				source[y][x] = (double) s[y][x];
			}
		}

		dxField = OxCaptcha.gaussian(dxField, 2, 2.2);
		dyField = OxCaptcha.gaussian(dyField, 2, 2.2);

		for (int y = 0; y < _height; y++) {
			for (int x = 0; x < _width; x++) {
				double dx = dxField[y][x] * alpha;
				double dy = dyField[y][x] * alpha;

				double sx = (double) x + dx;
				double sy = (double) y + dy;
				if (sx < 0 || sx > _width - 2 || sy < 0 || sy > _height - 2) {
					_img.setRGB(x, y, _bg_color.getRGB());
				} else {
					int sxleft = (int) Math.floor(sx);
					int sxright = sxleft + 1;
					double sxdist = sx % 1;

					int sytop = (int) Math.floor(sy);
					int sybottom = sytop + 1;
					double sydist = sy % 1;

					double top = (1. - sxdist) * source[sytop][sxleft] + sxdist * source[sytop][sxright];
					double bottom = (1. - sxdist) * source[sybottom][sxleft] + sxdist * source[sybottom][sxright];
					double target = (1. - sydist) * top + sydist * bottom;
					int t = Math.max(Math.min((int) target, 255), 0);
//                    double target = (dyField[y][x] + 1) * 128;
//                    System.out.println(target);
					_img.setRGB(x, y, new Color(t, t, t).getRGB());
				}

			}
		}

	}

	public void distortionShear2() {
		int xPhase = -_width + 2 * RAND.nextInt(_width);
		int xPeriod = 6 + RAND.nextInt(30);
		int xAmplitude = 2 + RAND.nextInt(7);
		int yPhase = -_height + 2 * RAND.nextInt(_height);
		int yPeriod = 10 + RAND.nextInt(20);
		int yAmplitude = 2 + RAND.nextInt(15);
		distortionShear2(xPhase, xPeriod, xAmplitude, yPhase, yPeriod, yAmplitude);
	}

	public void distortionShear2(int xPhase, int xPeriod, int xAmplitude, int yPhase, int yPeriod, int yAmplitude) {
		for (int i = 0; i < _width; i++) {
			int dst_x = i - 1;
			int dst_y = (int) (Math.sin((double) (xPhase + i) / (double) xPeriod) * xAmplitude);
			int src_x = i;
			int src_y = 0;
			int src_w = 1;
			int src_h = _height;
			int dx = dst_x - src_x;
			int dy = dst_y - src_y;
			_img_g.copyArea(src_x, src_y, src_w, src_h, dx, dy);
			_img_g.setColor(_bg_color);
			if (dy >= 0) {
				_img_g.drawLine(i, 0, i, dy);
			} else {
				_img_g.drawLine(i, _height + dy, i, _height);
			}
			_img_g.setColor(_fg_color);
		}
		for (int i = 0; i < _height; i++) {
			int dst_x = (int) (Math.sin((double) (yPhase + i) / (double) yPeriod) * yAmplitude);
			int dst_y = i - 1;
			int src_x = 0;
			int src_y = i;
			int src_w = _width;
			int src_h = 1;
			int dx = dst_x - src_x;
			int dy = dst_y - src_y;
			_img_g.copyArea(src_x, src_y, src_w, src_h, dx, dy);
			_img_g.setColor(_bg_color);
			if (dx >= 0) {
				_img_g.drawLine(0, i, dx, i);
			} else {
				_img_g.drawLine(_width + dx, i, _width, i);
			}
			_img_g.setColor(_fg_color);
		}
	}

	public void distortionShear() {
		int xPeriod = RAND.nextInt(10) + 8;
		int xPhase = RAND.nextInt(8) + 8;
		int yPeriod = RAND.nextInt(10) + 8;
		int yPhase = RAND.nextInt(8) + 8;

		distortionShear(xPeriod, xPhase, yPeriod, yPhase);
	}

	public void distortionShear(int xPeriod, int xPhase, int yPeriod, int yPhase) {
		shearX(_img_g, xPeriod, xPhase, _width, _height);
		shearY(_img_g, yPeriod, yPhase, _width, _height);
	}

	public void distortionElastic() {
		distortionElastic(38);
	}

	public void distortionElastic(double alpha) {
		distortionElastic(alpha, 11, 8);
	}

	public void distortionElastic(double alpha, int kernelSize, double sigma) {
		int s[][] = getImageArray2D();
		double source[][] = new double[_height][_width];
		double dxField[][] = new double[_height][_width];
		double dyField[][] = new double[_height][_width];
		for (int y = 0; y < _height; y++) {
			for (int x = 0; x < _width; x++) {
				dxField[y][x] = 2 * (RAND.nextDouble() - 0.5);
				dyField[y][x] = 2 * (RAND.nextDouble() - 0.5);
				if (RAND.nextDouble() < 0.1) {
					dxField[y][x] = dxField[y][x] * 5;
				}
				if (RAND.nextDouble() < 0.1) {
					dyField[y][x] = dyField[y][x] * 5;
				}
				source[y][x] = (double) s[y][x];
			}
		}

		dxField = OxCaptcha.gaussian(dxField, kernelSize, sigma);
		dyField = OxCaptcha.gaussian(dyField, kernelSize, sigma);

		for (int y = 0; y < _height; y++) {
			for (int x = 0; x < _width; x++) {
				double dx = dxField[y][x] * alpha;
				double dy = dyField[y][x] * alpha;

				double sx = (double) x + dx;
				double sy = (double) y + dy;
				if (sx < 0 || sx > _width - 2 || sy < 0 || sy > _height - 2) {
					_img.setRGB(x, y, _bg_color.getRGB());
				} else {
					int sxleft = (int) Math.floor(sx);
					int sxright = sxleft + 1;
					double sxdist = sx % 1;

					int sytop = (int) Math.floor(sy);
					int sybottom = sytop + 1;
					double sydist = sy % 1;

					double top = (1. - sxdist) * source[sytop][sxleft] + sxdist * source[sytop][sxright];
					double bottom = (1. - sxdist) * source[sybottom][sxleft] + sxdist * source[sybottom][sxright];
					double target = (1. - sydist) * top + sydist * bottom;
					int t = Math.max(Math.min((int) target, 255), 0);
//                    double target = (dyField[y][x] + 1) * 128;
//                    System.out.println(target);
					_img.setRGB(x, y, new Color(t, t, t).getRGB());
				}

			}
		}

	}

	public void normalize() {
		int p[] = getImageArray1D();
		int pmin = p[0];
		int pmax = p[0];
		for (int i = 0; i < p.length; i++) {
			if (p[i] < pmin) {
				pmin = p[i];
			} else if (p[i] > pmax) {
				pmax = p[i];
			}
		}
		int prange = pmax - pmin;

		if (prange > 2) {
			int i = 0;
			for (int y = 0; y < _height; y++) {
				for (int x = 0; x < _width; x++) {
					int pp = 255 * (p[i++] - pmin) / prange;
					_img.setRGB(x, y, new Color(pp, pp, pp).getRGB());
				}
			}
		}
	}

	public void recenter() {
		int p[][] = getImageArray2D();
		int pb = p[0][0];
		int xmin = _width - 1;
		int xmax = 0;
		int ymin = _height - 1;
		int ymax = 0;
		for (int y = 0; y < _height; y++) {
			for (int x = 0; x < _width; x++) {
				if (p[y][x] != pb) {
					if (x < xmin) {
						xmin = x;
					}
					if (x > xmax) {
						xmax = x;
					}
					if (y < ymin) {
						ymin = y;
					}
					if (y > ymax) {
						ymax = y;
					}
				}
			}
		}
		int w = xmax - xmin + 1;
		int h = ymax - ymin + 1;

		if (w > 0 && h > 0) {
			int xt = (_width - w) / 2;
			int yt = (_height - h) / 2;
			BufferedImage b = _img.getSubimage(xmin, ymin, w, h);
			BufferedImage b2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			b2.createGraphics().drawImage(b, 0, 0, null);
			_img_g.setColor(_bg_color);
			_img_g.fillRect(0, 0, _width, _height);
			_img_g.drawImage(b2, xt, yt, null);
		}

	}

	public int[][] load(String fileName) throws IOException {
		BufferedImage i = ImageIO.read(new File(fileName));
		int height = i.getHeight();
		int width = i.getWidth();
		int ret[][] = new int[height][width];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				ret[y][x] = i.getRGB(x, y) & 0xFF;
			}
		}
		return ret;
	}

	public void save(int[] pixels, int width, int height, String fileName) throws IOException {
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				img.setRGB(x, y, new Color(pixels[i], pixels[i], pixels[i]).getRGB());
				i++;
			}
		}
		ImageIO.write(img, "webp", new File(fileName));
	}

	public void save(int[][] pixels, String fileName) throws IOException {
		int height = pixels.length;
		int width = pixels[0].length;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				img.setRGB(x, y, new Color(pixels[x][y], pixels[x][y], pixels[x][y]).getRGB());
			}
		}
		ImageIO.write(img, "webp", new File(fileName));
	}

	public String getText() {
		return new String(_chars);
	}

	public BufferedImage getImage() {
		return _img;
	}

	public int[][] getImageArray2D() {
		int ret[][] = new int[_height][_width];
		for (int x = 0; x < _width; x++) {
			for (int y = 0; y < _height; y++) {
				int p = _img.getRGB(x, y);
				int red = (p >> 16) & 0xff;
				ret[y][x] = red;
			}
		}
		return ret;
	}

	public int[] getImageArray1D() {
		int ret[] = new int[_height * _width];
		int i = 0;
		for (int y = 0; y < _height; y++)
			for (int x = 0; x < _width; x++) {
				{
					int p = _img.getRGB(x, y);
					int red = (p >> 16) & 0xff;
					ret[i++] = red;
				}
			}
		return ret;
	}

	public void save(String fileName) throws IOException {
		ImageIO.write(_img, "webp", new File(fileName));
	}

	private int ranInt(int i, int j) {
		double d = RAND.nextDouble();
		return (int) (i + ((j - i) + 1) * d);
	}

	private double fishEyeFormula(double s) {
		// implementation of:
		// g(s) = - (3/4)s3 + (3/2)s2 + (1/4)s, with s from 0 to 1.
		if (s < 0.0D) {
			return 0.0D;
		}
		if (s > 1.0D) {
			return s;
		}

		return -0.75D * s * s * s + 1.5D * s * s + 0.25D * s;
	}

	/*
	 * private static final void applyFilter(BufferedImage img, ImageFilter filter)
	 * { FilteredImageSource src = new FilteredImageSource(img.getSource(), filter);
	 * Image fImg = Toolkit.getDefaultToolkit().createImage(src); Graphics2D g =
	 * img.createGraphics(); g.drawImage(fImg, 0, 0, null, null); //g.dispose(); }
	 */

	private void shearX(Graphics2D g, int period, int phase, int width, int height) {
		int frames = 15;

		for (int i = 0; i < height; i++) {
			double d = (period >> 1) * Math.sin((double) i / (double) period + (6.2831853071795862D * phase) / frames);
			g.copyArea(0, i, width, 1, (int) d, 0);
			g.setColor(_bg_color);
			if (d >= 0) {
				g.drawLine(0, i, (int) d, i);
			} else {
				g.drawLine(width + (int) d, i, width, i);
			}
			g.setColor(_fg_color);

		}
	}

	private void shearY(Graphics2D g, int period, int phase, int width, int height) {
		int frames = 15;

		for (int i = 0; i < width; i++) {
			double d = (period >> 1) * Math.sin((float) i / period + (6.2831853071795862D * phase) / frames);
			g.copyArea(i, 0, 1, height, 0, (int) d);
			g.setColor(_bg_color);
			if (d >= 0) {
				g.drawLine(i, 0, i, (int) d);
			} else {
				g.drawLine(i, height + (int) d, i, height);
			}
			g.setColor(_fg_color);
		}
	}

	public static double singlePixelConvolution(double[][] input, int x, int y, double[][] k, int kernelWidth,
			int kernelHeight) {
		double output = 0;
		for (int i = 0; i < kernelWidth; ++i) {
			for (int j = 0; j < kernelHeight; ++j) {
				output = output + (input[x + i][y + j] * k[i][j]);
			}
		}
		return output;
	}

	public static double[][] convolution2D(double[][] input, int width, int height, double[][] kernel, int kernelWidth,
			int kernelHeight) {
		int smallWidth = width - kernelWidth + 1;
		int smallHeight = height - kernelHeight + 1;
		double[][] output = new double[smallWidth][smallHeight];
		for (int i = 0; i < smallWidth; ++i) {
			for (int j = 0; j < smallHeight; ++j) {
				output[i][j] = 0;
			}
		}
		for (int i = 0; i < smallWidth; ++i) {
			for (int j = 0; j < smallHeight; ++j) {
				output[i][j] = singlePixelConvolution(input, i, j, kernel, kernelWidth, kernelHeight);
				// if (i==32- kernelWidth + 1 && j==100- kernelHeight + 1)
				// System.out.println("Convolve2D: "+output[i][j]);
			}
		}
		return output;
	}

	public static double[][] convolution2DPadded(double[][] input, int width, int height, double[][] kernel,
			int kernelWidth, int kernelHeight) {
		int smallWidth = width - kernelWidth + 1;
		int smallHeight = height - kernelHeight + 1;
		int top = kernelHeight / 2;
		int left = kernelWidth / 2;
		double small[][] = new double[smallWidth][smallHeight];
		small = convolution2D(input, width, height, kernel, kernelWidth, kernelHeight);
		double large[][] = new double[width][height];
		for (int j = 0; j < height; ++j) {
			for (int i = 0; i < width; ++i) {
				large[i][j] = 0;
			}
		}
		for (int j = 0; j < smallHeight; ++j) {
			for (int i = 0; i < smallWidth; ++i) {
				// if (i+left==32 && j+top==100) System.out.println("Convolve2DP:
				// "+small[i][j]);
				large[i + left][j + top] = small[i][j];
			}
		}
		return large;
	}

	public static double gaussianDiscrete2D(double theta, int x, int y) {
		double g = 0;
		for (double ySubPixel = y - 0.5; ySubPixel < y + 0.55; ySubPixel += 0.1) {
			for (double xSubPixel = x - 0.5; xSubPixel < x + 0.55; xSubPixel += 0.1) {
				g = g + ((1 / (2 * Math.PI * theta * theta))
						* Math.pow(Math.E, -(xSubPixel * xSubPixel + ySubPixel * ySubPixel) / (2 * theta * theta)));
			}
		}
		g = g / 121;
		return g;
	}

	public static double[][] gaussian2D(double theta, int size) {
		double[][] kernel = new double[size][size];
		for (int j = 0; j < size; ++j) {
			for (int i = 0; i < size; ++i) {
				kernel[i][j] = gaussianDiscrete2D(theta, i - (size / 2), j - (size / 2));
			}
		}

		double sum = 0;
		for (int j = 0; j < size; ++j) {
			for (int i = 0; i < size; ++i) {
				sum = sum + kernel[i][j];

			}
		}

		return kernel;
	}

	public static double[][] gaussian(double[][] input, int ks, double sigma) {
		int width = input.length;
		int height = input[0].length;
		double[][] gaussianKernel = new double[ks][ks];
		double[][] output = new double[width][height];
		gaussianKernel = gaussian2D(sigma, ks);
		output = convolution2DPadded(input, width, height, gaussianKernel, ks, ks);
		return output;
	}
}