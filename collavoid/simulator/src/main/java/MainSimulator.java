import geometry_msgs.Twist;
import nav_msgs.Odometry;
import org.ros.message.MessageListener;
import org.ros.message.Time;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava.tf.pubsub.TransformBroadcaster;
import sensor_msgs.PointCloud2;
import simbad.gui.Simbad;
import simbad.sim.*;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;


public class MainSimulator {

    /**
     * Describe the robot
     */
    static public class Robot extends Agent {

        //orientation
        double orientation;
        //kinematic
        DifferentialKinematic kinematic;
        private double vl;
        private double vr;
        private double wheelDistance;
        //sensors
        LaserScan laserScan;
        RangeSensorBelt sensors;
        CameraSensor camera;
        //node
        private ConnectedNode node;
        //odometry publisher
        Publisher<Odometry> odometryPublisher = null;
        Odometry odoMsg;
        int odomSeq;
        //laser scan publisher
        Publisher<PointCloud2> laserscanPublisher = null;
        PointCloud2 pc2;
        int pc2Seq;
        //velocity command subscriber
        Subscriber<Twist> velocitySubscriber = null;
        //tf broadcaster
        TransformBroadcaster tfb = null;
        //frame
        String robotFrame;
        String odomFrame;
        String globalFrame;


        public Robot(Vector3d position, double ori, String name) {
            // initialize position and orientation
            super(position, name);
            orientation = ori;
            //use differential model
            kinematic = RobotFactory.setDifferentialDriveKinematicModel(this);
            wheelDistance = this.getRadius();
            // Add camera
            camera = RobotFactory.addCameraSensor(this);
            laserScan = new LaserScan(this.getRadius(), Math.PI, 180, 1.5, 3);
            sensors = laserScan.getSensor();
            // if sensors are not on the center of the robot then height
            // should be assigned to the laserscan, in the robot frame
            this.addSensorDevice(sensors, new Vector3d(0, 0, 0), 0);
            //initialize frames
            robotFrame = this.getName() + "/base";
            odomFrame = this.getName() + "/odometry";
            globalFrame = "/map";
            initPubSub();
        }

        public void initPubSub() {
            // initialize node
            AgentNode agentNode = new AgentNode(this.getName());
            node = agentNode.getNode();
            // initialize odometry publisher and message
            if (odometryPublisher == null) {
                odometryPublisher = node.newPublisher(this.getName() + "/odometry", Odometry._TYPE);
                // initialize message
                odoMsg = odometryPublisher.newMessage();
                odoMsg.getHeader().setFrameId(odomFrame);
                odoMsg.setChildFrameId(robotFrame);
                odomSeq = 0;
            }
            //initialize laser scan publisher
            if (laserscanPublisher == null) {
                laserscanPublisher = node.newPublisher(this.getName() + "/scan/point_cloud2", PointCloud2._TYPE);
                // initialize message
                pc2 = laserscanPublisher.newMessage();
                pc2.getHeader().setFrameId(robotFrame);
                pc2Seq = 0;
            }

            //initialize velocity command subscriber
            if (velocitySubscriber == null) {
                velocitySubscriber = node.newSubscriber(this.getName() + "/cmd_vel", Twist._TYPE);
                velocitySubscriber.addMessageListener(new MessageListener<Twist>() {
                    @Override
                    public void onNewMessage(Twist msg) {
                        double v, w;
                        Vector3d vel = new Vector3d(msg.getLinear().getX(), msg.getLinear().getY(), msg.getLinear().getZ());
                        v = vel.length();
                        w = msg.getAngular().getZ();
                        vl = (2 * v - w * wheelDistance) / 2;
                        vr = (2 * v + w * wheelDistance) / 2;
                    }
                });
            }

            if (tfb == null) {
                tfb = new TransformBroadcaster(node);
            }
        }

        /**
         * This method is called by the simulator engine on reset.
         */
        public void initBehavior() {
            this.rotateY(orientation);
        }

        /**
         * This method is call cyclically (20 times per second)  by the simulator engine.
         */
        public void performBehavior() {

            kinematic.setWheelsVelocity(vl, vr);

            if (getCounter() % 20 == 0) {
                //publish odometry
                setOdoMsg();
                odometryPublisher.publish(odoMsg);
                //publish laser scan in pointcloud2 format
                setLaserscanMsg();
                laserscanPublisher.publish(pc2);
            }
            //send transform in frequency of 10 HZ
            if (getCounter() % 2 == 0) {
                sendTransform();
            }

        }

        public void setOdoMsg() {
            //get position
            Point3d cor = new Point3d();
            this.getCoords(cor);
            Quat4d ori = getOrientation(this);

            odoMsg.getHeader().setStamp(node.getCurrentTime());
            odoMsg.getHeader().setSeq(odomSeq++);

            odoMsg.getPose().setPose(utils.getPose(cor, ori));
            odoMsg.getTwist().getTwist().setLinear(utils.getTwist(this.linearVelocity));
            odoMsg.getTwist().getTwist().setAngular(utils.getTwist(this.angularVelocity));

        }


        public void setLaserscanMsg() {
            pc2.getHeader().setSeq(pc2Seq++);
            pc2.getHeader().setStamp(node.getCurrentTime());
            utils.toPointCloud2(pc2, laserScan.getScan());
        }

        public static Quat4d getOrientation(Robot robot) {
            //get orientation
            Transform3D tfr = new Transform3D();
            Quat4d ori = new Quat4d();
            robot.getRotationTransform(tfr);
            tfr.get(ori);
            return ori;
        }

        public void sendTransform() {
            String childFrame = robotFrame;
            String parentFrame = odomFrame;

            Transform3D tf = new Transform3D();
            Vector3d tft3d = new Vector3d();
            Quat4d tfrq = new Quat4d();

            this.getTranslationTransform(tf);
            tf.get(tft3d);
            tft3d = utils.toROSCoordinate(tft3d);
            this.getRotationTransform(tf);
            tf.get(tfrq);
            tfrq = utils.toROSCoordinate(tfrq);

            tfb.sendTransform(
                    parentFrame, childFrame,
                    tft3d.getX(), tft3d.getY(), tft3d.getZ(),
                    tfrq.getX(), tfrq.getY(), tfrq.getZ(), tfrq.getW()
            );
        }

    }

    /**
     * Describe the environement
     */
    static public class MyEnv extends EnvironmentDescription {
        public MyEnv() {
            final int robotNb = 5;
            final double posRadius = 6;

            light1IsOn = true;
            light2IsOn = false;

            Wall w1 = new Wall(new Vector3d(9, 0, 0), 19, 2, this);
            w1.rotate90(1);
            add(w1);
            Wall w2 = new Wall(new Vector3d(-9, 0, 0), 19, 2, this);
            w2.rotate90(1);
            add(w2);
            Wall w3 = new Wall(new Vector3d(0, 0, 9), 19, 2, this);
            add(w3);
            Wall w4 = new Wall(new Vector3d(0, 0, -9), 19, 2, this);
            add(w4);

            Box b1 = new Box(new Vector3d(4, 0, 0), new Vector3f(1, 1, 1),
                    this);
            add(b1);
//            add(new Arch(new Vector3d(3, 0, -3), this));
            double step = 2 * Math.PI / robotNb;
            for (int i = 0; i < robotNb; i++) {
                add(new Robot(new Vector3d(posRadius * Math.cos(i * step), 0, -posRadius * Math.sin(i * step)), Math.PI + i * step, "robot" + i));

            }
        }
    }

    public static void main(String[] args) {
        // request antialising
        System.setProperty("j3d.implicitAntialiasing", "true");
        // create Simbad instance with given environment
        Simbad frame = new Simbad(new MyEnv(), false);

    }

} 