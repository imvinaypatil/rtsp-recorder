package recorder.engine;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import recorder.record.triggers.MotionDetector;
import recorder.sampler.Sample;

import java.io.File;

import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvAbsDiff;
import static org.bytedeco.javacpp.opencv_imgproc.*;


public class OpencvMotionDetector extends MotionDetector {

    public OpencvMotionDetector(long durationBefore, long durationAfter, double thresholdMin, double thresholdMax) {
        super(durationBefore, durationAfter, thresholdMin, thresholdMax);
    }

    @Override
    protected boolean _check(Sample sample) {
        double maxMotionPercent = maxMotionPercentV1(sample.getFile());
        return maxMotionPercent >= getThresholdMin() && maxMotionPercent <= getThresholdMax();
    }

    private static double maxMotionPercentV0(File videoFile) {
        try {
            FFmpegFrameGrabber videoCapture = new FFmpegFrameGrabber(videoFile.getAbsolutePath());
            videoCapture.start();
            final Size frameSize = new Size(
                    (int) videoCapture.getImageWidth(),
                    (int) videoCapture.getImageHeight()
            );
            Mat mat;
            final Mat workImg = new Mat();
            Mat movingAvgImg = null;
            final Mat gray = new Mat();
            final Mat diffImg = new Mat();
            final Mat scaleImg = new Mat();
            final Size kSize = new Size(8, 8);
            final double totalPixels = frameSize.area();
            double motionPercent;
            double maxMotionPercent = 0;
            Frame frame;
            OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
            while ((frame = videoCapture.grabImage()) != null) {
                mat = converterToMat.convert(frame);

                // Generate work image by blurring
                opencv_imgproc.blur(mat, workImg, kSize);
                // Generate moving average image if needed
                if (movingAvgImg == null) {
                    movingAvgImg = new Mat();
                    workImg.convertTo(movingAvgImg, opencv_core.CV_32F);

                }
                // Generate moving average image
                // CHECKSTYLE:OFF MagicNumber - Magic numbers here for illustration
                opencv_imgproc.accumulateWeighted(workImg, movingAvgImg, .03);
                // Convert the scale of the moving average
                opencv_core.convertScaleAbs(movingAvgImg, scaleImg);
                // Subtract the work image frame from the scaled image average
                opencv_core.absdiff(workImg, scaleImg, diffImg);
                // Convert the image to grayscale
                opencv_imgproc.cvtColor(diffImg, gray, opencv_imgproc.COLOR_BGR2GRAY);
                // Convert to BW
                opencv_imgproc.threshold(gray, gray, 25, 255, opencv_imgproc.THRESH_BINARY);
                // Total number of changed motion pixels
                motionPercent = 100.0 * opencv_core.countNonZero(gray) / totalPixels;
                // Detect if camera is adjusting and reset reference if more than
                // 25%
                if (motionPercent > 25.0) {
                    workImg.convertTo(movingAvgImg, opencv_core.CV_32F);
                }

                if (maxMotionPercent < motionPercent) {
                    maxMotionPercent = motionPercent;
                }
                mat.release();

                // TO REDUCE CPU USAGE!!!
//                Thread.sleep(50);
            }
            videoCapture.stop();
            videoCapture.release();
            workImg.release();
            if (movingAvgImg != null) {
                movingAvgImg.release();
            }
            gray.release();
            diffImg.release();
            scaleImg.release();

            return maxMotionPercent;
        } catch (FrameGrabber.Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static double maxMotionPercentV1(File videoFile) {
        try {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile.getAbsolutePath());
            grabber.start();
            final Size frameSize = new Size(
                    (int) grabber.getImageWidth(),
                    (int) grabber.getImageHeight()
            );
            int totalPixels = frameSize.area();
            opencv_core.IplImage image = opencv_core.IplImage.create(grabber.getImageWidth(), grabber.getImageHeight(), IPL_DEPTH_8U, 1);
            opencv_core.IplImage prevImage = null;
            opencv_core.IplImage diff = opencv_core.IplImage.create(grabber.getImageWidth(), grabber.getImageHeight(), IPL_DEPTH_8U, 1);
            OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
            double motionPercent;
            double maxMotionPercent = 0;
            Frame frame;
            long lastTimestamp = -1;
            while ((frame = grabber.grabImage()) != null) {
                if (lastTimestamp != -1 && grabber.getTimestamp() - lastTimestamp < 1000000) {
                    continue;
                }
                lastTimestamp = grabber.getTimestamp();

                cvCvtColor(converter.convert(frame), image, CV_RGB2GRAY);
                if (prevImage != null) {
                    cvAbsDiff(image, prevImage, diff);
                    cvThreshold(diff, diff, 40, 255, CV_THRESH_BINARY);
                    motionPercent = 100.0 * opencv_core.cvCountNonZero(diff) / totalPixels;
                    if (maxMotionPercent < motionPercent) {
                        maxMotionPercent = motionPercent;
                    }
                } else {
                    prevImage = opencv_core.IplImage.createCompatible(image);
                }
                opencv_core.cvCopy(image, prevImage);
            }
            grabber.stop();
            grabber.release();
            image.release();
            if (prevImage != null) {
                prevImage.release();
            }
            diff.release();
            return maxMotionPercent;
        } catch (FrameGrabber.Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
