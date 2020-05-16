package com.zoowii.tracedebug.controllers.vo;

import lombok.Data;

import java.util.List;

@Data
public class StackVarSnapshotVo {
    private List<VarValueVo> variableValues;
}
