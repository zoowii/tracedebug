package com.zoowii.tracedebug.controllers.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TraceCallVo {
    private String traceId;
    private String spanId;
    private String moduleId;
    private String classname;
    private String methodName;
    private String filename;
    private Integer line;
}
