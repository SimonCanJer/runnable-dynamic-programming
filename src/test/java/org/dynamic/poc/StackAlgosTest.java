package org.dynamic.poc;

import org.junit.jupiter.api.Test;

import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StackAlgosTest {

    @Test
    public void test(){
        Stack<Integer> stack= new Stack<>();
        stack.push(1);
        StackReverseDynamics.reverseStack(stack);
        assertEquals(stack.size(),1);
        assertEquals(1,stack.pop());
        stack.push(1);
        stack.push(2);
        StackReverseDynamics.reverseStack(stack);
        assertEquals(2,stack.size());
        assertEquals(1,stack.pop());
        assertEquals(2,stack.pop());
        stack.push(1);
        stack.push(2);
        stack.push(3);
        StackReverseDynamics.reverseStack(stack);
        assertEquals(3,stack.size());
        assertEquals(1,stack.pop());
        assertEquals(2,stack.pop());
        assertEquals(3,stack.pop());
        stack.push(1);
        stack.push(2);
        stack.push(3);
        stack.push(4);
        StackReverseDynamics.reverseStack(stack);
        assertEquals(4,stack.size());
        assertEquals(1,stack.pop());
        assertEquals(2,stack.pop());
        assertEquals(3,stack.pop());
        assertEquals(4,stack.pop());
        for(int i=0;i<100000;i++){
            stack.push(i);
        }
        StackReverseDynamics.reverseStack(stack);
        assertEquals(100000,stack.size());
        for(int i=0;i<100000;i++){
            assertEquals(i,stack.pop());
        }
    }

}