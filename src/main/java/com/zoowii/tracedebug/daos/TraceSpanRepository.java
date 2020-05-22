package com.zoowii.tracedebug.daos;

import com.zoowii.tracedebug.http.BeanPaginator;
import com.zoowii.tracedebug.models.TraceSpanEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface TraceSpanRepository extends CrudRepository<TraceSpanEntity, Long> {

    List<TraceSpanEntity> findAllByTraceIdOrderByIdAsc(String traceId);

    TraceSpanEntity findBySpanId(String spanId);

    TraceSpanEntity findFirstByTraceIdOrderByIdAsc(String traceId);

    //  limit #{offset}, #{limit}
    @Query(value = "select max(id) as id, trace_id from trace_span group by trace_id order by id desc", nativeQuery = true)
    List<Map<String, Object>> findAllDistinctTraceIds(@Param("offset") long offset, @Param("limit") int limit);

    @Query("select count(distinct t.traceId) from TraceSpanEntity t")
    long countDistinctTraceIds();

}
