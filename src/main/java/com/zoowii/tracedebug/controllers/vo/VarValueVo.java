package com.zoowii.tracedebug.controllers.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VarValueVo {
    private String name;
    private String value;
}
