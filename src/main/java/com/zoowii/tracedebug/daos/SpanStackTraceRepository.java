package com.zoowii.tracedebug.daos;

import com.zoowii.tracedebug.models.SpanStackTraceEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SpanStackTraceRepository extends CrudRepository<SpanStackTraceEntity, Long> {
    List<SpanStackTraceEntity> findAllBySpanIdOrderByStackIndexAsc(String spanId);
    SpanStackTraceEntity findFirstBySpanIdAndStackIndex(String spanId, Integer stackIndex);
    SpanStackTraceEntity findFirstBySpanIdOrderByIdAsc(String spanId);
}

