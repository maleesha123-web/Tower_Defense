package com.mathtowerdefense.engine;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * The game map: a fixed winding path from the spawn portal to the base plus
 * decorative ground colours. Rendering and movement both use these logical
 * map coordinates (0..MAP_WIDTH x 0..MAP_HEIGHT).
 */
public final class GameMap {

    public static final int MAP_WIDTH = 1000;
    public static final int MAP_HEIGHT = 620;

    // Path waypoints (spawn -> base).
    private static final double[][] WAYPOINTS = {
            { -40, 80 },   // off-screen entry
            { 180, 80 },
            { 180, 220 },
            { 430, 220 },
            { 430, 110 },
            { 720, 110 },
            { 720, 330 },
            { 300, 330 },
            { 300, 470 },
            { 640, 470 },
            { 640, 560 },
            { 890, 560 },  // base entrance
            { 945, 560 }   // base centre
    };

    /** Ground / path colours (decorative background). */
    public static final Color GRASS_COLOR = new Color(30, 62, 48);
    public static final Color GRASS_DARK = new Color(24, 50, 40);
    public static final Color PATH_COLOR = new Color(148, 116, 78);
    public static final Color PATH_EDGE = new Color(108, 82, 54);
    public static final Color BASE_COLOR = new Color(200, 60, 60);

    private final List<Point2D.Double> waypoints;
    private final Path2D.Double pathOutline;

    public GameMap() {
        Point2D.Double[] pts = new Point2D.Double[WAYPOINTS.length];
        for (int i = 0; i < WAYPOINTS.length; i++) {
            pts[i] = new Point2D.Double(WAYPOINTS[i][0], WAYPOINTS[i][1]);
        }
        this.waypoints = List.of(pts);

        Path2D.Double p = new Path2D.Double();
        p.moveTo(pts[0].x, pts[0].y);
        for (int i = 1; i < pts.length; i++) {
            p.lineTo(pts[i].x, pts[i].y);
        }
        this.pathOutline = p;
    }

    /** Shared waypoint list handed to every enemy (never mutated). */
    public List<Point2D.Double> getWaypoints() {
        return waypoints;
    }

    /** Where enemies appear. */
    public Point2D.Double getSpawnPoint() {
        return waypoints.get(1);
    }

    /** Where the player base sits (last waypoint). */
    public Point2D.Double getBasePoint() {
        return waypoints.get(waypoints.size() - 1);
    }

    /** Center of the path segment 0-1, used to draw the spawn portal. */
    public Point2D.Double getPortalPoint() {
        return waypoints.get(0);
    }

    /** Centre of the path (for painting the road stroke). */
    public Path2D.Double getPathOutline() {
        return pathOutline;
    }

    /**
     * True when a tower candidate position overlaps the road.
     *
     * The waypoint list is an OPEN path, so fill-based hit tests
     * (Shape.contains) are invalid here: they implicitly close the polygon
     * and reject most of the map. This method strokes the path with the
     * road width instead - the mathematically correct road-proximity test.
     *
     * @param x candidate centre x
     * @param y candidate centre y
     * @param roadHalfWidth half thickness of the painted road
     */
    public boolean isOnPath(double x, double y, double roadHalfWidth) {
        java.awt.Shape road = new java.awt.BasicStroke(
                (float) (roadHalfWidth * 2),
                java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND).createStrokedShape(pathOutline);
        return road.contains(x, y);
    }

    /** True when the position is too close to an existing tower. */
    public boolean isInsideMap(double x, double y, double margin) {
        return x >= margin && x <= MAP_WIDTH - margin
                && y >= margin && y <= MAP_HEIGHT - margin;
    }
}
