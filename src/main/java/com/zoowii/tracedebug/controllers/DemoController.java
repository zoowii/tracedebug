package com.zoowii.tracedebug.controllers;

import com.zoowii.tracedebug.aspects.DebugTrace;
import com.zoowii.tracedebug.services.HelloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
//@DebugTrace
public class DemoController {

    @Resource
    private HelloService helloService;

//    @DebugTrace
    @GetMapping("/hello")
    public Object hello() {
        String msg = helloService.helloSum("world", 100, 222);
        return msg;
    }
}
