package org.dynamic.poc;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class CriticalDependenciesTest {

    @Test
    public void test() throws ExecutionException, InterruptedException {
        CriticalDependencies dependencies = new CriticalDependencies();
        dependencies.setup(new String[][]{{"A","F"},{"F","K"},
                {"B","D"},{"B","F"},{"D","G"},{"C","D"},{"C","L"},{"L","K"}});

        CriticalDependencies.ConnectedNode node= dependencies.node("B");
        assertNotNull(node);
        Set<String> nodes=node.findCriticallyDependant();
        assertEquals(1, nodes.size());
        assertEquals("G",nodes.iterator().next());
        Future<Set<String>> f =node.waitTofindCriticallyDependant();
        nodes=f.get();
        nodes=null;




    }

}