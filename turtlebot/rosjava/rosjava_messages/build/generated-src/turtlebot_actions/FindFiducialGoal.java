package turtlebot_actions;

public interface FindFiducialGoal extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "turtlebot_actions/FindFiducialGoal";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\n#goal definition\nuint8   CHESSBOARD = 1\nuint8   CIRCLES_GRID = 2\nuint8   ASYMMETRIC_CIRCLES_GRID =3\n\nstring    camera_name       # name of the camera \nuint8     pattern_width     # number of objects across\nuint8     pattern_height    # number of objects down\nfloat32   pattern_size      # size the object pattern (square size or circle size)\nuint8     pattern_type      # type of pattern (CHESSBOARD, CIRCLES_GRID, ASYMMETRIC_CIRCLES_GRID)\n";
  static final byte CHESSBOARD = 1;
  static final byte CIRCLES_GRID = 2;
  static final byte ASYMMETRIC_CIRCLES_GRID = 3;
  java.lang.String getCameraName();
  void setCameraName(java.lang.String value);
  byte getPatternWidth();
  void setPatternWidth(byte value);
  byte getPatternHeight();
  void setPatternHeight(byte value);
  float getPatternSize();
  void setPatternSize(float value);
  byte getPatternType();
  void setPatternType(byte value);
}