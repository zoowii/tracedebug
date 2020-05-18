package com.zoowii.tracedebug.controllers.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StepTargetVo {
    private String spanId;
    private Integer seqInSpan;
}
