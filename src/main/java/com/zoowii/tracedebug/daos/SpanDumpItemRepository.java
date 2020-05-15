package com.zoowii.tracedebug.daos;

import com.zoowii.tracedebug.models.SpanDumpItemEntity;
import org.springframework.data.repository.CrudRepository;

public interface SpanDumpItemRepository extends CrudRepository<SpanDumpItemEntity, Long> {
}
