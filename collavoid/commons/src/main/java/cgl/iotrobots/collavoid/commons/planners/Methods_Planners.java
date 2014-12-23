package cgl.iotrobots.collavoid.commons.planners;

import cgl.iotrobots.collavoid.commons.rmqmsg.Odometry_;
import cgl.iotrobots.collavoid.commons.rmqmsg.Pose_;
import cgl.iotrobots.collavoid.commons.rmqmsg.Vector3d_;
import cgl.iotrobots.collavoid.commons.rmqmsg.Vector4d_;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Methods_Planners {

    public static List<Vector2> rotateFootprint(final List<Vector2> footprint, double angle) {
        List<Vector2> result = new ArrayList<Vector2>();
        for (int i = 0; i < footprint.size(); ++i) {
            Vector2 rotated = Vector2.rotateVectorByAngle(footprint.get(i), angle);
            result.add(rotated);
        }
        return result;
    }
    // local planner related
    public static double getYaw(Vector4d_ q) {
        double q0 = q.getX();
        double q1 = q.getY();
        double q2 = q.getZ();
        double q3 = q.getW();
        //refer to roll in http://stackoverflow.com/questions/5782658/extracting-yaw-from-a-quaternion
        return Math.atan2(2.0 * (q0 * q1 + q3 * q2), q3 * q3 + q0 * q0 - q1 * q1 - q2 * q2);
    }

    public static Vector4d_ getQuaternion(Vector3d_ vec, double theta) {
        double theta_normalized = normalize_angle(theta);
        double w = Math.cos(normalize_angle(theta) / 2);        
        double s = Math.sqrt(1 - w * w) / vec.length();
        if (theta_normalized < 0)
            s = -s;
        vec.scale(s);
        return new Vector4d_(vec.getX(), vec.getY(), vec.getZ(), w);
    }

    public static double getGoalPositionDistance(Pose_ pose, double goalx, double goaly) {
        return Vector2.abs(new Vector2(pose.getPosition().getX() - goalx, pose.getPosition().getY() - goaly));
    }

    public static double getGoalOrientationAngleDifference(Pose_ pose, double goalth) {
        return getYaw(pose.getOrientation()) - goalth;
    }

    public static boolean stopped(Odometry_ base_odom, double rot_stopped_velocity_, double trans_stopped_velocity_) {
        Vector3d_ angular, linear;
        linear = base_odom.getTwist().getLinear();
        angular = base_odom.getTwist().getAngular();
        double lx = linear.getX(), ly = linear.getY(), lz = linear.getZ();
        double ax = angular.getX(), ay = angular.getY(), az = angular.getZ();
        return Math.sqrt(lx * lx + ly * ly + lz * lz) < trans_stopped_velocity_ && Math.sqrt(ax * ax + ay * ay + az * az) < rot_stopped_velocity_;
    }

    // angel related
    public static double normalize_angle_positive(double angle) {
        return ((angle % (2.0 * Math.PI)) + 2.0 * Math.PI) % (2.0 * Math.PI);
    }

    // from -PI to PI
    public static double normalize_angle(double angle) {
        double a = normalize_angle_positive(angle);
        if (a > Math.PI)
            a -= 2.0 * Math.PI;
        return a;
    }

    public static double shortest_angular_distance(double from, double to) {
        double result = normalize_angle_positive(normalize_angle_positive(to) - normalize_angle_positive(from));

        if (result > Math.PI)
            // If the result > 180,
            // It's shorter the other way.
            result = -(2.0 * Math.PI - result);

        return normalize_angle(result);
    }


    // line related
    public static Vector2 projectPointOnLine(final Vector2 pointLine, final Vector2 dirLine, final Vector2 point) {
        double r = (Vector2.dotProduct(Vector2.minus(point, pointLine), dirLine)) / Vector2.absSqr(dirLine);
        return Vector2.plus(pointLine, Vector2.scale(dirLine, r));
    }

    /*!
     *  @brief      Computes the squared distance from a line segment with the
     *              specified endpoints to a specified point.
     *  @param      a               The first endpoint of the line segment.
     *  @param      b               The second endpoint of the line segment.
     *  @param      c               The point to which the squared distance is to
     *                              be calculated.
     *  @returns    The squared distance from the line segment to the point.
     */
    public static double distSqPointLineSegment(final Vector2 a, final Vector2 b, final Vector2 c) {
        final double r = Vector2.dotProduct(Vector2.minus(c, a), Vector2.minus(b, a)) / Vector2.absSqr(Vector2.minus(b, a));

        if (r < 0.0f) {
            return Vector2.absSqr(Vector2.minus(c, a));
        } else if (r > 1.0f) {
            return Vector2.absSqr(Vector2.minus(c, b));
        } else {
            return Vector2.absSqr(Vector2.minus(c, Vector2.plus(a, Vector2.scale(Vector2.minus(b, a), r))));
        }
    }

    /*!
     *  @brief      Computes the sign from a line connecting the
     *              specified points to a specified point.
     *  @param      a               The first point on the line.
     *  @param      b               The second point on the line.
     *  @param      c               The point to which the signed distance is to
     *                              be calculated.
     *  @returns    Positive when the point c lies to the left of the line ab.
     */
    public static double signedDistPointToLineSegment(final Vector2 a, final Vector2 b, final Vector2 c) {
        return Vector2.det(Vector2.minus(a, c), Vector2.minus(b, a));
    }

    public static double left(final Vector2 pointLine, final Vector2 dirLine, final Vector2 point) {
        return signedDistPointToLineSegment(pointLine, Vector2.plus(pointLine, dirLine), point);
    }

    public static boolean leftOf(final Vector2 pointLine, final Vector2 dirLine, final Vector2 point) {
        return signedDistPointToLineSegment(pointLine, Vector2.plus(pointLine, dirLine), point) > Parameters.EPSILON;
    }

    public static boolean rightOf(final Vector2 pointLine, final Vector2 dirLine, final Vector2 point) {
        return signedDistPointToLineSegment(pointLine, Vector2.plus(pointLine, dirLine), point) < -Parameters.EPSILON;
    }

    public static double sign(double x) {
        return x < 0.0 ? -1.0 : 1.0;
    }

    public static List<Vector2> minkowskiSumConvexHull(final List<Vector2> polygon1, final List<Vector2> polygon2) {
        List<Vector2> result = new ArrayList<Vector2>();
        ConvexHullPoint[] convex_hull = new ConvexHullPoint[polygon1.size() * polygon2.size()];

        int n = 0;
        for (int i = 0; i < polygon1.size(); i++) {
            for (int j = 0; j < polygon2.size(); j++) {
                ConvexHullPoint p = new ConvexHullPoint();
                p.setPoint(Vector2.plus(polygon1.get(i), polygon2.get(j)));
                convex_hull[n++] = p;
            }
        }
        convex_hull = convexHull(convex_hull, false);
        for (int i = 0; i < convex_hull.length; i++) {
            result.add(new Vector2(convex_hull[i].getX(), convex_hull[i].getY()));
        }
        return result;
    }

    public static ConvexHullPoint[] convexHull(ConvexHullPoint[] P, boolean sorted) {
        int n = P.length, k = 0;
        ConvexHullPoint[] result = new ConvexHullPoint[2 * n];

        // Sort points lexicographically
        if (!sorted)
            Arrays.sort(P, new Comparators.VectorsLexigraphicComparator());

        //    ROS_WARN("points length %d", (int)P.size());

        // Build lower hull,
        for (int i = 0; i < n; i++) {
            while (k >= 2 && Vector2.det(
                    result[k - 2].getX() - result[k - 1].getX(),
                    result[k - 2].getY() - result[k - 1].getY(),
                    P[i].getX() - result[k - 1].getX(),
                    P[i].getY() - result[k - 1].getY()
            ) <= 0) k--;
            ConvexHullPoint pt = new ConvexHullPoint(P[i].getX(), P[i].getY());
            result[k++] = pt;
        }

        // Build upper hull
        for (int i = n - 2, t = k + 1; i >= 0; i--) {
            while (k >= t && Vector2.det(
                    result[k - 2].getX() - result[k - 1].getX(),
                    result[k - 2].getY() - result[k - 1].getY(),
                    P[i].getX() - result[k - 1].getX(),
                    P[i].getY() - result[k - 1].getY()
            ) <= 0) k--;
            ConvexHullPoint pt = new ConvexHullPoint(P[i].getX(), P[i].getY());
            result[k++] = pt;
        }


        //resize list
        ConvexHullPoint[] resultnew = new ConvexHullPoint[k];
        System.arraycopy(result, 0, resultnew, 0, k);

        return resultnew;
    }
}
