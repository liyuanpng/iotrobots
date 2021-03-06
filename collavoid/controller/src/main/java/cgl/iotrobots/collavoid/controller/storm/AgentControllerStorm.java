package cgl.iotrobots.collavoid.controller.storm;

import cgl.iotrobots.collavoid.commons.rmqmsg.Constant_msg;
import cgl.iotrobots.collavoid.commons.rmqmsg.Contexts;
import cgl.iotrobots.collavoid.commons.rmqmsg.Methods_RMQ;
import cgl.iotrobots.collavoid.commons.rmqmsg.RMQContext;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.IOException;
import java.util.Map;

public class AgentControllerStorm {
    private AgentROSNodeStorm agentROSNode;

    private NodeMainExecutor nodeMainExecutor;

    private Address[] addresses;

    private Channel channel;

    private String url;

    private String sensorName;

    private Map<String, RMQContext> RMQContexts;

    public AgentControllerStorm(String sensorName_,
                                Address[] rmqAddress,
                                String rmqUrl_) {
        this.sensorName = sensorName_;
        this.addresses = rmqAddress;
        this.url = rmqUrl_;
        // build exchange according to message types
        RMQContexts = new Contexts(sensorName_).getRMQContexts();
        channel = Methods_RMQ.getChannel(addresses, url, null);
        BindQueue(RMQContexts);
    }

    public void start(NodeConfiguration configuration) {
        //need to be a different node name
        agentROSNode = new AgentROSNodeStorm(sensorName + "_rmq", RMQContexts);
        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
        nodeMainExecutor.execute(agentROSNode, configuration);
        clearQueues();
    }

    private void BindQueue(Map<String, RMQContext> RMQParams) {
        try {
            for (Map.Entry<String, RMQContext> e : RMQParams.entrySet()) {
                e.getValue().CHANNEL = channel;
                e.getValue().CHANNEL.exchangeDeclare(
                        e.getValue().EXCHANGE_NAME,
                        e.getValue().EXCHANGE_TYPE,
                        e.getValue().DURABLE
                );
                if (e.getKey().equals(Constant_msg.KEY_VELOCITY_CMD)) {
                    e.getValue().CHANNEL.queueDeclare(e.getValue().QUEUE_NAME, false, false, true, null);
                    e.getValue().CHANNEL.queueBind(
                            e.getValue().QUEUE_NAME,
                            e.getValue().EXCHANGE_NAME,
                            e.getValue().ROUTING_KEY
                    );
                    e.getValue().CHANNEL.queuePurge(e.getValue().QUEUE_NAME);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isGoalReached() {
        return agentROSNode.isGoalReached();
    }

    public void clearQueues() {
        Methods_RMQ.clearQueues(RMQContexts);
        agentROSNode.getVelQueue().clear();
    }

    public void stop() {
        try {
            if (agentROSNode != null) {
                nodeMainExecutor.shutdown();
            }
            for (Map.Entry<String, RMQContext> context : RMQContexts.entrySet()) {
                if (!context.getValue().CHANNEL.isOpen())
                    continue;
                if (context.getKey().equals(Constant_msg.KEY_VELOCITY_CMD))
                    context.getValue().CHANNEL.queueDelete(context.getValue().QUEUE_NAME);
                context.getValue().CHANNEL.exchangeDelete(context.getValue().EXCHANGE_NAME);
                context.getValue().CHANNEL.close();
            }

        } catch (IOException e) {
            System.out.println("Error closing the rabbit MQ connection" + e);
        }
    }
}
