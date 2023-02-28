package org.dynamic.poc;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class provides to build a
 */
@Slf4j
public class MappedTreeSearch {
    /**
     * The value means a ration between number of matches found in search to length of pattern
     * For example : 'fund" word's match will be 0.75 in the pattern "find"
     */
    private final float patternThreshold = 0.2f;

    /**
     * ratio between
     */
    private final float matchThreshold= 1.0f;
    interface IEncoding{
        String encode(String s);

    }

    /**
     * The interface declares functionality of search result collection.
     */
    private interface ISearchCollector {

        /** threshold to accept a word as candidate */
        float matchThreshold();

        float patternThreashold();

        void add(String val, float freq);



    }

    static private class WordFound implements Comparable<WordFound> {

        private final String word;
        private final float frequency;

        private WordFound(String word, float weight) {
            this.word = word;
            this.frequency = weight;
        }


        @Override
        public int compareTo(WordFound other) {
            if (other == null) {
                return 0;
            }
            return (int) (other.frequency * 1000 - frequency * 1000);
        }
    }

    /**
     * The class represents logics of a pattern search progress over a tree.
     * The algoritgm is :
     * if a key character of dictionary map matches  a characterof the pattern on
     * current index, which is to be checked for match, then increment index and match
     */
    static private class SearchProgress implements Cloneable {

        int match;
        private final char[] searchedPattern;

        private SearchProgress(char[] search) {
            this.searchedPattern = search;
        }

        boolean compare(char c) {
            if (match == searchedPattern.length)
                return false;
            if (searchedPattern[match] == c) {
                match++;
            }
            return true;
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                log.error("error occured while cloning {}", e.getMessage());
                return null;
            }

        }

    }

    /**
     * The class computes ballance between number of added and already executed tasks, locks
     * initial thread of workflow untill all tasks are  executed by parallel executors
     */
    private static class LockingExecutionBallancer {
       /** a runnable, which must be executed on end of all tasks, when not null **/
        private final Runnable onEnd;

        /** is ballancer active? Can be a situation, when all tasks are executed in parallel before
         *  {@link #lock(long)} called.
         */
        private boolean active= true;
        /** number of tasks, which still a re nore executed */
        AtomicInteger ballance = new AtomicInteger();

        /** constructor which gets finally executed runnable **/

        private LockingExecutionBallancer(Runnable onEnd) {
            this.onEnd = onEnd;

        }

/** adding counter of not executed tasks **/
        private void add() {
            ballance.incrementAndGet();
        }

        /**
         * lock a calling thread untill all tasks are executed
         * @param msc        - time of locking
         */
        void lock(long msc){
            if(active){
                synchronized (this){
                    if(active){
                        synchronized (this){
                            try {
                                wait(msc);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }

        /**
         * reacts on a task execution.
         * Decrements the {@link #ballance} counter, notifies, when all
         * tasks are executed and calls a runnable , which must be  executed on end.
         * Is not to be called in a thread, which calls {@link #lock(long)}
         */
        private void onExecuted() {
            if (ballance.decrementAndGet() == 0) {
                synchronized (this) {
                    this.notifyAll();
                    active=false;
                }
                if (onEnd != null) {
                    onEnd.run();
                }
            }
        }
    }

    /**
     * Execution unit, which is sent to a paralle executor, accepts
     * a runnable, which is a business logic unit to be executed currently.
     */
    static class Task implements Runnable {
        private final LockingExecutionBallancer ballancer;
        private final Runnable execute;

        protected Task(LockingExecutionBallancer ballancer, Runnable execute) {
            this.ballancer = ballancer;
            ballancer.add();
            this.execute = execute;
        }

        @Override
        public void run() {
            try {
                execute.run();

            } catch (Throwable t)
            {

            } finally {
                ballancer.onExecuted();
            }

        }
    }

    /**
     * A tree node in the dictionary.
     */
    static class Node {
        AtomicInteger count= new AtomicInteger();

        Consumer<Task> taskAdder;
        private final Map<Character, Node> nodes = new ConcurrentHashMap<>();
        private String value;
        private final Set<String> decoded = new HashSet<>();

        Node(Consumer<Task> taskAdder) {
            this.taskAdder = taskAdder;
        }

        @Override
        public String toString(){
            return String.valueOf(value)+" "+nodes.toString();

        }

        /**
         * The method iterates adding a word (which may be encoded) to the dictionary.
         * It checks, whether the current step should add a word to the tree of
         * dictionary: index of current character is the last index of the word and adds the word as a last node,
         * otherwise it generates a new (next node) and delegates control to next task, which
         * will repeate the procedure recursively.
         *
         * @param blockingBallancer - instance of {@link LockingExecutionBallancer}
         * @param value             - word value to add (it may be a code of word!)
         * @param orig              - original value of the word.
         * @param index             - index of current character
         *
         */

        public void add(LockingExecutionBallancer blockingBallancer, String value, String orig, int index) {
            if (index == value.length() - 1) {
                taskAdder.accept(new Task(blockingBallancer, () -> {
                    this.value =value;
                    if(orig!=null){
                        decoded.add(orig);
                    }
                }));
            } else {
                Node n = nodes.computeIfAbsent(value.charAt(++index), (k) -> {
                    return new Node(taskAdder);
                });
                int indexF = index;
                taskAdder.accept(new Task(blockingBallancer, () -> {
                    n.add(blockingBallancer, value,orig, indexF);
                }));
            }
        }

       synchronized void search(LockingExecutionBallancer b, SearchProgress f, ISearchCollector collector) {
            if(count.incrementAndGet()>1){
                log.debug("count >1");
            }

            if (value != null) {

                float match = f.match;
                match/=f.searchedPattern.length;
                float patternThreashold= f.searchedPattern.length;
                patternThreashold/=value.length();
                float matchF=match;
                if (match>= collector.matchThreshold() && patternThreashold<1f&&patternThreashold>collector.patternThreashold()) {
                    log.debug("to collector : value {} size={}", value, nodes.size());
                   if(decoded.size()==0)
                       collector.add(value, matchF);
                   else
                       decoded.forEach((k)->collector.add(k,matchF));
                }
            }
            nodes.forEach((k, v) -> {
                SearchProgress next = (SearchProgress) f.clone();
                next.compare(k);
                taskAdder.accept(new Task(b, () -> {
                    v.search(b, next, collector);
                }));

            });
        }
    }

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private IEncoding encoding;
    Node root = new Node((t) -> {
        executor.submit(t::run);

    });

    public void setEncoding(IEncoding encoding){

        this.encoding = encoding;
    }

    public boolean addWord(String word) {
        String orig=null;
        String encoded= word;
        if(encoding!=null){
            encoded= encoding.encode(word);
            orig=word;
            }
        LockingExecutionBallancer b = new LockingExecutionBallancer(() -> {
        });
        root.add(b, word, orig,-1);
        b.lock(1000000);
        log.debug("WORDS AFTER ADDED {} is {}",word,root.toString());

        return true;
    }

    /**
     * This interface method  performs parallel  search for a pattern in dictionary
     * @param pattern - a pattern to search
     * @return        - list of words, which are found by pattern.
     */

    public List<String> patternSearch(String pattern) {
        final LockingExecutionBallancer b = new LockingExecutionBallancer(null);
        List<WordFound> found = new CopyOnWriteArrayList<>();

        root.search(b, new SearchProgress(pattern.toCharArray()), new ISearchCollector() {

            @Override
            public float matchThreshold() {
                return matchThreshold;
            }

            @Override
            public float patternThreashold() {
                return patternThreshold;
            }

            @Override
            public void add(String val, float freq) {

                found.add(new WordFound(val, freq));

            }
        });
        b.lock(1000000);
        Collections.sort(found);
        return found.stream().map((wf) -> wf.word).collect(Collectors.toList());
    }

    ;

    public void patternSearch(String fragment, Consumer<List<String>> dataAcceptor) {

        List<WordFound> found = new CopyOnWriteArrayList<>();
        LockingExecutionBallancer b = new LockingExecutionBallancer(() -> {
            Collections.sort(found);
            dataAcceptor.accept(found.stream().map(wf -> wf.word).collect(Collectors.toList()));
        });

        root.search(b, new SearchProgress(fragment.toCharArray()), new ISearchCollector() {

            @Override
            public float matchThreshold() {
                return matchThreshold;
            }

            @Override
            public float patternThreashold() {
                return patternThreshold;
            }

            @Override
            public void add(String val, float freq) {

                found.add(new WordFound(val, freq));

            }
        });

    }

    ;

}
