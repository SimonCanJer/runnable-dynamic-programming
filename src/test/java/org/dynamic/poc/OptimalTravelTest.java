package org.dynamic.poc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class OptimalTravelTest {

    @Test
    void optimalFromTo() {
        String[][] paths= new String[][]{{"A","B"},{"B","C"},{"C","D"},{"D","H"},{"A","F"},{"F","G"},{"G","D"}};
        int [] price={5,5,10,15,3,5,10};
        OptimalTravel travel= new OptimalTravel();
        travel.update(paths,price);
        OptimalTravel.TravelingPath node=travel.optimalFromTo("A","D");
        assertEquals(18,node.getPrice());

    }

    @Test
    void parallelFromTo() {
        String[][] paths= new String[][]{{"A","B"},{"B","C"},{"C","D"},{"D","H"},{"A","F"},{"F","G"},{"G","D"}};
        int [] price={5,5,10,15,3,5,10};
        OptimalTravel travel= new OptimalTravel();
        travel.update(paths,price);
        Future<OptimalTravel.TravelingPath> optimal=travel.optimalPathParallel("A","D");
        assertDoesNotThrow(()-> {
            assertEquals(18, optimal.get(1000000, TimeUnit.SECONDS).getPrice());
        });

    }

    @Test
    void update() {
        String[][] paths= new String[][]{{"A","B"},{"B","C"},{"C","D"},{"D","H"},{"A","F"},{"F","G"},{"G","D"}};
        int [] price={5,5,10,15,3,5,10};
        OptimalTravel travel= new OptimalTravel();

        assertDoesNotThrow(new Executable() {
            @Override
            public void execute() throws Throwable {
                travel.update(paths,price);
            }
        });

    }
}