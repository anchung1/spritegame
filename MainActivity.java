package com.example.spritegame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.andengine.engine.camera.BoundCamera;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl.IAnalogOnScreenControlListener;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.IEntity;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnAreaTouchListener;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.ITouchArea;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.sprite.TiledSprite;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.input.sensor.acceleration.IAccelerationListener;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.atlas.buildable.builder.ITextureAtlasBuilder.TextureAtlasBuilderException;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.color.Color;
import org.andengine.util.debug.Debug;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;

import android.support.v7.app.ActionBarActivity;
import android.opengl.GLES20;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends  SimpleBaseGameActivity implements IAccelerationListener, IUpdateHandler, ContactListener	{
	
	static final int CAMERA_WIDTH = 1280;
	static final int CAMERA_HEIGHT = 960;
	
	static final int SCENE_WIDTH = 1600;
	static final int SCENE_HEIGHT = 1200;

	private static final String TAG = "game";
	private static final boolean D = true;
	private static final String MISSILE = "missile";

	static final int screenMidX = CAMERA_WIDTH/2;
	static final int screenMidY = CAMERA_HEIGHT/2;
	
	private BuildableBitmapTextureAtlas mBitmapTextureAtlas;
	private TiledTextureRegion mTextureRegionSpaceship;
	private ITextureRegion mTextureRegionControlButton;
	private ITextureRegion mTextureRegionBackground;
	private ITextureRegion mTextureRegionControlKnob;
	private ITextureRegion mTextureRegionMissile;
	
	private TiledSprite mSpaceship;
	private Scene mScene;
	private Background mBackground;
	private PhysicsWorld mPhysicsWorld;
	private Body mSpaceshipBody;
	
	private float mGravityX;
	private float mGravityY;
	
	private Sprite mLeftButton;
	private Sprite mRightButton;
	
	private float thrustersTimeElapsed = 0;
	private boolean rotateTouched = false;
	private float rotateLastX = 0;
	
	private double lastRotationalAngle = 0;
	private BoundCamera mCamera;
	private Angle mAngle;
	
	private ArrayList<UserData> mActiveData;
	
	float[] mShipNoseCoord;
	private int cleanupCount=0;
	
	
	@Override
	public EngineOptions onCreateEngineOptions() {
		// TODO Auto-generated method stub
		mCamera = new BoundCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		mCamera.setBounds(0, 0, SCENE_WIDTH, SCENE_HEIGHT);
		mCamera.setCenter(SCENE_WIDTH/2, SCENE_HEIGHT/2);
		mCamera.setBoundsEnabled(true);
		
		final EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera);
		engineOptions.getTouchOptions().setNeedsMultiTouch(true);
		
		return engineOptions;
/*		
		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, 
				new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), mCamera);
*/	
	}


	@Override
	protected void onCreateResources() {
		// TODO Auto-generated method stub
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
		
		mBitmapTextureAtlas = new BuildableBitmapTextureAtlas(getTextureManager(), 2048, 1024, TextureOptions.NEAREST);
		mTextureRegionSpaceship = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBitmapTextureAtlas, this, "spaceships_small.png", 2, 1);
		mTextureRegionControlButton = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mBitmapTextureAtlas, this, "images.png");
		mTextureRegionBackground = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mBitmapTextureAtlas, this, "spacebgnd.jpg");
		mTextureRegionControlKnob = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mBitmapTextureAtlas, this, "onscreen_control_knob.png");
		mTextureRegionMissile = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mBitmapTextureAtlas, this, "dot.png");
		
		//compile the texture atlas
		try {
			mBitmapTextureAtlas.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(0, 0, 0));
			mBitmapTextureAtlas.load();
		} catch (TextureAtlasBuilderException e) {
			Debug.e(e);
		}
		
	}


	@Override
	protected Scene onCreateScene() {
		// TODO Auto-generated method stub

		mAngle = new Angle();
		mScene = new Scene();
		
		//mBackground = new Background(new Color(0.72f, 0.57f, 0.61f));
		//mScene.setBackground(mBackground);
		
		Sprite background = new Sprite((SCENE_WIDTH-CAMERA_WIDTH)/2, 
				(SCENE_HEIGHT-CAMERA_HEIGHT)/2, 
				mTextureRegionBackground, getVertexBufferObjectManager());
		mScene.attachChild(background);
		
		/* Create three lines that will form an arrow pointing to the eye. */
		final Line arrowLineMain = new Line(0, 0, 0, 0, 3, this.getVertexBufferObjectManager());
		final Line arrowLineWingLeft = new Line(0, 0, 0, 0, 3, this.getVertexBufferObjectManager());
		final Line arrowLineWingRight = new Line(0, 0, 0, 0, 3, this.getVertexBufferObjectManager());

		arrowLineMain.setColor(1, 0, 0);
		arrowLineWingLeft.setColor(1, 0, 0);
		arrowLineWingRight.setColor(1, 0, 0);
				
		
		mSpaceship = new TiledSprite(0, 0, mTextureRegionSpaceship, getVertexBufferObjectManager()) {
			@Override
			protected void onManagedUpdate(final float pSecondsElapsed) {
				super.onManagedUpdate(pSecondsElapsed);
				
				final float[] eyeCoordinates = this.convertLocalToSceneCoordinates(75, -10);
				final float eyeX = eyeCoordinates[VERTEX_INDEX_X];
				final float eyeY = eyeCoordinates[VERTEX_INDEX_Y];

				arrowLineMain.setPosition(eyeX, eyeY, eyeX, eyeY - 50);
				arrowLineWingLeft.setPosition(eyeX, eyeY, eyeX - 10, eyeY - 10);
				arrowLineWingRight.setPosition(eyeX, eyeY, eyeX + 10, eyeY - 10);
				//mShipNoseCoord = this.convertLocalToSceneCoordinates(75, 11);

			}			
		};
		mSpaceship.setCurrentTileIndex(0);
		background.attachChild(mSpaceship);
		background.attachChild(arrowLineMain);
		background.attachChild(arrowLineWingLeft);
		background.attachChild(arrowLineWingRight);

		

		
		
		final AnalogOnScreenControl rotationalControl = new AnalogOnScreenControl(0f, CAMERA_HEIGHT-mTextureRegionControlButton.getHeight(), mCamera, 
				mTextureRegionControlButton, mTextureRegionControlKnob, 0.1f, getVertexBufferObjectManager(), new IAnalogOnScreenControlListener() {

					@Override
					public void onControlChange(
							BaseOnScreenControl pBaseOnScreenControl,
							float pValueX, float pValueY) {
						// TODO Auto-generated method stub
						double angleInDeg = Math.atan2(pValueX, -pValueY) * 180/Math.PI;
						
						if (lastRotationalAngle == angleInDeg) {
							mSpaceshipBody.setAngularDamping(0.5f);
						}

						double angle = mAngle.normalizeAngle(angleInDeg);
						if (angle > 225 && angle < 315) {
							mSpaceshipBody.setAngularDamping(0.0f);
							fireThrusters();
							if (mSpaceshipBody.getAngularVelocity() > -5)
								mSpaceshipBody.applyAngularImpulse(-15f);
							
						} else if (angle > 45 && angle < 135) {
							mSpaceshipBody.setAngularDamping(0.0f);
							fireThrusters();
							if (mSpaceshipBody.getAngularVelocity() < 5)
								mSpaceshipBody.applyAngularImpulse(15f);
							
						} else if (angle > 315 || angle < 45) {
							thrust();
						} else {
							noThrust();
						}
						
						lastRotationalAngle = angleInDeg;

					}

					@Override
					public void onControlClick(
							AnalogOnScreenControl pAnalogOnScreenControl) {
						// TODO Auto-generated method stub
						printLog("on click control");
					}
			
		});
		rotationalControl.getControlBase().setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		rotationalControl.getControlBase().setAlpha(0.5f);
		mScene.setChildScene(rotationalControl);

		final AnalogOnScreenControl thrustControl = new AnalogOnScreenControl(CAMERA_WIDTH-mTextureRegionControlButton.getWidth(),
					CAMERA_HEIGHT - mTextureRegionControlButton.getHeight(),
					mCamera,
					mTextureRegionControlButton, mTextureRegionControlKnob, 0.1f, getVertexBufferObjectManager(), 
					new IAnalogOnScreenControlListener() {
						
						@Override
						public void onControlChange(BaseOnScreenControl pBaseOnScreenControl,
								float pValueX, float pValueY) {
							// TODO Auto-generated method stub
							double angleInDeg = Math.atan2(pValueX, -pValueY) * 180/Math.PI;
							double angle = mAngle.normalizeAngle(angleInDeg);
							if (angle > 315 || angle < 45 ) {
								
								/*
								SetTransform does exactly that. It moves a Box2D body to a Vector2(x,y) coordinate with a rotation factor as well. 
								So, pBody.SetTransform(new Vector2(1,2),0) will move pBody to World position 1,2 with rotation 0. 
								Remember that Box2D world coordinates are not the same as scene coordinates. 
								In standard AndEngine config, "1" Box2D World unit = "32" Scene units.

							    Any Sprite, Rectangle, or Text attached to the Box2D body via a PhysicsConnector will move with it.
							    PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT
								*/
								mSpaceshipBody.setTransform(3f, 3f, (float)mSpaceshipBody.getAngle());
								//mSpaceship.setPosition(CAMERA_WIDTH, CAMERA_HEIGHT);
								//fireMissile();
								//thrust();
							} else {
								//printLog("angle: " + Double.valueOf(angle).toString());
								//noThrust();
							}
							
							
						}
						
						@Override
						public void onControlClick(AnalogOnScreenControl pAnalogOnScreenControl) {
							// TODO Auto-generated method stub
							printLog("On click thrust");
							
						}
					});
		rotationalControl.setChildScene(thrustControl);
		thrustControl.getControlBase().setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		thrustControl.getControlBase().setAlpha(0.5f);

		
		mPhysicsWorld = new PhysicsWorld(new Vector2(0,0), false);
		final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
		final Rectangle ground = new Rectangle(0, SCENE_HEIGHT - 2, SCENE_WIDTH, 2, vertexBufferObjectManager);
		final Rectangle roof = new Rectangle(0, 0, SCENE_WIDTH, 2, vertexBufferObjectManager);
		final Rectangle left = new Rectangle(0, 0, 2, SCENE_HEIGHT, vertexBufferObjectManager);
		final Rectangle right = new Rectangle(SCENE_WIDTH - 2, 0, 2, SCENE_HEIGHT, vertexBufferObjectManager);
		
		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef).setUserData(new UserData("ground", null));
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, roof, BodyType.StaticBody, wallFixtureDef).setUserData(new UserData("roof", null));
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef).setUserData(new UserData("left", null));
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef).setUserData(new UserData("right", null));
		
		
		mScene.attachChild(ground);
		mScene.attachChild(roof);
		mScene.attachChild(left);
		mScene.attachChild(right);
		
		mSpaceshipBody = PhysicsFactory.createCircleBody(mPhysicsWorld, mSpaceship, BodyType.DynamicBody, PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f));
		mSpaceshipBody.setUserData(new UserData("spaceship", mSpaceship));
		
		
		
		mScene.registerUpdateHandler(mPhysicsWorld);
		mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(mSpaceship, mSpaceshipBody, true, true));
		
		//this is for collision detection
		mPhysicsWorld.setContactListener(this);

		
		//mScene.setOnSceneTouchListener(this);
		//mScene.setOnAreaTouchListener(this);
		mScene.registerUpdateHandler(this);
		
		mActiveData = new ArrayList<UserData>();
		
		return mScene;
	}



	@Override
	public void onResumeGame() {
		super.onResumeGame();
 
		//this.enableAccelerationSensor(this);
	}

	@Override
	public void onPauseGame() {
		super.onPauseGame();

		//this.disableAccelerationSensor();
	}



	
	void printLog(String s) {
		
		if (D) Log.d(TAG, s);
	}


	//
	//ContactListner interfaces start
	//
	

	@Override
	public void beginContact(Contact contact) {
		// TODO Auto-generated method stub
		//printLog("beginContact");
	}


	@Override
	public void endContact(Contact contact) {
		// TODO Auto-generated method stub
		printLog("endContact");
		//return;
				
		Body body1 = contact.getFixtureA().getBody();
		Body body2 = contact.getFixtureB().getBody();
		
		UserData ud1 = (UserData)body1.getUserData();
		UserData ud2 = (UserData)body2.getUserData();
		
		printLog("collision: " + ud1.getName() + " with " + ud2.getName());
		if (ud1.getName().contentEquals("spaceship") ||
			ud2.getName().contentEquals("spaceship")) {
			return;
		}
		
		UserData ud=null;
		if (ud1.getName().contentEquals(MISSILE)) {
			ud = ud1;
		} else if (ud2.getName().contentEquals(MISSILE)) {
			ud = ud2;
		} else {
			return;
		}

/*		
		final PhysicsConnector missilePhysicsConnector = mPhysicsWorld.getPhysicsConnectorManager().findPhysicsConnectorByShape(ud.getSprite());
		mPhysicsWorld.unregisterPhysicsConnector(missilePhysicsConnector);
		mPhysicsWorld.destroyBody(missilePhysicsConnector.getBody());
		mScene.detachChild(ud.getSprite());
*/		
		printLog("adding missile list");
		mActiveData.add(ud);
		
//		return;
		
//		printLog(ud.getName());
//		return;

	}


	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
		// TODO Auto-generated method stub
		//printLog("preSolve");
	}


	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
		// TODO Auto-generated method stub
		//printLog("postSolve");
	}
	
	//
	//ContactListener interfaces end
	//

	@Override
	public void onAccelerationAccuracyChanged(AccelerationData pAccelerationData) {
		// TODO Auto-generated method stub
		
	}

 
	@Override
	public void onAccelerationChanged(AccelerationData pAccelerationData) {
		// TODO Auto-generated method stub
		
		final Vector2 gravity = Vector2Pool.obtain(pAccelerationData.getX(), pAccelerationData.getY());
		this.mPhysicsWorld.setGravity(gravity);
		Vector2Pool.recycle(gravity);
	}
	

	private void fireThrusters() {
		mSpaceship.setCurrentTileIndex(1);
	}
	
	private void noThrust() {
		//printLog("no thrust");
		thrustersTimeElapsed = 0;
		mSpaceship.setCurrentTileIndex(0);
	}
	
	private void thrust() {
		//printLog("thrust");
		
		fireThrusters();
		float angle = mSpaceshipBody.getAngle();
		//sin theta = x/1 where 1 is normalized hypotenuse
		float x_thrust = (float) Math.sin(angle) * 1000;
		float y_thrust = (float) Math.cos(angle) * 1000 * -1;
		
		//printLog("angle: " + Float.valueOf(angle).toString());
		//printLog(Float.valueOf(x_thrust).toString() + "," + Float.valueOf(y_thrust).toString());
		
		final Vector2 impulse = Vector2Pool.obtain(x_thrust, y_thrust);
		
		mSpaceshipBody.applyForceToCenter(impulse);
		mSpaceshipBody.setAngularDamping(0.5f);
		Vector2Pool.recycle(impulse);	
		
		thrustersTimeElapsed = 0;
	}
	
	private void fireMissile() {
/*		
		x = x0 + r * cos(a)
		y = y0 + r * sin(a)

		x0,y0 - center of the circle(Point of rotation)
		a - angle of rotation(canon.getRotation())
		r - radious(distance from center to canon tip)
*/
	
		//the nose of the spaceship is 75,11
		mShipNoseCoord = mSpaceship.convertLocalToSceneCoordinates(75, 0);
		
		float angle = mSpaceshipBody.getAngle();
		//printLog("missile angle " + Float.valueOf(angle).toString());
		float x_thrust = (float) Math.sin(angle) *25;
		float y_thrust = (float) Math.cos(angle) *25 * -1;
		
		Sprite missile;
		if (mAngle.normalizeAngle(angle) <= 90 || mAngle.normalizeAngle(angle) >= 270 ) {
			missile = new Sprite(mShipNoseCoord[Sprite.VERTEX_INDEX_X], mShipNoseCoord[Sprite.VERTEX_INDEX_Y] - mTextureRegionMissile.getHeight(),
					mTextureRegionMissile, getVertexBufferObjectManager());
		} else {
			missile = new Sprite(mShipNoseCoord[Sprite.VERTEX_INDEX_X], mShipNoseCoord[Sprite.VERTEX_INDEX_Y],
					mTextureRegionMissile, getVertexBufferObjectManager());
		}

		missile.setRotation((float)(angle*180/Math.PI));
		final Vector2 impulse = Vector2Pool.obtain(x_thrust, y_thrust);
		
		Body missileBody = PhysicsFactory.createBoxBody(mPhysicsWorld, missile, BodyType.DynamicBody, PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f));
		missileBody.setBullet(true);
		missileBody.setUserData(new UserData(MISSILE, missile));
		
		mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(missile, missileBody, true, true));
		
		missileBody.setLinearVelocity(impulse);
		Vector2Pool.recycle(impulse);
		
		mScene.attachChild(missile);

	}
	
	@Override
	public void onUpdate(float pSecondsElapsed) {
		// TODO Auto-generated method stub
		
		
		if (mActiveData.isEmpty()) {
			return;
		}
	
		//printLog("onUpdate");

		Iterator<UserData> iter = mActiveData.iterator();
		
		while (iter.hasNext()) {
			UserData ud = iter.next();
			final PhysicsConnector missilePhysicsConnector = mPhysicsWorld.getPhysicsConnectorManager().findPhysicsConnectorByShape(ud.getSprite());
			mPhysicsWorld.unregisterPhysicsConnector(missilePhysicsConnector);
			mPhysicsWorld.destroyBody(missilePhysicsConnector.getBody());
			mScene.detachChild(ud.getSprite());
			iter.remove();
			cleanupCount++;
		}

		printLog("cleanup Count: " + Integer.valueOf(cleanupCount).toString());
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

}
