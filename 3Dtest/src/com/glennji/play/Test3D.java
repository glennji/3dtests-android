package com.glennji.play;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.threed.jpct.Camera;
import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.GenericVertexController;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Logger;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;

/**
 * A simple demo. This shows more how to use jPCT-AE than it shows how to write
 * a proper application for Android, because i have no idea how to do this. This
 * thing is more or less a hack to get you started...
 *
 * @author EgonOlsen
 *
 */
public class Test3D extends Activity {

	private GLSurfaceView mGLView;
	private MyRenderer renderer = null;
	private FrameBuffer fb = null;
	private World world = null;
	private int move = 0;
	private float turn = 0;
	private boolean paused = false;

	private float touchTurn = 0;
	private float touchTurnUp = 0;

	private float xpos = -1;
	private float ypos = -1;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mGLView = new GLSurfaceView(getApplication());

		mGLView.setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
			public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
				// Ensure that we get a 16bit framebuffer. Otherwise, we'll fall
				// back to Pixelflinger on some device (read: Samsung I7500)
				int[] attributes = new int[] { EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE };
				EGLConfig[] configs = new EGLConfig[1];
				int[] result = new int[1];
				egl.eglChooseConfig(display, attributes, configs, 1, result);
				return configs[0];
			}
		});

		renderer = new MyRenderer();
		mGLView.setRenderer(renderer);
		setContentView(mGLView);
	}

	@Override
	protected void onPause() {
		paused = true;
		super.onPause();
		mGLView.onPause();
	}

	@Override
	protected void onResume() {
		paused = false;
		super.onResume();
		mGLView.onResume();
	}

	protected void onStop() {
		renderer.stop();
		super.onStop();
	}

	public boolean onTouchEvent(MotionEvent me) {

		if (me.getAction() == MotionEvent.ACTION_DOWN) {
			xpos = me.getX();
			ypos = me.getY();
			return true;
		}

		if (me.getAction() == MotionEvent.ACTION_UP) {
			xpos = -1;
			ypos = -1;
			touchTurn = 0;
			touchTurnUp = 0;
			return true;
		}

		if (me.getAction() == MotionEvent.ACTION_MOVE) {
			float xd = me.getX() - xpos;
			float yd = me.getY() - ypos;

			xpos = me.getX();
			ypos = me.getY();

			touchTurn = xd / 100f;
			touchTurnUp = yd / 100f;
			return true;
		}
		return super.onTouchEvent(me);
	}

	public boolean onKeyDown(int keyCode, KeyEvent msg) {

		if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			move = 2;
			return true;
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			move = -2;
			return true;
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			turn = 0.05f;
			return true;
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			turn = -0.05f;
			return true;
		}

		return super.onKeyDown(keyCode, msg);
	}

	public boolean onKeyUp(int keyCode, KeyEvent msg) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			move = 0;
			return true;
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			move = 0;
			return true;
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			turn = 0;
			return true;
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			turn = 0;
			return true;
		}

		return super.onKeyUp(keyCode, msg);
	}

	protected boolean isFullscreenOpaque() {
		return true;
	}

	class MyRenderer implements GLSurfaceView.Renderer {

		private Object3D plane = null;
		private Object3D tree2 = null;
		private Object3D tree1 = null;
		private Object3D grass = null;
		private Texture font = null;

		private int fps = 0;
		private int lfps = 0;

		private long time = System.currentTimeMillis();

		private Light sun = null;
		private Object3D rock = null;

		private boolean stop = false;

		private float ind;

		private boolean deSer = true;

		public MyRenderer() {
			Config.maxPolysVisible = 5000;
			Config.farPlane = 1500;
		}

		public void stop() {
			stop = true;
			if (fb != null) {
				fb.dispose();
				fb = null;
			}
		}

		public void onSurfaceChanged(GL10 gl, int w, int h) {
			if (fb != null) {
				fb.dispose();
			}
			fb = new FrameBuffer(gl, w, h);
		}

		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			TextureManager.getInstance().flush();
			world = new World();
			Resources res = getResources();

			TextureManager tm = TextureManager.getInstance();
			Texture grass2 = new Texture(res.openRawResource(R.raw.grassy));
			Texture leaves = new Texture(res.openRawResource(R.raw.tree2y));
			Texture leaves2 = new Texture(res.openRawResource(R.raw.tree3y));
			Texture rocky = new Texture(res.openRawResource(R.raw.rocky));

			Texture planetex = new Texture(res.openRawResource(R.raw.planetex));

			font = new Texture(res.openRawResource(R.raw.numbers));

			tm.addTexture("grass2", grass2);
			tm.addTexture("leaves", leaves);
			tm.addTexture("leaves2", leaves2);
			tm.addTexture("rock", rocky);
			tm.addTexture("grassy", planetex);

			if (!deSer) {
				// Use the normal loaders...
				plane = Primitives.getPlane(20, 30);
				grass = Loader.load3DS(res.openRawResource(R.raw.grass), 5)[0];
				rock = Loader.load3DS(res.openRawResource(R.raw.rock), 15f)[0];
				tree1 = Loader.load3DS(res.openRawResource(R.raw.tree2), 5)[0];
				tree2 = Loader.load3DS(res.openRawResource(R.raw.tree3), 5)[0];

				plane.setTexture("grassy");
				rock.setTexture("rock");
				grass.setTexture("grass2");
				tree1.setTexture("leaves");
				tree2.setTexture("leaves2");

				plane.getMesh().setVertexController(new Mod(), false);
				plane.getMesh().applyVertexController();
				plane.getMesh().removeVertexController();
			} else {
				// Load the serialized version instead...
				plane = Loader.loadSerializedObject(res.openRawResource(R.raw.serplane));
				rock = Loader.loadSerializedObject(res.openRawResource(R.raw.serrock));
				tree1 = Loader.loadSerializedObject(res.openRawResource(R.raw.sertree1));
				tree2 = Loader.loadSerializedObject(res.openRawResource(R.raw.sertree2));
				grass = Loader.loadSerializedObject(res.openRawResource(R.raw.sergrass));
			}

			grass.translate(-45, -17, -50);
			grass.rotateZ((float) Math.PI);
			rock.translate(0, 0, -90);
			rock.rotateX(-(float) Math.PI / 2);
			tree1.translate(-50, -92, -50);
			tree1.rotateZ((float) Math.PI);
			tree2.translate(60, -95, 10);
			tree2.rotateZ((float) Math.PI);
			plane.rotateX((float) Math.PI / 2f);

			plane.setName("plane");
			tree1.setName("tree1");
			tree2.setName("tree2");
			grass.setName("grass");
			rock.setName("rock");

			world.addObject(plane);
			world.addObject(tree1);
			world.addObject(tree2);
			world.addObject(grass);
			world.addObject(rock);

			RGBColor dark = new RGBColor(100, 100, 100);

			grass.setTransparency(10);
			tree1.setTransparency(0);
			tree2.setTransparency(0);

			tree1.setAdditionalColor(dark);
			tree2.setAdditionalColor(dark);
			grass.setAdditionalColor(dark);

			world.setAmbientLight(20, 20, 20);
			world.buildAllObjects();

			sun = new Light(world);

			Camera cam = world.getCamera();
			cam.moveCamera(Camera.CAMERA_MOVEOUT, 250);
			cam.moveCamera(Camera.CAMERA_MOVEUP, 100);
			cam.lookAt(plane.getTransformedCenter());

			cam.setFOV(1.5f);
			sun.setIntensity(250, 250, 250);

			SimpleVector sv = new SimpleVector();
			sv.set(plane.getTransformedCenter());
			sv.y -= 300;
			sv.x -= 100;
			sv.z += 200;
			sun.setPosition(sv);
		}

		public void onDrawFrame(GL10 gl) {

			try {
				if (!stop) {
					if (paused) {
						Thread.sleep(500);
					} else {
						Camera cam = world.getCamera();
						if (turn != 0) {
							world.getCamera().rotateY(-turn);
						}

						if (touchTurn != 0) {
							world.getCamera().rotateY(touchTurn);
							touchTurn = 0;
						}

						if (touchTurnUp != 0) {
							world.getCamera().rotateX(touchTurnUp);
							touchTurnUp = 0;
						}

						if (move != 0) {
							world.getCamera().moveCamera(cam.getDirection(), move);
						}

						fb.clear();
						world.renderScene(fb);
						world.draw(fb);
						blitNumber(lfps, 5, 5);

						fb.display();

						sun.rotate(new SimpleVector(0, 0.05f, 0), plane.getTransformedCenter());

						if (System.currentTimeMillis() - time >= 1000) {
							lfps = (fps + lfps) >> 1;
							fps = 0;
							time = System.currentTimeMillis();
						}
						fps++;
						ind += 0.02f;
						if (ind > 1) {
							ind -= 1;
						}
					}
				} else {
					if (fb != null) {
						fb.dispose();
						fb = null;
					}
				}
			} catch (Exception e) {
				Logger.log("Drawing thread terminated!", Logger.MESSAGE);
			}
		}

		private class Mod extends GenericVertexController {
			private static final long serialVersionUID = 1L;

			public void apply() {
				SimpleVector[] s = getSourceMesh();
				SimpleVector[] d = getDestinationMesh();
				for (int i = 0; i < s.length; i++) {
					d[i].z = s[i].z - (10f * (FloatMath.sin(s[i].x / 50f) + FloatMath.cos(s[i].y / 50f)));
					d[i].x = s[i].x;
					d[i].y = s[i].y;
				}
			}
		}

		private void blitNumber(int number, int x, int y) {
			if (font != null) {
				String sNum = Integer.toString(number);
				for (int i = 0; i < sNum.length(); i++) {
					char cNum = sNum.charAt(i);
					int iNum = cNum - 48;
					fb.blit(font, iNum * 5, 0, x, y, 5, 9, FrameBuffer.TRANSPARENT_BLITTING);
					x += 5;
				}
			}
		}
	}
}