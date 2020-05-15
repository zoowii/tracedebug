package com.zoowii.tracedebug.daos;

import com.zoowii.tracedebug.models.SpanStackTraceEntity;
import org.springframework.data.repository.CrudRepository;

public interface SpanStackTraceRepository extends CrudRepository<SpanStackTraceEntity, Long> {
}
