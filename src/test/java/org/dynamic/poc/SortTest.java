package org.dynamic.poc;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.*;
import static org.junit.jupiter.api.Assertions.*;

class SortTest {

    private void assertSort(List<Integer> list){
        int min=Integer.MIN_VALUE;
        for(int i=0;i< list.size();i++){
            assertTrue(min<list.get(i));
            min=list.get(i);
        }

    }
    @Test
    public void test(){

        List<Integer>  array= Arrays.stream(new Integer[]{100,0,30,57}).collect(toList());
        Sort.binarySort(array);
        assertSort(array);
        array= Arrays.stream(new Integer[]{100,0,30,57,5}).collect(toList());
        Sort.binarySort(array);

    }

}