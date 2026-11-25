-- case: que/que_jobs.valid_args
-- source: https://github.com/que-rb/que/tree/57cfa4a4d4ad3f23a4f20972ff2c7f4ceed58d85/lib/que/migrations
-- license: MIT
CREATE TABLE que_jobs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  args jsonb NOT NULL DEFAULT '[]'::jsonb,
  CONSTRAINT valid_args CHECK ((jsonb_typeof(args) = 'array'::text))
);

-- case: que/que_lockers.valid_worker_priorities
-- source: https://github.com/que-rb/que/tree/57cfa4a4d4ad3f23a4f20972ff2c7f4ceed58d85/lib/que/migrations
-- license: MIT
CREATE TABLE que_lockers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  worker_priorities integer[] NOT NULL,
  CONSTRAINT valid_worker_priorities CHECK (((array_ndims(worker_priorities) = 1) AND (array_length(worker_priorities, 1) IS NOT NULL)))
);

-- case: que/que_lockers.valid_queues
-- source: https://github.com/que-rb/que/tree/57cfa4a4d4ad3f23a4f20972ff2c7f4ceed58d85/lib/que/migrations
-- license: MIT
CREATE TABLE que_lockers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  queues text[] NOT NULL,
  CONSTRAINT valid_queues CHECK (((array_ndims(queues) = 1) AND (array_length(queues, 1) IS NOT NULL)))
);

-- case: que/que_jobs.error_length
-- source: https://github.com/que-rb/que/tree/57cfa4a4d4ad3f23a4f20972ff2c7f4ceed58d85/lib/que/migrations
-- license: MIT
CREATE TABLE que_jobs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  last_error_message text,
  last_error_backtrace text,
  CONSTRAINT error_length CHECK (((char_length(last_error_message) <= 500) AND (char_length(last_error_backtrace) <= 10000)))
);

-- case: que/que_jobs.valid_data
-- source: https://github.com/que-rb/que/tree/57cfa4a4d4ad3f23a4f20972ff2c7f4ceed58d85/lib/que/migrations
-- license: MIT
CREATE OR REPLACE FUNCTION que_validate_tags(tags_array jsonb)
 RETURNS boolean
 LANGUAGE sql
AS $function$
  SELECT bool_and(
    jsonb_typeof(value) = 'string'
    AND
    char_length(value::text) <= 100
  )
  FROM jsonb_array_elements(tags_array)
$function$;
CREATE TABLE que_jobs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  data jsonb NOT NULL DEFAULT '{}'::jsonb,
  CONSTRAINT valid_data CHECK (((jsonb_typeof(data) = 'object'::text) AND ((NOT (data ? 'tags'::text)) OR ((jsonb_typeof((data -> 'tags'::text)) = 'array'::text) AND (jsonb_array_length((data -> 'tags'::text)) <= 5) AND que_validate_tags((data -> 'tags'::text))))))
);

-- case: que/que_jobs.job_class_length
-- source: https://github.com/que-rb/que/tree/57cfa4a4d4ad3f23a4f20972ff2c7f4ceed58d85/lib/que/migrations
-- license: MIT
CREATE TABLE que_jobs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  job_class text NOT NULL,
  args jsonb NOT NULL DEFAULT '[]'::jsonb,
  CONSTRAINT job_class_length CHECK ((char_length(
CASE job_class
    WHEN 'ActiveJob::QueueAdapters::QueAdapter::JobWrapper'::text THEN ((args -> 0) ->> 'job_class'::text)
    ELSE job_class
END) <= 200))
);

-- case: que/que_jobs.queue_length
-- source: https://github.com/que-rb/que/tree/57cfa4a4d4ad3f23a4f20972ff2c7f4ceed58d85/lib/que/migrations
-- license: MIT
CREATE TABLE que_jobs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  queue text NOT NULL DEFAULT 'default'::text,
  CONSTRAINT queue_length CHECK ((char_length(queue) <= 100))
);
