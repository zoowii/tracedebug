package com.zoowii.tracedebug.spring.aspects;

import org.springframework.stereotype.Component;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
@Documented
public @interface DebugTrace {

}
