package com.zoowii.tracedebug.controllers.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ViewStackVariablesForm {
    private String spanId;
    private Integer seqInSpan;
}
