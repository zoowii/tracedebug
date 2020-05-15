package com.zoowii.tracedebug.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "span_dump_item")
@ToString
public class SpanDumpItemEntity {
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
    @Column(name = "seq_in_span", nullable = false)
    private Integer seqInSpan;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "value", nullable = true)
    private String value;
    @Column(name = "line", nullable = true)
    private Integer line;

}
