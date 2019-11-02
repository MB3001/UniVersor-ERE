/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.bullet.joints.Point2PointJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;

/**
 *
 * @author Matías Bonino
 */
public class Link extends Point2PointJoint {
    /* It consists of the two points (indicated with right click) of the surfaces of two
    different bodies. Make the makeLink constructor with those two points. ¿Here or in Main.java?*/
    
    public Link(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB) {
        super(nodeA, nodeB, pivotA, pivotB);
        //createLink();
    }
    
    /**/protected void createLink() {
        //constraint = new Point2PointConstraint(nodeA.getObjectId(), nodeB.getObjectId(), Converter.convert(pivotA), Converter.convert(pivotB));
    }/**/

}
