package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import static com.jme3.math.Vector3f.ZERO;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 *
 * @author Matías Bonino
 */
public class Main extends SimpleApplication {

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    private BulletAppState bulletAppState;

    /**
     * Prepare physics.
     */
    private float gravitational_constant = 5f; //3.9f * (float) Math.pow(10, -6);
    private float standard_repulsion_parameter = 10f; //(float) Math.pow(10, 10);

    private float start_radius_of_repulsive_zone = 30f; //(float) Math.pow(10, 8);
    private float singular_radius = 50f; //start_radius_of_repulsive_zone + 3.16f * (float) Math.pow(10, 6);

    private float cannonball_speed = 2f;
    private float cannonball_mass = 1f;
    private float attractor_mass = 3f;

    /**
     * Prepare HUD.
     */
    BitmapText hudText; // provisional
    BitmapText ErrorHudText;
    private long t = 0;
    private int quantity = 0;
    private String region;

    /**
     * Prepare bodies.
     */
    private RigidBodyControl ball_phy;
    private static Sphere sphere;
    private static Sphere bigSphere;

    /**
     * Prepare links.
     */
    private Node linkables;
    private Geometry mark;
    private String link_data;

    /**
     * Prepare error messages.
     */
    private String error_data;

    /**
     * Prepare soundtrack.
     */
    private AudioNode audio_nature;
    private Geometry player;

    static {
        /**
         * Initialize the cannon ball geometry.
         */
        sphere = new Sphere(32, 32, 0.4f, true, false);
        sphere.setTextureMode(TextureMode.Projected);
        bigSphere = new Sphere(32, 32, 1f, true, false);
        bigSphere.setTextureMode(TextureMode.Projected);
    }

    @Override
    public void simpleInitApp() {

        /**
         * Set up Physics Game.
         */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().setGravity(ZERO);

        // Links.
        initLinks();

        // Attractors.
        initAttractors();

        // A blue sphere to mark the hit.
        initMark();

        // A centred plus sign to help the player aim.
        initCrossHairs();

        // Soundtrack.
        initAudio();

        // Camera speed.
        flyCam.setMoveSpeed(30 * speed);

        // Mouse actions.
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "shoot");
        inputManager.addMapping("mark", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(actionListener, "mark");

        // UniVersor center mark.
        Box b = new Box(1, 1, 1);
        Geometry b_geom = new Geometry("Box", b);
        Material b_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        b_mat.setColor("Color", ColorRGBA.Blue);
        b_geom.setMaterial(b_mat);
        rootNode.attachChild(b_geom);
    }

    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("shoot") && !keyPressed) {
                makeCannonBall();
            }
            if (name.equals("mark") && !keyPressed) {
                try {
                    makeLink();
                } catch (NullPointerException ex) {
                    error_data = "\n\n\nError: you cannot mark the void!!! XD\n\n" + ex;
                    System.out.println(error_data);
                }
            }
        }
    };

    private void initAttractors() {

        for (int i = 0; i < 5; i++) {

            Geometry ball_geo = new Geometry("Attractor", bigSphere);
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Gray);
            ball_geo.setMaterial(mat);
            rootNode.attachChild(ball_geo);
            ball_geo.setLocalTranslation(Vector3f.UNIT_X.mult(12 * i - 30));

            ball_phy = new Attractor(attractor_mass);
            ball_geo.addControl(ball_phy);
            bulletAppState.getPhysicsSpace().add(ball_phy);
            ball_phy.setLinearVelocity(
                    Vector3f.UNIT_Y.mult(5 * (float) (Math.random() - 0.5))
                            .add(Vector3f.UNIT_Z.mult(5 * (float) (Math.random() - 0.5)))
            );

            linkables.attachChild(ball_geo);
        }
    }

    private void initLinks() {

        linkables = new Node("Linkables");
        rootNode.attachChild(linkables);
    }

    private void initMark() {
        Sphere sphere = new Sphere(30, 30, 0.2f);
        mark = new Geometry("BOOM!", sphere);
        Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mark_mat.setColor("Color", ColorRGBA.Blue);
        mark.setMaterial(mark_mat);
    }

    private void initCrossHairs() {
        setDisplayStatView(false);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - ch.getLineWidth() / 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }

    private void initAudio() {

        audio_nature = new AudioNode(assetManager, "Sounds/The_Kyoto_Connection_-_11_-_Voyage_III_-_The_Space_Between_Us.ogg", DataType.Stream);
        audio_nature.setLooping(true);
        audio_nature.setPositional(false);
        audio_nature.setVolume(2);
        rootNode.attachChild(audio_nature);
        audio_nature.play();
    }

    public void makeCannonBall() {

        Geometry ball_geo = new Geometry("Cannon ball", sphere);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        ball_geo.setMaterial(mat);
        rootNode.attachChild(ball_geo);

        ball_geo.setLocalTranslation(cam.getLocation());
        ball_phy = new RigidBodyControl(cannonball_mass);

        ball_geo.addControl(ball_phy);
        bulletAppState.getPhysicsSpace().add(ball_phy);

        quantity++;

        ball_phy.setLinearVelocity(cam.getDirection().mult(cannonball_speed));
    }

    private void contactPoint() {

    }

    private void makeLink() {    // It is not ready. Please work.

        CollisionResults results = new CollisionResults();
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());

        // Collect intersections between Ray and Linkables in results list.
        linkables.collideWith(ray, results);

        // For each hit, we know distance, impact point, name of geometry.
        for (int i = 0; i < results.size(); i++) {
            float dist = results.getCollision(i).getDistance();
            Vector3f pt = results.getCollision(i).getContactPoint();
            String hit = results.getCollision(i).getGeometry().getName();
        }

        // We mark the hit object.
        if (results.size() > 0) {
            // The closest collision point is what was truly hit:
            CollisionResult closest = results.getClosestCollision();
            // We mark the hit with a dot.
            mark.setLocalTranslation(closest.getContactPoint());
            rootNode.attachChild(mark);
        } else {
            // No hits? Then remove the mark.
            rootNode.detachChild(mark);
        }

        //Point2PointJoint link = new Point2PointJoint(, closest, cam.getLocation());
        //link_data = results.size() + " collisions.";
    }

    @Override
    public void simpleUpdate(float tpf) {

        // Gravitation.
        for (PhysicsRigidBody prb : bulletAppState.getPhysicsSpace().getRigidBodyList()) {

            if (prb instanceof Attractor) {

                for (PhysicsRigidBody attracted : bulletAppState.getPhysicsSpace().getRigidBodyList()) {

                    if (prb != attracted) {

                        attracted.applyCentralForce(
                                (prb.getPhysicsLocation().subtract(attracted.getPhysicsLocation()))
                                        .normalize().mult(gravitational_constant * prb.getMass() * attracted.getMass()
                                                / attracted.getPhysicsLocation().distanceSquared(prb.getPhysicsLocation())));

                    }
                }
            }
        }

        // Repulsion.
        for (PhysicsRigidBody body : bulletAppState.getPhysicsSpace().getRigidBodyList()) {

            if (body.getPhysicsLocation().length() >= start_radius_of_repulsive_zone) {
                body.applyCentralForce(
                        body.getPhysicsLocation().normalize().mult(
                                -standard_repulsion_parameter * body.getMass()
                                / (float) Math.pow((body.getPhysicsLocation().length() - singular_radius), 2)
                        ));
            }
        }

        // HUD.
        if (t % 60 == 0) {
            if (hudText != null) {
                guiNode.detachChild(hudText);
            }

            // Region.
            if (cam.getLocation().length() >= start_radius_of_repulsive_zone) {
                if (cam.getLocation().length() > singular_radius) {
                    region = "Exterior region.";
                } else {
                    region = "Repulsive region. Repulsive acceleration: " + standard_repulsion_parameter
                            / (float) Math.pow((cam.getLocation().length() - singular_radius), 2);
                }
            } else {
                region = "Non repulsive region.";
            }

            hudText = new BitmapText(guiFont, false);
            hudText.setSize(guiFont.getCharSet().getRenderedSize());      // font size
            hudText.setColor(ColorRGBA.Cyan);                             // font color
            hudText.setText(
                    // The text.
                    "Position: " + cam.getLocation()
                    + "\nCamera direction: " + cam.getDirection()
                    + "\nAmount of cannonballs created: " + quantity
                    + "\nGravitational constant: " + gravitational_constant
                    + "\nCannonball initial speed: " + cannonball_speed
                    + "\nCannonball mass: " + cannonball_mass
                    + "\nLink data: " + link_data
                    + "\nStart radius of repulsive_zone: " + start_radius_of_repulsive_zone
                    + "\nSingular radius: " + singular_radius
                    + "\nRadius: " + cam.getLocation().length()
                    + "\nRegion: " + region
            );
            hudText.setLocalTranslation(300, hudText.getLineHeight() * 11, 0);
            guiNode.attachChild(hudText);

            // HUD for errors.
            ErrorHudText = new BitmapText(guiFont, true);
            ErrorHudText.setSize(guiFont.getCharSet().getRenderedSize());
            ErrorHudText.setColor(ColorRGBA.Red);
            ErrorHudText.setText(error_data);
            ErrorHudText.setLocalTranslation(1000, hudText.getLineHeight() * 11, 0);
            guiNode.attachChild(ErrorHudText);
        }

        t++;
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
