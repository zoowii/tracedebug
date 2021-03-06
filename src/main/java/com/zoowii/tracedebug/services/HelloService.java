package com.zoowii.tracedebug.services;

import com.zoowii.tracedebug.spring.aspects.DebugTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@DebugTrace
@Service
public class HelloService {
    protected Logger log = LoggerFactory.getLogger(HelloService.class);

    @DebugTrace
    public String helloSum(String name, int a, int b) {
        log.info("helloSum called");
        int s = a + b;
        return "hello, " + name + ", " + a + " + " + b + " = " + s;
    }
}
