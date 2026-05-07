-- ============================================================
-- Klex Report Server — Initial PostgreSQL Schema
-- ============================================================
-- This schema mirrors the Django ORM models across all apps:
--   accounts, repos, reports, dataadapter, github_integration, audit
--
-- Intended for fresh bootstrapping; Django migrations are the
-- canonical source for incremental changes.
-- ============================================================

-- enable uuid generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ────────────────────────────────────────────────────────────
-- organizations
-- ────────────────────────────────────────────────────────────
CREATE TABLE organizations (
  id            serial       PRIMARY KEY,
  name          text         UNIQUE NOT NULL,
  description   text         DEFAULT '',
  created_at    timestamptz  DEFAULT now()
);

-- ────────────────────────────────────────────────────────────
-- users  (Django custom user — AbstractBaseUser + PermissionsMixin)
-- ────────────────────────────────────────────────────────────
CREATE TABLE users (
  id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
  username        text         UNIQUE NOT NULL,
  password        text         NOT NULL,        -- Django password hash
  email           text         UNIQUE,
  display_name    text,
  is_admin        boolean      DEFAULT false,
  is_super_admin  boolean      DEFAULT false,
  organization_id int          REFERENCES organizations(id) ON DELETE SET NULL,
  is_active       boolean      DEFAULT true,
  is_staff        boolean      DEFAULT false,
  last_login      timestamptz,
  is_superuser    boolean      DEFAULT false,
  created_at      timestamptz  DEFAULT now()
);

-- ────────────────────────────────────────────────────────────
-- repos
-- ────────────────────────────────────────────────────────────
CREATE TABLE repos (
  id              serial       PRIMARY KEY,
  owner           text         NOT NULL,
  name            text         NOT NULL,
  path_prefix     text         DEFAULT '',
  branch          text         DEFAULT 'main',
  last_synced_at  timestamptz,
  git_remote_url  text,
  created_by_id   uuid         REFERENCES users(id) ON DELETE SET NULL,
  created_at      timestamptz  DEFAULT now(),
  UNIQUE(owner, name)
);

-- ────────────────────────────────────────────────────────────
-- reports
-- ────────────────────────────────────────────────────────────
CREATE TABLE reports (
  id              serial       PRIMARY KEY,
  repo_id         int          NOT NULL REFERENCES repos(id) ON DELETE CASCADE,
  path            text         NOT NULL,
  report_name     text         NOT NULL,
  latest_commit   text,
  display_name    text,
  is_public       boolean      DEFAULT true,
  created_at      timestamptz  DEFAULT now(),
  UNIQUE(repo_id, path)
);

-- ────────────────────────────────────────────────────────────
-- report_permissions  (user → report)
-- ────────────────────────────────────────────────────────────
CREATE TABLE report_permissions (
  id            serial       PRIMARY KEY,
  user_id       uuid         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  report_id     int          NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
  can_access    boolean      DEFAULT true,
  can_schedule  boolean      DEFAULT false,
  created_at    timestamptz  DEFAULT now(),
  UNIQUE(user_id, report_id)
);
CREATE INDEX idx_report_permissions_user   ON report_permissions(user_id);
CREATE INDEX idx_report_permissions_report ON report_permissions(report_id);

-- ────────────────────────────────────────────────────────────
-- organization_report_permissions  (org → report)
-- ────────────────────────────────────────────────────────────
CREATE TABLE organization_report_permissions (
  id              serial       PRIMARY KEY,
  organization_id int          NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
  report_id       int          NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
  can_access      boolean      DEFAULT true,
  can_schedule    boolean      DEFAULT false,
  created_at      timestamptz  DEFAULT now(),
  UNIQUE(organization_id, report_id)
);
CREATE INDEX idx_org_report_perm_org    ON organization_report_permissions(organization_id);
CREATE INDEX idx_org_report_perm_report ON organization_report_permissions(report_id);

-- ────────────────────────────────────────────────────────────
-- executions
-- ────────────────────────────────────────────────────────────
CREATE TABLE executions (
  id              serial       PRIMARY KEY,
  report_id       int          NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
  invoked_by_id   uuid         REFERENCES users(id) ON DELETE SET NULL,
  status          text         DEFAULT 'queued'
                               CHECK (status IN ('queued','running','success','failure')),
  output_format   text         DEFAULT 'pdf',
  started_at      timestamptz,
  finished_at     timestamptz,
  output_location text,
  created_at      timestamptz  DEFAULT now()
);

-- ────────────────────────────────────────────────────────────
-- scheduled_jobs  (unified shadow for Quartz / Airflow)
-- ────────────────────────────────────────────────────────────
CREATE TABLE scheduled_jobs (
  id                       serial       PRIMARY KEY,
  dag_id                   text         UNIQUE NOT NULL,
  schedule_name            text         NOT NULL,
  report_id                int          REFERENCES reports(id) ON DELETE SET NULL,
  created_by_id            uuid         REFERENCES users(id) ON DELETE SET NULL,
  status                   text         DEFAULT 'running'
                                        CHECK (status IN ('running','finished','failed','on_hold')),
  priority                 int          DEFAULT 0,
  department               text         DEFAULT 'General',
  cron_expression          text         DEFAULT '',
  frequency_label          text         DEFAULT '',
  machine_name             text         DEFAULT '',
  estimated_runtime_min    float,
  max_runtime_min          float        DEFAULT 30,
  next_run                 timestamptz,
  last_run                 timestamptz,
  is_active                boolean      DEFAULT true,
  termination_description  text         DEFAULT '',
  exit_code                text         DEFAULT '',

  -- migration support
  schedule_payload         jsonb        DEFAULT '{}'::jsonb,
  created_on_engine        text         DEFAULT 'quartz'
                                        CHECK (created_on_engine IN ('quartz','airflow')),

  created_at               timestamptz  DEFAULT now(),
  updated_at               timestamptz  DEFAULT now()
);

-- ────────────────────────────────────────────────────────────
-- job_comments
-- ────────────────────────────────────────────────────────────
CREATE TABLE job_comments (
  id          serial       PRIMARY KEY,
  job_id      int          NOT NULL REFERENCES scheduled_jobs(id) ON DELETE CASCADE,
  user_id     uuid         REFERENCES users(id) ON DELETE SET NULL,
  text        text         NOT NULL,
  created_at  timestamptz  DEFAULT now()
);

-- ────────────────────────────────────────────────────────────
-- report_execution_jobs  (Airflow dispatch queue)
-- ────────────────────────────────────────────────────────────
CREATE TABLE report_execution_jobs (
  id                       uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
  created_by_id            uuid         REFERENCES users(id) ON DELETE SET NULL,
  organization_id          int          REFERENCES organizations(id) ON DELETE SET NULL,
  report_id                int          REFERENCES reports(id) ON DELETE SET NULL,
  dag_id                   text         NOT NULL,
  dedupe_key               text         NOT NULL,
  airflow_run_id           text,

  -- timestamps
  schedule_time            timestamptz,
  requested_at             timestamptz  DEFAULT now(),
  admitted_at              timestamptz,
  started_at               timestamptz,
  completed_at             timestamptz,

  -- classification
  priority                 text         DEFAULT 'NORMAL'
                                        CHECK (priority IN ('CRITICAL','HIGH','NORMAL','LOW')),
  workload_class           text         DEFAULT 'MEDIUM'
                                        CHECK (workload_class IN ('INTERACTIVE','LIGHT','MEDIUM','HEAVY')),

  -- lifecycle
  status                   text         DEFAULT 'RECEIVED'
                                        CHECK (status IN (
                                          'RECEIVED','WAITING','ADMITTED','TRIGGERED',
                                          'RUNNING','SUCCESS',
                                          'FAILED_RETRYABLE','FAILED_FINAL',
                                          'SKIPPED','EXPIRED','CANCELLED'
                                        )),

  -- retry
  retry_count              int          DEFAULT 0,
  max_retries              int          DEFAULT 3,
  last_retry_at            timestamptz,

  -- payload
  payload                  jsonb        DEFAULT '{}'::jsonb,

  -- overlap / runtime
  overlap_policy           text         DEFAULT 'QUEUE_ALL'
                                        CHECK (overlap_policy IN (
                                          'QUEUE_ALL','SKIP_IF_RUNNING','LATEST_ONLY','COALESCE'
                                        )),
  expected_runtime_seconds int,

  -- error tracking
  last_error               text         DEFAULT '',
  last_error_type          text         DEFAULT '',

  -- debug / observability
  attempt_metadata         jsonb        DEFAULT '{}'::jsonb,

  -- expiry
  expiry_time              timestamptz,

  -- auto timestamps
  created_at               timestamptz  DEFAULT now(),
  updated_at               timestamptz  DEFAULT now()
);
CREATE INDEX idx_rej_status_pri  ON report_execution_jobs(status, priority);
CREATE INDEX idx_rej_dag_status  ON report_execution_jobs(dag_id, status);
CREATE INDEX idx_rej_dedupe      ON report_execution_jobs(dedupe_key);
CREATE INDEX idx_rej_user_status ON report_execution_jobs(created_by_id, status);
CREATE INDEX idx_rej_org_status  ON report_execution_jobs(organization_id, status);

-- ────────────────────────────────────────────────────────────
-- audit_logs
-- ────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
  id       serial       PRIMARY KEY,
  user_id  uuid         REFERENCES users(id) ON DELETE SET NULL,
  action   text         NOT NULL,
  meta     jsonb        DEFAULT '{}'::jsonb,
  ts       timestamptz  DEFAULT now()
);

-- ────────────────────────────────────────────────────────────
-- data_adapters
-- ────────────────────────────────────────────────────────────
CREATE TABLE data_adapters (
  id                 serial       PRIMARY KEY,
  name               text         NOT NULL,
  adapter_type       text         NOT NULL
                                  CHECK (adapter_type IN ('jdbc','csv','json','xml','inmemory','mock')),
  connection_details jsonb        NOT NULL DEFAULT '{}'::jsonb,
  is_active          boolean      DEFAULT true,
  created_by_id      uuid         REFERENCES users(id) ON DELETE SET NULL,
  created_at         timestamptz  DEFAULT now(),
  updated_at         timestamptz  DEFAULT now()
);

-- ────────────────────────────────────────────────────────────
-- report_groups  (server-side report folders)
-- ────────────────────────────────────────────────────────────
CREATE TABLE report_groups (
  id            serial       PRIMARY KEY,
  name          text         NOT NULL,
  color         text         DEFAULT 'violet',
  description   text         DEFAULT '',
  created_by_id uuid         REFERENCES users(id) ON DELETE SET NULL,
  created_at    timestamptz  DEFAULT now(),
  updated_at    timestamptz  DEFAULT now()
);

-- ────────────────────────────────────────────────────────────
-- report_group_members  (M2M: group ↔ report, reports can be in many groups)
-- ────────────────────────────────────────────────────────────
CREATE TABLE report_group_members (
  id         serial       PRIMARY KEY,
  group_id   int          NOT NULL REFERENCES report_groups(id) ON DELETE CASCADE,
  report_id  int          NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
  added_at   timestamptz  DEFAULT now(),
  UNIQUE(group_id, report_id)
);
CREATE INDEX idx_rgm_group  ON report_group_members(group_id);
CREATE INDEX idx_rgm_report ON report_group_members(report_id);

-- ────────────────────────────────────────────────────────────
-- group_user_permissions  (grant user access to all reports in a group)
-- ────────────────────────────────────────────────────────────
CREATE TABLE group_user_permissions (
  id           serial       PRIMARY KEY,
  group_id     int          NOT NULL REFERENCES report_groups(id) ON DELETE CASCADE,
  user_id      uuid         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  can_access   boolean      DEFAULT true,
  can_schedule boolean      DEFAULT false,
  created_at   timestamptz  DEFAULT now(),
  UNIQUE(group_id, user_id)
);
CREATE INDEX idx_gup_group ON group_user_permissions(group_id);
CREATE INDEX idx_gup_user  ON group_user_permissions(user_id);

-- ────────────────────────────────────────────────────────────
-- group_organization_permissions  (grant org access to all reports in a group)
-- ────────────────────────────────────────────────────────────
CREATE TABLE group_organization_permissions (
  id              serial       PRIMARY KEY,
  group_id        int          NOT NULL REFERENCES report_groups(id) ON DELETE CASCADE,
  organization_id int          NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
  can_access      boolean      DEFAULT true,
  can_schedule    boolean      DEFAULT false,
  created_at      timestamptz  DEFAULT now(),
  UNIQUE(group_id, organization_id)
);
CREATE INDEX idx_gop_group ON group_organization_permissions(group_id);
CREATE INDEX idx_gop_org   ON group_organization_permissions(organization_id);

-- ────────────────────────────────────────────────────────────
-- github_tokens  (1:1 with users)
-- ────────────────────────────────────────────────────────────
CREATE TABLE github_tokens (
  id            serial       PRIMARY KEY,
  user_id       uuid         NOT NULL REFERENCES users(id) ON DELETE CASCADE UNIQUE,
  access_token  text         NOT NULL,
  token_type    text         DEFAULT 'bearer',
  scope         text,
  created_at    timestamptz  DEFAULT now(),
  updated_at    timestamptz  DEFAULT now()
);
