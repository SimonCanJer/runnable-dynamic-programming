package org.dynamic.poc;


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * This class demonstrates the dynamic programming apporach to sorting arrays, which is based
 * not on direct call of recisive methods but generation of runnable tasks, which just
 * executed way of runner.
 *
 */

public class Sort {

    /**
     * public method which is to be called for sorting
     * @param list               -   a list of objects to be sorted.
     * @param <T>                -   a type of sorting
     */
    static public <T extends Comparable<T>> void binarySort(List<T> list) {
        List<Runnable> runs = new CopyOnWriteArrayList<>();
        runs.add(() -> {
            splitAndMerge(list, 0, list.size() - 1, runs::add, () -> {
            });
        });
        while (runs.size() > 0) {
            for (Runnable r : runs) {
                r.run();
                runs.remove(r);
            }
        }

    }

    /**
     * This is  the crucial method or the whole mechanism.
     * It words quite simply:
     * 1- divides interval of srting by 2 and, before delegating execution to newly created task,
     * 2- it creates call back to the child tasks,to be called for merging.
     * 3. Creates a new recursive tasks, for every one of intervals.
     * 4. Calls parent's callback when merge iss done.
     * @param list                 - list  to be sorted
     * @param lower                - lower index to start processing
     * @param upper                - upper index to stop porcessing
     * @param executorSink         - callback to add new task to executor
     * @param prevCallback         - callback to tring to the previous /parent task to continue processing/merge
     * @param <T>                  - type of element of tyhe array
     */
    static private <T extends Comparable<T>> void splitAndMerge(List<T> list, int lower, int upper, Consumer<Runnable> executorSink, Runnable prevCallback) {
        if (upper - lower <= 1) {
            merge(list, lower, upper, prevCallback);
            return;
        }
        int middle = (lower + upper) / 2;
        AtomicInteger counter = new AtomicInteger(0);
        Runnable callback = () -> {
            if (counter.incrementAndGet() == 2)
                merge(list, lower, upper, prevCallback);
        };
        executorSink.accept(() -> {
            splitAndMerge(list, lower, middle, executorSink, callback);
            splitAndMerge(list, middle + 1, upper, executorSink, callback);
        });

    }


    static private <T extends Comparable<T>> void merge(List<T> list, int lower, int upper, Runnable prevCallback) {
        if (upper - lower <= 1) {
            if (upper - lower == 1) {
                T temp = list.get(lower);
                if (temp.compareTo(list.get(upper)) > 0) {
                    list.set(lower, list.get(upper));
                    list.set(upper, temp);
                }
            }
            prevCallback.run();
            return;
        }
        int middle = (upper + lower) / 2 + 1;
        int indexLower = lower;
        int indexUpper = middle;
        int common = 0;
        Object[] summary = new Object[upper - lower + 1];
        while (indexLower < middle && indexUpper <= upper) {
            if (list.get(indexLower).compareTo(list.get(indexUpper)) <= 0) {
                summary[common++] = list.get(indexLower++);
            } else
                summary[common++] = list.get(indexUpper++);
        }
        int remainingStart = indexLower;
        int remainingEnd = middle;
        if (remainingStart == middle) {
            remainingStart = indexUpper;
            remainingEnd = upper;
        }
        for (int i = remainingStart; i < remainingEnd; i++) {

            summary[common++] = list.get(i);

        }
        for (int i = 0; i < summary.length; i++) {
            list.set(lower + i, (T) summary[i]);
        }
        prevCallback.run();

    }


}
