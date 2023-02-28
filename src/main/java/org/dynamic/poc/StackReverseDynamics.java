package org.dynamic.poc;

import java.util.Stack;
import java.util.function.Consumer;

/**
 * The class  performs solution of the famous task of a Stack reverse without another collections,
 * but without also usage of process as a temproral shade for values keeping and makes the solution applicable
 * for even huge stack sizes.
 * The solution uses dynamic programming apprach, but unlike recursive calls generates a
 * primitive runnable and transfers control to it. Basically, the algorithm has the same logics,s
 * as any recursion, but, instead of calling a function inside self, the workflow envelops the function call
 * by a created runnable and delegates furthering execution to common runner. The rest of the body of the function
 * flow will be called way of callback to resume control execution, when recursion is done. The callback is passed also
 * to the recursion's function as one of parameters.
 *
 * @see #runDelegatedControl(Runnable[])
 * @see #reverseStep(Stack, int, int, Object[], Consumer, Runnable)
 */
@SuppressWarnings("unchecked")
public class StackReverseDynamics {


    /**
     * Stack reverse algorithm
     *
     * @param stack - a stack to be reversed
     * @param <T>   -  a type of stack's element
     */
    static public <T> void reverseStack(Stack<T> stack) {
        int size = stack.size();
        T[] push = (T[]) new Object[1];
        Runnable[] currentRun = new Runnable[1];

        /*
        running the algorithm of stack reverse.  We sequentially  replace an element of the stack by
        element on the stack bottom. Algorithm pops elements from stack,keeping then in allocated
        (see the implementation) runs, then pushes back to stack. except to the lowest element, which will be pushed after
        element, which is permuted now (i in cycle). Make an experiment in the mind. When stack size is
        1, then no steps must be done. When 2, only one.
        Bellow,each one step in the cycle creates an initial run, which calls business logic function
         */

        for (int i = 0; i < size - 1; i++) {
            int indexF = i;
            currentRun[0] = () -> {
                reverseStep(stack, 0, indexF, push, (r) -> currentRun[0] = r, () -> {
                });
            };
            runDelegatedControl(currentRun);
        }

    }

    private static void runDelegatedControl(Runnable[] currentRun) {
        while (true) {
            Runnable r = currentRun[0];
            if (r == null)
                break;
            currentRun[0] = null;
            r.run();
        }
    }

    /**
     * The method maked the whole sense of the algorithm.
     * Basically the function decomposes a workflow step by number of executable runs, which are also called
     * by external runner
     *
     * @param stack                -    the stack is reversed currently.
     * @param currDepth            -    current depth of stack, whch
     * @param depthToChange        -    depth of stack, where element must be replaced by bottom  element
     * @param push                 -    element, which should be pushed after underlying recursion is done and the element,
     *                             which is pop currently is pushed back
     * @param runAcceptor          -    consumer, which accepts  next run, where workflow control is delegated tro.
     * @param previousStepCallback -    a callback from the previous step (also will be transferred to runner)                      -
     * @param <T>                  -    and element of the stack
     */
    static private <T> void reverseStep(Stack<T> stack, int currDepth, int depthToChange, T[] push, Consumer<Runnable> runAcceptor, Runnable previousStepCallback) {

        if (stack.size() == 0)
            return;
        T t = stack.pop();// extract element in any case (finally we must access
        if (stack.size() == 0) {// if the pop element is the last in the stack, then do not push it
            // immediately, it will be pushed back on a related upper level
            push[0] = t;
            runAcceptor.accept(previousStepCallback);// now parent run must be notified about
            // the step is done. But not directly, but way of runner: otherwise it would
            // cause recursive calls, that we are avoiding
            return;
        }
        // generate logical recursion as run and delegate control to runner
        runAcceptor.accept(() -> {
            reverseStep(stack, currDepth + 1, depthToChange, push, runAcceptor, () -> {
                stack.push(t);
                if (currDepth == depthToChange) {
                    stack.push(push[0]);
                }
                runAcceptor.accept(previousStepCallback);// after recursive logic is executed by runner
                // parent run must be called back, but not directly:
                // callback run is delegated to runner to avoid recursion
            });
        });
    }
}
