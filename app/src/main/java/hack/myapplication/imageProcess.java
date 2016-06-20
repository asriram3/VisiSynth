package hack.myapplication;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by aditya on 4/2/16.
 */
public class imageProcess {

    public static class Point {
        public int x;
        public int y;
    }

    public static class Line {
        public double m;
        public double c;
    }

    Context ctx;

    imageProcess(Context context){
        ctx = context;
    }

    public static double[][] BufferedImage2Mat(Bitmap src, int width, int height) {
        double[][] mat = new double[width][height];
        int srcwidth = src.getWidth();
        int srcheight = src.getHeight();
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++) {
                int pixel = (src.getPixel(i * srcwidth / width, j * srcheight / height));
                mat[i][j] = (((pixel >> 16) & 0xFF) + ((pixel >> 8) & 0xFF) + (pixel & 0xFF)) / 3;
            }
        return mat;
    }

    public static double[][] BufferedYUVImage2Mat(byte [] src, int srcwidth, int srcheight, int width, int height) {
        double[][] mat = new double[width][height];
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++) {
                int srci = i * srcwidth / width;
                int srcj = j * srcheight / height;
                int pixel = src[srci + srcj * srcwidth];
                mat[i][j] = pixel;
            }
        return mat;
    }

//    public static BufferedImage Mat2BufferedImage(double [][] mat) {
//        int width = mat.length;
//        int height = mat[0].length;
//        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
//        for(int i = 0; i < width; i ++)
//            for(int j = 0; j < height; j ++) {
//                int intgray = (int)mat[i][j];
//                img.setRGB(i, j, intgray + (intgray << 8)+ (intgray << 16));
//            }
//        return img;
//    }

    public static double[][] Resize(double[][] src, int width, int height) {
        double[][] ret = new double[width][height];
        int srcwidth = src.length;
        int srcheight = src[0].length;
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                ret[i][j] = src[i * srcwidth / width][j * srcheight / height];
        return ret;
    }

    public static void Normalize(double[][] mat) {
        int m = mat.length;
        int n = mat[0].length;
        double min = 999, max = -999;
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++) {
                if (mat[i][j] < min) min = mat[i][j];
                if (mat[i][j] > max) max = mat[i][j];
            }
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                mat[i][j] = (mat[i][j] - min) / (max - min) * 255;
    }

    public static void Threshold(double[][] mat, double threshold) {
        int m = mat.length;
        int n = mat[0].length;
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                if (mat[i][j] > threshold)
                    mat[i][j] = 1;
                else
                    mat[i][j] = 0;
    }

    public static double[][] MatCopy(double[][] mat) {
        int m = mat.length;
        int n = mat[0].length;
        double[][] ret = new double[m][n];
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                ret[i][j] = mat[i][j];
        return ret;
    }

    public static double[][] SobelOperator(double[][] mat) {
        int m = mat.length;
        int n = mat[0].length;
        double[][] Y1 = new double[m][n];
        double[][] Y2 = new double[m][n];
        double[] tmpm = new double[m];
        for (int i = 0; i < m; i++)
            for (int j = 1; j < n - 1; j++)
                Y1[i][j] = mat[i][j - 1] + mat[i][j] * 2.0 + mat[i][j + 1];
        for (int j = 0; j < n; j++) {
            for (int i = 1; i < m - 1; i++)
                tmpm[i] = -Y1[i - 1][j] + Y1[i + 1][j];
            for (int i = 1; i < m - 1; i++)
                Y1[i][j] = tmpm[i];
        }
        for (int i = 0; i < m; i++)
            for (int j = 1; j < n - 1; j++)
                Y2[i][j] = -mat[i][j - 1] + mat[i][j + 1];
        for (int j = 0; j < n; j++) {
            for (int i = 1; i < m - 1; i++)
                tmpm[i] = Y2[i - 1][j] + Y2[i][j] * 2.0 + Y2[i + 1][j];
            for (int i = 1; i < m - 1; i++)
                Y2[i][j] = tmpm[i];
        }
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                Y1[i][j] = Y1[i][j] * Y1[i][j] + Y2[i][j] * Y2[i][j];
        return Y1;
    }

    public static List<Point> EdgePoints(double[][] mat, double threshold) {
        List<Point> ret = new LinkedList<Point>();
        int m = mat.length;
        int n = mat[0].length;
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                if (mat[i][j] > threshold) {
                    Point tmpPoint = new Point();
                    tmpPoint.x = i;
                    tmpPoint.y = j;
                    ret.add(tmpPoint);
                }
        return ret;
    }

    public static double[][] MatVSlice(double[][] src, int from, int to) {
        int m = src.length;
        int n = src[0].length;
        int n1 = to - from;
        double[][] ret = new double[m][n1];
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n1; j++)
                ret[i][j] = src[i][j + from];
        return ret;
    }

    public static Point FindPeak2D(double[][] mat) {
        Point ret = new Point();
        double maxval = -999;
        int m = mat.length;
        int n = mat[0].length;
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                if (mat[i][j] > maxval) {
                    maxval = mat[i][j];
                    ret.x = i;
                    ret.y = j;
                }
        return ret;
    }

    public static double[][] HoughTransform(List<Point> points, double m_low, double m_high, int m_num,
                                            double c_low, double c_high, int c_num) {
        double[][] H = new double[m_num][c_num];
        for (Point p : points) {
            for (int j = 0; j < m_num; j++) {
                double jm = m_low + (double) j / m_num * (m_high - m_low);
                double jc = p.y - p.x * jm;
                int jcidx = (int) Math.round((jc - c_low) / (c_high - c_low) * c_num);
                if (jcidx >= 0 && jcidx < c_num)
                    H[j][jcidx]++;
            }
        }
        return H;
    }

    public static Line IterativeHoughPeak(List<Point> points, double m_low, double m_high, int m_num,
                                          double c_low, double c_high, int c_num, int depth) {
        double[][] H = HoughTransform(points, m_low, m_high, m_num, c_low, c_high, c_num);
        Point Hpeak = FindPeak2D(H);
        Line Hline = new Line();
        Hline.m = m_low + (double) Hpeak.x / m_num * (m_high - m_low);
        Hline.c = c_low + (double) Hpeak.y / c_num * (c_high - c_low);
        if (depth == 0)
            return Hline;
        else {
            double mrange = (m_high - m_low) / 5;
            double crange = (c_high - c_low) / 5;
            return IterativeHoughPeak(points, Hline.m - mrange, Hline.m + mrange, m_num,
                    Hline.c - crange, Hline.c + crange, c_num, depth - 1);
        }
    }

//    public static void BufferedImageDrawLine(BufferedImage dst, int x1, int x2, int y1, int y2) {
//        double l = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
//        for(int i = 0; i < l; i ++) {
//            double x = x1 + (x2 - x1) * i / l;
//            double y = y1 + (y2 - y1) * i / l;
//            dst.setRGB((int)x, (int)y, Color.RED.getRGB());
//        }
//    }

    public static Point[] NormalPoints(Line L1, Line L2, double q, double width) {
        double center_m = (L1.m + L2.m) / 2;
        double center_c = (L1.c + L2.c) / 2;
        double center_x = q * width;
        double center_y = center_m * center_x + center_c;
        double k = center_m * center_y + center_x;
        double x1 = (k - center_m * L1.c) / (1 + center_m * L1.m);
        double y1 = L1.m * x1 + L1.c;
        double x2 = (k - center_m * L2.c) / (1 + center_m * L2.m);
        double y2 = L2.m * x2 + L2.c;
        int l = (int) Math.round(Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
        int l_low = (int) (l * 0.1);
        int l_high = (int) (l * 0.9);
        Point[] ret = new Point[l_high - l_low];
        for (int i = 0; i < l_high - l_low; i++) {
            ret[i] = new Point();
            ret[i].x = (int) (x1 + (double) (i + l_low) / l * (x2 - x1));
            ret[i].y = (int) (y1 + (double) (i + l_low) / l * (y2 - y1));
        }
        return ret;
    }

    public static double[] LineAvg2D(double[][] mat, Point[] points) {
        int m = mat.length;
        int n = mat[0].length;
        int np = points.length;
        double[] val = new double[np];
        for (int i = 0; i < np; i++) {
            int x = points[i].x;
            int y = points[i].y;
            if (x > 1 && x < m - 1 && y > 1 && y < n - 1)
                val[i] = mat[x - 1][y - 1] + mat[x][y - 1] + mat[x + 1][y - 1] +
                        mat[x - 1][y - 0] + mat[x][y - 0] + mat[x + 1][y - 0] +
                        mat[x - 1][y + 1] + mat[x][y + 1] + mat[x + 1][y + 1];
            val[i] /= 9;
        }
        return val;
    }

    public static List<Double> FindPeaks(double[] x) {
        int n = x.length;
        if(n < 10) return new ArrayList<Double>();
        double[] xcpy = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            y[i] = x[i];
            xcpy[i] = x[i];
        }
        Arrays.sort(xcpy);
        double median = xcpy[n / 2];
        double max = 0;
        for (int i = 0; i < n; i ++) {
            y[i] = Math.max(0, median - y[i]);
            if (y[i] > max) max = y[i];
        }
        for (int i = 0; i < n; i ++) // normalize into [0, 1]
            y[i] /= max;

        List<Double> peaks = new ArrayList<Double>();
        /*
        for(int i = 0; i < n; i ++) {
            if(y[i] - 5 / max < 0.3) continue;
            int iend = i;
            while(iend < n && y[iend] - 5 / max > 0.3) iend ++;
            peaks.add(0.5 * (i + iend) / n);
            i = iend;
        }*/

        while (true) {
            int maxi = 0;
            max = 0;
            for (int i = 0; i < n; i++)
                if (y[i] > max) {
                    max = y[i];
                    maxi = i;
                }
            if (max < 0.2) break;
            int bound_high, bound_low;
            for (bound_high = maxi + 1; bound_high < maxi + 8; bound_high++)
                if (bound_high >= n) bound_high = maxi + 8;
                else if (y[bound_high] < max - 0.3) break;
            if (bound_high >= maxi + 8) break;
            for (bound_low = maxi - 1; bound_low > maxi - 8; bound_low--)
                if (bound_low < 0) bound_low = maxi - 8;
                else if (y[bound_low] < max - 0.3) break;
            if (bound_low <= maxi - 8) break;

            peaks.add(0.5 * (bound_high + bound_low) / n);
            for (int i = bound_low - 5; i < bound_high + 5; i++)
                if (i >= 0 && i < n)
                    y[i] = 0;
        }
        return peaks;
    }

    public static List<Double> AnalyzeMat(double[][] mat, double q) {
        int width = mat.length;
        int height = mat[0].length;

        double[][] mat_s = Resize(mat, 180, 120);
        double[][] edgemat = SobelOperator(mat_s);
        List<Point> edges = EdgePoints(edgemat, 40000);
        Line Hl1 = imageProcess.IterativeHoughPeak(edges, -0.1, 0.1, 100, 0, 60, 60, 0);
        Line Hl2 = imageProcess.IterativeHoughPeak(edges, -0.1, 0.1, 100, 60, 120, 60, 0);
        Hl1.c = Hl1.c / 120 * height;
        Hl2.c = Hl2.c / 120 * height;
        List<Double> candidates;
        if (Hl1.c > 0 && Hl1.c < height && Hl2.c > 0 && Hl2.c < height) {
            Point[] normal = imageProcess.NormalPoints(Hl1, Hl2, q, width);
            double[] normal_val = imageProcess.LineAvg2D(mat, normal);
            candidates = imageProcess.FindPeaks(normal_val);
        } else {
            candidates = new ArrayList<Double>();
        }
        return candidates;
    }

    private final int duration = 1; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private double freqOfTone;

    private final byte generatedSnd[] = new byte[2 * numSamples];

    void genTone(){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }



}