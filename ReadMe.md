- The main task of this project is attempt to display approaches of dynamic programming, whichg are suitable for large number subelements, and, besides,
can be solved in parallel.
- Basically dynamic programming is splitting a large scale problem, by subelements of reduced size, down to atomic level and integration of results
of computation of subtasks.
- Sometimes dynamic programming is defiend as tree of overlapping subtasks of the lower complexity.
- Usually and mainly a flow of the solution represented as recursive algorithms. In the frames of this concept task is broken by elementar functional tasks,
wherer each method calls self for next iteration of compelexity decrement, and then uses results of previous computations.
- Also, a core of the idea is to  store the results of a subproblem solution on a stack so that we can use it on upper level of recursion.
- The  Febonacchi algorithm, or task of 8 queens on chess desks. Absolute majority of appoaches use recusive call of functions,
using stack as a data keeper and worlflow together. Yes, it is simple
- But recursion, as calling function from functions enlarges size of stack. and this is a realization problem large (even
not huge) number elemenst of task (for example, Febonacchi algorithm for 10000000: stack overflow);
The same is about deep tree traversal and all recursive algorithms, when next next step is in depth of invocation of the same formal logic.
- The approach, which is represented in this project , suggests  dynamic creation and execution of an executable objects/functors, while
decomposing main task by overlapped elementary operations,but (unlike in o stack recursion) the executables are run by a simple runner in the same or a parallel threads. 
- In this case  and iterative function will have additional arguments, which transmits next executable step out of its body,exports results.
- Fortunately, modern languages, as Java 8+ provide mechanisms of lambdas, pointer to function, which can be sucessfully applied for this approach
- The project demonstrates usage of the dynamic programming principle, where each of iteration is an executable code, which is encapsulated in a run,
which is delegated to a runner.
- Logical recursion is assumed and implemented in the project's example classes,
but implementation is not based on recursive call of singe stack,but as run time creation of runnables (and callbacks when needed) with
transfer control to a runner, which is the executor of the whole logics.
- This approach enables us to: void stack overflow problem and run tasks (when possible ) in parallel.
- Single thread implementation assumes collection of next stage runnables and sequential run in a cycle.
- A real business recursive business logic can require notification about underlaying recursion calls are done.
- A recursive  call of functions on the same stack get this automatically by the fact, that execution of recursive call is done.
- In the case of control transfer, which we use now, the recursive part of code will be implenmented in another part of stack (or in another stack),
   so, we need a callback. The callback is delegated down to runnable of recursive call. But each of recrusively called methods does notcall provided
   methods directly, by delegates to an external runner.
-  The class org.dynamic.poc.StackReverseDynamics demonstrates the approach as solution of the famous task of stack reverse without using any collections.
- Parallel implementation of this apprach assumes control transfer to an executor service, which launches  generated runs.
- However this apporach is applicable to model "call and forget", when caller does not need to be informed about completion of recursive stage.
- The classes   org.dynamic.poc.OptimalTravel,org.dynamic.poc.MappedTreeSearch,org.dynamic.poc.CriticalDependencies provide an examples of the  parallel apporach to search in trees and graph. Despite of they implement different algorithms, the general manner is the same: create runnable task, which generates also a runnable task in recursion and transmits stacks to executor.This way we build connected chains of problem decompostion with intermediate results merge
  
