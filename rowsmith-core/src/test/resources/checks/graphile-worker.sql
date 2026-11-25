-- case: graphile-worker/_private_job_queues.job_queues_queue_name_check
-- source: https://github.com/graphile/worker/blob/b4f1b41c7abd747586879bf68a46a5a22897fa96/__tests__/schema.sql
-- license: MIT
CREATE TABLE _private_job_queues (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  queue_name text NOT NULL,
  CONSTRAINT job_queues_queue_name_check CHECK ((length(queue_name) <= 128))
);

-- case: graphile-worker/_private_jobs.jobs_key_check
-- source: https://github.com/graphile/worker/blob/b4f1b41c7abd747586879bf68a46a5a22897fa96/__tests__/schema.sql
-- license: MIT
CREATE TABLE _private_jobs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  key text,
  CONSTRAINT jobs_key_check CHECK (((length(key) > 0) AND (length(key) <= 512)))
);

-- case: graphile-worker/_private_jobs.jobs_max_attempts_check
-- source: https://github.com/graphile/worker/blob/b4f1b41c7abd747586879bf68a46a5a22897fa96/__tests__/schema.sql
-- license: MIT
CREATE TABLE _private_jobs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  max_attempts smallint NOT NULL DEFAULT 25,
  CONSTRAINT jobs_max_attempts_check CHECK ((max_attempts >= 1))
);
