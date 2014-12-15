package cgl.iotrobots.sim;

import cgl.iotrobots.slam.core.app.GFSAlgorithm;
import cgl.iotrobots.slam.core.app.LaserScan;
import cgl.iotrobots.slam.core.gridfastsalm.GridSlamProcessor;
import cgl.iotrobots.slam.core.utils.DoubleOrientedPoint;
import cgl.iotrobots.utils.rabbitmq.RabbitMQReceiver;
import cgl.iotrobots.utils.rabbitmq.RabbitMQSender;
import simbad.gui.Simbad;
import simbad.sim.*;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import java.util.ArrayList;


public class SimbardDistributed {
    public static final int SENSORS = 180;

    public static final double ANGLE = 2 * Math.PI;

    static MapUI mapUI;
    /** Describe the robot */
    public static class Robot extends Agent {
        GFSAlgorithm gfsAlgorithm = new GFSAlgorithm();
        RangeSensorBelt sonars;
        CameraSensor camera;

        RabbitMQSender sender;
        RabbitMQReceiver receiver;

        public Robot(Vector3d position, String name) {
            super(position, name);
            try {
                sender = new RabbitMQSender("amqp://localhost:5672", "slam");
                receiver = new RabbitMQReceiver("amqp://localhost:5672", "slam");
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Add camera
            camera = RobotFactory.addCameraSensor(this);
            // Add sonars
            double agentHeight = this.getHeight();
            double agentRadius = this.getRadius();
            sonars = new RangeSensorBelt((float) agentRadius,
                    0f, 100.0f, SENSORS, RangeSensorBelt.TYPE_SONAR,0);
            sonars.setUpdatePerSecond(1000);

            Vector3d pos = new Vector3d(0, agentHeight / 2, 0.0);
            this.addSensorDevice(sonars, pos, 0);
        }

        /** This method is called by the simulator engine on reset. */
        public void initBehavior() {
            // nothing particular in this case
            gfsAlgorithm.gsp_ = new GridSlamProcessor();
            gfsAlgorithm.init();
            LaserScan scanI = new LaserScan();
            scanI.angle_increment = ANGLE / SENSORS;
            scanI.angle_max = ANGLE ;
            scanI.angle_min = 0;
            scanI.ranges = new ArrayList<Double>();
            for (int i = 0; i < SENSORS; i++) {
                scanI.ranges.add(100.0);
            }
            scanI.range_min = 0;
            scanI.rangeMax = 100;
            scanI.timestamp = System.currentTimeMillis();

            gfsAlgorithm.initMapper(scanI);
        }

        boolean forward = false;
        double prevX = 0;
        /** This method is call cyclically (20 times per second)  by the simulator engine. */
        public void performBehavior() {
            System.out.println("\n\n");
            Point3d point3D = new Point3d(0.0, 0.0, 0.0);
            getCoords(point3D);

            System.out.format("%f, %f, %f\n", point3D.x, point3D.y, point3D.z);
            LaserScan laserScan = getLaserScan();

            gfsAlgorithm.laserScan(laserScan, new DoubleOrientedPoint(point3D.x, 0.0, 0.0));
            prevX = point3D.x;
            // progress at 0.5 m/s
            if (getCounter() % 50 == 0) {
                forward = !forward;
            }

            if (forward) {
                setTranslationalVelocity(5);
            } else {
                setTranslationalVelocity(-5);
            }

            mapUI.setMap(gfsAlgorithm.getMap());
        }

        private LaserScan getLaserScan() {
            int n = sonars.getNumSensors();

            LaserScan laserScan = new LaserScan();
            laserScan.angle_max = ANGLE;
            laserScan.angle_min = 0;
            laserScan.rangeMax = 100;
            laserScan.range_min = 0;
            laserScan.angle_increment = ANGLE/ SENSORS;

            int angle = 0;
            for (int i = angle; i < n + angle; i++) {
                if (sonars.hasHit(i % n)) {
                    laserScan.ranges.add(sonars.getMeasurement(i % n));
                }
            }
            laserScan.timestamp = System.currentTimeMillis();

            return laserScan;
        }
    }


    /**
     * Describe the environement
     * */
    public static class MyEnv extends EnvironmentDescription {
        public MyEnv() {
            light1IsOn = true;
            light2IsOn = false;
            Wall w1 = new Wall(new Vector3d(9, 0, 0), 19, 1, this);
            w1.rotate90(1);
            add(w1);
            Wall w2 = new Wall(new Vector3d(-9, 0, 0), 19, 2, this);
            w2.rotate90(1);
            add(w2);
            Wall w3 = new Wall(new Vector3d(0, 0, 9), 19, 1, this);
            add(w3);
            Wall w4 = new Wall(new Vector3d(0, 0, -9), 19, 2, this);
            add(w4);
            Box b1 = new Box(new Vector3d(-3, 0, -3), new Vector3f(1, 1, 1),
                    this);
            add(b1);

            Box b2 = new Box(new Vector3d(3, 0, 3), new Vector3f(1, 1, 1),
                    this);
            add(b2);

            Box b3 = new Box(new Vector3d(6, 0, 6), new Vector3f(1, 1, 1),
                    this);
            add(b3);

            Box b4 = new Box(new Vector3d(-6, 0, -6), new Vector3f(1, 1, 1),
                    this);
            add(b4);
            add(new Arch(new Vector3d(3, 0, -3), this));
            add(new Robot(new Vector3d(0, 0, 0), "robot 1"));
        }
    }

    public static void main(String[] args) {
        // request antialising
        System.setProperty("j3d.implicitAntialiasing", "true");
        // create Simbad instance with given environment
        Simbad frame = new Simbad(new MyEnv(), false);
        mapUI = new MapUI();
    }
}
