package com.zhougang.camera_opengl;


import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.opengl.GLES10;
import android.opengl.GLES11;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.*;

public class MainActivity extends AppCompatActivity {
    private GLSurfaceView mGLSurfaceView;
    private Camera mCamera;
    private int mOESTextureId = -1;
    private SurfaceTexture mSurfaceTexture;
    private float[] transformMatrix = new float[]{ 1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f};
    private FloatBuffer mDataBuffer;
    private int mProgram;
    private int[] mFBOIds = new int[1];
    private static final String TAG = "MainActivity";
    private static final float[] vertexData = {
            1f, 1f, 1f, 1f,
           -1f, 1f, 0f, 1f,
           -1f,-1f, 0f, 0f,
            1f, 1f, 1f, 1f,
           -1f,-1f, 0f, 0f,
            1f,-1f, 1f, 0f
    };
    private static final String VERTEX_SHADER = "" +
            "attribute vec4 aPosition;\n" +
            "uniform mat4 uTextureMatrix;\n" +
            "attribute vec4 aTextureCoordinate;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main()\n" +
            "{\n" +
            "  vTextureCoord = (uTextureMatrix * aTextureCoordinate).xy;\n" +
            "  gl_Position = aPosition;\n" +
            "}\n";
    private static final String FRAGMENT_SHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES uTextureSampler;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main()\n" +
            "{\n" +
            " vec2 offset[25];\n" +
            " offset[0]=vec2(-2.0,-2.0); offset[1]=vec2(-1.0,-2.0); offset[2]=vec2(0.0,-2.0); offset[3]=vec2(1.0, -2.0); offset[4]=vec2(2.0, -2.0);\n" +
            " offset[5]=vec2(-2.0,-1.0); offset[6]=vec2(-1.0,-1.0); offset[7]=vec2(0.0,-1.0); offset[8]=vec2(1.0, -1.0); offset[9]=vec2(2.0, -1.0);\n" +
            " offset[10]=vec2(-2.0,0.0); offset[11]=vec2(-1.0,0.0); offset[12]=vec2(0.0,0.0); offset[13]=vec2(1.0, 0.0); offset[14]=vec2(2.0, 0.0);\n" +
            " offset[15]=vec2(-2.0,1.0); offset[16]=vec2(-1.0,1.0); offset[17]=vec2(0.0,1.0); offset[18]=vec2(1.0, 1.0); offset[19]=vec2(2.0, 1.0);\n" +
            " offset[20]=vec2(-2.0,2.0); offset[21]=vec2(-1.0,2.0); offset[22]=vec2(0.0,2.0); offset[23]=vec2(1.0, 2.0); offset[24]=vec2(2.0, 2.0);\n" +
            " vec4 sample[25];\n" +
            " for (int i = 0; i < 25; i++){\n" +
            "    sample[i] = texture2D(uTextureSampler, vTextureCoord + offset[i]);\n" +
            " }\n" +
            " gl_FragColor = 25.0 * sample[12];\n" +
            " for (int i = 0; i < 25; i++){\n" +
            "    if (i != 12){\n" +
            "        gl_FragColor-=sample[i];\n" +
            "    }\n" +
            " }\n" +
            "  gl_FragColor = texture2D(uTextureSampler, vTextureCoord);\n" +
            "}\n";
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        final int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        mCamera = Camera.open(mCameraId);
        mCamera.setDisplayOrientation(90);
        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                mOESTextureId = createOESTexture();
                mDataBuffer = createBuffer(vertexData);
                int vertexShader = loadShader(GL_VERTEX_SHADER, VERTEX_SHADER);
                int fragmentShader = loadShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
                mProgram = linkProgram(vertexShader, fragmentShader);
                initSurfaceTexture();
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                glViewport(0,0,width,height);
            }

            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onDrawFrame(GL10 gl) {

                if (mSurfaceTexture != null) {
                    mSurfaceTexture.updateTexImage();
                   // mSurfaceTexture.getTransformMatrix(transformMatrix);
                    Log.e("zhou: ", "transform " + transformMatrix[0] + transformMatrix[1]);
                }
                glClearColor(1.0f,0.0f,0.0f,0.0f);
                int aPosition = glGetAttribLocation(mProgram,"aPosition");
                int aTextureCoord = glGetAttribLocation(mProgram, "aTextureCoordinate");
                int uTextureMatrix = glGetUniformLocation(mProgram, "uTextureMatrix");
                int uTextureSampler = glGetUniformLocation(mProgram,"uTextureSampler");
                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_EXTERNAL_OES,mOESTextureId);
                glUniform1i(uTextureSampler, 0);
                glUniformMatrix4fv(uTextureMatrix, 1, false, transformMatrix, 0);
                if (mDataBuffer == null){
                    Log.e("buffer:","create buffer failed!");
                }
                mDataBuffer.position(0);
                glEnableVertexAttribArray(aPosition);
                glVertexAttribPointer(aPosition, 2, GL_FLOAT, false, 16, mDataBuffer);
                mDataBuffer.position(2);
                glEnableVertexAttribArray(aTextureCoord);
                glVertexAttribPointer(aTextureCoord, 2, GL_FLOAT, false, 16, mDataBuffer);
                glDrawArrays(GL_TRIANGLES, 0, 6);

            }
        });
        setContentView(mGLSurfaceView);
    }
    @TargetApi(Build.VERSION_CODES.FROYO)
    public static int createOESTexture(){
        int[] tex = new int[1];
        GLES20.glGenTextures(1,tex,0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES,tex[0]);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_NEAREST);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER,GL10.GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S,GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T,GL10.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES,0);

        return tex[0];
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public boolean initSurfaceTexture(){
        if (mCamera == null || mGLSurfaceView == null) {
            Log.e(TAG, "mCamera or mGLSurfaceView is null!");
            return false;
        }
        if (mOESTextureId == -1) {
            Log.e(TAG, "mOESTextureId is null!");
            return false;
        }
        mSurfaceTexture = new SurfaceTexture(mOESTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                Log.e("render:","render start!");
                mGLSurfaceView.requestRender();

            }
        });
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.startPreview();
        } catch (IOException e){
            e.printStackTrace();
        }

        return true;
    }
    public FloatBuffer createBuffer(float[] vertexData) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(vertexData, 0, vertexData.length).position(0);
        return buffer;
    }
    @TargetApi(Build.VERSION_CODES.FROYO)
    public int loadShader(int type, String shaderSource) {
        int shader = glCreateShader(type);
        glShaderSource(shader, shaderSource);
        glCompileShader(shader);
        final int[] compileStatus = new int[1];
        glGetShaderiv(shader, GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0){
            Log.e("compile shader:","compile failed!");
            Log.e("compile shader detail：", "Results of compiling source:" + "\n" + shaderSource + "\n:"
                    + glGetShaderInfoLog(shader));
        }
        return shader;
    }
    @TargetApi(Build.VERSION_CODES.FROYO)
    public int linkProgram(int verShader, int fragShader){
        int program = glCreateProgram();
        glAttachShader(program,verShader);
        glAttachShader(program, fragShader);
        glLinkProgram(program);
        final int[] linkStatus = new int[1];
        glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0){
            Log.e("link shader:","link failed!");
        }
        else{
            Log.e("link shader:","link success!");
        }
        glUseProgram(program);
        return program;
    }
}
