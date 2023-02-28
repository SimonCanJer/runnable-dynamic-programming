package org.dynamic.poc;

import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * This class  demmostrates an example of  dynamic programming task find nodes, which are depedaant
 * critically on s given node in a graph virtual grap.
 *  The graph is  set to up by means of array of pairs of start and end point of edges.
 * {@link #setup(String[][])}
 * Graph of nodes is modeled by means of the {@link ConnectedNode}, which contains set of start and end
 * points of edges.
 * The {@link ConnectedNode} objects are mapped by start point.
 *
 */
public class CriticalDependencies {

    private final Map<String, ConnectedNode> connectedNodes = new HashMap<>();

    public ConnectedNode node(String id) {
        return connectedNodes.get(id);
    }

    /**
     * The class represents a node in a graph and has set of incoming and outgoing
     * edges.
     *   The class expose methods:
     * {@link #findCriticallyDependant()} searches for nodes, which are critically dependent
     *  on this node: becomes to be unaccessible if this node about to be removed.
     *  {@link #waitTofindCriticallyDependant()} is the same as previous, but traverses
     *  graph (starting from this node) in parallel
     */
    @Getter
    class ConnectedNode {
        Executor runner= Executors.newCachedThreadPool();
        private final String id;
        Set<String> incoming = new HashSet<>();
        Set<String> outgoing = new HashSet<>();

        ConnectedNode(String id) {
            this.id = id;
        }

        /**
         * performs traversal of graph, starting from this point, to find nodes, which becomes to be
         * inaccessible. when the current node is absent.
         * Algoritm creates visitors, which start traversal visiting of nodes, starting  from outgoing
         * edges. Visitors share  a map of counters, each of them mean number of visitor accessed
         * a node. If number of accesses eauals to number of incoming edges, then the visited node cannot be accessed
         * either than from the node , which is tested.
         * @return set of critically dependant node
         */
        public Set<String> findCriticallyDependant() {
            Map<String, AtomicInteger> mapCounters = new HashMap<>();
            CopyOnWriteArrayList<Runnable> runs = new CopyOnWriteArrayList<>();
            ConnectedNode node =this;
            Set<String> setCritical = new CopyOnWriteArraySet<>();
            node.outgoing.forEach((out) -> {
                runs.add(() -> {
                    createVisitor(runs::add, setCritical::add, mapCounters).accept(connectedNodes.get(out));
                });
            });
            while (runs.size() > 0) {
               runs.remove(0).run();
            }
            return setCritical;
        }


      /*  public Future<Set<String>> waitTofindCriticallyDependant() {
            Map<String, AtomicInteger> mapCounters = new HashMap<>();
            AtomicInteger ballancer= new AtomicInteger();
            CompletableFuture<Set<String>> pending = new CompletableFuture<>();
            ConnectedNode node =this;
            Set<String> setCritical = new CopyOnWriteArraySet<>();
            node.outgoing.forEach((out) -> {
                ballancer.incrementAndGet();
                runner.execute(() -> {
                    createVisitor((r)->{
                        ballancer.incrementAndGet();
                        runner.execute(()->{
                            r.run();
                            if(ballancer.decrementAndGet()==0){
                                pending.complete(setCritical);
                            }
                        });
                    }, setCritical::add, mapCounters).accept(connectedNodes.get(out));
                    if(0==ballancer.decrementAndGet())
                    {
                        pending.complete(setCritical);
                    }
                });

            });

            return pending;
        }*/


        public Future<Set<String>> waitTofindCriticallyDependant() {
            Map<String, AtomicInteger> mapCounters = new HashMap<>();
            AtomicInteger ballancer= new AtomicInteger();
            CompletableFuture<Set<String>> pending = new CompletableFuture<>();
            ConnectedNode node =this;
            Set<String> setCritical = new CopyOnWriteArraySet<>();
            node.outgoing.forEach((out) -> {

                createRunner(ballancer,()->{

                    createVisitor((r)->{
                        createRunner(ballancer,()->{
                            r.run();
                        },()->pending.complete(setCritical)).run();
                    }, setCritical::add, mapCounters).accept(connectedNodes.get(out));


                },()->{
                   pending.complete(setCritical);
                }).run();

            });

            return pending;
        }

        private Consumer<ConnectedNode> createVisitor(Consumer<Runnable> newVisitRun, Consumer<String> add2CriticallyDependant, Map<String, AtomicInteger> meetCounters) {
            return new Consumer<ConnectedNode>() {
                @Override
                public void accept(ConnectedNode node) {
                    AtomicInteger count = meetCounters.computeIfAbsent(node.id, (k) -> new AtomicInteger());
                    if (count.incrementAndGet() == node.incoming.size()) {
                        add2CriticallyDependant.accept(node.getId());
                    }
                     node.outgoing.forEach((out) -> {
                        Runnable next = () -> {
                            this.accept(connectedNodes.get(out));
                        };
                        newVisitRun.accept(next);
                    });
                }
            };
        }

        private Runnable createRunner(AtomicInteger ballancer,Runnable toDo, Runnable onEnd){

            return ()->{
                ballancer.incrementAndGet();
                runner.execute(()->{
                   toDo.run();
                   if(ballancer.decrementAndGet()==0){
                       onEnd.run();
                   }
                });
            };
        }

    }

    void setup(String[][] edges) {

        for (int i = 0; i < edges.length; i++) {

            ConnectedNode curr = connectedNodes.computeIfAbsent(edges[i][0], ConnectedNode::new);
            curr.outgoing.add(edges[i][1]);
            curr = connectedNodes.computeIfAbsent(edges[i][1], ConnectedNode::new);
            curr.incoming.add(edges[i][0]);

        }

    }
}
