package org.opencv.samples.facedetect;

import android.content.Context;
import android.util.AttributeSet;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Size;

import java.util.List;

/**
 * Created by rongyile on 2017/9/14.
 */

public abstract  class MCameraBirdge extends CameraBridgeViewBase {

    public MCameraBirdge(Context context, int cameraId) {
        super(context, cameraId);
    }

    public MCameraBirdge(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected Size calculateCameraFrameSize(List<?> supportedSizes, ListItemAccessor accessor, int surfaceWidth, int surfaceHeight) {
        return super.calculateCameraFrameSize(supportedSizes, accessor, surfaceWidth, surfaceHeight);
    }
}
