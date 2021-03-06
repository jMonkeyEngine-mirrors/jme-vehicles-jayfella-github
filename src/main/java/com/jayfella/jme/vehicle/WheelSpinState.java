package com.jayfella.jme.vehicle;

import com.jayfella.jme.vehicle.part.Gear;
import com.jayfella.jme.vehicle.part.Wheel;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class WheelSpinState extends BaseAppState {

    private final Car car;
    private int wheelCount;

    private Quaternion[] rot;
    private float[][] angles;// = new float[3];

    public WheelSpinState(Car car) {
        this.car = car;
    }

    @Override
    protected void initialize(Application app) {
        this.wheelCount = car.getVehicleControl().getNumWheels();
        this.angles = new float[wheelCount][3];

         rot = new Quaternion[wheelCount];

         for (int i = 0; i < rot.length; i++) {
             rot[i] = new Quaternion();
         }
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }

    /**
     * Calculates how many radians per second the wheel should rotate at the speed the vehicle is travelling.
     * @param wheel the wheel in question.
     * @return the amount in radians the wheel rotates in one second at the speed the vehicle is travelling.
     */
    private float calcWheelRotation(Wheel wheel) {

        // https://sciencing.com/calculate-wheel-speed-7448165.html
        float speed = car.getSpeed(Vehicle.SpeedUnit.MPH);

        // convert mph to meters per minute
        float metersPerHour = speed * 1609;
        float metersPerMin = metersPerHour / 60;

        // calculate the circumference of the wheel.
        float c = wheel.getSize() * FastMath.PI;

        // calc the wheel speed in revs per min
        float revsPerMin = metersPerMin / c;
        float revsPerSec = revsPerMin / 60;

        // convert revolutions per second to radians.
        float radPerSec = revsPerSec * FastMath.TWO_PI;

        return radPerSec;
    }

    @Override
    public void update(float tpf) {

        for (int i = 0; i < wheelCount; i++) {

            /*
            Wheel wheel = car.getWheel(i);

            // rotation since last physics step
            float currentRot = wheel.getVehicleWheel().getDeltaRotation();

            // how many times this wheel "should" rotate per second.
            float potentialRevsPerSec = calcWheelspin(wheel);

            // multiply that by TWO_PI to get radians.
            float potentialRot = potentialRevsPerSec / FastMath.TWO_PI;

            // should give us a rotation in radians for "extre" rotation as a result
            // of slip.
            float diff = potentialRot - currentRot;

            wheel.setRotationDelta(diff);

            System.out.println(wheel.getVehicleWheel().getWheelSpatial().getName() + ": " + currentRot + " / " + potentialRot + " / " + diff);

             */

            Wheel wheel = car.getWheel(i);

            // only calculate wheelspin when the vehicle is actually accelerating.
            if (car.getAccelerationForce() > 0) {


                // the acceleration force this wheel can apply. 0 = it doesnt give power, 1 = it gives full power.
                float wheelforce = wheel.getAccelerationForce();

                // the acceleration force of the accelerator pedal in 0-1 range.
                float acceleration = car.getAccelerationForce();

                // how much this wheel is "skidding".
                float skid = 1.0f - wheel.getVehicleWheel().getSkidInfo();

                // would equal at most 57 degrees in one frame (one radian).
                float skidForce = (acceleration * wheelforce) * skid;

                //System.out.println(wheel.getVehicleWheel().getWheelSpatial().getName() + ": " + skidForce);

                // set this before we do any "scene" modifications to make it look better.
                wheel.setRotationDelta(skidForce);

                // the numbers below alter the scene only. they have no relation to any calculations.
                // These calculations will add an additional rotation to the wheel to simulate wheelspin.

                // so if we mult this by say 10(?) for 570 degrees and then mult it by tpf, we should be about right.
                // this means if we are slipping 100% it will add ( 57 * x ) degrees per second.

                // actually we can work this out. get the max revs.

                skidForce *= 10;
                skidForce *= tpf;



                Node wheelNode = (Node) wheel.getVehicleWheel().getWheelSpatial();
                Spatial wheelGeom = wheelNode.getChild("wheel");

                float[] existingAngles = wheelGeom.getLocalRotation().toAngles(null);

                // add the additional rotation for wheelspin.

                // the wheel model is rotated 180 on the Y axis for the left-side of the vehicle.
                float[] wheelRot = wheelNode.getChild(0).getLocalRotation().toAngles(null);
                // - for left
                // + for right
                if (wheelRot[1] == 0) {
                    angles[i][0] += skidForce;
                }
                else {
                    angles[i][0] -= skidForce;
                }


                // around and around we go...
                if (angles[i][0] < -FastMath.PI) {
                    angles[i][0] += FastMath.PI;
                }

                angles[i][1] = existingAngles[1];
                angles[i][2] = existingAngles[2];

                rot[i].fromAngles(angles[i]);

                wheelGeom.setLocalRotation(rot[i]);
            }


            else if (wheel.getBrakeStrength() > 0) {

                // calculate how fast this wheel should be rotating at the speed it's travelling.
                // multiply it by how much slip the wheel is experiencing
                // and use that rotation as a counter-rotation to simulate wheel stopping spinning.
                // at full slip with brakes applied, the wheel should stop spinning completely.
                // if the wheel it slipping 50% the wheel reduces rotation by 50%.
                float slip = 1.0f - wheel.getVehicleWheel().getFrictionSlip();
                float rotation = calcWheelRotation(wheel) * tpf;
                rotation *= slip;

                Node wheelNode = (Node) wheel.getVehicleWheel().getWheelSpatial();
                Spatial wheelGeom = wheelNode.getChild("wheel");

                float[] existingAngles = wheelGeom.getLocalRotation().toAngles(null);

                // add the additional rotation for skidding.

                // the wheel model is rotated 180 on the Y axis for the left-side of the vehicle.
                float[] wheelRot = wheelNode.getChild(0).getLocalRotation().toAngles(null);
                // + for left
                // - for right
                if (wheelRot[1] == 0) {
                    angles[i][0] -= rotation;
                }
                else {
                    angles[i][0] += rotation;
                }

                // around and around we go...
                if (angles[i][0] < -FastMath.PI) {
                    angles[i][0] += FastMath.PI;
                }

                angles[i][1] = existingAngles[1];
                angles[i][2] = existingAngles[2];

                rot[i].fromAngles(angles[i]);

                wheelGeom.setLocalRotation(rot[i]);

            }

        }


    }

}
