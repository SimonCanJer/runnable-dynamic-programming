package org.dynamic.poc;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@Slf4j
class MappedTreeSearchTest {

   /// @Test
    public void test(){

        MappedTreeSearch inst= new MappedTreeSearch();
        boolean b=inst.addWord("test");
        assertTrue(b);
        b=inst.addWord("tost");
        assertTrue(b);
        b=inst.addWord("toster");
        assertTrue(b);
        List<String> res=inst.patternSearch("st");
        assertEquals(3,res.size());


    }
    @Test
    public void testPhone(){

        MappedTreeSearch inst= new MappedTreeSearch();
        boolean b=inst.addWord("0544245526");
        assertTrue(b);
        b=inst.addWord("0544245527");
        assertTrue(b);
        b=inst.addWord("0545990531");
        assertTrue(b);
        List<String> res;
        res=inst.patternSearch("05426");
        assertEquals(1,res.size());
        res=inst.patternSearch("054256");
        assertEquals(1,res.size());

    }

}