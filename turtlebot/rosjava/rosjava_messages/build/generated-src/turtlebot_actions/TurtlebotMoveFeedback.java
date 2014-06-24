package turtlebot_actions;

public interface TurtlebotMoveFeedback extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "turtlebot_actions/TurtlebotMoveFeedback";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\n#feedback\nfloat32 turn_distance\nfloat32 forward_distance\n\n";
  float getTurnDistance();
  void setTurnDistance(float value);
  float getForwardDistance();
  void setForwardDistance(float value);
}