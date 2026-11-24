-- A small task tracker used by the typed API and code generation tests.
CREATE TYPE priority AS ENUM ('low', 'medium', 'high', 'urgent');

CREATE TABLE app_user (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  email varchar(120) NOT NULL UNIQUE,
  display_name text NOT NULL,
  role text NOT NULL DEFAULT 'member' CHECK (role IN ('owner', 'admin', 'member', 'guest')),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE workspace (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  owner_id bigint NOT NULL REFERENCES app_user(id),
  slug text NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9-]{3,30}$'),
  name text NOT NULL,
  plan varchar(10) NOT NULL CHECK (plan = ANY (ARRAY['free', 'team', 'enterprise'])),
  settings jsonb NOT NULL DEFAULT '{}'
);

CREATE TABLE project (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id bigint NOT NULL REFERENCES workspace(id),
  name text NOT NULL,
  budget numeric(10,2) CHECK (budget >= 0),
  starts_on date NOT NULL,
  ends_on date,
  archived boolean NOT NULL DEFAULT false,
  CHECK (ends_on IS NULL OR ends_on >= starts_on),
  UNIQUE (workspace_id, name)
);

CREATE TABLE task (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  project_id uuid NOT NULL REFERENCES project(id),
  assignee_id bigint REFERENCES app_user(id),
  reporter_id bigint NOT NULL REFERENCES app_user(id),
  title text NOT NULL CHECK (length(title) BETWEEN 3 AND 120),
  status text NOT NULL CHECK (status IN ('todo', 'in progress', 'done', 'won''t do')),
  priority priority NOT NULL DEFAULT 'medium',
  estimate_hours integer CHECK (estimate_hours > 0),
  due_on date,
  done_at timestamptz,
  labels text[] NOT NULL DEFAULT '{}',
  CHECK ((status = 'done') = (done_at IS NOT NULL))
);

CREATE TABLE task_comment (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  task_id bigint NOT NULL REFERENCES task(id) ON DELETE CASCADE,
  author_id bigint NOT NULL REFERENCES app_user(id),
  body text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
