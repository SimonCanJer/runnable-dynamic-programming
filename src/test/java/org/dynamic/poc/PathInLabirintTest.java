package org.dynamic.poc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PathInLabirintTest {

    @Test
    void goToTarget() {
    }

    @Test
    void runToTarget() {
        PathInLabirint test= new PathInLabirint(0,0,3,3);
        test.withWall(new int[][]{{2,0},{2,1},{2,2}});
        List<List<int[]>> paths= test.runToTarget(3,3);
        assertTrue(paths.size()>0);
    }
}