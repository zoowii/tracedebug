package com.zoowii.tracedebug.daos;

import com.zoowii.tracedebug.models.SpanDumpItemEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SpanDumpItemRepository extends CrudRepository<SpanDumpItemEntity, Long> {
    List<SpanDumpItemEntity> findAllBySpanIdOrderBySeqInSpan(String spanId);

    List<SpanDumpItemEntity> findAllBySpanIdAndSeqInSpanGreaterThanOrderBySeqInSpan(String spanId, int seqInSpan);

    SpanDumpItemEntity findFirstBySpanIdAndSeqInSpanOrderByIdAsc(String spanId, int seqInSpan);

    SpanDumpItemEntity findFirstBySpanIdOrderByIdAsc(String spanId);

    List<SpanDumpItemEntity> findAllByTraceIdAndIdGreaterThanOrderByIdAsc(String traceId, long id);
}
