package cgl.iotrobots.slam.streaming;

public abstract class Constants {
    public static final String ARGS_NAME = "name";
    public static final String ARGS_LOCAL = "local";
    public static final String ARGS_DS_MODE = "ds_mode";

    // configurations
    public static final String MAP_UPDATE_INTERVAL = "map_update_interval";
    public static final String MAXURANGE = "maxUrange";
    public static final String SIGMA = "sigma";
    public static final String KERNELSIZE = "kernelSize";
    public static final String LSTEP = "lstep";
    public static final String ASTEP = "astep";
    public static final String ITERATIONS = "iterations";
    public static final String LSIGMA = "lsigma";
    public static final String OGAIN = "ogain";
    public static final String LSKIP = "lskip";
    public static final String SRR = "srr";
    public static final String SRT = "srt";
    public static final String STR = "str";
    public static final String STT = "stt";
    public static final String LINEAR_UPDATE = "linearUpdate";
    public static final String ANGULAR_UPDATE = "angularUpdate";
    public static final String TEMPORAL_UPDATE = "temporalUpdate";
    public static final String RESAMPLE_THRESHOLD = "resampleThreshold";
    public static final String PARTICLES = "particles";
    public static final String XMIN = "xmin";
    public static final String YMIN = "ymin";
    public static final String XMAX = "xmax";
    public static final String YMAX = "ymax";
    public static final String DELTA = "delta";
    public static final String LLSAMPLERANGE = "llsamplerange";
    public static final String LLSAMPLESTEP = "llsamplestep";
    public static final String LASAMPLERANGE = "lasamplerange";
    public static final String LASAMPLESTEP = "lasamplestep";

    public abstract class Fields {
        public static final String TIME_FIELD = "time";
        public static final String LASER_SCAN_TUPLE = "laser_scan";
        public static final String PARTICLE_VALUE = "particle_value";
        public static final String SENSOR_ID_FIELD = "sensorID";
        public static final String PARTICLE_VALUE_FIELD = "particleValue";
    }

    public abstract class Messages {
        public static final String BROADCAST_EXCHANGE = "slam_broadcast";
        public static final String DIRECT_EXCHANGE = "slam_direct";
        public static final String PARTICLE_ASSIGNMENT_ROUTING_KEY = "pa";
        public static final String PARTICLE_MAP_ROUTING_KEY ="pm";
    }

    public abstract class Topology {
        public static final String RECEIVE_SPOUT = "receive_spout";
        public static final String SCAN_MATCH_BOLT = "scan_match_bolt";
        public static final String RE_SAMPLE_BOLT = "re_sample_bolt";
        public static final String MAP_BOLT = "map_bolt";
        public static final String SEND_BOLD = "send_bolt";
    }
}
