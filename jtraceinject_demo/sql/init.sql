CREATE TABLE `trace_span` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `trace_id` varchar(200) NOT NULL,
  `span_id` varchar(200) NOT NULL,
  `module_id` varchar(200) NOT NULL,
  `classname` text NULL,
  `method_name` text NULL,
  `stack_depth` int(11) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `span_stack_trace` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `trace_id` varchar(200) NOT NULL,
  `span_id` varchar(200) NOT NULL,
  `stack_index` int(11) NOT NULL,
  `module_id` varchar(200) NULL,
  `classname` text NOT NULL,
  `method_name` text NOT NULL,
  `line` int(11) NULL,
  `filename` text NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE `span_dump_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `trace_id` varchar(200) NOT NULL,
  `span_id` varchar(200) NOT NULL,
  `seq_in_span` int(11) NOT NULL,
  `name` varchar(200) NOT NULL,
  `value` varchar(200) NOT NULL,
  `line` int(11) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
