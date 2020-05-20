package com.zoowii.tracedebug.controllers.vo;

import lombok.Data;

@Data
public class ViewStackTraceForm {
    private String spanId;
    private Integer seqInSpan;
}
