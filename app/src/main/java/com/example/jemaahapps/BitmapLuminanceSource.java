package com.example.jemaahapps;

import android.graphics.Bitmap;
import com.google.zxing.LuminanceSource;

/**
 * This class converts an Android Bitmap image into a grayscale (luminance) format
 * so it can be processed by the ZXing library for barcode/QR code decoding.
 */
public class BitmapLuminanceSource extends LuminanceSource {

    // Stores grayscale brightness values for each pixel
    private final byte[] luminances;

    /**
     * Constructor that takes a color Bitmap and converts it into a grayscale (luminance) byte array.
     * ZXing works with grayscale images, so this step is essential for decoding.
     *
     * @param bitmap The input color Bitmap image.
     */
    public BitmapLuminanceSource(Bitmap bitmap) {
        // Call superclass constructor with width and height of the bitmap
        super(bitmap.getWidth(), bitmap.getHeight());

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Create an array to hold all pixel values (ARGB format)
        int[] pixels = new int[width * height];
        // Extract all pixels from the bitmap into the array
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // Create an array to hold grayscale (luminance) values
        luminances = new byte[width * height];

        // Convert each pixel to grayscale
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                int pixel = pixels[offset + x];

                // Extract red, green, blue values from ARGB pixel
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;

                // Convert RGB to grayscale using simple average method
                luminances[offset + x] = (byte) ((r + g + b) / 3);
            }
        }
    }

    /**
     * Returns a specific row (line) of grayscale pixel data from the image.
     * ZXing uses this to scan the image line by line.
     *
     * @param y   The row index to return.
     * @param row An optional buffer to reuse for performance.
     * @return A byte array containing grayscale values for the specified row.
     */
    @Override
    public byte[] getRow(int y, byte[] row) {
        int width = getWidth();

        // If no buffer is provided or too small, create a new one
        if (row == null || row.length < width) {
            row = new byte[width];
        }

        // Copy the luminance values for the requested row into the buffer
        System.arraycopy(luminances, y * width, row, 0, width);
        return row;
    }

    /**
     * Returns the full image as a one-dimensional array of grayscale values.
     * This is used when ZXing needs access to the entire image at once.
     *
     * @return A byte array containing all grayscale (luminance) pixel values.
     */
    @Override
    public byte[] getMatrix() {
        return luminances;
    }
}
