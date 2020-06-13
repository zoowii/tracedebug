package com.zoowii.tracedebug.controllers.vo;

import lombok.Data;

@Data
public class BreakpointVo {
    private String moduleId;
    private String filename;
    private String filepath;
    private Integer line;
}
