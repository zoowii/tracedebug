package com.zoowii.tracedebug.http;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BeanPage<T> {
    private Collection<T> data;
    private Long total;
}
