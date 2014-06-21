package com.client.puzzleSolver;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Almog on 9/5/2014.
 */
public class CalculationsUtils {


    public static List<Point> sortPointsByXValue(List<Point> points) {
        Collections.sort(points, xComparator);
        return points;
    }

    public static List<Point> sortPointsByYValue(List<Point> points) {
        Collections.sort(points, yComparator);
        return points;
    }

    public static Comparator<Point> xComparator = new Comparator<Point>() {
        @Override
        public int compare(Point lhs, Point rhs) {
            return lhs.x > rhs.x ? 1 : (lhs.x == rhs.x ? (lhs.y > rhs.y ? 1 : -1) : -1);
        }
    };

    public static Comparator<Point> yComparator = new Comparator<Point>() {
        @Override
        public int compare(Point lhs, Point rhs) {
            return lhs.y > rhs.y ? 1 : (lhs.y == rhs.y ? (lhs.x > rhs.x ? 1 : -1) : -1);
        }
    };

    /**
     * This method calculate the intersections between two point and returns it
     * @param pointA - the first point
     * @param pointB - the second point
     * @return the intersection between the two points or (-1, -1) if not exist
     */
    public static Point calculateIntersect(Mat pointA, Mat pointB) {
        float x1 = ((int) pointA.get(0, 0)[0]),
                y1 = ((int) pointA.get(0, 0)[1]),
                x2 = ((int) pointA.get(0, 0)[2]),
                y2 = ((int) pointA.get(0, 0)[3]),
                x3 = ((int) pointB.get(0, 0)[0]),
                y3 = ((int) pointB.get(0, 0)[1]),
                x4 = ((int) pointB.get(0, 0)[2]),
                y4 = ((int) pointB.get(0, 0)[3]);

        float d = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));
        if (d != 0) {
            int x = (int) (((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d);
            int y = (int) (((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d);
            return new Point(x, y);
        } else {
            return new Point(-1, -1);
        }
    }

    public static Comparator<double[]> verticalComparator = new Comparator<double[]>() {
        @Override
        public int compare(double[] lhs, double[] rhs) {
            return lhs[0] > rhs[0] ? 1 :  -1;
        }
    };

    public static Comparator<double[]> horizontalComparator = new Comparator<double[]>() {
        @Override
        public int compare(double[] lhs, double[] rhs) {
            return lhs[1] > rhs[1] ? 1 :  -1;
        }
    };
}
