#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include <android/log.h>

using namespace cv;
using namespace std;

extern "C" {

    float resizeImg(Mat img_src, Mat &img_resize, int resize_width) {

        float scale = resize_width / (float) img_src.cols;
        if (img_src.cols > resize_width) {
            int new_height = cvRound(img_src.rows * scale);
            resize(img_src, img_resize, Size(resize_width, new_height));
        } else {
            img_resize = img_src;
        }
        return scale;
    }

    int iou() {

    }

    JNIEXPORT jlong JNICALL
    Java_kr_co_ecoletree_facedetection_MainActivity_loadCascade(JNIEnv *env, jobject instance,
                                                                jstring cascadeFileName_) {

        const char *nativeFileNameString = env->GetStringUTFChars(cascadeFileName_, 0);

        string baseDir("/storage/emulated/0/");
        baseDir.append(nativeFileNameString);
        const char *pathDir = baseDir.c_str();

        jlong ret = 0;
        ret = (jlong) new CascadeClassifier(pathDir);
        if (((CascadeClassifier *) ret)->empty()) {
            __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                                "CascadeClassifier로 로딩 실패  %s", nativeFileNameString);
        }
        else
            __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                                "CascadeClassifier로 로딩 성공 %s", nativeFileNameString);


        env->ReleaseStringUTFChars(cascadeFileName_, nativeFileNameString);

        return ret;
    }

    JNIEXPORT jint JNICALL
    Java_kr_co_ecoletree_facedetection_MainActivity_detect1(JNIEnv *env, jobject instance,
                                                           jlong cascadeClassifier_face,
                                                           jlong matAddrInput, jlong matAddrResult) {

        Mat &img_input = *(Mat *) matAddrInput;
        Mat &img_result = *(Mat *) matAddrResult;

        img_result = img_input.clone();

        std::vector<Rect> faces;
        Mat img_gray;

        cvtColor(img_input, img_gray, COLOR_BGR2GRAY);
        equalizeHist(img_gray, img_gray);

        Mat img_resize;
        float resizeRatio = resizeImg(img_gray, img_resize, 640);

        /*
         void CascadeClassifier::detectMultiScale(const Mat& image, vector<Rect>& objects, double scaleFactor=1.1, int minNeighbors=3, int flags=0, Size minSize=Size(), Size maxSize=Size())
        // Detects objects of different sizes in the input image.
        // The detected objects are returned as a list of rectangles.

        Parameters:
            * cascade – Haar classifier cascade (OpenCV 1.x API only). It can be loaded from XML or YAML file using Load(). When the cascade is not needed anymore, release it using cvReleaseHaarClassifierCascade(&cascade).
            * image – Matrix of the type CV_8U containing an image where objects are detected.
            * objects – Vector of rectangles where each rectangle contains the detected object.
            * scaleFactor – Parameter specifying how much the image size is reduced at each image scale.
            * minNeighbors – Parameter specifying how many neighbors each candidate rectangle should have to retain it.
            * flags – Parameter with the same meaning for an old cascade as in the function cvHaarDetectObjects. It is not used for a new cascade.
            * minSize – Minimum possible object size. Objects smaller than that are ignored.
            * maxSize – Maximum possible object size. Objects larger than that are ignored.
        */

        //-- Detect faces
        ((CascadeClassifier *) cascadeClassifier_face)->detectMultiScale( img_resize, faces, 1.05, 3, 0 | CASCADE_FIND_BIGGEST_OBJECT, Size(30, 30) ); // CASCADE_SCALE_IMAGE

        __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ",
                            (char *) "face %lu found ", faces.size());

        for (int i = 0; i < faces.size(); i++) {
            double real_facesize_x = faces[i].x / resizeRatio;
            double real_facesize_y = faces[i].y / resizeRatio;
            double real_facesize_width = faces[i].width / resizeRatio;
            double real_facesize_height = faces[i].height / resizeRatio;

            Point center( real_facesize_x + real_facesize_width / 2, real_facesize_y + real_facesize_height / 2);
//            Point center(
//                static_cast<int>(std::floor(real_facesize_x + real_facesize_width / 2))
//                , static_cast<int>(std::floor(real_facesize_y + real_facesize_height / 2))
//            );
//            Size size(
//                static_cast<int>(std::floor(real_facesize_width / 2))
//                , static_cast<int>(std::floor(real_facesize_height / 2))
//            );
            rectangle(img_result
                    , Point(real_facesize_x, real_facesize_y)
                    , Point(real_facesize_x+real_facesize_width, real_facesize_y+real_facesize_height)
                    ,Scalar(255, 0, 255)
                    , 10, LINE_8, 0);
//            ellipse(img_result, center, Size( real_facesize_width / 2, real_facesize_height / 2), 0, 0, 360,
//                    Scalar(255, 0, 255), 15, LINE_8, 0);
        }

        return static_cast<jint>(faces.size());
    }

    JNIEXPORT jint JNICALL
    Java_kr_co_ecoletree_facedetection_MainActivity_detect2(JNIEnv *env, jobject instance,
                                                                 jlong cascadeClassifier_1,
                                                                 jlong cascadeClassifier_2,
                                                                 jlong matAddrInput,
                                                                 jlong matAddrResult) {
        Mat &img_input = *(Mat *) matAddrInput;
        Mat &img_result = *(Mat *) matAddrResult;

        img_result = img_input.clone();

        std::vector<Rect> faces1;
        std::vector<Rect> faces2;
        Mat img_gray;

        cvtColor(img_input, img_gray, COLOR_BGR2GRAY);
        equalizeHist(img_gray, img_gray);

        Mat img_resize;
        float resizeRatio = resizeImg(img_gray, img_resize, 640);

        //-- Detect faces
        ((CascadeClassifier *) cascadeClassifier_1)->detectMultiScale( img_resize, faces1, 1.05, 3, 0 | CASCADE_FIND_BIGGEST_OBJECT, Size(80, 80) ); // CASCADE_SCALE_IMAGE
        ((CascadeClassifier *) cascadeClassifier_2)->detectMultiScale( img_resize, faces2, 1.05, 3, 0 | CASCADE_FIND_BIGGEST_OBJECT, Size(80, 80) ); // CASCADE_SCALE_IMAGE

        __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ",
                            (char *) "face %lu found ", faces1.size());

        for (int i = 0; i < faces1.size(); i++) {
            double real_facesize_x = faces1[i].x / resizeRatio;
            double real_facesize_y = faces1[i].y / resizeRatio;
            double real_facesize_width = faces1[i].width / resizeRatio;
            double real_facesize_height = faces1[i].height / resizeRatio;

            rectangle(img_result
                    , Point(real_facesize_x, real_facesize_y)
                    , Point(real_facesize_x+real_facesize_width, real_facesize_y+real_facesize_height)
                    ,Scalar(255, 0, 255)
                    , 5, LINE_8, 0);
        }

        for (int i = 0; i < faces2.size(); i++) {
            double real_facesize_x = faces2[i].x / resizeRatio;
            double real_facesize_y = faces2[i].y / resizeRatio;
            double real_facesize_width = faces2[i].width / resizeRatio;
            double real_facesize_height = faces2[i].height / resizeRatio;

            rectangle(img_result
                    , Point(real_facesize_x, real_facesize_y)
                    , Point(real_facesize_x+real_facesize_width, real_facesize_y+real_facesize_height)
                    ,Scalar(0, 255, 255)
                    , 5, LINE_8, 0);
        }

        return static_cast<jint>(faces1.size() + faces2.size());

    }
}