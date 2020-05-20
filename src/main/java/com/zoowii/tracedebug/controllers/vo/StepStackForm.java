package com.zoowii.tracedebug.controllers.vo;

import lombok.Data;

import java.util.List;

@Data
public class StepStackForm {
    private List<BreakpointVo> breakpoints;
    private String currentSpanId;
    private int currentSeqInSpan;
    private String stepType; // 断点调试步进的类型，"step_over", "step_in", "step_out", "continue"
}
