package com.client.puzzleSolver;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.client.puzzleSolver.logic.computation.PuzzleSolverCalculator;
import com.client.puzzleSolver.logic.elements.Node;
import com.client.puzzleSolver.logic.elements.State;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Almog on 19/4/2014.
 */
public class CalculationsTask extends AsyncTask<String, Integer, Void> {

    private static final String TAG = "15PuzzleSolver-CalculationsTask";

    //Task state
    private final int UPLOADING_PHOTO_STATE = 0;
    private final int SERVER_PROC_STATE = 1;

    /**
     * The Threshold number for chacking if the checked saqare is a number
     */
    private final int CORRECTNESS_OF_NUMBER_THRESHOLD = 2500000;

    /**
     * Maximal number of rotations
     */
    private final int MAX_NUMBER_OF_ROTATIONS = 4;

    private ProgressDialog dialog;
    private CalculationListener listener;
    private CalculationState calculationState;
    private Context context;

    /**
     * Number templates for comparing
     */
    Mat oneTemplate, twoTemplate, threeTemplate, fourTemplate,
            fiveTemplate, sixTemplate, sevenTemplate,
            eightTemplate, nineTemplate, tenTemplate,
            elevenTemplate, twelveTemplate,
            thirteenTemplate, fourteenTemplate, fifteenTemplate;
    Mat thirteenMat, fifteenMat, oneTemplate2, oneTemplate3, oneTemplate4,
            oneTemplate5, oneTemplate6, twoTemplate1, twoTemplate2,
            threeTemplate1, threeTemplate2, threeTemplate3,
            sevenTemplate1, sevenTemplate2, sevenTemplate3;

    public CalculationsTask(Context context,
                            CalculationListener listener,
                            CalculationState calculationState) {
        dialog = new ProgressDialog(context);
        this.listener = listener;
        this.calculationState = calculationState;
        this.context = context;
        if (calculationState.isStateHigherOrEqual(CalculationState.PERSPECTIVE_TRANSFORM)) {
            createNumbersMatrices();
        }
    }

    //upload photo to server
    InputStream getPhoto(FileInputStream fileInputStream) {
        return new BufferedInputStream(fileInputStream);
    }

    //get image result from server and display it in result view
    void getResultImage(InputStream is) {
        try {
            //get result image from input stream

            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            listener.onImageReady(bitmap);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    //Main code for processing image algorithm
    void processImage(String inputImageFilePath) {
        publishProgress(UPLOADING_PHOTO_STATE);
        File inputFile = new File(inputImageFilePath);
        try {

            //create file stream for captured image file
            FileInputStream fileInputStream = new FileInputStream(inputFile);

            //upload photo
            final InputStream is = getPhoto(fileInputStream);

            //get processed photo from server
            if (is != null) {
                Bitmap origBitmap, resultBitmap;
                origBitmap = resultBitmap = BitmapFactory.decodeStream(is);

                //Stop calculations if state is original image
                if (calculationState.isStateHigherOrEqual(CalculationState.CANNY_EDGE)) {
                    resultBitmap = CalculateCannyEdgeAndHough(resultBitmap);
                    //Failed to find the square
                    if (resultBitmap == null) {
                        resultBitmap = origBitmap;
                    }
                }
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                InputStream newIs = new ByteArrayInputStream(stream.toByteArray());
                getResultImage(newIs);
            }
            fileInputStream.close();
        } catch (FileNotFoundException ex) {
            Log.e(TAG, ex.toString());
        } catch (IOException ex) {
            Log.e(TAG, ex.toString());
        }
    }

    /**
     * This function calculate the canny edge of the image and the hough lines if needed
     * @param bitmap Input image
     * @return The result image after canny edge and hough line transform
     */
    private Bitmap CalculateCannyEdgeAndHough(Bitmap bitmap) {
        Bitmap resultImg = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        Mat mImg = new Mat();
        Utils.bitmapToMat(resultImg, mImg);

        List<Mat> channels = new LinkedList<Mat>();
        Core.split(mImg.clone(), channels);

        Mat mGray = channels.get(2);
        Imgproc.threshold(mGray, mGray, 50, 200, Imgproc.THRESH_BINARY);

        Mat resMat = new Mat();

        Imgproc.Canny(mGray, resMat, 50, 50);

        //Stop calculations here for only canny edge - else continue to hough transform calculations
        if (calculationState.isStateHigher(CalculationState.CANNY_EDGE)) {
            Mat lines = new Mat();
            int threshold = 100;
            int minLineSize = 150;
            int lineGap = 50;

            Imgproc.HoughLinesP(resMat, lines, 1, Math.PI / 180, threshold, minLineSize, lineGap);

            lines = getOnlyRelevantLines(lines);

            if (lines == null) {
                return null;
            }
            MatOfPoint2f newCorners = getCorners(lines);

            if (newCorners != null) {
                if (calculationState.isEqual(CalculationState.HOUGH_TRANSFORM_DRAW)) {
                    drawHoughLinesAndCorners(mImg, lines, newCorners, true, true);
                }

                if (calculationState.isStateHigherOrEqual(CalculationState.PERSPECTIVE_TRANSFORM)) {
                    MatOfPoint2f sortedCorners = sortCorners(newCorners);
                    if (sortedCorners == null) {
                        return null;
                    } else {
                        mImg = findImageDirection(mImg, sortedCorners);
                        if (mImg == null)
                            return null;
                    }
                }
            } else {
                return null;
            }
        } else {
            mImg = resMat;
        }

        resultImg.recycle();
        System.gc();
        resultImg = Bitmap.createBitmap(mImg.cols(), mImg.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mImg, resultImg);

        return resultImg;
    }

    /**
     * This function get multiple lines and returns only the relevant lines after sorting them and
     * returns only the lines that surrounding the box
     */
    private Mat getOnlyRelevantLines(Mat lines) {
        List<double[]> vertical = new ArrayList<double[]>(),
                       horizontal = new ArrayList<double[]>();

        int size = lines.cols();
        for (int i = 0; i < size; i++) {
            double[] line = lines.get(0, i);
            if (Math.abs(line[0] - line[2]) < 150) {
                vertical.add(line);
            } else if (Math.abs(line[1] - line[3]) < 150) {
                horizontal.add(line);
            }
        }

        if (vertical.size() < 2 || horizontal.size() < 2)
            return null;

        Collections.sort(vertical, CalculationsUtils.verticalComparator);
        Collections.sort(horizontal, CalculationsUtils.horizontalComparator);

        Mat result = new Mat();
        result.create(1, 4, CvType.CV_32SC4);
        result.put(0, 0, horizontal.get(0));
        result.put(0, 1, horizontal.get(horizontal.size() - 1));
        result.put(0, 2, vertical.get(0));
        result.put(0, 3, vertical.get(vertical.size() - 1));

        return result;
    }

    /**
     * This function gets an color image and returns its canny edge image
     */
    private Mat rgbaColorToCanny(Mat img) {
        Mat gray = new Mat(img.rows(), img.cols(), CvType.CV_8UC1, new Scalar(0));
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGRA2GRAY, 4);
        Imgproc.threshold(gray, gray, 50, 200, Imgproc.THRESH_BINARY);
        Imgproc.Canny(gray, gray, 50, 70);

        return gray;
    }

    /**
     * This function gets the image and two numbers to compare validates that both number templates
     * appear in this image and return the point of second one, otherwise returns null
     */
    private Point templateMatchingValidation(Mat mImg, Mat compareNumMat,
                                                        Mat secondCompareNumMat) {
        Mat gray = rgbaColorToCanny(mImg);

        Imgproc.cvtColor(compareNumMat, compareNumMat, Imgproc.COLOR_BGRA2GRAY, 4);
        Imgproc.cvtColor(secondCompareNumMat, secondCompareNumMat, Imgproc.COLOR_BGRA2GRAY, 4);

        Point matchLoc, secondMatchLoc;

        Core.MinMaxLocResult mmr = templateMatchingResource(gray, compareNumMat);
        Core.MinMaxLocResult secondMmr = templateMatchingResource(gray, secondCompareNumMat);

        matchLoc = mmr.maxLoc;
        double minMaxVal = mmr.maxVal;
        System.out.println("min max val for 15: " + minMaxVal);
        System.out.println("Point: " + matchLoc.toString());

        secondMatchLoc = secondMmr.maxLoc;
        double minMaxVal2 = secondMmr.maxVal;
        System.out.println("min max val for 13: " + minMaxVal2);
        System.out.println("Point: " + secondMatchLoc.toString());

        if (minMaxVal > CORRECTNESS_OF_NUMBER_THRESHOLD &&
                minMaxVal2 > CORRECTNESS_OF_NUMBER_THRESHOLD) {
            return secondMatchLoc;
        } else {
            return null;
        }
    }

    private Core.MinMaxLocResult templateMatching(Mat mImg, Mat compareNumMat) {
        Imgproc.cvtColor(compareNumMat, compareNumMat, Imgproc.COLOR_BGRA2GRAY, 4);

        Core.MinMaxLocResult mmr = templateMatchingResource(mImg, compareNumMat);
        return mmr;
    }

    private Core.MinMaxLocResult templateMatchingResource(Mat grayImg, Mat resourceMat) {
        Mat result = new Mat();
        int templateMatchMethod = Imgproc.TM_CCOEFF;
        Imgproc.matchTemplate(grayImg, resourceMat, result, templateMatchMethod);

        return Core.minMaxLoc(result);
    }

    /**
     * This function gets an image and four corners and find it right rotation - if exists.
     * Returns the image after rotation or null if not exists
     */
    private Mat findImageDirection(Mat mImg, MatOfPoint2f sortedCorners) {
        Mat transformRes = Mat.zeros(mImg.rows(), mImg.cols(), CvType.CV_8UC3);

        //Create rectangle points for transformation coordinates
        List<Point> rectPointsList = new LinkedList<Point>();
        rectPointsList.add(new Point());
        rectPointsList.add(new Point(transformRes.cols(), 0));
        rectPointsList.add(new Point(transformRes.cols(), transformRes.rows()));
        rectPointsList.add(new Point(0, transformRes.rows()));
        MatOfPoint2f rectListMat = convertListToMap(rectPointsList);

        //Check that the polygon is a closed square
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(sortedCorners, approx, Imgproc.arcLength(sortedCorners, true) * 0.02, true);
        if (approx.size().height == 4) {
            boolean isCorrectDirection = false;
            int numberOfRotations = 0;

            while (!isCorrectDirection && numberOfRotations < MAX_NUMBER_OF_ROTATIONS) {
                Mat perspectiveTransform = Imgproc.getPerspectiveTransform(sortedCorners, rectListMat);
                Mat destination = transformRes.clone();
                Imgproc.warpPerspective(mImg.clone(), destination, perspectiveTransform, destination.size());

                if (calculationState.isEqual(CalculationState.PERSPECTIVE_TRANSFORM_SHOW)) {
                    Mat gray = rgbaColorToCanny(destination);
                    return gray;
                }

                Point matchLocation =
                        templateMatchingValidation(destination,
                                fifteenMat.clone(),
                                thirteenMat.clone());
                if (matchLocation != null) {
                    isCorrectDirection = true;
                    if (calculationState.isEqual(CalculationState.PERSPECTIVE_TRANSFORM)) {
                        Core.rectangle(destination,
                                matchLocation,
                                new Point(matchLocation.x + fifteenMat.cols(),
                                        matchLocation.y + fifteenMat.rows()),
                                new Scalar(0, 255, 0), 5
                        );
                        return destination;
                    }
                    break;
                } else {
                    List<Point> corners = sortedCorners.toList();
                    List<Point> newOrder = new LinkedList<Point>();
                    newOrder.add(corners.get(3));
                    newOrder.add(corners.get(0));
                    newOrder.add(corners.get(1));
                    newOrder.add(corners.get(2));
                    sortedCorners.fromList(newOrder);
                    numberOfRotations++;
                }
            }

            if (isCorrectDirection) {
                //Make sure that the original image being rotated to the correct side
                Mat perspectiveTransform = Imgproc.getPerspectiveTransform(sortedCorners, rectListMat);
                Mat destination = transformRes.clone();
                Imgproc.warpPerspective(mImg.clone(), destination, perspectiveTransform, destination.size());

                return calculateFullLogicOnBoard(destination);
            }
        } else {
            //The the approximation is not a rectangle
        }

        return null;
    }

    /**
     * This function gets an rotated image finds all the numbers in it and mark the number needs to
     * move in the next step on the image. returns the image after marking
     */
    private Mat calculateFullLogicOnBoard(Mat image) {
        int[] locations = returnLocations(image);

        int sum = 120;
        for (int i = 0; i < locations.length; i++) {
            sum -= locations[i];
        }

        if (sum == 0) { //In case all the numbers appears
            State startState = State.createStartState(locations);
            Node nextMove = PuzzleSolverCalculator.solve(startState);
            System.out.println("Next move: " + nextMove.toString());
            State.CellLocation cellLocation = nextMove.getAction().getCellLocation();
            int col = cellLocation.getColumnIndex();
            int row = cellLocation.getRowIndex();

            Point numberPoint = returnPointForPosition(col, row);
            Core.rectangle(image,
                    numberPoint,
                    new Point(numberPoint.x + fifteenMat.cols(),
                            numberPoint.y + fifteenMat.rows()),
                    new Scalar(0, 0, 255), 5
            );

        } else {
            System.out.println("Something wrong. Please try again");
        }

        return image;
    }

    private Point returnPointForPosition(int col, int row) {
        int y = 50 + (row * 100);
        int x = 70;
        if (col == 1) {
            x += 100;
        } else if (col == 2) {
            x = 320;
        } else if (col == 3) {
            x = 445;
        }
        return new Point(x, y);
    }

    private int[] returnLocations(Mat image) {
        int[] locations = {0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0};

        Mat grayImage = rgbaColorToCanny(image.clone());

        Rect r = new Rect(60, 40, 150, 100);
        locations[0] = findNumberForCube(grayImage.clone(), r);
        r = new Rect(160, 40, 150, 100);
        locations[1] = findNumberForCube(grayImage.clone(), r);
        r = new Rect(310, 40, 150, 100);
        locations[2] = findNumberForCube(grayImage.clone(), r);
        r = new Rect(435, 40, 150, 100);
        locations[3] = findNumberForCube(grayImage.clone(), r);

        r = new Rect(60, 140, 150, 100);
        locations[4] = findNumberForCube(grayImage.clone(), r);
        r = new Rect(160, 140, 150, 100);
        locations[5] = findNumberForCube(grayImage.clone(), r);
        r = new Rect(310, 140, 150, 100);
        locations[6] = findNumberForCube(grayImage.clone(), r);
        r = new Rect(435, 140, 150, 100);
        locations[7] = findNumberForCube(grayImage.clone(), r);

        r = new Rect(60, 240, 150, 100);
        locations[8] = findNumberForCube(grayImage.clone(), r);
        r = new Rect(160, 240, 150, 100);
        locations[9] = findNumberForCube(grayImage.clone(), r);
        r = new Rect(310, 240, 150, 100);
        locations[10] = findNumberForCube(grayImage.clone(), r);
        r = new Rect(435, 240, 150, 100);
        locations[11] = findNumberForCube(grayImage.clone(), r);

        r = new Rect(60, 340, 150, 100);
        locations[12] = findNumberForCube(grayImage.clone(), r);
        r = new Rect(160, 340, 150, 100);
        locations[13] = findNumberForCube(grayImage.clone(), r);
        r = new Rect(310, 340, 150, 100);
        locations[14] = findNumberForCube(grayImage.clone(), r);
        r = new Rect(435, 340, 150, 100);
        locations[15] = findNumberForCube(grayImage.clone(), r);

        return locations;
    }

    private int findNumberForCube(Mat grayImage, Rect r) {

        Mat cropImage = grayImage.submat(r);

        int number = 1;
        Core.MinMaxLocResult maxVal = templateMatching(cropImage, oneTemplate.clone());
        Core.MinMaxLocResult value = templateMatching(cropImage, oneTemplate2.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 1;
        }
        value = templateMatching(cropImage, oneTemplate3.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 1;
        }
        value = templateMatching(cropImage, oneTemplate4.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 1;
        }

        value = templateMatching(cropImage, oneTemplate5.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 1;
        }

        value = templateMatching(cropImage, oneTemplate6.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 1;
        }

        value = templateMatching(cropImage, twoTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 2;
        }

        value = templateMatching(cropImage, twoTemplate1.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 2;
        }

        value = templateMatching(cropImage, twoTemplate2.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 2;
        }

        value = templateMatching(cropImage, threeTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 3;
        }

        value = templateMatching(cropImage, threeTemplate1.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 3;
        }

        value = templateMatching(cropImage, threeTemplate2.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 3;
        }

        value = templateMatching(cropImage, threeTemplate3.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 3;
        }

        value = templateMatching(cropImage, fourTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 4;
        }

        value = templateMatching(cropImage, fiveTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 5;
        }

        value = templateMatching(cropImage, sixTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 6;
        }

        value = templateMatching(cropImage, sevenTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 7;
        }

        value = templateMatching(cropImage, sevenTemplate1.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 7;
        }

        value = templateMatching(cropImage, sevenTemplate2.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 7;
        }

        value = templateMatching(cropImage, sevenTemplate3.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 7;
        }

        value = templateMatching(cropImage, eightTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 8;
        }

        value = templateMatching(cropImage, nineTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 9;
        }

        value = templateMatching(cropImage, tenTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 10;
        }

        value = templateMatching(cropImage, elevenTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 11;
        }

        value = templateMatching(cropImage, twelveTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 12;
        }

        value = templateMatching(cropImage, thirteenTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 13;
        }

        value = templateMatching(cropImage, fourteenTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 14;
        }

        value = templateMatching(cropImage, fifteenTemplate.clone());
        if (value.maxVal > maxVal.maxVal) {
            maxVal = value; number = 15;
        }

        if (maxVal.maxVal < CORRECTNESS_OF_NUMBER_THRESHOLD) {
            System.out.println("Number: 0");
            return 0;
        } else {
            System.out.println("Number: " + number);
            return number;
        }
    }

    /**
     * This function sorting the corners in the right order of topLetf, topRight, bottomRight,
     * bottomLeft and returns them.
     */
    private MatOfPoint2f sortCorners(MatOfPoint2f newCorners) {
        List<Point> corners = newCorners.toList();

        List<Point> xOrder = CalculationsUtils.sortPointsByXValue(new LinkedList<Point>(corners));
        List<Point> yOrder = CalculationsUtils.sortPointsByYValue(new LinkedList<Point>(corners));

        Point[] array = new Point[4];

        if (xOrder.get(0).equals(yOrder.get(0)) ||
                xOrder.get(0).equals(yOrder.get(1))) {
            array[0] = xOrder.get(0);
            array[3] = xOrder.get(1);
        } else {
            array[0] = xOrder.get(1);
            array[3] = xOrder.get(0);
        }

        if (xOrder.get(3).equals(yOrder.get(2)) ||
                xOrder.get(3).equals(yOrder.get(3))) {
            array[2] = xOrder.get(3);
            array[1] = xOrder.get(2);
        } else {
            array[2] = xOrder.get(2);
            array[1] = xOrder.get(3);
        }

        MatOfPoint2f result = new MatOfPoint2f();
        result.fromArray(array);
        return result;
    }

    private void drawHoughLinesAndCorners(Mat mImg, Mat lines, MatOfPoint2f newCorners,
                                          boolean drawLines, boolean drawCorners) {
        //Draw lines
        if (drawLines) {
            for (int x = 0; x < lines.cols(); x++) {
                double[] vec = lines.get(0, x);
                double x1 = vec[0],
                        y1 = vec[1],
                        x2 = vec[2],
                        y2 = vec[3];
                Point start = new Point(x1, y1);
                Point end = new Point(x2, y2);

                Core.line(mImg, start, end, new Scalar(255, 0, 0), 3);
            }
        }

        //Draw corners
        if (drawCorners) {
            for (int i = 0; i < newCorners.rows(); i++) {
                double[] vec = newCorners.get(i, 0);
                Point point = new Point(vec[0], vec[1]);

                Core.circle(mImg, point, 15, new Scalar(0, 255, 0));
            }
        }
    }

    private MatOfPoint2f getCorners(Mat lines) {
        LinkedList<Point> list = new LinkedList<Point>();
        int numOfLines = lines.cols();
        for (int i = 0; i < numOfLines; i++) {
            for (int j = i + 1; j < numOfLines; j++) {
                Point corner = CalculationsUtils.calculateIntersect(lines.col(i), lines.col(j));

                if (corner.x >= 0 && corner.y >= 0) {
                    list.add(corner);
                }
            }
        }

        return convertListToMap(list);
    }

    /**
     * This function convert list of points into MatOfPoint2f
     * @param list - List of points to convert
     * @return The MatOfPoint2f we converted the list to
     */
    private MatOfPoint2f convertListToMap(List<Point> list) {
        Point[] array;
        array = list.toArray(new Point[list.size()]);
        MatOfPoint2f result = new MatOfPoint2f();
        result.fromArray(array);
        return  result;
    }

    /**
     * This function convert MatOfPoint2f to MatOfPoint and returns it
     * @param map - the MatOfPoint2f to convert
     * @return the new MatOfPoint
     */
    private MatOfPoint point2fToPoint(MatOfPoint2f map) {
        List<Point> list = map.toList();
        MatOfPoint mat = new MatOfPoint();
        mat.fromList(list);
        return mat;
    }

    /**
     * Create all the numbers resources once for using it in
     * prospective case and template matching
     * Otherwise the matrices will not be initialized
     */
    private void createNumbersMatrices() {
        int[] resourceNum = {R.drawable.one_template, R.drawable.two_template,
                R.drawable.three_template, R.drawable.four_template,
                R.drawable.five_template, R.drawable.six_template,
                R.drawable.seven_template, R.drawable.eight_template,
                R.drawable.nine_template, R.drawable.ten_template,
                R.drawable.eleven_template, R.drawable.twelve_template,
                R.drawable.thirteen_template, R.drawable.fourteen_template,
                R.drawable.fifteen_template};

        oneTemplate      = buildMatFromResource(resourceNum[0]);
        twoTemplate      = buildMatFromResource(resourceNum[1]);
        threeTemplate    = buildMatFromResource(resourceNum[2]);
        fourTemplate     = buildMatFromResource(resourceNum[3]);
        fiveTemplate     = buildMatFromResource(resourceNum[4]);
        sixTemplate      = buildMatFromResource(resourceNum[5]);
        sevenTemplate    = buildMatFromResource(resourceNum[6]);
        eightTemplate    = buildMatFromResource(resourceNum[7]);
        nineTemplate     = buildMatFromResource(resourceNum[8]);
        tenTemplate      = buildMatFromResource(resourceNum[9]);
        elevenTemplate   = buildMatFromResource(resourceNum[10]);
        twelveTemplate   = buildMatFromResource(resourceNum[11]);
        thirteenTemplate = buildMatFromResource(resourceNum[12]);
        fourteenTemplate = buildMatFromResource(resourceNum[13]);
        fifteenTemplate  = buildMatFromResource(resourceNum[14]);

        thirteenMat      = buildMatFromResource(R.drawable.thirteen);
        fifteenMat       = buildMatFromResource(R.drawable.fifteen);
        oneTemplate2     = buildMatFromResource(R.drawable.one_template2);
        oneTemplate3     = buildMatFromResource(R.drawable.one_template3);
        oneTemplate4     = buildMatFromResource(R.drawable.one_template4);
        oneTemplate5     = buildMatFromResource(R.drawable.one_template5);
        oneTemplate6     = buildMatFromResource(R.drawable.one_template6);

        twoTemplate1     = buildMatFromResource(R.drawable.two_template1);
        twoTemplate2     = buildMatFromResource(R.drawable.two_template2);
        threeTemplate1   = buildMatFromResource(R.drawable.three_template1);
        threeTemplate2   = buildMatFromResource(R.drawable.three_template2);
        threeTemplate3   = buildMatFromResource(R.drawable.three_template3);
        sevenTemplate1   = buildMatFromResource(R.drawable.seven_template1);
        sevenTemplate2   = buildMatFromResource(R.drawable.seven_template2);
        sevenTemplate3   = buildMatFromResource(R.drawable.seven_template3);
    }

    /**
     * Build Mat from resource using the context resources
     * @param resource - the resource we want to build
     * @return mat - the matrix we want to save the resource mat on
     */
    private Mat buildMatFromResource(int resource) {
        Bitmap compareNumBitmap =
                BitmapFactory.decodeResource(context.getResources(), resource);
        Mat mat = new Mat();
        Utils.bitmapToMat(compareNumBitmap, mat);

        return mat;
    }

    /**********************************************
     *** AsyncTask mandatory override functions ***
     **********************************************/

    protected void onPreExecute() {
        this.dialog.setMessage("Photo captured");
        this.dialog.show();
    }

    // background operation
    @Override
    protected Void doInBackground(String... params) {
        String uploadFilePath = params[0];
        processImage(uploadFilePath);

        //release camera when previous image is processed
        listener.onCameraReadyStateChange(true);
        return null;
    }

    // progress update, display dialogs
    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (progress[0] == UPLOADING_PHOTO_STATE) {
            dialog.setMessage("Uploading");
            dialog.show();
        } else if (progress[0] == SERVER_PROC_STATE) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog.setMessage("Processing");
            dialog.show();
        }
    }

    @Override
    protected void onPostExecute(Void param) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}