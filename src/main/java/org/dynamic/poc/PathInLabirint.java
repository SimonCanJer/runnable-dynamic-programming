package org.dynamic.poc;

import lombok.NonNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * search path in labirint
 */
public class PathInLabirint {
    /* list of passed nodes*/
    //Set<Long> passed = new CopyOnWriteArraySet<>();
    /**
     * Obstacle points coordinate
     **/
    private final Set<Long> wall = new HashSet<>();

    /**
     * left bound
     **/
    private final int xLeft;

    /**
     * right bound
     **/
    private final int xRight;

    /**
     * start point's x coordinate
     */

    private int startX = 0;

    /**
     * start point's y coordinate
     **/
    private int startY = 0;

    /**
     * top's  coordinate
     **/
    private final int yTop;

    /**
     * bottom's y coordinate
     */
    private final int yBottom;

    /**
     * the internal class performs a  step to destination point and does
     * the following {@link #nextStepTo(int, int, List)}
     * 1. checks whether  destination point achieved and asks to delegate control to previous step
     * 2. if not 1, then tries to select optimal direction for next step:
     * (selects maximally suitable vector on movement on next step, regarding position of detination point:
     * closest to vector to destination point, consudering that coordinates may be varied by +/- 1)
     * 3. Creates next step and delegated control to it.
     */
    private class Step {

        /**
         * current point coordinates
         */
        private final int x, y;

        /**
         * listener for next run: set when we want to use runner concept
         */
        private final Consumer<Runnable> nextRunAcceptor;

        /**
         * previous step
         **/

        final Step prev;

        /**
         * listener to accepts path
         **/
        private final @NonNull BiConsumer<Step, List<List<int[]>>> onFound;

        /**
         * possible directions of next step : all steps around the current point
         **/

        private final List<int[]> directions = new CopyOnWriteArrayList<>();

        {
            for (int i = -1; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    if (i == j && j == 0)
                        continue;
                    directions.add(new int[]{i, j});

                }
            }
        }


        private Step(int x, int y, Consumer<Runnable> nextRunAcceptor, Step prev, @NonNull BiConsumer<Step, List<List<int[]>>> onFound) {
            this.x = x;
            this.y = y;
            this.nextRunAcceptor = nextRunAcceptor;
            this.prev = prev;
            this.onFound = onFound;
        }

        /**
         * the method performs  recursive generation (but not execution ) of next step instance with
         * new coordinates, removes  selected coordinates from directions list
         *
         * @param xT        -   target  X
         * @param yT        -   target  Y
         * @param collector -   collector list
         * @return -    next Step
         */

        protected Step nextStepTo(int xT, int yT, List<List<int[]>> collector) {

            Step next = null;
            if (x == xT && y == yT) {
                onFound.accept(this, collector);
            } else
                next = nextSteps(xT, yT);
            if (next == null)
                next = prev;
            Step nextF = next;
            if (nextRunAcceptor != null && nextF != null)
                nextRunAcceptor.accept(() -> {
                    nextF.nextStepTo(xT, yT, collector);
                });
            return next;

        }

        /**
         * computes next step to target
         *
         * @param xT - x of target
         * @param yT - y of target
         * @return - next step
         */

        protected Step nextSteps(int xT, int yT) {
            Step next = null;
            float normalizer = (float) Math.sqrt(2);
            float dx = xT - x;
            float dy = yT - y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            dx /= dist;
            dy /= dist;
            while (directions.size() > 0) {

                double deviation = Double.MAX_VALUE;
                int[] selected = null;
                for (int[] pair : directions) {
                    float devX = dx - pair[0] / normalizer;
                    float devY = dy - pair[1] / normalizer;
                    float devCurr = (float) Math.sqrt(devX * devX + devY * devY);
                    if (devCurr < deviation) {
                        deviation = devCurr;
                        selected = pair;
                    }
                }
                if (selected == null) {
                    break;
                }
                int nextX = x + selected[0];
                int nextY = y + selected[1];
                long lCode = ((long) nextX) << 32 | nextY;
                directions.remove(selected);
                if (wall.contains(lCode))
                    continue;
                if (nextX > xRight || nextX < xLeft || nextY > yBottom || y < yTop)
                    continue;
                next = new Step(nextX, nextY, nextRunAcceptor, this, onFound);
                break;
            }
            return next;
        }

    }

    /**
     * collect paths after getting from step
     *
     * @param step          -  final step
     * @param collectorList -  list if paths
     */

    private void collectPaths(Step step, List<List<int[]>> collectorList) {

        Step curr = step;
        LinkedList<int[]> currPath = new LinkedList<>();
        while (curr != null) {
            currPath.addFirst(new int[]{curr.x, curr.y});
            curr = curr.prev;
        }
        collectorList.add(currPath);
    }

    public List<List<int[]>> goToTarget(int x, int y) {
        List<List<int[]>> allPaths = new CopyOnWriteArrayList<>();
        Step currPoint = new Step(startX, startY, null, null, this::collectPaths);
        while (currPoint != null) {
            currPoint = currPoint.nextStepTo(x, y, allPaths);
        }
        return allPaths;
    }

    public List<List<int[]>> runToTarget(int x, int y) {
        List<Runnable> runs = new CopyOnWriteArrayList<>();
        List<List<int[]>> allPaths = new CopyOnWriteArrayList<>();
        Consumer<Runnable> runAcceptor = runs::add;
        Step currPoint = new Step(startX, startY, runAcceptor, null, this::collectPaths);
        runs.add(() -> {
            currPoint.nextStepTo(x, y, allPaths);
        });
        while (runs.size() > 0) {
            runs.remove(0).run();
        }

        return allPaths;
    }

    public PathInLabirint(int xLeft, int yTop, int width, int height) {

        this.xLeft = xLeft;
        this.yTop = yTop;
        xRight = xLeft + width;
        yBottom = yTop + height;

    }

    public PathInLabirint withWall(int[][] wallPoints) {

        for (int[] wallPoint : wallPoints) {

            long code = wallPoint[0];
            code <<= 32;
            code |= wallPoint[1];
            wall.add(code);

        }
        return this;

    }

    public PathInLabirint startingFrom(int x, int y) {

        startX = x;
        startY = y;
        return this;

    }



}
