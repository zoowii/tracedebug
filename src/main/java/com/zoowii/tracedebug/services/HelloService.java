package com.zoowii.tracedebug.services;

import com.zoowii.tracedebug.aspects.DebugTrace;
import org.springframework.stereotype.Service;

@Service
public class HelloService {
    @DebugTrace
    public String helloSum(String name, int a, int b) {
        int s = a + b;
        return "hello, " + name + ", " + a + " + " + b + " = " + s;
    }
}
