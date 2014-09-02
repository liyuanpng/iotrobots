package cgl.iotrobots.perf.sensor;

import cgl.iotcloud.core.*;
import cgl.iotcloud.core.msg.MessageContext;
import cgl.iotcloud.core.sensorsite.SiteContext;
import cgl.iotcloud.core.transport.Channel;
import cgl.iotcloud.core.transport.Direction;
import cgl.iotcloud.core.transport.TransportConstants;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PerformanceSensor extends AbstractSensor {
    private static Logger LOG = LoggerFactory.getLogger(PerformanceSensor.class);

    public static final String PERF_SEND_DATA_ROUTING_KEY = "perf_send_data";
    public static final String PERF_SEND_DATA_QUEUE_NAME = "perf_send_data";
    public static final String PERF_RECV_QUEUE_NAME = "perf_recv_data";
    public static final String PERF_RECV_ROUTING_KEY = "perf_recv_data";
    public static final String DATA_SENDER = "data_sender";
    public static final String DATA_RECEIVER = "data_receiver";
    private static final String PERF_EXCHANGE = "perf";

    public static final String MODE_ARG = "mode";
    public static final String TRP_ARG = "trp";
    public static final String DATA_SIZE_ARG = "data";
    public static final String DATA_INTERVAL_ARG = "freq";

    public static final int CAPACITY = 64;

    private boolean run = true;

    private BlockingQueue<byte []> messages = new ArrayBlockingQueue<byte[]>(64);

    private DataGenerator dataGenerator;

    public static void main(String[] args) {
        Map<String, String> properties = getProperties(args);
        SensorSubmitter.submitSensor(properties, new java.io.File(PerformanceSensor.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getName(),
                PerformanceSensor.class.getCanonicalName(), Arrays.asList("local"));
    }

    @Override
    public Configurator getConfigurator(Map map) {
        return new STSensorConfigurator();
    }

    @Override
    public void open(SensorContext context) {
        final Channel sendChannel = context.getChannel(TransportConstants.TRANSPORT_RABBITMQ, DATA_SENDER);
        final Channel receiveChannel = context.getChannel(TransportConstants.TRANSPORT_RABBITMQ, DATA_RECEIVER);

        String dataSizeString = (String) context.getProperty(DATA_SIZE_ARG);
        String dataIntervalString = (String) context.getProperty(DATA_INTERVAL_ARG);
        String trp = (String) context.getProperty(TRP_ARG);

        dataGenerator = new DataGenerator((Integer.parseInt(dataIntervalString), Integer.parseInt(dataSizeString), messages, CAPACITY);
        dataGenerator.start();
        // startSend(sendChannel, receivingQueue);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (run) {
                    try {
                        byte []body = messages.take();
                        Map<String, Object> props = new HashMap<String, Object>();
                        props.put("time", Long.toString(System.currentTimeMillis()));
                        sendChannel.publish(body, props);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();

        startListen(receiveChannel, new cgl.iotcloud.core.MessageReceiver() {
            @Override
            public void onMessage(Object message) {
                if (message instanceof MessageContext) {
                    String time = (String) ((MessageContext) message).getProperties().get("time");

                    try {
                        PrintWriter writer = new PrintWriter(new BufferedWriter(new LatencyWriter("/home/supun/dev/projects/LatencyTest.txt", true)));
                        writer.println(System.currentTimeMillis() + " " + (System.currentTimeMillis() - Long.parseLong(time)));
                        writer.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    LOG.info("Message received " + message.toString());

                } else {
                    LOG.error("Unexpected message");
                }
            }
        });
        LOG.info("Received request for opening sensor: {} with id: {}", context.getSensorID());
    }

    @Override
    public void close() {
        run = false;
        super.close();
    }

    @SuppressWarnings("unchecked")
    private class STSensorConfigurator extends AbstractConfigurator {
        @Override
        public SensorContext configure(SiteContext siteContext, Map conf) {
            String mode = (String) conf.get(MODE_ARG);
            String trp = (String) conf.get(TRP_ARG);
            String dataSize = (String) conf.get(DATA_SIZE_ARG);
            String dataInterval = (String) conf.get(DATA_INTERVAL_ARG);

            SensorContext context = new SensorContext("turtle_sensor");
            context.addProperty(MODE_ARG, mode);
            context.addProperty(TRP_ARG, trp);
            context.addProperty(DATA_SIZE_ARG, dataSize);
            context.addProperty(DATA_INTERVAL_ARG, dataInterval);

            Map sendProps = new HashMap();
            sendProps.put("exchange", PERF_EXCHANGE);
            sendProps.put("routingKey", PERF_SEND_DATA_ROUTING_KEY);
            sendProps.put("queueName", PERF_SEND_DATA_QUEUE_NAME);
            Channel sendChannel = createChannel(DATA_SENDER, sendProps, Direction.OUT, 1024);

            Map receiveProps = new HashMap();
            receiveProps.put("queueName", PERF_RECV_QUEUE_NAME);
            receiveProps.put("exchange", PERF_EXCHANGE);
            receiveProps.put("routingKey", PERF_RECV_ROUTING_KEY);
            Channel receiveChannel = createChannel(DATA_RECEIVER, receiveProps, Direction.IN, 1024);

            context.addChannel(TransportConstants.TRANSPORT_RABBITMQ, sendChannel);
            context.addChannel(TransportConstants.TRANSPORT_RABBITMQ, receiveChannel);

            return context;
        }
    }

    private static Map<String, String> getProperties(String []args) {
        Map<String, String> conf = new HashMap<String, String>();
        Options options = new Options();
        options.addOption(MODE_ARG, true, "possible options are (nt, t) nt means without connecting to turtle");
        options.addOption(TRP_ARG, true, "k or r");
        CommandLineParser commandLineParser = new BasicParser();
        try {
            CommandLine cmd = commandLineParser.parse(options, args);
            String mode = cmd.getOptionValue(MODE_ARG);
            String trp = cmd.getOptionValue(TRP_ARG);
            String dataSizeString = cmd.getOptionValue(DATA_SIZE_ARG);
            String dataIntervalString = cmd.getOptionValue(DATA_INTERVAL_ARG);

            conf.put(MODE_ARG, mode);
            conf.put(TRP_ARG, trp);
            conf.put(DATA_SIZE_ARG, dataSizeString);
            conf.put(DATA_INTERVAL_ARG, dataIntervalString);

            return conf;
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sensor", options );
        }
        return null;
    }
}
