package cgl.iotrobots.sim;

import cgl.iotrobots.slam.core.app.GFSAlgorithm;
import cgl.iotrobots.slam.core.app.LaserScan;
import cgl.iotrobots.slam.core.gridfastsalm.GridSlamProcessor;
import cgl.iotrobots.slam.core.utils.DoubleOrientedPoint;
import cgl.iotrobots.slam.threading.ParallelGridSlamProcessor;
import nav_msgs.Odometry;
import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.*;
import org.ros.node.topic.Subscriber;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TurtleSimulator {
    public static final int SENSORS = 360;

    public static final double ANGLE = 2 * Math.PI;

    GFSAlgorithm gfsAlgorithm = new GFSAlgorithm();

    BufferedReader br = null;

    int parallel = 2;

    MapUI mapUI;



    public TurtleSimulator() {
        mapUI = new MapUI();
    }

    public void start(boolean parallel) throws InterruptedException {
        // nothing particular in this case
        if (!parallel) {
            gfsAlgorithm.gsp_ = new GridSlamProcessor();
        } else {
            gfsAlgorithm.gsp_ = new ParallelGridSlamProcessor();
        }
        gfsAlgorithm.init();
        LaserScan scanI = new LaserScan();
        scanI.setAngleIncrement(ANGLE / SENSORS);
        scanI.setAngleMax(ANGLE);
        scanI.setAngleMin(0);
        List<Double> ranges  = new ArrayList<Double>();
        for (int i = 0; i < SENSORS; i++) {
            ranges.add(100.0);
        }
        scanI.setRanges(ranges);
        scanI.setRangeMin(0);
        scanI.setRangeMax(100);
        scanI.setTimestamp(System.currentTimeMillis());

        gfsAlgorithm.initMapper(scanI);

        try {
            br = new BufferedReader(new FileReader("out.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        connectToRos();
    }

    private void connectToRos() {
        // register with ros_java
        NodeConfiguration nodeConfiguration;
        try {
            nodeConfiguration = NodeConfiguration.newPublic("localhost", new URI("http://localhost:11311"));
            NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
            RosTurtle turtle = new RosTurtle();
            nodeMainExecutor.execute(turtle, nodeConfiguration);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        TurtleSimulator simulator = new TurtleSimulator();
        if (args.length > 0) {
            simulator.start(true);
            simulator.parallel = Integer.parseInt(args[0]);
        } else {
            simulator.start(false);
        }
    }

    public class RosTurtle extends AbstractNodeMain {
        private boolean stop = false;

        private String name = "/ts_controller";

        DoubleOrientedPoint lastPose;

        public RosTurtle() {
        }

        public GraphName getDefaultNodeName() {
            return GraphName.of("/" + name);
        }

        @Override
        public void onStart(final ConnectedNode connectedNode) {
            System.out.println("Starting....");
            final Subscriber<Odometry> odometrySubscriber =
                    connectedNode.newSubscriber("/odom", Odometry._TYPE);

            final Subscriber<sensor_msgs.LaserScan> laserScanSubscriber =
                    connectedNode.newSubscriber("/scan", sensor_msgs.LaserScan._TYPE);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }

            odometrySubscriber.addMessageListener(new MessageListener<Odometry>() {
                @Override
                public void onNewMessage(Odometry odometry) {
                    lastPose = new DoubleOrientedPoint(odometry.getPose().getPose().getPosition().getX(),
                            odometry.getPose().getPose().getPosition().getX(),
                            odometry.getPose().getPose().getOrientation().getZ());
                }
            });

            laserScanSubscriber.addMessageListener(new MessageListener<sensor_msgs.LaserScan>() {
                @Override
                public void onNewMessage(sensor_msgs.LaserScan laserScanMsg) {
                    LaserScan scan = turtleScanToLaserScan(laserScanMsg);
                    // update this with odomentry
                    if (lastPose != null) {
                        scan.setPose(lastPose);
                        gfsAlgorithm.laserScan(scan);
                        mapUI.setMap(gfsAlgorithm.getMap());
                    }
                }
            });

            // This CancellableLoop will be canceled automatically when the node shuts down.
            connectedNode.executeCancellableLoop(new CancellableLoop() {
                @Override
                protected void loop() throws InterruptedException {

                }
            });
        }

        public void onShutdown(Node node) {
            node.shutdown();
        }

        public void stop() {
            stop = true;
        }
    }

    private LaserScan turtleScanToLaserScan(sensor_msgs.LaserScan ls) {
        LaserScan laserScan = new LaserScan();
        laserScan.setAngleIncrement(ls.getAngleIncrement());
        laserScan.setAngleMin(ls.getAngleMin());
        laserScan.setAngleMax(ls.getAngleMax());
        laserScan.setRangeMax(ls.getRangeMax());
        laserScan.setRangeMin(ls.getRangeMin());
        if (ls.getRanges() != null) {
            List<Double> ranges = new ArrayList<Double>();
            float[] floats = ls.getRanges();
            for (float r : floats) {
                ranges.add((double) r);
            }
            laserScan.setRanges(ranges);
        }
        return laserScan;
    }

    private DoubleOrientedPoint getOdomPose(long t) {
        return null;
    }
}
