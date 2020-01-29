package android_image_sequence.android_image_sequence;

import java.lang.Thread;

// Take a picture if the camera is not in use, mark the camera
// in use, and wait until the picture is processed and the
// camera is again not marked in use before continuing.

public class PictureThread implements Runnable  {

    private AndroidImageSequence m_parent;

    public PictureThread(AndroidImageSequence parent) {
        m_parent = parent;
    }
    
    public void run() {
            
        while (true) {
            try {

                // TODO: Not sure if all the logic here must be in the critical section,
                // but it is safer that way
                
                synchronized(m_parent) {
                    
                    // Stop if told so
                    if (m_parent.doStop) {
                        break;
                    }
                    
                    // wait until the user clicks on the button to capture pictures
                    if (!m_parent.doStart) {
                        Thread.sleep(10); // sleep 0.01 seconds
                        continue;
                    }
                    
                    if (m_parent.inUse) {
                        // Wait until the picture is processed
                        Thread.sleep(10); // sleep 0.01 seconds
                        continue;
                    }
                    
                    
                    // Mark the camera in use
                    m_parent.inUse = true;
                        
                    // refresh the preview (must happen before the picture is taken)
                    m_parent.mPreview.refreshCamera(m_parent.mCamera);

                    // Take a picture
                    m_parent.mCamera.takePicture(null, null, m_parent.mPicture);

                }
            } catch (Exception e) {
                continue;
            }
                    
        }
            
    }
    
}
