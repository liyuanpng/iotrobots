package cgl.iotrobots.sim;

import cgl.iotcloud.core.transport.TransportConstants;
import cgl.iotrobots.slam.core.app.GFSAlgorithm;
import cgl.iotrobots.slam.core.app.GFSMap;
import cgl.iotrobots.slam.core.app.LaserScan;
import cgl.iotrobots.slam.core.gridfastsalm.GridSlamProcessor;
import cgl.iotrobots.slam.core.utils.DoubleOrientedPoint;
import cgl.iotrobots.slam.streaming.Utils;
import cgl.iotrobots.utils.rabbitmq.*;
import com.esotericsoftware.kryo.Kryo;
import simbad.gui.Simbad;
import simbad.sim.*;
import simbad.sim.Box;

import javax.swing.*;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SimbardDistributed {
    public static final int SENSORS = 360;

    public static final double ANGLE = 2 * Math.PI;

    static MapUI mapUI;
    /** Describe the robot */
    static public class Robot extends Agent {
        GFSAlgorithm gfsAlgorithm = new GFSAlgorithm();
        RangeSensorBelt sonars;
        CameraSensor camera;
        RabbitMQSender sender;
        RabbitMQReceiver receiver;
        RabbitMQReceiver bestReceiver;
        RabbitMQSender controlSender;

        Kryo kryo = new Kryo();

        PrintWriter pw;
//        private String url = "amqp://149.165.159.12:5672";
        private String url = "amqp://localhost:5672";

        public Robot(Vector3d position, String name) {
            super(position, name);

            try {
                controlSender = new RabbitMQSender(url, "simbard_control");
                sender = new RabbitMQSender(url, "simbard_laser");
                receiver = new RabbitMQReceiver(url, "simbard_map");
                bestReceiver = new RabbitMQReceiver(url, "simbard_best");
                sender.open();
                controlSender.open();
                receiver.listen(new MapReceiver());
                bestReceiver.listen(new BestParticleReceiver());
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
            scanI.setAngle_increment(ANGLE / SENSORS);
            scanI.setAngle_max(ANGLE);
            scanI.setAngle_min(0);
            List<Double> ranges  = new ArrayList<Double>();
            for (int i = 0; i < SENSORS; i++) {
                ranges.add(100.0);
            }
            scanI.setRanges(ranges);
            scanI.setRange_min(0);
            scanI.setRangeMax(100);
            scanI.setTimestamp(System.currentTimeMillis());

            gfsAlgorithm.initMapper(scanI);

            SimUtils.sendControl(controlSender);
        }

        boolean forward = false;

        long lastTime = System.currentTimeMillis();

        /** This method is call cyclically (20 times per second)  by the simulator engine. */
        public void performBehavior() {
            System.out.println("\n\n");
            Point3d point3D = new Point3d(0.0, 0.0, 0.0);
            getCoords(point3D);

            System.out.format("%f, %f, %f\n", point3D.x, point3D.y, point3D.z);
            LaserScan laserScan = getLaserScan();
            laserScan.setPose(new DoubleOrientedPoint(point3D.x, 0.0, 0.0));

            byte []body = Utils.serialize(kryo, laserScan);
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("time", System.currentTimeMillis());
            props.put(TransportConstants.SENSOR_ID, System.currentTimeMillis());

            if (System.currentTimeMillis() - lastTime > 3000) {
                lastTime = System.currentTimeMillis();
                Message message = new Message(body, props);
                try {
                    sender.send(message, "test.test.laser_scan");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // progress at 0.5 m/s
            if (getCounter() % 500 == 0) {
                forward = !forward;
            }

            if (forward) {
                setTranslationalVelocity(.5);
            } else {
                setTranslationalVelocity(-.5);
            }
        }

        private long bestSum;
        private long mapSum;
        private long bestCount;
        private long mapCount;

        private class BestParticleReceiver implements MessageHandler {
            @Override
            public Map<String, String> getProperties() {
                Map<String, String> props = new HashMap<String, String>();
                props.put(MessagingConstants.RABBIT_ROUTING_KEY, "test.test.best");
                props.put(MessagingConstants.RABBIT_QUEUE, "test.test.best");
                return props;
            }

            @Override
            public void onMessage(Message message) {
//                GFSMap map = (GFSMap) Utils.deSerialize(kryo, message.getBody(), GFSMap.class);
                Object time = message.getProperties().get("time");
                Long t = Long.parseLong(time.toString());
                bestSum += System.currentTimeMillis() - t;
                bestCount++;
                System.out.println("*******************Best Time: " + (System.currentTimeMillis() - t) + "Average: " + ((double)(bestSum) / bestCount) +" ***************************");
//                mapUI.setMap(map);
            }
        }

        private class MapReceiver implements MessageHandler {
            @Override
            public Map<String, String> getProperties() {
                Map<String, String> props = new HashMap<String, String>();
                props.put(MessagingConstants.RABBIT_ROUTING_KEY, "test.test.map");
                props.put(MessagingConstants.RABBIT_QUEUE, "test.test.map");
                return props;
            }

            @Override
            public void onMessage(Message message) {
                GFSMap map = (GFSMap) Utils.deSerialize(kryo, message.getBody(), GFSMap.class);
                Object time = message.getProperties().get("time");
                Long t = Long.parseLong(time.toString());
                mapCount++;
                mapSum += (System.currentTimeMillis() - t);
                System.out.println("*******************Map Time: " + (System.currentTimeMillis() - t) + "Average: " + ((double)(mapSum) / mapCount) +" ***************************");
                mapUI.setMap(map);
            }
        }

        private LaserScan getLaserScan() {
            int n = sonars.getNumSensors();

            LaserScan laserScan = new LaserScan();
            laserScan.setAngle_max(ANGLE);
            laserScan.setAngle_min(0);
            laserScan.setRangeMax(100);
            laserScan.setRange_min(0);
            laserScan.setAngle_increment(ANGLE/ SENSORS);

            int angle = 0;
            List<Double> ranges  = new ArrayList<Double>();
            for (int i = angle; i < n + angle; i++) {
                if (sonars.hasHit(i % n)) {
                    ranges.add(sonars.getMeasurement(i % n));
                } else {
                    ranges.add(0.0);
                }
            }
            laserScan.setRanges(ranges);
            laserScan.setTimestamp(System.currentTimeMillis());
            return laserScan;
        }
    }

    /** Describe the environement */
    static public class MyEnv extends EnvironmentDescription {
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
            //add(new Robot(new Vector3d(0, 0, 0), "robot 2"));
        }
    }

    public static void main(String[] args) {
        // request antialising
        System.setProperty("j3d.implicitAntialiasing", "true");
        // create Simbad instance with given environment
        Simbad frame = new Simbad(new MyEnv(), false);

        showOnScreen(1, frame);
        mapUI = new MapUI();
//        showOnScreen(1, mapUI);
    }

    public static void showOnScreen( int screen, JFrame frame )
    {
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        if (screen > -1 && screen < gs.length) {
            gs[screen].setFullScreenWindow(frame);
        } else if (gs.length > 0) {
            gs[0].setFullScreenWindow(frame);
        } else {
            throw new RuntimeException("No Screens Found");
        }
    }
}
