import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import custom.FlyByCamera;


public class Driver extends SimpleApplication implements ActionListener
{
    private Spatial sceneModel;
    private Spatial wallModel;
    private Spatial stairs;
    private Spatial testWallModel;
    private BulletAppState bulletAppState;
    private RigidBodyControl landscape;
    private RigidBodyControl wall;
    private CharacterControl player;
    private Vector3f walkDirection = new Vector3f();
    private boolean
              left = false,
              right = false,
              up = false,
              down = false,
              run = false;
    
    //Temporary vectors used on each frame.
    //They here to avoid instanciating new vectors on each frame.
    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();
    
    private Vector3f xDirection = new Vector3f();
    private Vector3f zDirection = new Vector3f();
    
    private float walkSpeedFactor;
    private float runSpeedFactor;
    private float currentSpeedFactor;
    private float rotationSpeed;
    
    // public:
    
    
    public static void main( String[] args )
    {
        // This is jmonkey's settings class for the simple app.
        // Normal
        AppSettings settings = new AppSettings(true);
        settings.put("Width", 1280);
        settings.put("Height", 720);
        settings.put("Title", "Awesome Maze");
        settings.put("VSync", true);
        // Anti-Aliasing
        settings.put("Samples", 4);
        
        // This is where the program starts and calls the start() method
        //  which runs the simpleInitApp() method.
        Driver driver = new Driver();
        driver.setSettings(settings);   // apply the settings object above to our driver
        driver.setShowSettings(false);  // disable the settings windows from showing
        driver.start();
    }
    
    
    @Override
    public void simpleInitApp()
    {
        // Override default flyByCam
        // We have to special-case the FlyCamAppState because too
        // many custom.SimpleApplication subclasses expect it to exist in
        // simpleInit().  But at least it only gets initialized if
        // the app state is added.
        
        // remove default FlyCamAppState as suggested in javadocs
        stateManager.detach(stateManager.getState(FlyCamAppState.class));
        stateManager.attach(new custom.FlyCamAppState());
        
        if ( stateManager.getState(custom.FlyCamAppState.class) != null )
        {
            flyCam = new custom.FlyByCamera(cam);
            flyCam.setMoveSpeed(1f); // odd to set this here but it did it before
            stateManager.getState(custom.FlyCamAppState.class).setCamera(flyCam);
        }
          
        /* The BulletAppState gives our application access to physics features,
         *  such as collision detection. It is integerated by the jME's jBullet integeration
         *  which is an external physics engine. This piece of code is required in every
         *  application that works with physics.
         */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        /* These are the default run and walk speed factors. We also set the move speed according to
         *  what mode are we on. Additionally, we can adjust the rotation speed of the mouse.
         */
        runSpeedFactor = 1.0f;
        walkSpeedFactor = 0.5f;
        rotationSpeed = 1.2f;
        
        currentSpeedFactor = walkSpeedFactor;
        flyCam.setRotationSpeed(rotationSpeed);
        
        // We re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        setUpKeys();
        setUpLight();
        flyCam.setMoveSpeed(100f);
        
        // We load the scene from the zip file and adjust its size.
        assetManager.registerLocator("town.zip", ZipLocator.class);
        sceneModel = assetManager.loadModel("main.scene");
        sceneModel.setLocalScale(2f);
        sceneModel.setLocalTranslation(1, 1, 1);
        
        // Added test floor model
        wallModel = assetManager.loadModel("models/stonetiles.obj");
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("models/color.png"));
        //mat.setTexture("NormalMap", assetManager.loadTexture("models/normals.png"));
        wallModel.setMaterial(mat);
        //wallModel.setLocalScale(20f);
        wallModel.setLocalScale(new Vector3f(50f, 50f, 50f));
        wallModel.setLocalTranslation(0.0f, 1.0f, -50f);
        wallModel.setLocalRotation(new Quaternion(-1f, 0f, 0f, 1f));
        CollisionShape wallShape = CollisionShapeFactory.createMeshShape(wallModel);
        //wall = new RigidBodyControl(wallShape, 0);
        //wallModel.addControl(wall);
        
        // Added test floor model
        stairs = assetManager.loadModel("models/Stairs2.obj");
        //mat2.setTexture("ColorMap", assetManager.loadTexture("models/color.png"));
        //stairs.setMaterial(mat2);
        //Material mat2 = new Material(assetManager, "models/Stairs2.mtl");
        stairs.setLocalTranslation(1f, 0f, 0f);
        
        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0f);
        sceneModel.addControl(landscape);
        
        // Adding another test floor model
        testWallModel = assetManager.loadModel("models/Block-1x1x2.mesh.xml");
        testWallModel.setLocalScale(new Vector3f(10f, 40f, 30f));
        testWallModel.setLocalTranslation(-50f, 2f, -20f);
        Material testMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        testMat.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg"));
        testWallModel.setMaterial(testMat);
        CollisionShape testWallShape = CollisionShapeFactory.createBoxShape(testWallModel);
        wall = new RigidBodyControl(testWallShape, 0f);
        testWallModel.addControl(wall);
        
        // We set up collision detection for the player by creating
        // a capsule collision shape and a CharacterControl.
        // The CharacterControl offers extra settings for
        // size, stepheight, jumping, falling, and gravity.
        // We also put the player in its starting position.
        
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        player = new CharacterControl(capsuleShape, 0.05f);
        player.setJumpSpeed(20);
        player.setFallSpeed(30);
        player.setGravity(30);
        player.setPhysicsLocation(new Vector3f(0, 10, 0));


        /* We attach the scene and the player to the rootnode and the physics space,
         *  to make them appear in the game world. Note that you have to add all kinds of physics
         *  control to the physics space for them to have physics.
         */
        rootNode.attachChild(sceneModel);
        rootNode.attachChild(wallModel);
        rootNode.attachChild(stairs);
        rootNode.attachChild(testWallModel);
        bulletAppState.getPhysicsSpace().add(landscape);
        bulletAppState.getPhysicsSpace().add(player);
        bulletAppState.getPhysicsSpace().add(wall);

        /* The debug feature shows us the wireframes of the mesh renderer.
         * This should be enabled for debugging only.
         */
        //bulletAppState.setDebugEnabled(true);
    }
    
    
    /* This is the main event loop--walking happens here.
     * We check in which direction the player is walking by interpreting
     * the camera direction forward (camDir) and to the side (camLeft).
     * The setWalkDirection() command is what lets a physics-controlled player walk.
     * We also make sure here that the camera moves with player.
     */
    @Override
    public void simpleUpdate( float tpf )
    {
        if ( run )
        {
            currentSpeedFactor = runSpeedFactor;
        }
        else
        {
            currentSpeedFactor = walkSpeedFactor;
        }

        /* The up and down movement has been allowed only in the x and z direction because
         * changing all direction would enable to player to jump awkwardly.
         */
        camDir.setX(cam.getDirection().getX() * currentSpeedFactor);
        camDir.setZ(cam.getDirection().getZ() * currentSpeedFactor);
        camDir.setY(0f);
        
        camLeft.set(cam.getLeft()).multLocal(currentSpeedFactor);

        /* The multiple if statements helps us determine which position the character wants to walk
         *  in. The walk direction is calculated based on the boolean values determined by the onAction method.
         */
        walkDirection.set(0, 0, 0);
        if ( left )
        {
            walkDirection.addLocal(camLeft);
        }
        if ( right )
        {
            walkDirection.addLocal(camLeft.negate());
        }
        if ( up )
        {
            walkDirection.addLocal(camDir);
        }
        if ( down )
        {
            walkDirection.addLocal(camDir.negate());
        }
        
        // Sets the player walk direction and resets the cam location to where the player is now.
        player.setWalkDirection(walkDirection);
        cam.setLocation(player.getPhysicsLocation());
    }


    /* These are our custom actions triggered by key presses.
     * We do not walk yet, we just keep track of the direction the user pressed.
     */
    
    /**
     * Called when an input to which this listener is registered to is invoked.
     *
     * @param name      The name of the mapping that was invoked
     * @param isPressed True if the action is "pressed", false otherwise
     * @param tpf       The time per frame value.
     */
    @Override
    public void onAction( String name, boolean isPressed, float tpf )
    {
        // isPressed: no preference
        if ( name.equals("Left") )
        {
            left = isPressed;
        }
        if ( name.equals("Right") )
        {
            right = isPressed;
        }
        if ( name.equals("Up") )
        {
            up = isPressed;
        }
        if ( name.equals("Down") )
        {
            down = isPressed;
        }
        if ( name.equals("Jump") )
        {
            // Jump functionality not determined yet
            //if (isPressed) { player.jump(); }
        }
        if ( name.equals("Run") )
        {
            run = isPressed;
        }
        
        // isPressed: true
        if ( name.equals("Pause") && isPressed )
        {
            flipCamEnable();
        }
    }
    
    
    // private:
    
    
    private void setUpLight()
    {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);
        
        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
    }
    
    
    /* We over-write some navigational key mappings here, so we can
     *  add physics-controlled walking and jumping:
     */
    private void setUpKeys()
    {
        // TODO
        // this looks like a terrible way to map keys
        // we should create a helper function for it
        
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Run", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Pause", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addListener(this, "Left", "Right", "Up", "Down", "Jump", "Run", "Pause");
    }
    
    private void flipCamEnable()
    {
        boolean value = flyCam.isEnabled();
        inputManager.setCursorVisible(value);
        flyCam.setEnabled(!value);
    }
    
}
