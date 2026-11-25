-- case: gitlab/abuse_report_uploads.check_2849dedce7
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE abuse_report_uploads (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  path text NOT NULL,
  CONSTRAINT check_2849dedce7 CHECK ((char_length(path) <= 511))
);

-- case: gitlab/namespace_package_settings.check_31340211b1
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE namespace_package_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  generic_duplicate_exception_regex text NOT NULL DEFAULT ''::text,
  CONSTRAINT check_31340211b1 CHECK ((char_length(generic_duplicate_exception_regex) <= 255))
);

-- case: gitlab/virtual_registries_container_cache_remote_entries_13.check_ff83dec0d4
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE virtual_registries_container_cache_remote_entries_13 (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  relative_path text NOT NULL,
  CONSTRAINT check_ff83dec0d4 CHECK ((char_length(relative_path) <= 1024))
);

-- case: gitlab/abuse_report_events.check_bb4cd85618
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE abuse_report_events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  comment text,
  CONSTRAINT check_bb4cd85618 CHECK ((char_length(comment) <= 1024))
);

-- case: gitlab/duo_workflows_workflows.check_6b3ca83ad7
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE duo_workflows_workflows (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  goal text,
  CONSTRAINT check_6b3ca83ad7 CHECK ((char_length(goal) <= 65536))
);

-- case: gitlab/project_settings.check_1a30456322
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE project_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pages_unique_domain text,
  CONSTRAINT check_1a30456322 CHECK ((char_length(pages_unique_domain) <= 63))
);

-- case: gitlab/abuse_events.check_9b41e64a86
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE abuse_events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  organization_id bigint,
  CONSTRAINT check_9b41e64a86 CHECK ((organization_id IS NOT NULL))
);

-- case: gitlab/gitlab_subscriptions.check_77fea3f0e7
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE gitlab_subscriptions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  namespace_id bigint,
  CONSTRAINT check_77fea3f0e7 CHECK ((namespace_id IS NOT NULL))
);

-- case: gitlab/packages_nuget_symbols.check_7a67b0fc8b
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE packages_nuget_symbols (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  project_id bigint,
  CONSTRAINT check_7a67b0fc8b CHECK ((project_id IS NOT NULL))
);

-- case: gitlab/virtual_registries_container_cache_entries.chk_rails_cf0e2a2780
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE virtual_registries_container_cache_entries (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  file_sha1 bytea NOT NULL,
  CONSTRAINT chk_rails_cf0e2a2780 CHECK ((octet_length(file_sha1) = 20))
);

-- case: gitlab/ai_audit_events.check_547b01c40f
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE ai_audit_events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  project_id bigint,
  namespace_id bigint,
  CONSTRAINT check_547b01c40f CHECK ((num_nonnulls(namespace_id, project_id) = 1))
);

-- case: gitlab/protected_branch_unprotect_access_levels.check_a5a558921b
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE protected_branch_unprotect_access_levels (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  protected_branch_project_id bigint,
  protected_branch_namespace_id bigint,
  CONSTRAINT check_a5a558921b CHECK ((num_nonnulls(protected_branch_namespace_id, protected_branch_project_id) = 1))
);

-- case: gitlab/ai_catalog_item_consumers.check_0acef721fa
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE ai_catalog_item_consumers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  group_id bigint,
  project_id bigint,
  CONSTRAINT check_0acef721fa CHECK ((num_nonnulls(group_id, project_id) = 1))
);

-- case: gitlab/ai_flow_triggers.check_ai_flow_triggers_filter_is_hash
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE ai_flow_triggers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  filter jsonb NOT NULL DEFAULT '{}'::jsonb,
  CONSTRAINT check_ai_flow_triggers_filter_is_hash CHECK ((jsonb_typeof(filter) = 'object'::text))
);

-- case: gitlab/application_settings.check_application_settings_importers_is_hash
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE application_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  importers jsonb NOT NULL DEFAULT '{}'::jsonb,
  CONSTRAINT check_application_settings_importers_is_hash CHECK ((jsonb_typeof(importers) = 'object'::text))
);

-- case: gitlab/application_settings.check_application_settings_usage_billing_is_hash
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE application_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usage_billing jsonb NOT NULL DEFAULT '{}'::jsonb,
  CONSTRAINT check_application_settings_usage_billing_is_hash CHECK ((jsonb_typeof(usage_billing) = 'object'::text))
);

-- case: gitlab/virtual_registries_packages_maven_cache_entries.check_cc222855d6
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE virtual_registries_packages_maven_cache_entries (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  file_md5 bytea,
  CONSTRAINT check_cc222855d6 CHECK (((file_md5 IS NULL) OR (octet_length(file_md5) = 16)))
);

-- case: gitlab/namespace_descendants.check_60ae9ef706
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE namespace_descendants (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  all_unarchived_project_ids bigint[] DEFAULT '{}'::bigint[],
  CONSTRAINT check_60ae9ef706 CHECK ((all_unarchived_project_ids IS NOT NULL))
);

-- case: gitlab/vulnerability_reads.check_f5ba7c2496
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE vulnerability_reads (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  traversal_ids bigint[] DEFAULT '{}'::bigint[],
  CONSTRAINT check_f5ba7c2496 CHECK ((traversal_ids IS NOT NULL))
);

-- case: gitlab/appearance_uploads.check_84e287699b
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE appearance_uploads (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  organization_id bigint,
  namespace_id bigint,
  project_id bigint,
  CONSTRAINT check_84e287699b CHECK ((num_nonnulls(namespace_id, organization_id, project_id) = 0))
);

-- case: gitlab/events.check_5766fc2949
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  project_id bigint,
  group_id bigint,
  personal_namespace_id bigint,
  CONSTRAINT check_5766fc2949 CHECK ((num_nonnulls(group_id, personal_namespace_id, project_id) = 1))
);

-- case: gitlab/atlassian_identities.atlassian_identities_refresh_token_iv_length_constraint
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE atlassian_identities (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  encrypted_refresh_token_iv bytea,
  CONSTRAINT atlassian_identities_refresh_token_iv_length_constraint CHECK ((octet_length(encrypted_refresh_token_iv) <= 12))
);

-- case: gitlab/events.check_97e06e05ad
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  fingerprint bytea,
  CONSTRAINT check_97e06e05ad CHECK ((octet_length(fingerprint) <= 128))
);

-- case: gitlab/ai_catalog_items.check_ai_catalog_items_star_count_non_negative
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE ai_catalog_items (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  star_count integer NOT NULL DEFAULT 0,
  CONSTRAINT check_ai_catalog_items_star_count_non_negative CHECK ((star_count >= 0))
);

-- case: gitlab/batched_background_migrations.check_69e4fcc53b
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE batched_background_migrations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pause_ms integer NOT NULL DEFAULT 100,
  CONSTRAINT check_69e4fcc53b CHECK ((pause_ms >= 100))
);

-- case: gitlab/virtual_registries_container_cache_remote_entries.check_digest_length
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE virtual_registries_container_cache_remote_entries (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  digest text,
  CONSTRAINT check_digest_length CHECK (((digest IS NULL) OR (char_length(digest) = 71)))
);

-- case: gitlab/cd_rollouts.check_1eef9fc5c9
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE cd_rollouts (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  iid integer,
  CONSTRAINT check_1eef9fc5c9 CHECK ((iid IS NOT NULL))
);

-- case: gitlab/oauth_access_tokens.check_70f294ef54
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE oauth_access_tokens (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  expires_in integer DEFAULT 7200,
  CONSTRAINT check_70f294ef54 CHECK ((expires_in IS NOT NULL))
);

-- case: gitlab/cd_artifact_sources.check_892918d4b4
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE cd_artifact_sources (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  source_ref text,
  CONSTRAINT check_892918d4b4 CHECK ((source_ref IS NOT NULL))
);

-- case: gitlab/ci_runner_taggings_instance_type.check_tag_name_not_null
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE ci_runner_taggings_instance_type (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tag_name text,
  CONSTRAINT check_tag_name_not_null CHECK ((tag_name IS NOT NULL))
);

-- case: gitlab/audit_events_group_external_streaming_destinations.check_audit_event_streams_group_secret_token_max_length
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE audit_events_group_external_streaming_destinations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  encrypted_secret_token bytea NOT NULL,
  CONSTRAINT check_audit_event_streams_group_secret_token_max_length CHECK ((octet_length(encrypted_secret_token) <= 4112))
);

-- case: gitlab/packages_conan_package_revisions.check_revision_length
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE packages_conan_package_revisions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  revision bytea NOT NULL,
  CONSTRAINT check_revision_length CHECK ((octet_length(revision) <= 20))
);

-- case: gitlab/application_settings.check_be6ab41dcc
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE application_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  secret_push_protection_available boolean DEFAULT false,
  CONSTRAINT check_be6ab41dcc CHECK ((secret_push_protection_available IS NOT NULL))
);

-- case: gitlab/analytics_cycle_analytics_aggregations.chk_rails_1ef688e577
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE analytics_cycle_analytics_aggregations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  incremental_runtimes_in_seconds integer[] NOT NULL DEFAULT '{}'::integer[],
  CONSTRAINT chk_rails_1ef688e577 CHECK ((cardinality(incremental_runtimes_in_seconds) <= 10))
);

-- case: gitlab/protected_environment_approval_rules.chk_rails_cfa90ae3b5
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE protected_environment_approval_rules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  required_approvals smallint NOT NULL,
  CONSTRAINT chk_rails_cfa90ae3b5 CHECK ((required_approvals > 0))
);

-- case: gitlab/abuse_reports.abuse_reports_links_to_spam_length_check
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE abuse_reports (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  links_to_spam text[] NOT NULL DEFAULT '{}'::text[],
  CONSTRAINT abuse_reports_links_to_spam_length_check CHECK ((cardinality(links_to_spam) <= 20))
);

-- case: gitlab/background_operation_jobs.check_22e75767e4
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE background_operation_jobs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  min_cursor jsonb,
  max_cursor jsonb,
  CONSTRAINT check_22e75767e4 CHECK (((jsonb_typeof(min_cursor) = 'array'::text) AND (jsonb_typeof(max_cursor) = 'array'::text)))
);

-- case: gitlab/billable_usage_daily_namespace_aggregates.check_billable_usage_daily_ns_aggs_events_count_non_negative
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE billable_usage_daily_namespace_aggregates (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  events_count bigint NOT NULL DEFAULT 0,
  CONSTRAINT check_billable_usage_daily_ns_aggs_events_count_non_negative CHECK ((events_count >= 0))
);

-- case: gitlab/application_settings.app_settings_dep_proxy_ttl_policies_worker_capacity_positive
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE application_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  dependency_proxy_ttl_group_policy_worker_capacity smallint NOT NULL DEFAULT 2,
  CONSTRAINT app_settings_dep_proxy_ttl_policies_worker_capacity_positive CHECK ((dependency_proxy_ttl_group_policy_worker_capacity >= 0))
);

-- case: gitlab/application_settings.app_settings_yaml_max_depth_positive
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE application_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  max_yaml_depth integer NOT NULL DEFAULT 100,
  CONSTRAINT app_settings_yaml_max_depth_positive CHECK ((max_yaml_depth > 0))
);

-- case: gitlab/catalog_resource_components.check_47c5537132
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE catalog_resource_components (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  component_type smallint DEFAULT 1,
  CONSTRAINT check_47c5537132 CHECK ((component_type IS NOT NULL))
);

-- case: gitlab/oauth_consents.check_oauth_consents_granted_scopes_size
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE oauth_consents (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  granted_scopes text[],
  CONSTRAINT check_oauth_consents_granted_scopes_size CHECK ((cardinality(granted_scopes) <= 50))
);

-- case: gitlab/application_settings.check_17d9558205
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE application_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  kroki_url character varying,
  CONSTRAINT check_17d9558205 CHECK ((char_length((kroki_url)::text) <= 1024))
);

-- case: gitlab/ci_gitlab_hosted_runner_monthly_usages.ci_hosted_runner_monthly_usages_month_constraint
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE ci_gitlab_hosted_runner_monthly_usages (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  billing_month date NOT NULL,
  CONSTRAINT ci_hosted_runner_monthly_usages_month_constraint CHECK ((billing_month = date_trunc('month'::text, (billing_month)::timestamp with time zone)))
);

-- case: gitlab/ai_flow_triggers.check_ai_flow_triggers_user_consumer_mutually_exclusive
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE ai_flow_triggers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id bigint,
  ai_catalog_item_consumer_id bigint,
  CONSTRAINT check_ai_flow_triggers_user_consumer_mutually_exclusive CHECK ((num_nonnulls(ai_catalog_item_consumer_id, user_id) <= 1))
);

-- case: gitlab/import_sync_external_objects.chk_import_sync_external_objects_kind
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE import_sync_external_objects (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  kind smallint NOT NULL,
  CONSTRAINT chk_import_sync_external_objects_kind CHECK (((kind >= 0) AND (kind <= 4)))
);

-- case: gitlab/analytics_dashboards_pointers.chk_analytics_dashboards_pointers_project_or_namespace
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE analytics_dashboards_pointers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  namespace_id bigint,
  project_id bigint,
  CONSTRAINT chk_analytics_dashboards_pointers_project_or_namespace CHECK (((project_id IS NULL) <> (namespace_id IS NULL)))
);

-- case: gitlab/application_settings.app_settings_yaml_max_size_positive
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE application_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  max_yaml_size_bytes bigint NOT NULL DEFAULT 2097152,
  CONSTRAINT app_settings_yaml_max_size_positive CHECK ((max_yaml_size_bytes > 0))
);

-- case: gitlab/background_operation_jobs.check_fc1d4517f5
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE background_operation_jobs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  min_cursor jsonb,
  max_cursor jsonb,
  CONSTRAINT check_fc1d4517f5 CHECK ((num_nonnulls(min_cursor, max_cursor) = 2))
);

-- case: gitlab/virtual_registries_container_registry_upstreams.check_918a83b32b
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE virtual_registries_container_registry_upstreams (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  position smallint NOT NULL DEFAULT 1,
  CONSTRAINT check_918a83b32b CHECK (((1 <= "position") AND ("position" <= 5)))
);

-- case: gitlab/application_settings.check_app_settings_namespace_storage_forks_cost_factor_range
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE application_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  namespace_storage_forks_cost_factor double precision NOT NULL DEFAULT 1.0,
  CONSTRAINT check_app_settings_namespace_storage_forks_cost_factor_range CHECK (((namespace_storage_forks_cost_factor >= (0)::double precision) AND (namespace_storage_forks_cost_factor <= (1)::double precision)))
);

-- case: gitlab/ai_active_context_migrations.c_ai_active_context_migrations_on_retries_left
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE ai_active_context_migrations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status smallint NOT NULL DEFAULT 0,
  retries_left smallint NOT NULL,
  CONSTRAINT c_ai_active_context_migrations_on_retries_left CHECK (((retries_left > 0) OR ((retries_left = 0) AND (status = 255))))
);

-- case: gitlab/dependency_proxy_group_settings.check_7ed6c2f608
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE dependency_proxy_group_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  identity jsonb,
  secret jsonb,
  CONSTRAINT check_7ed6c2f608 CHECK (((num_nonnulls(identity, secret) = 2) OR (num_nulls(identity, secret) = 2)))
);

-- case: gitlab/dora_daily_metrics.dora_daily_metrics_deployment_frequency_positive
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE dora_daily_metrics (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  deployment_frequency integer,
  CONSTRAINT dora_daily_metrics_deployment_frequency_positive CHECK ((deployment_frequency >= 0))
);

-- case: gitlab/background_operation_workers.check_c74b62c410
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE background_operation_workers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  batch_size integer NOT NULL,
  sub_batch_size integer NOT NULL,
  CONSTRAINT check_c74b62c410 CHECK ((batch_size >= sub_batch_size))
);

-- case: gitlab/sprints.check_ccd8a1eae0
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE sprints (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  start_date date,
  CONSTRAINT check_ccd8a1eae0 CHECK ((start_date IS NOT NULL))
);

-- case: gitlab/ml_candidates.check_cd160587d4
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE ml_candidates (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  eid uuid,
  CONSTRAINT check_cd160587d4 CHECK ((eid IS NOT NULL))
);

-- case: gitlab/ci_runner_taggings_instance_type.check_organization_id_nullness
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE ci_runner_taggings_instance_type (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  organization_id bigint,
  CONSTRAINT check_organization_id_nullness CHECK ((organization_id IS NULL))
);

-- case: gitlab/packages_conan_package_references.chk_conan_references_info_length
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE packages_conan_package_references (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  info jsonb NOT NULL DEFAULT '{}'::jsonb,
  CONSTRAINT chk_conan_references_info_length CHECK ((char_length((info)::text) <= 20000))
);

-- case: gitlab/approval_merge_request_rules.check_501a843588
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE approval_merge_request_rules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  coverage_minimum_threshold double precision,
  CONSTRAINT check_501a843588 CHECK (((coverage_minimum_threshold IS NULL) OR ((coverage_minimum_threshold >= (0)::double precision) AND (coverage_minimum_threshold <= (100)::double precision))))
);

-- case: gitlab/ai_tool_rules.chk_ai_tool_rules_local_access_enum
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE ai_tool_rules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  local_access smallint,
  CONSTRAINT chk_ai_tool_rules_local_access_enum CHECK (((local_access IS NULL) OR (local_access = ANY (ARRAY[0, 1, 2]))))
);

-- case: gitlab/ai_tool_rules.chk_ai_tool_rules_background_access_enum
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE ai_tool_rules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  background_access smallint,
  CONSTRAINT chk_ai_tool_rules_background_access_enum CHECK (((background_access IS NULL) OR (background_access = ANY (ARRAY[0, 2]))))
);

-- case: gitlab/import_sync_repositories.chk_import_sync_repositories_installation_xid
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE import_sync_repositories (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  provider_installation_xid bigint,
  CONSTRAINT chk_import_sync_repositories_installation_xid CHECK (((provider_installation_xid IS NULL) OR (provider_installation_xid > 0)))
);

-- case: gitlab/batched_background_migration_jobs.check_c1ce96fe3b
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE batched_background_migration_jobs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  min_value bigint,
  max_value bigint,
  min_cursor jsonb,
  max_cursor jsonb,
  CONSTRAINT check_c1ce96fe3b CHECK (((num_nonnulls(min_value, max_value) = 2) OR (num_nonnulls(min_cursor, max_cursor) = 2)))
);

-- case: gitlab/duo_workflows_workflow_workflows.check_duo_wf_wf_wf_no_self_link
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE duo_workflows_workflow_workflows (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  workflow_id bigint NOT NULL,
  linked_workflow_id bigint NOT NULL,
  CONSTRAINT check_duo_wf_wf_wf_no_self_link CHECK ((workflow_id <> linked_workflow_id))
);

-- case: gitlab/catalog_bundled_resources.check_catalog_bundled_resources_fqdn_lowercase
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE catalog_bundled_resources (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  server_fqdn text NOT NULL,
  CONSTRAINT check_catalog_bundled_resources_fqdn_lowercase CHECK ((server_fqdn = lower(server_fqdn)))
);

-- case: gitlab/billable_usage_daily_namespace_aggregates.check_billable_usage_daily_ns_aggs_quantity_non_negative
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE billable_usage_daily_namespace_aggregates (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  quantity numeric(14,4) NOT NULL,
  CONSTRAINT check_billable_usage_daily_ns_aggs_quantity_non_negative CHECK ((quantity >= (0)::numeric))
);

-- case: gitlab/application_settings.app_settings_failed_login_attempts_unlock_period_positive
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE application_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  failed_login_attempts_unlock_period_in_minutes integer,
  CONSTRAINT app_settings_failed_login_attempts_unlock_period_positive CHECK ((failed_login_attempts_unlock_period_in_minutes > 0))
);

-- case: gitlab/notes.check_1244cbd7d0
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE notes (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  noteable_type character varying,
  CONSTRAINT check_1244cbd7d0 CHECK ((noteable_type IS NOT NULL))
);

-- case: gitlab/historical_data.check_640e8cf66c
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE historical_data (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  recorded_at timestamp with time zone,
  CONSTRAINT check_640e8cf66c CHECK ((recorded_at IS NOT NULL))
);

-- case: gitlab/ai_active_context_migrations.c_ai_active_context_migrations_version_format
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE ai_active_context_migrations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version text NOT NULL,
  CONSTRAINT c_ai_active_context_migrations_version_format CHECK ((version ~ '^[0-9]{14}$'::text))
);

-- case: gitlab/dast_site_profile_secret_variables.check_236213f179
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE dast_site_profile_secret_variables (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  encrypted_value bytea NOT NULL,
  CONSTRAINT check_236213f179 CHECK ((length(encrypted_value) <= 13352))
);

-- case: gitlab/ai_governance_sessions.check_ai_session_is_duo_or_external
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE ai_governance_sessions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  workflow_id bigint,
  external_xid text,
  CONSTRAINT check_ai_session_is_duo_or_external CHECK ((num_nonnulls(workflow_id, external_xid) = 1))
);

-- case: gitlab/ai_audit_events.check_ai_audit_event_has_workflow_or_session
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE ai_audit_events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  workflow_id bigint,
  ai_governance_session_id bigint,
  CONSTRAINT check_ai_audit_event_has_workflow_or_session CHECK ((num_nonnulls(ai_governance_session_id, workflow_id) >= 1))
);

-- case: gitlab/container_registry_protection_rules.check_0dc4ab5f43
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE container_registry_protection_rules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  minimum_access_level_for_push smallint,
  minimum_access_level_for_delete smallint,
  CONSTRAINT check_0dc4ab5f43 CHECK ((num_nonnulls(minimum_access_level_for_delete, minimum_access_level_for_push) > 0))
);

-- case: gitlab/application_settings.default_branch_protection_defaults_size_constraint
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE application_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  default_branch_protection_defaults jsonb NOT NULL DEFAULT '{}'::jsonb,
  CONSTRAINT default_branch_protection_defaults_size_constraint CHECK ((octet_length((default_branch_protection_defaults)::text) <= 1024))
);

-- case: gitlab/ci_instance_variables.check_956afd70f2
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE ci_instance_variables (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  variable_type smallint NOT NULL DEFAULT 1,
  encrypted_value text,
  CONSTRAINT check_956afd70f2 CHECK ((((variable_type = 2) AND (char_length(encrypted_value) <= 67800)) OR (char_length(encrypted_value) <= 13579)))
);

-- case: gitlab/vs_code_settings.check_vs_code_settings_settings_context_hash_nullable
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE vs_code_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  setting_type text NOT NULL,
  settings_context_hash text,
  CONSTRAINT check_vs_code_settings_settings_context_hash_nullable CHECK ((((setting_type = 'extensions'::text) AND (settings_context_hash IS NOT NULL)) OR ((setting_type <> 'extensions'::text) AND (settings_context_hash IS NULL))))
);

-- case: gitlab/protected_environment_approval_rules.chk_rails_bed75249bc
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE protected_environment_approval_rules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id bigint,
  group_id bigint,
  access_level smallint,
  CONSTRAINT chk_rails_bed75249bc CHECK ((((access_level IS NOT NULL) AND (group_id IS NULL) AND (user_id IS NULL)) OR ((user_id IS NOT NULL) AND (access_level IS NULL) AND (group_id IS NULL)) OR ((group_id IS NOT NULL) AND (user_id IS NULL) AND (access_level IS NULL))))
);

-- case: gitlab/merge_request_diff_commit_users.merge_request_diff_commit_users_name_or_email_existence
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE merge_request_diff_commit_users (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name text,
  email text,
  CONSTRAINT merge_request_diff_commit_users_name_or_email_existence CHECK (((COALESCE(name, ''::text) <> ''::text) OR (COALESCE(email, ''::text) <> ''::text)))
);

-- case: gitlab/workspaces_agent_configs.check_6d7baef494
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE workspaces_agent_configs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  max_active_hours_before_stop smallint NOT NULL DEFAULT 36,
  max_stopped_hours_before_termination smallint NOT NULL DEFAULT 744,
  CONSTRAINT check_6d7baef494 CHECK (((max_active_hours_before_stop + max_stopped_hours_before_termination) <= 8760))
);

-- case: gitlab/boards_epic_lists.boards_epic_lists_position_constraint
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE boards_epic_lists (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  position integer,
  list_type smallint NOT NULL DEFAULT 1,
  CONSTRAINT boards_epic_lists_position_constraint CHECK (((list_type <> 1) OR (("position" IS NOT NULL) AND ("position" >= 0))))
);

-- case: gitlab/dependency_firewall_activity_stats.check_allowed_outcome_has_null_rule
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE dependency_firewall_activity_stats (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  dependency_firewall_policy_rule_id bigint,
  outcome smallint NOT NULL,
  CONSTRAINT check_allowed_outcome_has_null_rule CHECK (((outcome <> 2) OR (dependency_firewall_policy_rule_id IS NULL)))
);

-- case: gitlab/lists.check_5ed14cb08c
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE lists (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  list_type integer NOT NULL DEFAULT 1,
  custom_status_id bigint,
  system_defined_status_identifier smallint,
  CONSTRAINT check_5ed14cb08c CHECK (((list_type <> 6) OR (num_nonnulls(system_defined_status_identifier, custom_status_id) = 1)))
);

-- case: gitlab/pm_checkpoints.check_pm_checkpoints_data_type_and_purl_type
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE pm_checkpoints (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  purl_type smallint,
  data_type smallint NOT NULL DEFAULT 1,
  CONSTRAINT check_pm_checkpoints_data_type_and_purl_type CHECK (((data_type = 3) OR (purl_type IS NOT NULL)))
);

-- case: gitlab/approval_merge_request_rules.check_approval_m_r_rules_allowed_role_approvers_valid_entries
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE approval_merge_request_rules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  role_approvers integer[] NOT NULL DEFAULT '{}'::integer[],
  CONSTRAINT check_approval_m_r_rules_allowed_role_approvers_valid_entries CHECK (((role_approvers = '{}'::integer[]) OR (role_approvers <@ ARRAY[20, 30, 40, 50, 60])))
);

-- case: gitlab/vulnerability_merge_request_links.check_7350959ecc
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE vulnerability_merge_request_links (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  readiness_score double precision,
  CONSTRAINT check_7350959ecc CHECK (((readiness_score >= (0)::double precision) AND (readiness_score <= (1)::double precision)))
);

-- case: gitlab/duo_code_review_findings.chk_duo_code_review_findings_confidence
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE duo_code_review_findings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  confidence smallint,
  CONSTRAINT chk_duo_code_review_findings_confidence CHECK (((confidence >= 0) AND (confidence <= 10)))
);

-- case: gitlab/zoekt_repositories.c_zoekt_repositories_on_retries_left
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE zoekt_repositories (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  state smallint NOT NULL DEFAULT 0,
  retries_left smallint NOT NULL DEFAULT 10,
  CONSTRAINT c_zoekt_repositories_on_retries_left CHECK (((retries_left > 0) OR ((retries_left = 0) AND (state >= 200))))
);

-- case: gitlab/bulk_import_trackers.check_next_page_requirement
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE bulk_import_trackers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  next_page text,
  has_next_page boolean NOT NULL DEFAULT false,
  CONSTRAINT check_next_page_requirement CHECK (((has_next_page IS FALSE) OR (next_page IS NOT NULL)))
);

-- case: gitlab/scan_result_policy_violations.chk_policy_violations_rule_id_or_policy_id_not_null
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE scan_result_policy_violations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  scan_result_policy_id bigint,
  approval_policy_rule_id bigint,
  CONSTRAINT chk_policy_violations_rule_id_or_policy_id_not_null CHECK (((approval_policy_rule_id IS NOT NULL) OR (scan_result_policy_id IS NOT NULL)))
);

-- case: gitlab/events.check_events_sharding_key_is_not_null
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  project_id bigint,
  group_id bigint,
  personal_namespace_id bigint,
  CONSTRAINT check_events_sharding_key_is_not_null CHECK (((group_id IS NOT NULL) OR (project_id IS NOT NULL) OR (personal_namespace_id IS NOT NULL)))
);

-- case: gitlab/ai_tool_rules.chk_ai_tool_rules_has_permission
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE ai_tool_rules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  web_access smallint,
  local_access smallint,
  background_access smallint,
  CONSTRAINT chk_ai_tool_rules_has_permission CHECK (((web_access IS NOT NULL) OR (local_access IS NOT NULL) OR (background_access IS NOT NULL)))
);

-- case: gitlab/import_sync_repositories.chk_import_sync_repositories_disconnect_reason
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE import_sync_repositories (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  disconnect_reason smallint,
  CONSTRAINT chk_import_sync_repositories_disconnect_reason CHECK (((disconnect_reason IS NULL) OR ((disconnect_reason >= 0) AND (disconnect_reason <= 9))))
);

-- case: gitlab/dependency_firewall_prevented_packages.check_dep_fw_prevented_packages_severity_rule_type
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE dependency_firewall_prevented_packages (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  rule_type smallint NOT NULL,
  severity smallint,
  CONSTRAINT check_dep_fw_prevented_packages_severity_rule_type CHECK (((severity IS NULL) OR (rule_type = ANY (ARRAY[1, 4]))))
);

-- case: gitlab/duo_workflows_workflows.check_duo_workflows_workflows_parent_workflow_id_flow
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE duo_workflows_workflows (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  trigger_source smallint NOT NULL DEFAULT 0,
  parent_workflow_id bigint,
  CONSTRAINT check_duo_workflows_workflows_parent_workflow_id_flow CHECK (((parent_workflow_id IS NULL) OR (trigger_source = 4)))
);

-- case: gitlab/zoekt_repositories.c_zoekt_repositories_on_project_id_and_project_identifier
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE zoekt_repositories (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  project_id bigint,
  project_identifier bigint NOT NULL,
  CONSTRAINT c_zoekt_repositories_on_project_id_and_project_identifier CHECK (((project_id IS NULL) OR (project_identifier = project_id)))
);

-- case: gitlab/scan_result_policies.age_value_null_or_positive
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE scan_result_policies (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  age_value integer,
  CONSTRAINT age_value_null_or_positive CHECK (((age_value IS NULL) OR (age_value >= 0)))
);

-- case: gitlab/scan_result_policies.check_scan_result_policies_rule_idx_positive
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE scan_result_policies (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  rule_idx smallint,
  CONSTRAINT check_scan_result_policies_rule_idx_positive CHECK (((rule_idx IS NULL) OR (rule_idx >= 0)))
);

-- case: gitlab/work_item_custom_status_mappings.check_34fa9b844a
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE work_item_custom_status_mappings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  valid_from timestamp with time zone,
  valid_until timestamp with time zone,
  CONSTRAINT check_34fa9b844a CHECK (((valid_from IS NULL) OR (valid_until IS NULL) OR (valid_from < valid_until)))
);

-- case: gitlab/cd_rollout_steps.check_cd_rollout_steps_params_is_hash
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE cd_rollout_steps (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  params jsonb,
  CONSTRAINT check_cd_rollout_steps_params_is_hash CHECK (((params IS NULL) OR (jsonb_typeof(params) = 'object'::text)))
);

-- case: gitlab/analytics_cycle_analytics_value_stream_settings.project_ids_filter_array_check
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE analytics_cycle_analytics_value_stream_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  project_ids_filter bigint[] DEFAULT '{}'::bigint[],
  CONSTRAINT project_ids_filter_array_check CHECK (((cardinality(project_ids_filter) <= 100) AND (array_position(project_ids_filter, NULL::bigint) IS NULL)))
);

-- case: gitlab/approval_policy_merge_request_bypass_events.check_3169f0d109
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE approval_policy_merge_request_bypass_events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  reason text NOT NULL,
  CONSTRAINT check_3169f0d109 CHECK (((length(TRIM(BOTH FROM reason)) >= 1) AND (length(TRIM(BOTH FROM reason)) <= 1024)))
);

-- case: gitlab/dependency_proxy_packages_settings.check_14a2818907
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE dependency_proxy_packages_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  encrypted_maven_external_registry_username bytea,
  encrypted_maven_external_registry_password bytea,
  CONSTRAINT check_14a2818907 CHECK (((num_nulls(encrypted_maven_external_registry_username, encrypted_maven_external_registry_password) = 0) OR (num_nulls(encrypted_maven_external_registry_username, encrypted_maven_external_registry_password) = 2)))
);

-- case: gitlab/packages_conan_file_metadata.check_conan_file_metadata_ref_null_for_recipe_files
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE packages_conan_file_metadata (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  conan_file_type smallint NOT NULL,
  package_reference_id bigint,
  CONSTRAINT check_conan_file_metadata_ref_null_for_recipe_files CHECK ((NOT ((conan_file_type = 1) AND (package_reference_id IS NOT NULL))))
);

-- case: gitlab/scan_result_policies.custom_roles_array_check
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE scan_result_policies (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  custom_roles bigint[] NOT NULL DEFAULT '{}'::bigint[],
  CONSTRAINT custom_roles_array_check CHECK ((array_position(custom_roles, NULL::bigint) IS NULL))
);

-- case: gitlab/work_item_custom_statuses.check_custom_status_name_characters
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE work_item_custom_statuses (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name text NOT NULL,
  CONSTRAINT check_custom_status_name_characters CHECK ((name !~ '^["''`]|["''`]$|[\x00-\x1F\x7F]'::text))
);

-- case: gitlab/billable_usage_daily_namespace_aggregates.check_billable_usage_daily_ns_aggs_quantity_within_ceiling
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE billable_usage_daily_namespace_aggregates (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  quantity numeric(14,4) NOT NULL,
  CONSTRAINT check_billable_usage_daily_ns_aggs_quantity_within_ceiling CHECK ((quantity <= (2147483647)::numeric))
);

-- case: gitlab/import_sync_repository_states.chk_import_sync_repository_states_generation_order
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE import_sync_repository_states (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  desired_generation bigint NOT NULL DEFAULT 0,
  synced_generation bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_import_sync_repository_states_generation_order CHECK ((synced_generation <= desired_generation))
);

-- case: gitlab/targeted_messages.check_targeted_messages_starts_at_before_ends_at
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE targeted_messages (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  starts_at timestamp with time zone NOT NULL,
  ends_at timestamp with time zone NOT NULL,
  CONSTRAINT check_targeted_messages_starts_at_before_ends_at CHECK ((starts_at < ends_at))
);

-- case: gitlab/virtual_registries_cleanup_policies.chk_rails_d543512016
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE virtual_registries_cleanup_policies (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  cadence smallint NOT NULL DEFAULT 7,
  CONSTRAINT chk_rails_d543512016 CHECK ((cadence = ANY (ARRAY[1, 7, 14, 30, 90])))
);

-- case: gitlab/dependency_firewall_namespace_prevented_stats.check_dep_fw_ns_prevented_stats_window_days
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE dependency_firewall_namespace_prevented_stats (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  window_days smallint NOT NULL,
  CONSTRAINT check_dep_fw_ns_prevented_stats_window_days CHECK ((window_days = ANY (ARRAY[7, 30])))
);

-- case: gitlab/import_sync_repositories.chk_import_sync_repositories_provider
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE import_sync_repositories (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  provider smallint NOT NULL DEFAULT 0,
  CONSTRAINT chk_import_sync_repositories_provider CHECK ((provider = 0))
);

-- case: gitlab/trial_usages.check_trial_usages_credits_used_non_negative
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE trial_usages (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  credits_used numeric(14,4),
  CONSTRAINT check_trial_usages_credits_used_non_negative CHECK ((credits_used >= (0)::numeric))
);

-- case: gitlab/virtual_registries_cleanup_policies.chk_rails_8c9cfeda41
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE virtual_registries_cleanup_policies (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  last_run_deleted_size bigint DEFAULT 0,
  CONSTRAINT chk_rails_8c9cfeda41 CHECK ((last_run_deleted_size >= 0))
);

-- case: gitlab/batched_background_migrations.check_max_value_in_range
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE batched_background_migrations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  min_value bigint DEFAULT 1,
  max_value bigint,
  CONSTRAINT check_max_value_in_range CHECK ((max_value >= min_value))
);

-- case: gitlab/billable_usage_export_logs.check_billable_usage_export_logs_period_end_after_start
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE billable_usage_export_logs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  period_start date NOT NULL,
  period_end date NOT NULL,
  CONSTRAINT check_billable_usage_export_logs_period_end_after_start CHECK ((period_end >= period_start))
);

-- case: gitlab/batched_background_migrations.check_positive_min_value
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE batched_background_migrations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  min_value bigint DEFAULT 1,
  CONSTRAINT check_positive_min_value CHECK ((min_value > 0))
);

-- case: gitlab/trial_usages.check_trial_usages_trial_ends_on_after_starts_on
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE trial_usages (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  trial_starts_on date NOT NULL,
  trial_ends_on date NOT NULL,
  CONSTRAINT check_trial_usages_trial_ends_on_after_starts_on CHECK ((trial_ends_on > trial_starts_on))
);

-- case: gitlab/vulnerability_identifiers.check_e0c2cd4f3e
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE vulnerability_identifiers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  partition_id bigint NOT NULL DEFAULT 1,
  CONSTRAINT check_e0c2cd4f3e CHECK ((partition_id IS NOT NULL))
);

-- case: gitlab/security_policy_dismissals.check_88beed9dc9
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE security_policy_dismissals (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  security_findings_uuids text[] DEFAULT '{}'::text[],
  CONSTRAINT check_88beed9dc9 CHECK ((security_findings_uuids IS NOT NULL))
);

-- case: gitlab/vulnerability_statistics.check_vulnerability_statistics_traversal_ids_not_empty
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE vulnerability_statistics (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  traversal_ids bigint[] NOT NULL DEFAULT '{}'::bigint[],
  CONSTRAINT check_vulnerability_statistics_traversal_ids_not_empty CHECK ((cardinality(traversal_ids) > 0))
);

-- case: gitlab/design_management_designs.check_07155e2715
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE design_management_designs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  filename character varying NOT NULL,
  CONSTRAINT check_07155e2715 CHECK ((char_length((filename)::text) <= 255))
);

-- case: gitlab/packages_npm_metadata.chk_rails_e5cbc301ae
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE packages_npm_metadata (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  package_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  CONSTRAINT chk_rails_e5cbc301ae CHECK ((char_length((package_json)::text) < 20000))
);

-- case: gitlab/application_settings.check_application_settings_duo_workflow_is_hash
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE application_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  duo_workflow jsonb DEFAULT '{}'::jsonb,
  CONSTRAINT check_application_settings_duo_workflow_is_hash CHECK ((jsonb_typeof(duo_workflow) = 'object'::text))
);

-- case: gitlab/container_registry_protection_tag_rules.check_ae29637175
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE container_registry_protection_tag_rules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  minimum_access_level_for_push smallint,
  minimum_access_level_for_delete smallint,
  CONSTRAINT check_ae29637175 CHECK ((num_nonnulls(minimum_access_level_for_delete, minimum_access_level_for_push) <> 1))
);

-- case: gitlab/pm_licenses.check_59a1ddda07
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE pm_licenses (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  spdx_identifier text,
  spdx_expression text,
  CONSTRAINT check_59a1ddda07 CHECK ((num_nonnulls(spdx_expression, spdx_identifier) = 1))
);

-- case: gitlab/dependency_firewall_prevented_packages.check_dep_fw_prevented_packages_timestamps
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE dependency_firewall_prevented_packages (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  last_blocked_at timestamp with time zone,
  last_warned_at timestamp with time zone,
  CONSTRAINT check_dep_fw_prevented_packages_timestamps CHECK ((num_nonnulls(last_blocked_at, last_warned_at) >= 1))
);

-- case: gitlab/protected_environment_deploy_access_levels.check_deploy_access_levels_user_group_access_level_any_not_null
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE protected_environment_deploy_access_levels (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  access_level integer,
  user_id bigint,
  group_id bigint,
  CONSTRAINT check_deploy_access_levels_user_group_access_level_any_not_null CHECK ((num_nonnulls(user_id, group_id, access_level) = 1))
);

-- case: gitlab/notes.check_82f260979e
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE notes (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  project_id bigint,
  namespace_id bigint,
  organization_id bigint,
  CONSTRAINT check_82f260979e CHECK ((num_nonnulls(namespace_id, organization_id, project_id) >= 1))
);

-- case: gitlab/dependency_proxy_packages_settings.check_93afb1690f
-- source: https://github.com/gitlabhq/gitlabhq/blob/d7b9a6176ab4e279465ba3ee09922cde2b74de47/db/structure.sql
-- license: MIT
CREATE TABLE dependency_proxy_packages_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  encrypted_npm_external_registry_basic_auth bytea,
  encrypted_npm_external_registry_auth_token bytea,
  CONSTRAINT check_93afb1690f CHECK ((num_nulls(encrypted_npm_external_registry_basic_auth, encrypted_npm_external_registry_auth_token) > 0))
);
