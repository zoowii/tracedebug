package com.zoowii.tracedebug.daos;

import com.zoowii.tracedebug.http.BeanPaginator;
import com.zoowii.tracedebug.models.TraceSpanEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TraceSpanRepository extends CrudRepository<TraceSpanEntity, Long> {

    List<TraceSpanEntity> findAllByTraceIdOrderByIdAsc(String traceId);

    TraceSpanEntity findBySpanId(String spanId);

    TraceSpanEntity findFirstByTraceIdOrderByIdAsc(String traceId);

    @Query("select distinct t.traceId from TraceSpanEntity t")
    List<String> findAllDistinctTraceIds(@Param("offset") long offset, @Param("limit") int limit);

    @Query("select count(distinct t.traceId) from TraceSpanEntity t")
    long countDistinctTraceIds();

}
