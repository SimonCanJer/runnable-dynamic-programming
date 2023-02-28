package org.dynamic.poc;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 *   The class provides a solution of selection of travel between 2 stations with minimal price, which
 * is based on dynamical programming.
 *   Initially, station to station paths are provided by pairs of String[] and Integer, where String[]
 *  represents arrival and destination stations, and integer - price for the travel.
 *   The  paths are provied in a setup method in the {@link #update(String[][], int[])}.
 *  In order to adapt the simple data to algorithm data and classes, the input is transformed to  map
 *  of the {@link Station}, where each of stations has a collection of Pair<String[], Integer>, where
 *  each of pairs means a trip segment from current station to another.
 *    The algorithm dynamically builds a tree, starting from the destination point. The recursive flow
 *  consist of visiting  an elementary station{@link Station} on the top of a path,
 *  updating related path and repeating a new step for each of detination points of the station recursively.
 *   The recursion is implemented by creation of a rannable, which encapsulates the step and transfer  control
 *   to a parallel executor.
 *   The thread, which calls the root interface function {@link #optimalPathParallel(String, String)} is blocked
 *   Path is represented by {@link TravelingPath}, which is a linked list of path elements, where
 *  every element represents a  step of the travel. The workflow builds and compare instances of the class and
 *  selects an instance {@link TravelingPath} with the  lowest price.
 *    The class provides interface functions {@link #optimalFromTo(String, String)} and {@link #optimalPathParallel(String, String)}
 *   to perform sequential and parallel search
 */
@Slf4j
public class OptimalTravel {

/** name to station map : key is departure point**/
    Map<String, Station> mapStations = new HashMap<>();

    /** eecution service, which executed runs*/
    ExecutorService parallelExecutor = Executors.newCachedThreadPool();

    /**
     *  the class representates a station, from which
     *  travel departs to another
     */
    static class Station {

        /** station's name */
        String name;

        /**  set of stations which are directly accessible  **/
        Set<Pair<String[], Integer>> departures = new HashSet<>();

        public Station(String s) {
            name = s;
        }
    }

    /**
     * The public class represents a traveling step, which is achieved by
     * moving from station to station during any path and price to arrive.
     * The class assumes that there a part of travel steps is done and contains last
     * segment of traveling {@link #arrivalFrom}, which is done to arrive.
     * Thus, if #arrival from is a pair like {10,{"A","B"}}, then the step represents
     * an arrival in the station "B" from "A" for 10 ($USD- for instance).
     * An instance of the class represents and path of traveling, so eny step has a previous.
     * The class implements the Compare interface as well.
     */
    @Getter
    static public class TravelingPath implements Comparable<TravelingPath> {

        /** price to arrive up to this  along the whole path **/
        private int price;

        /** previous step of traveling **/
        private final TravelingPath prev;

        /** stations, which are last segment of traveling with price for the segment **/
        private final Pair<String[], Integer> arrivalFrom;

        /**
         * The method lists segments of traveling and represents it as
         * list of departure-arrival stations with price for the segment
         * @return  - list of station to station segments and price for it
         */

        public List<Pair<String[], Integer>> travelPath() {
            LinkedList<Pair<String[], Integer>> path = new LinkedList<>();
            TravelingPath curr = this;
            while (curr != null) {
                if (curr.arrivalFrom != null) {
                    path.addFirst(curr.arrivalFrom);
                    curr=curr.prev;
                }
            }
            return path;
        }

        /**
         * Constructor
         */
        TravelingPath() {

            price = 0;
            prev = null;
            arrivalFrom = null;
        }

        /**
         * Constructor , which initiates the instance on the top of previous
         * @param node  -  previous node
         * @param step  -  related step (departure, destination price)
         */
        TravelingPath(TravelingPath node, Pair<String[], Integer> step) {

            price += node.price + step.getRight();
            prev = node;
            this.arrivalFrom = step;
        }

        @Override
        public int compareTo(TravelingPath o) {

            return price - o.price;
        }

    }

    /**
     * The method is visitor, and performs the follwing things, while visiting a station on
     * a top of previous path.
     *  During a visit, the visitor enumerates outgoing (departure) directions of this station,
     *  creates new instancces of {@link TravelingPath} for each of outgoing directions, with updated prices,
     *  and:
     *    if end of the destination is the end point of the trip, then it adds the new, updated step to list of travels,
     *    else: creates a new updated step on the top of the current and runnable, where the method will be
     *    called for next station on the top of currently crreated setp.
     *
     * @param station -          a station, which is built from input {@link Station}
     * @param prev    -          a previous {@link TravelingPath}
     * @param selected-          collection of {@link TravelingPath}, which lead to destination point
     * @param targetNode-        a station, where the trip must be ended.
     * @param nextStepListener   a consumer which accepts a runnable for the next step.
     */

    private void visitStation(Station station, TravelingPath prev, List<TravelingPath> selected, String targetNode, Consumer<Runnable> nextStepListener) {


        for (Pair<String[], Integer> pair : station.departures) {
            TravelingPath next = new TravelingPath(prev, pair);
            if (targetNode.equals((pair.getLeft()[1]))) {
                selected.add(next);
                continue;
            }
            if (mapStations.containsKey(pair.getLeft()[1])) {
                Station nextStation = mapStations.get(pair.getLeft()[1]);
                nextStepListener.accept(() -> {
                    visitStation(nextStation, next, selected, targetNode, nextStepListener);
                });
            }
        }
    }

    /**
     * thist method is not a part of business logic, but it is an important part of parallel run control while
     * executing a workflow.
     * The method runs a runnable, which encapsulates a stage execution, and decresed and atomic requestExecuteBallance, which
     * really is an execution ballance between runnable sent to an execution and execured runnable.
     * When  the requestExecuteBallance becomes to be 0 after a run executed, it means that all denerated runs are executed and
     * the related  completeable future can be completed with result.
     * @param future                  - future to complete on last executed run.
     * @param list                    - list of {@link TravelingPath}which are on top of paths
     * @param requestExecuteBallance  - counter of not executed in a parallel executor
     * @param r                       - runnable to execute
     */
    private void performTrailRunComplete(CompletableFuture<TravelingPath> future, List<TravelingPath> list, AtomicInteger requestExecuteBallance, Runnable r) {

        try {
            log.debug("completing step, requestExecuteBallance before decrement = {}", requestExecuteBallance.get());
            r.run();
        }
        catch(Throwable e){

            log.error(" exception occured  while running a step {}",e.getMessage());

        }
        finally{
            if (requestExecuteBallance.decrementAndGet() == 0) {
                log.debug("completing step, requestExecuteBallance == 0, list size=={}", list.size());
                if (list.size() == 0) {
                    future.complete(null);

                }
                else {
                    Collections.sort(list);
                    future.complete(list.get(0));
                }

            }
            log.debug("completing step, requestExecuteBallance after decrement = {}", requestExecuteBallance.get());
        }

    }


    /**
     *  searches for cheapest route from a point to point
     * @param from        -  departure point
     * @param to          -   destination point
     * @return            -   the optimal part
     */

    public TravelingPath optimalFromTo(String from, String to) {
        List<TravelingPath> optim = new CopyOnWriteArrayList<>();
        if (mapStations.containsKey(from) && mapStations.containsKey(to)) {
            List<Runnable> iterations = new CopyOnWriteArrayList<>();
            TravelingPath p = new TravelingPath();
            iterations.add(() -> {
                visitStation(mapStations.get(from), p, optim, to, iterations::add);
            });
            while (iterations.size() != 0) {
                iterations.remove(0).run();
            }
            Collections.sort(optim);
        }
        if (optim.size() > 0)
            return optim.get(0);
        return null;
    }

    /**
     * This method implements dynamic programming concept by generating Runnable per path search step, sending
     * the runnable to a parallel executor {@link #parallelExecutor}
     * @param from         -  station to start traveling from
     * @param to           -  detionation station of traveling
     * @return             -  {@link Future} object accepting top level element in traveling steps.
     */

    public Future<TravelingPath> optimalPathParallel(String from, String to) {

        CompletableFuture<TravelingPath> future = new CompletableFuture<>();
        if (mapStations.containsKey(from) && mapStations.containsKey(to)) {
            TravelingPath p = new TravelingPath();
            List<TravelingPath> optimal = new CopyOnWriteArrayList<>();
            AtomicInteger counter = new AtomicInteger();
            Runnable initail=() -> {
                counter.incrementAndGet();
                log.debug("parellel start: initially calling acceptNode(), counter {} ", counter.get());
                visitStation(mapStations.get(from), p, optimal, to, (r) -> {
                    counter.incrementAndGet();
                    parallelExecutor.submit(() -> {
                        log.debug("parallel run: r.run() ");
                        performTrailRunComplete(future, optimal, counter,r);
                    });
                });

            };
            parallelExecutor.submit(()->{
                performTrailRunComplete(future,optimal,counter,initail);
            });
        }
        return future;

    }

    /**
     *  updates traveling information by providing direct routes and prices in a paralle arrays
     * @param directRouts   - direct routes (departures) from this station to another    -
     * @param coast         - coast for this route
     */
    public void update(String[][] directRouts, int[] coast) {
        assert (directRouts.length == coast.length);
        for (int i = 0; i < directRouts.length; i++) {

            Station station = mapStations.computeIfAbsent(directRouts[i][0], Station::new);
            station.departures.add(Pair.of(directRouts[i], coast[i]));
        }

    }



}
