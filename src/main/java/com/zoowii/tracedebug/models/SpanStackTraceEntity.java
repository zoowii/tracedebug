package com.zoowii.tracedebug.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "span_stack_trace")
@ToString
public class SpanStackTraceEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime DEFAULT CURRENT_TIMESTAMP", updatable = false, insertable = false)
    private Date createdAt;
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", updatable = false, insertable = false)
    private Date updatedAt;
    @Column(name = "trace_id", nullable = false)
    private String traceId;
    @Column(name = "span_id", nullable = false)
    private String spanId;
    @Column(name = "stack_index", nullable = false)
    private Integer stackIndex;
    @Column(name = "classname", nullable = false)
    private String classname;
    @Column(name = "method_name", nullable = false)
    private String methodName;
    @Column(name = "line", nullable = false)
    private Integer line;
    @Column(name = "filename")
    private String filename;
}
