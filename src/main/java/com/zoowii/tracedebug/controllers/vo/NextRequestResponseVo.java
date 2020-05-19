package com.zoowii.tracedebug.controllers.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NextRequestResponseVo {
    private String traceId;
    private String spanId;
    private Integer seqInSpan;
}
