/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.media.Image;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Config.InstantPlacementMode;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.InstantPlacementPoint;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;

import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DepthSettings;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.InstantPlacementSettings;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.helpers.TapHelper;
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper;
import com.google.ar.core.examples.java.common.samplerender.Framebuffer;
import com.google.ar.core.examples.java.common.samplerender.GLError;
import com.google.ar.core.examples.java.common.samplerender.Mesh;
import com.google.ar.core.examples.java.common.samplerender.SampleRender;
import com.google.ar.core.examples.java.common.samplerender.Shader;
import com.google.ar.core.examples.java.common.samplerender.Texture;
import com.google.ar.core.examples.java.common.samplerender.VertexBuffer;
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.PlaneRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.SpecularCubemapFilter;

import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.javagl.obj.*;

/** <h1>AR Furniture Simulator </h1>
 * AR_function_simuluoter는 AR 코어 API를 사용하여 생성하는 증강현실(AR) 애플리케이션이다.
 * 상자 모양의 가구 응용 프로그램은 의 너비, 깊이 및 높이를 사용한다.
 * 사용자가 만든 상자를 사용하여 모델을 만든 다음 AR로 이동하여 탐지된 평면을 표시하고 다음을 허용한다.
 * 평면을 클릭하여 3D 모델을 배치할 수 있다.
 *
 * @author : team 13
 * @version : 1.0
 * @since : 2022-11-25
 */
public class HelloArActivity extends AppCompatActivity implements SampleRender.Renderer {

  private static final String TAG = HelloArActivity.class.getSimpleName();

  private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";
  private static final String WAITING_FOR_TAP_MESSAGE = "Tap on a surface to place an object.";

  private static final float[] sphericalHarmonicFactors = {
    0.282095f,
    -0.325735f,
    0.325735f,
    -0.325735f,
    0.273137f,
    -0.273137f,
    0.078848f,
    -0.273137f,
    0.136569f,
  };

  private static final float Z_NEAR = 0.1f;
  private static final float Z_FAR = 100f;

  private static final int CUBEMAP_RESOLUTION = 16;
  private static final int CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32;

  // 렌더링. 렌더러는 여기서 생성되며 GLsurface가 생성될 때 초기화된다.
  private GLSurfaceView surfaceView;

  private boolean installRequested;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
  private TapHelper tapHelper;
  private SampleRender render;

  private PlaneRenderer planeRenderer;
  private BackgroundRenderer backgroundRenderer;
  private Framebuffer virtualSceneFramebuffer;
  private boolean hasSetTextureNames = false;

  private final DepthSettings depthSettings = new DepthSettings();
  private boolean[] depthSettingsMenuDialogCheckboxes = new boolean[2];

  private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
  private boolean[] instantPlacementSettingsMenuDialogCheckboxes = new boolean[1];
  // 기기 카메라에서 사용자가 물체를 배치할 표면까지의 추정 거리. 이 값은 객체의 겉보기 스케일에 영향을 미치지만
  // Instant Placement 포인트의 추적 방법은 SCREENSPACE_WITH_APTHATE_Distance입니다. [0.2, 2.0] 미터
  // 범위의 값은 대부분의 경우에 적합하다. 사용자가 카메라에 가까운 표면에 객체를 배치해야 하는 AR 경험에 더 낮은
  // 값을 사용한다. 사용자가 서서 물체를 바닥이나 바닥 앞에 놓으려고 할 가능성이 높은 경험에 더 큰 값을 사용한다.
  private static final float APPROXIMATE_DISTANCE_METERS = 2.0f;

  // 포인트 클라우드
  private VertexBuffer pointCloudVertexBuffer;
  private Mesh pointCloudMesh;
  private Shader pointCloudShader;
  //포인트클라우드가 변경되지 않은 경우 VBO가 업데이트되지 않도록 마지막으로 렌더링된 포인트클라우드를 추적한다.
  // PointCloud 개체를 비교할 수 없으므로 타임스탬프를 사용하여 이 작업을 수행한다.
  private long lastPointCloudTimestamp = 0;

  // 모델링 된 물체
  private Mesh virtualObjectMesh;
  private Shader virtualObjectShader;
  private Texture virtualObjectAlbedoTexture;
  private Texture virtualObjectAlbedoInstantPlacementTexture;

  private final List<WrappedAnchor> wrappedAnchors = new ArrayList();

  // 환경 인식에 사용될 변수
  private Texture dfgTexture;
  private SpecularCubemapFilter cubemapFilter;

  // 각 프레임에 대한 할당 수를 줄이기 위해 여기에 할당된 임시 매트릭스.
  private final float[] modelMatrix = new float[16];
  private final float[] viewMatrix = new float[16];
  private final float[] projectionMatrix = new float[16];
  private final float[] modelViewMatrix = new float[16]; // view x model
  private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model
  private final float[] sphericalHarmonicsCoefficients = new float[9 * 3];
  private final float[] viewInverseMatrix = new float[16];
  private final float[] worldLightDirection = {0.0f, 0.0f, 0.0f, 0.0f};
  private final float[] viewLightDirection = new float[4]; // view x world light direction
  public static int wid1,dep1,hei1;
  Button button;
  public static float[] vertices;
  public static float[] normals =
          {
                  0.000000f,-1.000000f, 0.000000f,
                  0.000000f, 1.000000f, 0.000000f,
                  1.000000f, 0.000000f, 0.000000f,
                  -0.000000f, 0.000000f, 1.000000f,
                  -1.000000f, -0.000000f, -0.000000f,
                  0.000000f,0.000000f ,-1.000000f,
          };

  // 면을 생성하는 순서
  public static int indices[] =
          {
                  0,3,1,  1,3,2,

                  1,2,6,  1,6,5,

                  0,1,4,  1,5,4,

                  4,5,7,  7,5,6,

                  0,4,7,  0,7,3,

                  3,7,2,  2,7,6,

          };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent intent = getIntent();
    Bundle bundle = intent.getExtras();
    String width = bundle.getString("width");
    String depth = bundle.getString("depth");
    String height = bundle.getString("height");
    try {
      wid1 = Integer.parseInt(width);
      dep1 = Integer.parseInt(depth);
      hei1 = Integer.parseInt(height);
    } catch (NumberFormatException ex) {
      ex.printStackTrace();
    }
    vertices =
            new float[]{
                    wid1 * 0.0005f, hei1 * 0f, -dep1 * 0.0005f,
                    wid1 * 0.0005f, hei1 * 0.001f, -dep1 * 0.0005f,
                    -wid1 * 0.0005f, hei1 * 0.001f, -dep1 * 0.0005f,
                    -wid1 * 0.0005f, hei1 * 0f, -dep1 * 0.0005f,
                    wid1 * 0.0005f, hei1 * 0f, dep1 * 0.0005f,
                    wid1 * 0.0005f, hei1 * 0.001f, dep1 * 0.0005f,
                    -wid1 * 0.0005f, hei1 * 0.001f, dep1 * 0.0005f,
                    -wid1 * 0.0005f, hei1 * 0, dep1 * 0.0005f,
            };
    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);
    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

    // 화면 터치 입력 관리
    tapHelper = new TapHelper(/*context=*/ this);
    surfaceView.setOnTouchListener(tapHelper);

    button = findViewById(R.id.button);

    // 렌더러 설정
    render = new SampleRender(surfaceView, this, getAssets());

    installRequested = false;

    depthSettings.onCreate(this);
    instantPlacementSettings.onCreate(this);
    ImageButton settingsButton = findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            PopupMenu popup = new PopupMenu(HelloArActivity.this, v);
            popup.setOnMenuItemClickListener(HelloArActivity.this::settingsMenuClick);
            popup.inflate(R.menu.settings_menu);
            popup.show();
          }
        });
  }

  /** depth setting을 사용하기 위한 설정버튼 */
  protected boolean settingsMenuClick(MenuItem item) {
    if (item.getItemId() == R.id.depth_settings) {
      launchDepthSettingsMenuDialog();
      return true;
    } else if (item.getItemId() == R.id.instant_placement_settings) {
      launchInstantPlacementSettingsMenuDialog();
      return true;
    }
    return false;
  }

  @Override
  protected void onDestroy() {
    if (session != null) {
      session.close();
      session = null;
    }

    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
          case INSTALL_REQUESTED:
            installRequested = true;
            return;
          case INSTALLED:
            break;
        }

        // ARCore를 사용하기 위한 카메라 권한 요청
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        // 세션 생성
        session = new Session(/* context= */ this);
      } catch (UnavailableArcoreNotInstalledException
          | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (UnavailableDeviceNotCompatibleException e) {
        message = "This device does not support AR";
        exception = e;
      } catch (Exception e) {
        message = "Failed to create AR session";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }
    }

    // onPause의 순서에 주의
    try {
      configureSession();
      session.resume();
    } catch (CameraNotAvailableException e) {
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      session = null;
      return;
    }



    surfaceView.onResume();
    displayRotationHelper.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      //  순서에 주의, GLSurfaceView는 세션을 쿼리하지 않도록 먼저 일시 중지된다. GLSurfaceView 전에
      //  세션이 일시 중지된 경우에도 GLSurfaceView는 session.update()를 호출하여
      //  SessionPauseedException을 받을 수 있다.
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      // Activity가 종료되니 Snackbar 대신 Toast를 사용
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
          .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // "Do not ask again"을 선택해 사용 권한 거부
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  /** GLsurface가 만들어질 때 렌더러의 동작과 DrawFrame에서 펠요한 선언, shader와 3d 모델을 읽어오며
   * 관련 IO Exception들을 처리한다.
   * */
  @Override
  public void onSurfaceCreated(SampleRender render) {
    try {
      planeRenderer = new PlaneRenderer(render);
      backgroundRenderer = new BackgroundRenderer(render);
      virtualSceneFramebuffer = new Framebuffer(render, /*width=*/ 1, /*height=*/ 1);

      cubemapFilter =
          new SpecularCubemapFilter(
              render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES);
      // assets/models/dfg.raw를 사용해 조명 인식
      dfgTexture =
          new Texture(
              render,
              Texture.Target.TEXTURE_2D,
              Texture.WrapMode.CLAMP_TO_EDGE,
              /*useMipmaps=*/ false);
      final int dfgResolution = 64;
      final int dfgChannels = 2;
      final int halfFloatSize = 2;

      ByteBuffer buffer =
          ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize);
      try (InputStream is = getAssets().open("models/dfg.raw")) {
        is.read(buffer.array());
      }
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId());
      GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture");
      GLES30.glTexImage2D(
          GLES30.GL_TEXTURE_2D,
          /*level=*/ 0,
          GLES30.GL_RG16F,
          /*width=*/ dfgResolution,
          /*height=*/ dfgResolution,
          /*border=*/ 0,
          GLES30.GL_RG,
          GLES30.GL_HALF_FLOAT,
          buffer);
      GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D");



      // 포인트클라우드
      pointCloudShader =
          Shader.createFromAssets(
                  render, "shaders/point_cloud.vert", "shaders/point_cloud.frag", /*defines=*/ null)
              .setVec4(
                  "u_Color", new float[] {31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f})
              .setFloat("u_PointSize", 5.0f);
      pointCloudVertexBuffer =
          new VertexBuffer(render, /*numberOfEntriesPerVertex=*/ 4, /*entries=*/ null);
      final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
      pointCloudMesh =
          new Mesh(
              render, Mesh.PrimitiveMode.POINTS, /*indexBuffer=*/ null, pointCloudVertexBuffers);

      // 모델 겉에 입힐 텍스쳐 assets에서 불러오기
      virtualObjectAlbedoTexture =
          Texture.createFromAsset(
              render,
              "models/gl_color.png",
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.SRGB);
      virtualObjectAlbedoInstantPlacementTexture =
          Texture.createFromAsset(
              render,
              "models/transparent.png",
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.SRGB);
      Texture virtualObjectPbrTexture =
          Texture.createFromAsset(
              render,
              "models/transgrid.png",
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.LINEAR);

      // 모델의 3d 모양 불러오기
      virtualObjectMesh = Mesh.createFromAsset(render, "models/myObj.obj");
      virtualObjectShader =
          Shader.createFromAssets(
                  render,
                  "shaders/environmental_hdr.vert",
                  "shaders/environmental_hdr.frag",
                  /*defines=*/ new HashMap<String, String>() {
                    {
                      put(
                          "NUMBER_OF_MIPMAP_LEVELS",
                          Integer.toString(cubemapFilter.getNumberOfMipmapLevels()));
                    }
                  })
              .setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture)
              .setTexture("u_RoughnessMetallicAmbientOcclusionTexture", virtualObjectPbrTexture)
              .setTexture("u_Cubemap", cubemapFilter.getFilteredCubemapTexture())
              .setTexture("u_DfgTexture", dfgTexture);
    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
    }
  }

  // DisplayRotationHelper에서 받은 회전정보를 바탕으로 GLsurfaceview의 화면 회전
  @Override
  public void onSurfaceChanged(SampleRender render, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    virtualSceneFramebuffer.resize(width, height);
  }

  // GLsurfaceview 생성 후 화면에 그리기
  @Override
  public void onDrawFrame(SampleRender render) {
    if (session == null) {
      return;
    }

    if (!hasSetTextureNames) {
      session.setCameraTextureNames(
          new int[] {backgroundRenderer.getCameraColorTexture().getTextureId()});
      hasSetTextureNames = true;
    }

    // 매 프레임마다 상태 갱신

    // session에다 displayRotationHelper에서 받은 정보를 전달해서 거리에 따라 물체의 크기를 수정시킴
    displayRotationHelper.updateSessionIfNeeded(session);

    // 현재 카메라의 프레임정보를 session에서 받아온다.
    Frame frame;
    try {
      frame = session.update();
    } catch (CameraNotAvailableException e) {
      Log.e(TAG, "Camera not available during onDrawFrame", e);
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      return;
    }
    Camera camera = frame.getCamera();

    // 띄울 카메라 배경의 크기를 depthSettings에 맞춘다.
    try {
      backgroundRenderer.setUseDepthVisualization(
          render, depthSettings.depthColorVisualizationEnabled());
      backgroundRenderer.setUseOcclusion(render, depthSettings.useDepthForOcclusion());
    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
      return;
    }
    // 카메라로 찍은 배경 이미지를 업데이트 하기 위해 매 프레임마다 updateDisplayGeometry가 호출되어야 함.
    backgroundRenderer.updateDisplayGeometry(frame);

    if (camera.getTrackingState() == TrackingState.TRACKING
        && (depthSettings.useDepthForOcclusion()
            || depthSettings.depthColorVisualizationEnabled())) {
      try (Image depthImage = frame.acquireDepthImage16Bits()) {
        backgroundRenderer.updateCameraDepthTexture(depthImage);
      } catch (NotYetAvailableException e) {
        // depth 정보를 아직 사용할 수 없음을 알림. default상태에선 자연스러우니 다른 조치는 취하지 않음
      }
    }

    // 화면터치 인식
    handleTap(frame, camera);

    // 바닥인식중엔 터치 방지
    trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

    // 인식 실패 시 경고문 출력
    String message = null;
    if (camera.getTrackingState() == TrackingState.PAUSED) {
      if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
        message = SEARCHING_PLANE_MESSAGE;
      } else {
        message = TrackingStateHelper.getTrackingFailureReasonString(camera);
      }
    } else if (hasTrackingPlane()) {
      if (wrappedAnchors.isEmpty()) {
        message = WAITING_FOR_TAP_MESSAGE;
      }
    } else {
      message = SEARCHING_PLANE_MESSAGE;
    }
    if (message == null) {
      messageSnackbarHelper.hide(this);
    } else {
      messageSnackbarHelper.showMessage(this, message);
    }

    // 여기서부터 카메라로 찍은 배경 그리기

    if (frame.getTimestamp() != 0) {
      backgroundRenderer.drawBackground(render);
    }

    if (camera.getTrackingState() == TrackingState.PAUSED) {
      return;
    }

    // 여기서부터 평면 그리기
    // 카메라 시야정보 획득
    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);
    camera.getViewMatrix(viewMatrix, 0);

    try (PointCloud pointCloud = frame.acquirePointCloud()) {
      if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
        pointCloudVertexBuffer.set(pointCloud.getPoints());
        lastPointCloudTimestamp = pointCloud.getTimestamp();
      }
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
      pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
      render.draw(pointCloudMesh, pointCloudShader);
    }

    // 인식한 평면 시각화화
   planeRenderer.drawPlanes(
        render,
        session.getAllTrackables(Plane.class),
        camera.getDisplayOrientedPose(),
        projectionMatrix);

    // 여기서부터 물체 그리기

    // 조명 관련 파라미터 갱신
    updateLightEstimation(frame.getLightEstimate(), viewMatrix);

    // 터치로 앵커 시각화
    render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);
    for (WrappedAnchor wrappedAnchor : wrappedAnchors) {
      Anchor anchor = wrappedAnchor.getAnchor();
      Trackable trackable = wrappedAnchor.getTrackable();
      if (anchor.getTrackingState() != TrackingState.TRACKING) {
        continue;
      }

      // 앵커의 방향정보 획득, session.update()가 불릴 때 갱신
      anchor.getPose().toMatrix(modelMatrix, 0);

      Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

      // 섀이더 정보 갱신
      virtualObjectShader.setMat4("u_ModelView", modelViewMatrix);
      virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);

      if (trackable instanceof InstantPlacementPoint
          && ((InstantPlacementPoint) trackable).getTrackingMethod()
              == InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE) {
        virtualObjectShader.setTexture(
            "u_AlbedoTexture", virtualObjectAlbedoInstantPlacementTexture);
      } else {
        virtualObjectShader.setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture);
      }

      render.draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer);
    }

    // 가상물체와 카메라로 찍은 배경 합성
    backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR);
  }

  // 터치 한 번은 일반적으로 프레임 속도에 비해 주파수가 낮기 때문에 프레임당 하나의 터치만 처리
  private void handleTap(Frame frame, Camera camera) {
    MotionEvent tap = tapHelper.poll();
    if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
      List<HitResult> hitResultList;
      if (instantPlacementSettings.isInstantPlacementEnabled()) {
        hitResultList =
            frame.hitTestInstantPlacement(tap.getX(), tap.getY(), APPROXIMATE_DISTANCE_METERS);
      } else {
        hitResultList = frame.hitTest(tap);
      }
      for (HitResult hit : hitResultList) {
        // 만약 plane, Oriented Point, Instant Placement Point가 hit하면, anchor를 생성
        Trackable trackable = hit.getTrackable();
        // 만약 plane이 hit하면, plane polygon내부에 hit했는지 확인
        // DepthPoints는 Config.DepthMode가 AUTOMATIC으로 설정된 경우에만 return
        if ((trackable instanceof Plane
                && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
            || (trackable instanceof Point
                && ((Point) trackable).getOrientationMode()
                    == OrientationMode.ESTIMATED_SURFACE_NORMAL)
            || (trackable instanceof InstantPlacementPoint)
            || (trackable instanceof DepthPoint)) {
          // 생성된 objects의 수를 제한
          // 이를 통해 rendering system and ARCore의 과부화를 방지
          if (wrappedAnchors.size() >= 1) {
            wrappedAnchors.get(0).getAnchor().detach();
            wrappedAnchors.remove(0);
          }

          // Anchor를 추가하면 이 공간에서 그 지점을 추적해야함을 ARCore에 알림
          // 이 Anchor는 Plane에 생성되며, 3D model을 실제 세계와 평면을 비교해 올바른 위치에 배치
          wrappedAnchors.add(new WrappedAnchor(hit.createAnchor(), trackable));
          // Depth API를 지원하는 장치의 경우, depth-based occlusion을 활성화할 것을 제안하는 dialog를 표시
          // 이 dialog는 UI thread에서 생성
          this.runOnUiThread(this::showOcclusionDialogIfNeeded);

          // Hits는 깊이별로 정렬됨
          // Plane, Oriented Point, Instant Placement Point에서 가장 가까운 hit만을 고려
          break;
        }
      }
    }
  }

  /**
   * 뎁스 카메라 기능을 쓸 수 있는 경우 사용하여 정확도를 향상시킬지 물음
   * useDepthForOcclusion()를 호출하여 활성화
   */
  private void showOcclusionDialogIfNeeded() {
    boolean isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
    if (!depthSettings.shouldShowDepthEnableDialog() || !isDepthSupported) {
      return; // dialog를 표시할 필요없음
    }

    // 사용자에게 depth-based occlusion을 사용할지 물음
    new AlertDialog.Builder(this)
        .setTitle(R.string.options_title_with_depth)
        .setMessage(R.string.depth_use_explanation)
        .setPositiveButton(
            R.string.button_text_enable_depth,
            (DialogInterface dialog, int which) -> {
              depthSettings.setUseDepthForOcclusion(true);
            })
        .setNegativeButton(
            R.string.button_text_disable_depth,
            (DialogInterface dialog, int which) -> {
              depthSettings.setUseDepthForOcclusion(false);
            })
        .show();
  }

  private void launchInstantPlacementSettingsMenuDialog() {
    resetSettingsMenuDialogCheckboxes();
    Resources resources = getResources();
    new AlertDialog.Builder(this)
        .setTitle(R.string.options_title_instant_placement)
        .setMultiChoiceItems(
            resources.getStringArray(R.array.instant_placement_options_array),
            instantPlacementSettingsMenuDialogCheckboxes,
            (DialogInterface dialog, int which, boolean isChecked) ->
                instantPlacementSettingsMenuDialogCheckboxes[which] = isChecked)
        .setPositiveButton(
            R.string.done,
            (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
        .setNegativeButton(
            android.R.string.cancel,
            (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
        .show();
  }

  /** Depth-based effects를 쉽게 전환할 수 있도록 사용자에게 checkbox를 표시 */
  private void launchDepthSettingsMenuDialog() {
    // 현재 설정을 체크박스에 보여줌
    resetSettingsMenuDialogCheckboxes();

    // 사용자에세 Dialog를 표시
    Resources resources = getResources();
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      // depth support로 사용자는 visualization options을 선택할 수 있음
      new AlertDialog.Builder(this)
              .setTitle(R.string.options_title_with_depth)
              .setMultiChoiceItems(
                      resources.getStringArray(R.array.depth_options_array),
                      depthSettingsMenuDialogCheckboxes,
                      (DialogInterface dialog, int which, boolean isChecked) ->
                              depthSettingsMenuDialogCheckboxes[which] = isChecked)
              .setPositiveButton(
                      R.string.done,
                      (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
              .setNegativeButton(
                      android.R.string.cancel,
                      (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
              .show();
    } else {
      // depth support없이 setting을 사용할 수 없음
      new AlertDialog.Builder(this)
          .setTitle(R.string.options_title_without_depth)
          .setPositiveButton(
              R.string.done,
              (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
          .show();
    }
  }

  private void applySettingsMenuDialogCheckboxes() {
    depthSettings.setUseDepthForOcclusion(depthSettingsMenuDialogCheckboxes[0]);
    depthSettings.setDepthColorVisualizationEnabled(depthSettingsMenuDialogCheckboxes[1]);
    instantPlacementSettings.setInstantPlacementEnabled(
        instantPlacementSettingsMenuDialogCheckboxes[0]);
    configureSession();
  }

  private void resetSettingsMenuDialogCheckboxes() {
    depthSettingsMenuDialogCheckboxes[0] = depthSettings.useDepthForOcclusion();
    depthSettingsMenuDialogCheckboxes[1] = depthSettings.depthColorVisualizationEnabled();
    instantPlacementSettingsMenuDialogCheckboxes[0] =
        instantPlacementSettings.isInstantPlacementEnabled();
  }

  /** 최소 한 개의 plane을 탐지하면 표시 */
  private boolean hasTrackingPlane() {
    for (Plane plane : session.getAllTrackables(Plane.class)) {
      if (plane.getTrackingState() == TrackingState.TRACKING) {
        return true;
      }
    }
    return false;
  }

  /** 현재 frame의 light estimation을 기반으로 상태 갱신 */
  private void updateLightEstimation(LightEstimate lightEstimate, float[] viewMatrix) {
    if (lightEstimate.getState() != LightEstimate.State.VALID) {
      virtualObjectShader.setBool("u_LightEstimateIsValid", false);
      return;
    }
    virtualObjectShader.setBool("u_LightEstimateIsValid", true);

    Matrix.invertM(viewInverseMatrix, 0, viewMatrix, 0);
    virtualObjectShader.setMat4("u_ViewInverse", viewInverseMatrix);

    updateMainLight(
        lightEstimate.getEnvironmentalHdrMainLightDirection(),
        lightEstimate.getEnvironmentalHdrMainLightIntensity(),
        viewMatrix);
    updateSphericalHarmonicsCoefficients(
        lightEstimate.getEnvironmentalHdrAmbientSphericalHarmonics());
    cubemapFilter.update(lightEstimate.acquireEnvironmentalHdrCubeMap());
  }

  private void updateMainLight(float[] direction, float[] intensity, float[] viewMatrix) {
    // view space로 전환하려면 vec4에서 0.0을 final component로 하는 방향이 필요
    worldLightDirection[0] = direction[0];
    worldLightDirection[1] = direction[1];
    worldLightDirection[2] = direction[2];
    Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, worldLightDirection, 0);
    virtualObjectShader.setVec4("u_ViewLightDirection", viewLightDirection);
    virtualObjectShader.setVec3("u_LightIntensity", intensity);
  }

  private void updateSphericalHarmonicsCoefficients(float[] coefficients) {
    // shader에 전달하기 전에 spherical harmonics coefficients를 먼저 곱함
    // sphericalHarmonicFactors는 아래의 3가지 terms에서 파생:
    //
    // 1. The normalized spherical harmonics basis functions (y_lm)
    //
    // 2. The lambertian diffuse BRDF factor (1/pi)
    //
    // 3. A <cos> convolution. 이는 shader(environmental_hdr.frag)가 예상하는
    // 주어진 평면normal에 대해 결과 함수가 반구를 통해 들어오는 모든 빛의 조도를 출력
    //
    // 자세한 내용은 아래 주소를 참조:
    // https://google.github.io/filament/Filament.html#annex/sphericalharmonics

    if (coefficients.length != 9 * 3) {
      throw new IllegalArgumentException(
          "The given coefficients array must be of length 27 (3 components per 9 coefficients");
    }

    // 각각의 factor를 각각의 coefficient의 모든component에 적용
    for (int i = 0; i < 9 * 3; ++i) {
      sphericalHarmonicsCoefficients[i] = coefficients[i] * sphericalHarmonicFactors[i / 3];
    }
    virtualObjectShader.setVec3Array(
        "u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients);
  }

  /** Feature settings로 session구성 */
  private void configureSession() {
    Config config = session.getConfig();
    config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      config.setDepthMode(Config.DepthMode.AUTOMATIC);
    } else {
      config.setDepthMode(Config.DepthMode.DISABLED);
    }
    if (instantPlacementSettings.isInstantPlacementEnabled()) {
      config.setInstantPlacementMode(InstantPlacementMode.LOCAL_Y_UP);
    } else {
      config.setInstantPlacementMode(InstantPlacementMode.DISABLED);
    }
    session.configure(config);
  }
}

/**
 * Anchor에 부착된 trackable위치에 연결
 * Anchor가 원래 {@link InstantPlacementPoint}에 연결되었는지 여부를 확인
 */
class WrappedAnchor {
  private Anchor anchor;
  private Trackable trackable;

  public WrappedAnchor(Anchor anchor, Trackable trackable) {
    this.anchor = anchor;
    this.trackable = trackable;
  }

  public Anchor getAnchor() {
    return anchor;
  }

  public Trackable getTrackable() {
    return trackable;
  }
}
