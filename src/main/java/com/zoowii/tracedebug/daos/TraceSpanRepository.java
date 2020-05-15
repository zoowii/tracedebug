package com.zoowii.tracedebug.daos;

import com.zoowii.tracedebug.models.TraceSpanEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TraceSpanRepository extends CrudRepository<TraceSpanEntity, Long> {

    List<TraceSpanEntity> findAllByTraceId(String traceId);

}
