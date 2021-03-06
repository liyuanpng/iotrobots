package cgl.iotrobots.collavoid.topologyStreaming;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import cgl.iotrobots.collavoid.commons.TimeDelayAnalysis.Constants;
import cgl.iotrobots.collavoid.commons.TimeDelayAnalysis.TimeDelayRecorder;
import cgl.iotrobots.collavoid.commons.planners.Methods_Planners;
import cgl.iotrobots.collavoid.commons.planners.Vector2;
import cgl.iotrobots.collavoid.commons.rmqmsg.Constant_msg;
import cgl.iotrobots.collavoid.commons.rmqmsg.PoseArray_;
import cgl.iotrobots.collavoid.commons.storm.Constant_storm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetMinkowskiFootprintBolt extends BaseRichBolt {
    private List<Vector2> ownFootprint;
    private Map<String, List<Vector2>> contexts = new HashMap<String, List<Vector2>>();
    private String sensorID;
    private OutputCollector collector;
    TimeDelayRecorder poseArrayDelayRecorder;

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        collector = outputCollector;
        poseArrayDelayRecorder = new TimeDelayRecorder(
                Constants.PARAMETER_DELAY,
                Constant_msg.KEY_POSE_ARRAY,
                topologyContext.getThisComponentId());
        poseArrayDelayRecorder.open(false);
    }

    @Override
    public void execute(Tuple input) {
        sensorID = (String) input.getValueByField(Constant_storm.FIELDS.SENSOR_ID_FIELD);
        ownFootprint = contexts.get(sensorID);
        if (input.getSourceComponent().equals(Constant_storm.Components.POSE_ARRAY_COMPONENT)) {
            if (ownFootprint != null) {
                poseArrayDelayRecorder.append(
                        input.getStringByField(Constant_storm.FIELDS.SENSOR_ID_FIELD),
                        input.getLongByField(Constant_storm.FIELDS.TIME_FIELD),
                        System.currentTimeMillis());
                PoseArray_ poseArray_ = (PoseArray_) input.getValueByField(Constant_storm.FIELDS.POSE_ARRAY_FIELD);
                List<Object> emit = new ArrayList<Object>();
                emit.add(input.getValue(0));
                emit.add(input.getValue(1));
                emit.add(getMinkowskiFootprint(poseArray_, ownFootprint));
                collector.emit(emit);
            }
        }
//        else if (input.getSourceStreamId().equals(Constant_storm.Streams.FOOTPRINT_OWN_STREAM))
        else {
            contexts.put(sensorID,
                    (List<Vector2>) input.getValueByField(Constant_storm.FIELDS.FOOTPRINT_OWN_FIELD));
        }
        collector.ack(input);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(
                Constant_storm.FIELDS.TIME_FIELD,
                Constant_storm.FIELDS.SENSOR_ID_FIELD,
                Constant_storm.FIELDS.FOOTPRINT_MINKOWSK_FIELD
        ));
    }

    //for test replaced the algorithm computeNewMinkowskiFootprint
    private List<Vector2> getMinkowskiFootprint(PoseArray_ poseArray_, List<Vector2> ownFootprint) {
        // in robot base frame do not need transform
        double x, y;
        List<Vector2> localization_footprint = new ArrayList<Vector2>();
        // select valid localization
        for (int i = 0; i < poseArray_.getPoses().size(); i++) {
            x = poseArray_.getPoses().get(i).getPosition().getX();
            y = poseArray_.getPoses().get(i).getPosition().getY();
            Vector2 p = new Vector2(x, y);
            if (p.VectorLength() > 0.1)
                continue;
            localization_footprint.add(p);
        }
        return Methods_Planners.minkowskiSumConvexHull(localization_footprint, ownFootprint);
    }
}
