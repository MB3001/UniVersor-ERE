package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import static com.jme3.math.Vector3f.ZERO;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
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
    float gravitational_constant = 5f; //3.9f * (float) Math.pow(10, -6);
    float standard_repulsion_parameter = 10f; //(float) Math.pow(10, 10);

    float start_radius_of_repulsive_zone = 30f; //(float) Math.pow(10, 8);
    float singular_radius = 50f; //start_radius_of_repulsive_zone + 3.16f * (float) Math.pow(10, 6);

    float cannonball_speed = 2f;
    float cannonball_mass = 1f;
    float attractor_mass = 3f;

    /**
     * Prepare HUD.
     */
    BitmapText hudText;
    private long t = 0;
    private int quantity = 0;

    /**
     * Prepare bodies.
     */
    private RigidBodyControl ball_phy;
    private static Sphere sphere;
    private static Sphere bigSphere;

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

        flyCam.setMoveSpeed(30 * speed);

        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "shoot");

        // Attractors.
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
            ball_phy.setGravity(ZERO);
            ball_phy.setLinearVelocity(
                    Vector3f.UNIT_Y.mult(5 * (float) (Math.random() - 0.5))
                            .add(Vector3f.UNIT_Z.mult(5 * (float) (Math.random() - 0.5)))
            );

        }

        // Center mark.
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
        }
    };

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
        ball_phy.setGravity(ZERO);

        quantity++;

        ball_phy.setLinearVelocity(cam.getDirection().mult(cannonball_speed));
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
            );
            hudText.setLocalTranslation(300, hudText.getLineHeight() * 6, 0); // position
            guiNode.attachChild(hudText);
        }

        t++;
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
