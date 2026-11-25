-- case: 0xCompute/rubidity/contract_calls.from_address_format
-- source: https://github.com/0xCompute/rubidity/blob/5138a1bfad196a6b635d0dd360f18955a824ac8d/upstream/schema/schema.rb
-- license: CC0-1.0
CREATE TABLE contract_calls (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  from_address character varying NOT NULL,
  CONSTRAINT from_address_format CHECK (((from_address)::text ~ '^0x[a-f0-9]{40}$'::text))
);

-- case: 0xCompute/rubidity/contract_calls.created_contract_address_format
-- source: https://github.com/0xCompute/rubidity/blob/5138a1bfad196a6b635d0dd360f18955a824ac8d/upstream/schema/schema.rb
-- license: CC0-1.0
CREATE TABLE contract_calls (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  created_contract_address character varying,
  CONSTRAINT created_contract_address_format CHECK (((created_contract_address IS NULL) OR ((created_contract_address)::text ~ '^0x[a-f0-9]{40}$'::text)))
);

-- case: 0xCompute/rubidity/contract_calls.effective_contract_address_correct
-- source: https://github.com/0xCompute/rubidity/blob/5138a1bfad196a6b635d0dd360f18955a824ac8d/upstream/schema/schema.rb
-- license: CC0-1.0
CREATE TABLE contract_calls (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  to_contract_address character varying,
  created_contract_address character varying,
  effective_contract_address character varying,
  call_type integer NOT NULL,
  CONSTRAINT effective_contract_address_correct CHECK ((((call_type = 2) AND ((effective_contract_address)::text = (created_contract_address)::text)) OR ((call_type <> 2) AND ((effective_contract_address)::text = (to_contract_address)::text))))
);

-- case: 0xCompute/rubidity/contract_calls.status_0_logs_empty_or_status_not_0
-- source: https://github.com/0xCompute/rubidity/blob/5138a1bfad196a6b635d0dd360f18955a824ac8d/upstream/schema/schema.rb
-- license: CC0-1.0
CREATE TABLE contract_calls (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  logs jsonb NOT NULL DEFAULT '{}'::jsonb,
  status integer NOT NULL,
  CONSTRAINT status_0_logs_empty_or_status_not_0 CHECK ((((status = 0) AND (logs = '[]'::jsonb)) OR (status <> 0)))
);

-- case: 0xCompute/rubidity/contract_calls.status_0_error_or_status_not_0_error
-- source: https://github.com/0xCompute/rubidity/blob/5138a1bfad196a6b635d0dd360f18955a824ac8d/upstream/schema/schema.rb
-- license: CC0-1.0
CREATE TABLE contract_calls (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  error character varying,
  status integer NOT NULL,
  CONSTRAINT status_0_error_or_status_not_0_error CHECK ((((status = 0) AND (error IS NOT NULL)) OR ((status <> 0) AND (error IS NULL))))
);

-- case: 0xCompute/rubidity/contract_calls.call_type_2_error_or_created_contract_address2
-- source: https://github.com/0xCompute/rubidity/blob/5138a1bfad196a6b635d0dd360f18955a824ac8d/upstream/schema/schema.rb
-- license: CC0-1.0
CREATE TABLE contract_calls (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  created_contract_address character varying,
  call_type integer NOT NULL,
  error character varying,
  CONSTRAINT call_type_2_error_or_created_contract_address2 CHECK ((((call_type = 2) AND (error IS NULL)) OR (created_contract_address IS NULL)))
);

-- case: andersonkrs/malheatmap/items.items_kind_check
-- source: https://github.com/andersonkrs/malheatmap/blob/bbdac0c780773586fdebd0c933ca188cc5dd7a84/db/schema.rb
-- license: MIT
CREATE TABLE items (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  kind character varying NOT NULL,
  CONSTRAINT items_kind_check CHECK (((kind)::text = ANY ((ARRAY['anime'::character varying, 'manga'::character varying])::text[])))
);

-- case: andersonkrs/malheatmap/entries.entries_amount_check
-- source: https://github.com/andersonkrs/malheatmap/blob/bbdac0c780773586fdebd0c933ca188cc5dd7a84/db/schema.rb
-- license: MIT
CREATE TABLE entries (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  amount integer NOT NULL,
  CONSTRAINT entries_amount_check CHECK ((amount > 0))
);

-- case: Approximated-Inc/rails-custom-domains-example/custom_domains.custom_domains_lifecycle
-- source: https://github.com/Approximated-Inc/rails-custom-domains-example/blob/7e5444cf0f916efd337ef7111f26e94c347b8a05/db/schema.rb
-- license: MIT
CREATE TABLE custom_domains (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  lifecycle character varying NOT NULL DEFAULT 'unconfirmed'::character varying,
  CONSTRAINT custom_domains_lifecycle CHECK (((lifecycle)::text = ANY ((ARRAY['provisioning'::character varying, 'active'::character varying, 'unconfirmed'::character varying, 'removal_pending'::character varying, 'fixture'::character varying])::text[])))
);

-- case: ashwin47/splitmysub/billing_configs.final_notice_days_range
-- source: https://github.com/ashwin47/splitmysub/blob/5c863f96d7ebebf1cfa852fb1a29c0634a0a9e3f/db/schema.rb
-- license: MIT
CREATE TABLE billing_configs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  final_notice_days_overdue integer NOT NULL DEFAULT 14,
  CONSTRAINT final_notice_days_range CHECK (((final_notice_days_overdue >= 0) AND (final_notice_days_overdue <= 90)))
);

-- case: ashwin47/splitmysub/billing_configs.archiving_months_threshold_range
-- source: https://github.com/ashwin47/splitmysub/blob/5c863f96d7ebebf1cfa852fb1a29c0634a0a9e3f/db/schema.rb
-- license: MIT
CREATE TABLE billing_configs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  archiving_months_threshold integer NOT NULL DEFAULT 6,
  CONSTRAINT archiving_months_threshold_range CHECK (((archiving_months_threshold > 0) AND (archiving_months_threshold <= 24)))
);

-- case: ashwin47/splitmysub/projects.projects_currency_valid
-- source: https://github.com/ashwin47/splitmysub/blob/5c863f96d7ebebf1cfa852fb1a29c0634a0a9e3f/db/schema.rb
-- license: MIT
CREATE TABLE projects (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  currency character varying NOT NULL DEFAULT 'USD'::character varying,
  CONSTRAINT projects_currency_valid CHECK (((currency)::text = ANY ((ARRAY['USD'::character varying, 'EUR'::character varying, 'GBP'::character varying, 'CAD'::character varying, 'AUD'::character varying, 'JPY'::character varying, 'CHF'::character varying, 'CNY'::character varying, 'INR'::character varying, 'BRL'::character varying, 'MXN'::character varying, 'KRW'::character varying, 'SGD'::character varying, 'HKD'::character varying, 'NZD'::character varying, 'SEK'::character varying, 'NOK'::character varying, 'DKK'::character varying, 'PLN'::character varying, 'CZK'::character varying, 'HUF'::character varying, 'BGN'::character varying, 'RON'::character varying, 'HRK'::character varying, 'RUB'::character varying, 'TRY'::character varying, 'ZAR'::character varying, 'THB'::character varying, 'MYR'::character varying, 'IDR'::character varying, 'PHP'::character varying, 'VND'::character varying])::text[])))
);

-- case: badBlackShark/shrkbot/channel_overwrites.channel_overwrites_target_type_check
-- source: https://github.com/badBlackShark/shrkbot/blob/3d586d560d33d383e81b47aa13112968867744d2/db/schema.rb
-- license: MIT
CREATE TABLE channel_overwrites (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  target_type character varying NOT NULL,
  CONSTRAINT channel_overwrites_target_type_check CHECK (((target_type)::text = ANY (ARRAY[('role'::character varying)::text, ('member'::character varying)::text])))
);

-- case: badBlackShark/shrkbot/image_scanning_settings.image_scanning_settings_confirmed_timeout_seconds_check
-- source: https://github.com/badBlackShark/shrkbot/blob/3d586d560d33d383e81b47aa13112968867744d2/db/schema.rb
-- license: MIT
CREATE TABLE image_scanning_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  confirmed_timeout_seconds integer NOT NULL DEFAULT 3600,
  CONSTRAINT image_scanning_settings_confirmed_timeout_seconds_check CHECK (((confirmed_timeout_seconds >= 60) AND (confirmed_timeout_seconds <= 2419200)))
);

-- case: badBlackShark/shrkbot/feed_twitch_settings.feed_twitch_settings_live_end_mode_check
-- source: https://github.com/badBlackShark/shrkbot/blob/3d586d560d33d383e81b47aa13112968867744d2/db/schema.rb
-- license: MIT
CREATE TABLE feed_twitch_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  live_end_mode character varying NOT NULL DEFAULT 'mark_ended_with_vod'::character varying,
  CONSTRAINT feed_twitch_settings_live_end_mode_check CHECK (((live_end_mode)::text = ANY (ARRAY[('keep'::character varying)::text, ('delete'::character varying)::text, ('mark_ended'::character varying)::text, ('mark_ended_with_vod'::character varying)::text])))
);

-- case: badBlackShark/shrkbot/lfg_pingable_roles.lfg_pingable_roles_min_membership_days_check
-- source: https://github.com/badBlackShark/shrkbot/blob/3d586d560d33d383e81b47aa13112968867744d2/db/schema.rb
-- license: MIT
CREATE TABLE lfg_pingable_roles (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  min_membership_days integer,
  CONSTRAINT lfg_pingable_roles_min_membership_days_check CHECK (((min_membership_days IS NULL) OR ((min_membership_days >= 0) AND (min_membership_days <= 3650))))
);

-- case: badBlackShark/shrkbot/lfg_settings.lfg_settings_allowed_channel_ids_count_check
-- source: https://github.com/badBlackShark/shrkbot/blob/3d586d560d33d383e81b47aa13112968867744d2/db/schema.rb
-- license: MIT
CREATE TABLE lfg_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  allowed_channel_ids bigint[] NOT NULL DEFAULT '{}'::bigint[],
  CONSTRAINT lfg_settings_allowed_channel_ids_count_check CHECK ((cardinality(allowed_channel_ids) <= 50))
);

-- case: badBlackShark/shrkbot/lfg_pingable_roles.lfg_pingable_roles_excluded_role_ids_count_check
-- source: https://github.com/badBlackShark/shrkbot/blob/3d586d560d33d383e81b47aa13112968867744d2/db/schema.rb
-- license: MIT
CREATE TABLE lfg_pingable_roles (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  excluded_role_ids bigint[],
  CONSTRAINT lfg_pingable_roles_excluded_role_ids_count_check CHECK (((excluded_role_ids IS NULL) OR (cardinality(excluded_role_ids) <= 50)))
);

-- case: chloerei/geeknote/ruby_llm_usages.ruby_llm_usages_operation_check
-- source: https://github.com/chloerei/geeknote/blob/819c29f4adad61abc8d41d8aaaf94cca71e2c1b8/db/schema.rb
-- license: MIT
CREATE TABLE ruby_llm_usages (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  operation character varying NOT NULL,
  CONSTRAINT ruby_llm_usages_operation_check CHECK (((operation)::text = ANY (ARRAY[('chat'::character varying)::text, ('embedding'::character varying)::text, ('moderation'::character varying)::text, ('image'::character varying)::text, ('speech'::character varying)::text, ('transcription'::character varying)::text, ('ocr'::character varying)::text, ('rerank'::character varying)::text])))
);

-- case: creativesorcery/goodsorcery_web/apm_hourly_stats.apm_hourly_stats_bucket_db_ms_size
-- source: https://github.com/creativesorcery/goodsorcery_web/blob/94330a979135e807ebbb8b25ae729f97c4b2cc90/db/schema.rb
-- license: MIT
CREATE TABLE apm_hourly_stats (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  bucket_db_ms double precision[] NOT NULL,
  CONSTRAINT apm_hourly_stats_bucket_db_ms_size CHECK ((cardinality(bucket_db_ms) = 31))
);

-- case: creativesorcery/goodsorcery_web/apm_hourly_stats.apm_hourly_stats_bucket_counts_size
-- source: https://github.com/creativesorcery/goodsorcery_web/blob/94330a979135e807ebbb8b25ae729f97c4b2cc90/db/schema.rb
-- license: MIT
CREATE TABLE apm_hourly_stats (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  bucket_counts bigint[] NOT NULL,
  CONSTRAINT apm_hourly_stats_bucket_counts_size CHECK ((cardinality(bucket_counts) = 31))
);

-- case: creativesorcery/goodsorcery_web/uptime_monitors.uptime_monitors_status_check
-- source: https://github.com/creativesorcery/goodsorcery_web/blob/94330a979135e807ebbb8b25ae729f97c4b2cc90/db/schema.rb
-- license: MIT
CREATE TABLE uptime_monitors (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying NOT NULL DEFAULT 'pending'::character varying,
  CONSTRAINT uptime_monitors_status_check CHECK (((status)::text = ANY (ARRAY[('pending'::character varying)::text, ('up'::character varying)::text, ('down'::character varying)::text])))
);

-- case: creativesorcery/goodsorcery_web/alert_rules.alert_rules_threshold_range
-- source: https://github.com/creativesorcery/goodsorcery_web/blob/94330a979135e807ebbb8b25ae729f97c4b2cc90/db/schema.rb
-- license: MIT
CREATE TABLE alert_rules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  threshold integer,
  CONSTRAINT alert_rules_threshold_range CHECK (((threshold IS NULL) OR ((threshold >= 1) AND (threshold <= 100000))))
);

-- case: creativesorcery/goodsorcery_web/uptime_monitors.uptime_monitors_interval_minutes_check
-- source: https://github.com/creativesorcery/goodsorcery_web/blob/94330a979135e807ebbb8b25ae729f97c4b2cc90/db/schema.rb
-- license: MIT
CREATE TABLE uptime_monitors (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  interval_minutes integer NOT NULL DEFAULT 10,
  CONSTRAINT uptime_monitors_interval_minutes_check CHECK ((interval_minutes = ANY (ARRAY[1, 5, 10, 60])))
);

-- case: DFE-Digital/early-years-foundation-recovery/que_lockers.valid_worker_priorities
-- source: https://github.com/DFE-Digital/early-years-foundation-recovery/blob/720c6fd4a3133b895954e603070acb3083a6966a/db/schema.rb
-- license: MIT
CREATE TABLE que_lockers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  worker_priorities integer[] NOT NULL,
  CONSTRAINT valid_worker_priorities CHECK (((array_ndims(worker_priorities) = 1) AND (array_length(worker_priorities, 1) IS NOT NULL)))
);

-- case: DFE-Digital/early-years-foundation-recovery/que_lockers.valid_queues
-- source: https://github.com/DFE-Digital/early-years-foundation-recovery/blob/720c6fd4a3133b895954e603070acb3083a6966a/db/schema.rb
-- license: MIT
CREATE TABLE que_lockers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  queues text[] NOT NULL,
  CONSTRAINT valid_queues CHECK (((array_ndims(queues) = 1) AND (array_length(queues, 1) IS NOT NULL)))
);

-- case: DFE-Digital/early-years-foundation-recovery/que_values.valid_value
-- source: https://github.com/DFE-Digital/early-years-foundation-recovery/blob/720c6fd4a3133b895954e603070acb3083a6966a/db/schema.rb
-- license: MIT
CREATE TABLE que_values (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  value jsonb NOT NULL DEFAULT '{}'::jsonb,
  CONSTRAINT valid_value CHECK ((jsonb_typeof(value) = 'object'::text))
);

-- case: dilolabs/nosia/ruby_llm_usages.ruby_llm_usages_operation_check
-- source: https://github.com/dilolabs/nosia/blob/8b668dd059cc3d529a0aae76ecf61766a8c341ca/db/schema.rb
-- license: MIT
CREATE TABLE ruby_llm_usages (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  operation character varying NOT NULL,
  CONSTRAINT ruby_llm_usages_operation_check CHECK (((operation)::text = ANY (ARRAY['chat'::text, 'embedding'::text, 'moderation'::text, 'image'::text, 'speech'::text, 'transcription'::text, 'ocr'::text, 'rerank'::text, 'judgment'::text, 'video'::text, 'research'::text, 'code_execution'::text])))
);

-- case: gather-community/gather/gdrive_migration_requests.status_enum
-- source: https://github.com/gather-community/gather/blob/86226ade1cd99a1f1fddc963de64e9f33ebfd8d5/db/schema.rb
-- license: MIT
CREATE TABLE gdrive_migration_requests (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(16) NOT NULL DEFAULT 'new'::character varying,
  CONSTRAINT status_enum CHECK (((status)::text = ANY (ARRAY[('new'::character varying)::text, ('opened'::character varying)::text, ('opted_out'::character varying)::text])))
);

-- case: gather-community/gather/reminder_deliveries.reminder_deliveries_741100539
-- source: https://github.com/gather-community/gather/blob/86226ade1cd99a1f1fddc963de64e9f33ebfd8d5/db/schema.rb
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE reminder_deliveries (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  meal_id bigint,
  shift_id bigint,
  CONSTRAINT reminder_deliveries_741100539 CHECK ((((shift_id IS NOT NULL) AND (meal_id IS NULL)) OR ((meal_id IS NOT NULL) AND (shift_id IS NULL))))
);

-- case: gather-community/gather/calendar_eventlet_overrides.eventlet_override_end_offset_within_bounds
-- source: https://github.com/gather-community/gather/blob/86226ade1cd99a1f1fddc963de64e9f33ebfd8d5/db/schema.rb
-- license: MIT
CREATE TABLE calendar_eventlet_overrides (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  end_offset integer,
  CONSTRAINT eventlet_override_end_offset_within_bounds CHECK (((end_offset >= '-43200'::integer) AND (end_offset <= 43200)))
);

-- case: gather-community/gather/calendar_eventlets.eventlet_end_offset_within_bounds
-- source: https://github.com/gather-community/gather/blob/86226ade1cd99a1f1fddc963de64e9f33ebfd8d5/db/schema.rb
-- license: MIT
CREATE TABLE calendar_eventlets (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  end_offset integer NOT NULL DEFAULT 0,
  CONSTRAINT eventlet_end_offset_within_bounds CHECK (((end_offset >= '-43200'::integer) AND (end_offset <= 43200)))
);

-- case: gather-community/gather/gdrive_migration_files.name_length
-- source: https://github.com/gather-community/gather/blob/86226ade1cd99a1f1fddc963de64e9f33ebfd8d5/db/schema.rb
-- license: MIT
CREATE TABLE gdrive_migration_files (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name text NOT NULL,
  CONSTRAINT name_length CHECK ((char_length(name) <= 32767))
);

-- case: gather-community/gather/calendar_nodes.group_id_null
-- source: https://github.com/gather-community/gather/blob/86226ade1cd99a1f1fddc963de64e9f33ebfd8d5/db/schema.rb
-- license: MIT
CREATE TABLE calendar_nodes (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  group_id bigint,
  type character varying NOT NULL,
  CONSTRAINT group_id_null CHECK ((((type)::text <> 'Calendars::Group'::text) OR (((type)::text = 'Calendars::Group'::text) AND (group_id IS NULL))))
);

-- case: github/replica/github_app_configurations.github_app_configurations_positive_app_id
-- source: https://github.com/github/replica/blob/71a3f1a5d9394f709ddef8acd025569eb59cd019/hub/db/schema.rb
-- license: MIT
CREATE TABLE github_app_configurations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  github_app_id bigint NOT NULL,
  CONSTRAINT github_app_configurations_positive_app_id CHECK ((github_app_id > 0))
);

-- case: github/replica/comment_mentions.comment_mentions_login
-- source: https://github.com/github/replica/blob/71a3f1a5d9394f709ddef8acd025569eb59cd019/hub/db/schema.rb
-- license: MIT
CREATE TABLE comment_mentions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  login character varying NOT NULL,
  CONSTRAINT comment_mentions_login CHECK ((length(TRIM(BOTH FROM login)) > 0))
);

-- case: github/replica/github_app_configurations.github_app_configurations_positive_installation_id
-- source: https://github.com/github/replica/blob/71a3f1a5d9394f709ddef8acd025569eb59cd019/hub/db/schema.rb
-- license: MIT
CREATE TABLE github_app_configurations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  installation_id bigint,
  CONSTRAINT github_app_configurations_positive_installation_id CHECK (((installation_id IS NULL) OR (installation_id > 0)))
);

-- case: github/replica/analytics_deliveries.analytics_delivery_attempts
-- source: https://github.com/github/replica/blob/71a3f1a5d9394f709ddef8acd025569eb59cd019/hub/db/schema.rb
-- license: MIT
CREATE TABLE analytics_deliveries (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  attempts integer NOT NULL DEFAULT 0,
  CONSTRAINT analytics_delivery_attempts CHECK ((attempts >= 0))
);

-- case: github/replica/github_app_configurations.github_app_configurations_installation_state
-- source: https://github.com/github/replica/blob/71a3f1a5d9394f709ddef8acd025569eb59cd019/hub/db/schema.rb
-- license: MIT
CREATE TABLE github_app_configurations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  installation_state character varying NOT NULL DEFAULT 'not_installed'::character varying,
  CONSTRAINT github_app_configurations_installation_state CHECK (((installation_state)::text = ANY ((ARRAY['not_installed'::character varying, 'active'::character varying, 'suspended'::character varying, 'removed'::character varying])::text[])))
);

-- case: github/replica/canvas_pairings.canvas_pairings_consumption
-- source: https://github.com/github/replica/blob/71a3f1a5d9394f709ddef8acd025569eb59cd019/hub/db/schema.rb
-- license: MIT
CREATE TABLE canvas_pairings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  approved_at timestamp(6) without time zone,
  consumed_at timestamp(6) without time zone,
  CONSTRAINT canvas_pairings_consumption CHECK (((consumed_at IS NULL) OR (approved_at IS NOT NULL)))
);

-- case: hackclub/hackatime/heartbeat_exclusions.heartbeat_exclusions_poison_shape
-- source: https://github.com/hackclub/hackatime/blob/f4e27107e9e24bb309fb21d5fea01af8d220f9a6/db/schema.rb
-- license: MIT
CREATE TABLE heartbeat_exclusions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ends_at timestamp(6) without time zone,
  kind integer NOT NULL,
  project character varying,
  CONSTRAINT heartbeat_exclusions_poison_shape CHECK (((kind <> 0) OR ((ends_at IS NOT NULL) AND (project IS NULL))))
);

-- case: hackclub/hackatime/heartbeat_exclusions.heartbeat_exclusions_project_deletion_shape
-- source: https://github.com/hackclub/hackatime/blob/f4e27107e9e24bb309fb21d5fea01af8d220f9a6/db/schema.rb
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE heartbeat_exclusions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  kind integer NOT NULL,
  project character varying,
  CONSTRAINT heartbeat_exclusions_project_deletion_shape CHECK (((kind <> 1) OR (project IS NOT NULL)))
);

-- case: hackclub/hackatime/documentation_feedbacks.documentation_feedbacks_have_one_identity
-- source: https://github.com/hackclub/hackatime/blob/f4e27107e9e24bb309fb21d5fea01af8d220f9a6/db/schema.rb
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE documentation_feedbacks (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id bigint,
  visitor_token uuid,
  CONSTRAINT documentation_feedbacks_have_one_identity CHECK (((user_id IS NOT NULL) <> (visitor_token IS NOT NULL)))
);

-- case: hackclub/hackatime/heartbeat_exclusions.heartbeat_exclusions_range_order
-- source: https://github.com/hackclub/hackatime/blob/f4e27107e9e24bb309fb21d5fea01af8d220f9a6/db/schema.rb
-- license: MIT
CREATE TABLE heartbeat_exclusions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ends_at timestamp(6) without time zone,
  starts_at timestamp(6) without time zone,
  CONSTRAINT heartbeat_exclusions_range_order CHECK (((starts_at IS NULL) OR (ends_at IS NULL) OR (starts_at < ends_at)))
);

-- case: hackclub/nemo/channel_audience.channel_audience_kind
-- source: https://github.com/hackclub/nemo/blob/b65b3107b4d93598f8e0585b44d7b919c2089fb2/web/db/schema.rb
-- license: MIT
CREATE TABLE channel_audience (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  audience character varying NOT NULL,
  CONSTRAINT channel_audience_kind CHECK (((audience)::text = ANY (ARRAY[('private'::character varying)::text, ('shared'::character varying)::text, ('everyone'::character varying)::text, ('granted'::character varying)::text, ('public'::character varying)::text])))
);

-- case: hackclub/nemo/grant.grant_effect_known
-- source: https://github.com/hackclub/nemo/blob/b65b3107b4d93598f8e0585b44d7b919c2089fb2/web/db/schema.rb
-- license: MIT
CREATE TABLE "grant" (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  effect text NOT NULL DEFAULT 'allow'::text,
  CONSTRAINT grant_effect_known CHECK ((effect = ANY (ARRAY['allow'::text, 'deny'::text])))
);

-- case: hackclub/nemo/sync_request.sync_request_stage_matches_kind
-- source: https://github.com/hackclub/nemo/blob/b65b3107b4d93598f8e0585b44d7b919c2089fb2/web/db/schema.rb
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE sync_request (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  kind character varying NOT NULL,
  stage character varying,
  CONSTRAINT sync_request_stage_matches_kind CHECK (((((kind)::text = 'full'::text) AND (stage IS NULL)) OR (((kind)::text = 'stage'::text) AND (stage IS NOT NULL))))
);

-- case: hackclub/nemo/sync_request.sync_request_status_known
-- source: https://github.com/hackclub/nemo/blob/b65b3107b4d93598f8e0585b44d7b919c2089fb2/web/db/schema.rb
-- license: MIT
CREATE TABLE sync_request (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying NOT NULL DEFAULT 'queued'::character varying,
  CONSTRAINT sync_request_status_known CHECK (((status)::text = ANY (ARRAY[('queued'::character varying)::text, ('claimed'::character varying)::text, ('done'::character varying)::text, ('failed'::character varying)::text, ('cancelling'::character varying)::text, ('cancelled'::character varying)::text])))
);

-- case: hackclub/nemo/grant.grant_deny_is_a_capability
-- source: https://github.com/hackclub/nemo/blob/b65b3107b4d93598f8e0585b44d7b919c2089fb2/web/db/schema.rb
-- license: MIT
CREATE TABLE "grant" (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  effect text NOT NULL DEFAULT 'allow'::text,
  kind text NOT NULL,
  CONSTRAINT grant_deny_is_a_capability CHECK (((effect = 'allow'::text) OR (kind = 'capability'::text)))
);

-- case: hackclub/nemo/channel_grants.channel_grants_one_subject
-- source: https://github.com/hackclub/nemo/blob/b65b3107b4d93598f8e0585b44d7b919c2089fb2/web/db/schema.rb
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE channel_grants (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  role text,
  user_id character varying,
  CONSTRAINT channel_grants_one_subject CHECK (((user_id IS NULL) <> (role IS NULL)))
);

-- case: hostolab/covidliste/campaign_batches.duration_in_minutes_gt_zero
-- source: https://github.com/hostolab/covidliste/blob/9229721af77297102ecb425cb7d77bcedd09502b/db/schema.rb
-- license: MIT
CREATE TABLE campaign_batches (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  duration_in_minutes integer NOT NULL DEFAULT 10,
  CONSTRAINT duration_in_minutes_gt_zero CHECK ((duration_in_minutes > 0))
);

-- case: hostolab/covidliste/campaigns.available_doses_gt_zero
-- source: https://github.com/hostolab/covidliste/blob/9229721af77297102ecb425cb7d77bcedd09502b/db/schema.rb
-- license: MIT
CREATE TABLE campaigns (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  available_doses integer NOT NULL,
  CONSTRAINT available_doses_gt_zero CHECK (((available_doses >= 0) AND (available_doses <= 1000)))
);

-- case: hostolab/covidliste/campaigns.max_age_gt_zero
-- source: https://github.com/hostolab/covidliste/blob/9229721af77297102ecb425cb7d77bcedd09502b/db/schema.rb
-- license: MIT
CREATE TABLE campaigns (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  min_age integer NOT NULL,
  max_age integer,
  CONSTRAINT max_age_gt_zero CHECK (((max_age > 0) AND (max_age > min_age)))
);

-- case: hostolab/covidliste/campaigns.starts_at_lt_ends_at
-- source: https://github.com/hostolab/covidliste/blob/9229721af77297102ecb425cb7d77bcedd09502b/db/schema.rb
-- license: MIT
CREATE TABLE campaigns (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  starts_at timestamp(6) without time zone NOT NULL,
  ends_at timestamp(6) without time zone NOT NULL,
  CONSTRAINT starts_at_lt_ends_at CHECK ((starts_at < ends_at))
);

-- case: infoeducatie/infoeducatie-api/robotics_competitions.robotics_competitions_allocation_positive
-- source: https://github.com/infoeducatie/infoeducatie-api/blob/5cfc13ba3f9b984b352b7664a83bc541b46357e3/db/schema.rb
-- license: MIT
CREATE TABLE robotics_competitions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  team_allocation_seconds integer NOT NULL DEFAULT 10800,
  CONSTRAINT robotics_competitions_allocation_positive CHECK ((team_allocation_seconds > 0))
);

-- case: infoeducatie/infoeducatie-api/robotics_turns.robotics_turns_state_valid
-- source: https://github.com/infoeducatie/infoeducatie-api/blob/5cfc13ba3f9b984b352b7664a83bc541b46357e3/db/schema.rb
-- license: MIT
CREATE TABLE robotics_turns (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  state character varying NOT NULL,
  CONSTRAINT robotics_turns_state_valid CHECK (((state)::text = ANY ((ARRAY['offered'::character varying, 'active'::character varying, 'turnover'::character varying, 'completed'::character varying, 'passed'::character varying, 'expired'::character varying, 'withdrawn'::character varying])::text[])))
);

-- case: infoeducatie/infoeducatie-api/robotics_time_entries.robotics_time_entries_kind_valid
-- source: https://github.com/infoeducatie/infoeducatie-api/blob/5cfc13ba3f9b984b352b7664a83bc541b46357e3/db/schema.rb
-- license: MIT
CREATE TABLE robotics_time_entries (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  kind character varying NOT NULL,
  CONSTRAINT robotics_time_entries_kind_valid CHECK (((kind)::text = ANY ((ARRAY['initial_grant'::character varying, 'session_usage'::character varying, 'admin_adjustment'::character varying])::text[])))
);

-- case: infoeducatie/infoeducatie-api/robotics_competitions.robotics_competitions_turn_duration_valid
-- source: https://github.com/infoeducatie/infoeducatie-api/blob/5cfc13ba3f9b984b352b7664a83bc541b46357e3/db/schema.rb
-- license: MIT
CREATE TABLE robotics_competitions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  duration_seconds integer NOT NULL DEFAULT 72000,
  turn_duration_seconds integer NOT NULL DEFAULT 600,
  CONSTRAINT robotics_competitions_turn_duration_valid CHECK (((turn_duration_seconds > 0) AND (turn_duration_seconds <= duration_seconds)))
);

-- case: infoeducatie/infoeducatie-api/robotics_turns.robotics_turns_charged_nonnegative
-- source: https://github.com/infoeducatie/infoeducatie-api/blob/5cfc13ba3f9b984b352b7664a83bc541b46357e3/db/schema.rb
-- license: MIT
CREATE TABLE robotics_turns (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  charged_seconds integer,
  CONSTRAINT robotics_turns_charged_nonnegative CHECK (((charged_seconds IS NULL) OR (charged_seconds >= 0)))
);

-- case: infoeducatie/infoeducatie-api/robotics_turns.robotics_turns_reserved_positive
-- source: https://github.com/infoeducatie/infoeducatie-api/blob/5cfc13ba3f9b984b352b7664a83bc541b46357e3/db/schema.rb
-- license: MIT
CREATE TABLE robotics_turns (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  reserved_seconds integer,
  CONSTRAINT robotics_turns_reserved_positive CHECK (((reserved_seconds IS NULL) OR (reserved_seconds > 0)))
);

-- case: kenyounot123/moneypouch/imports.draft_has_a_sheet
-- source: https://github.com/kenyounot123/moneypouch/blob/dd674dedaf4c6e409cb2ff79b04d56b5925911cf/db/schema.rb
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE imports (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  sheet text,
  completed_at timestamp(6) without time zone,
  CONSTRAINT draft_has_a_sheet CHECK (((completed_at IS NULL) = (sheet IS NOT NULL)))
);

-- case: kenyounot123/moneypouch/rules.rule_is_a_category_or_a_transfer
-- source: https://github.com/kenyounot123/moneypouch/blob/dd674dedaf4c6e409cb2ff79b04d56b5925911cf/db/schema.rb
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE rules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  category_id integer,
  transfer boolean NOT NULL DEFAULT false,
  CONSTRAINT rule_is_a_category_or_a_transfer CHECK (((category_id IS NULL) = transfer))
);

-- case: kenyounot123/moneypouch/transactions.transfer_has_no_category
-- source: https://github.com/kenyounot123/moneypouch/blob/dd674dedaf4c6e409cb2ff79b04d56b5925911cf/db/schema.rb
-- license: MIT
CREATE TABLE transactions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  category_id integer,
  transfer boolean NOT NULL DEFAULT false,
  CONSTRAINT transfer_has_no_category CHECK ((NOT (transfer AND (category_id IS NOT NULL))))
);

-- case: KevinBongart/easy-rsvp/events.events_spam_source_matches_spam_at
-- source: https://github.com/KevinBongart/easy-rsvp/blob/c07d1b0f055125894eaff71f23e58a3934212b69/db/schema.rb
-- license: MIT
CREATE TABLE events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  spam_at timestamp(6) without time zone,
  spam_source character varying,
  CONSTRAINT events_spam_source_matches_spam_at CHECK ((((spam_at IS NULL) AND ((spam_source IS NULL) OR ((spam_source)::text = 'cleared'::text))) OR ((spam_at IS NOT NULL) AND ((spam_source)::text = ANY (ARRAY[('operator'::character varying)::text, ('automatic'::character varying)::text])))))
);

-- case: KevinBongart/easy-rsvp/events.events_schedule_complete
-- source: https://github.com/KevinBongart/easy-rsvp/blob/c07d1b0f055125894eaff71f23e58a3934212b69/db/schema.rb
-- license: MIT
CREATE TABLE events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ends_at timestamp(6) without time zone,
  starts_at timestamp(6) without time zone,
  time_zone character varying,
  CONSTRAINT events_schedule_complete CHECK ((((starts_at IS NULL) AND (ends_at IS NULL) AND (time_zone IS NULL)) OR ((starts_at IS NOT NULL) AND (ends_at IS NOT NULL) AND (time_zone IS NOT NULL) AND (btrim((time_zone)::text) <> ''::text))))
);

-- case: KevinBongart/easy-rsvp/rsvps.rsvps_supported_response
-- source: https://github.com/KevinBongart/easy-rsvp/blob/c07d1b0f055125894eaff71f23e58a3934212b69/db/schema.rb
-- license: MIT
CREATE TABLE rsvps (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  response character varying,
  CONSTRAINT rsvps_supported_response CHECK (((response IS NOT NULL) AND ((response)::text = ANY (ARRAY[('yes'::character varying)::text, ('maybe'::character varying)::text, ('no'::character varying)::text]))))
);

-- case: KevinBongart/easy-rsvp/rsvps.rsvps_name_present
-- source: https://github.com/KevinBongart/easy-rsvp/blob/c07d1b0f055125894eaff71f23e58a3934212b69/db/schema.rb
-- license: MIT
CREATE TABLE rsvps (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name character varying NOT NULL,
  CONSTRAINT rsvps_name_present CHECK (((name IS NOT NULL) AND ((name)::text ~ '[^[:space:]]'::text)))
);

-- case: KevinBongart/easy-rsvp/events.events_schedule_matches_date
-- source: https://github.com/KevinBongart/easy-rsvp/blob/c07d1b0f055125894eaff71f23e58a3934212b69/db/schema.rb
-- license: MIT
CREATE TABLE events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  date date NOT NULL,
  ends_at timestamp(6) without time zone,
  starts_at timestamp(6) without time zone,
  time_zone character varying,
  CONSTRAINT events_schedule_matches_date CHECK (((starts_at IS NULL) OR (((((starts_at AT TIME ZONE 'UTC'::text) AT TIME ZONE time_zone))::date = date) AND ((((ends_at AT TIME ZONE 'UTC'::text) AT TIME ZONE time_zone))::date = date))))
);

-- case: KevinBongart/easy-rsvp/events.events_schedule_ordered
-- source: https://github.com/KevinBongart/easy-rsvp/blob/c07d1b0f055125894eaff71f23e58a3934212b69/db/schema.rb
-- license: MIT
CREATE TABLE events (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ends_at timestamp(6) without time zone,
  starts_at timestamp(6) without time zone,
  CONSTRAINT events_schedule_ordered CHECK (((ends_at IS NULL) OR (starts_at IS NULL) OR (ends_at > starts_at)))
);

-- case: MaMpf-HD/mampf/programs.programs_degree_check
-- source: https://github.com/MaMpf-HD/mampf/blob/a46ddf1e26dbbce4d2e2e4e9a49e1696b64f1064/db/schema.rb
-- license: MIT
CREATE TABLE programs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  degree character varying,
  CONSTRAINT programs_degree_check CHECK (((degree)::text = ANY (ARRAY[('bsc100'::character varying)::text, ('bsc50'::character varying)::text, ('msc'::character varying)::text, ('med'::character varying)::text, ('med_extension'::character varying)::text, ('phd'::character varying)::text])))
);

-- case: MaMpf-HD/mampf/assignments.test_week_count_only_for_tests
-- source: https://github.com/MaMpf-HD/mampf/blob/a46ddf1e26dbbce4d2e2e4e9a49e1696b64f1064/db/schema.rb
-- license: MIT
CREATE TABLE assignments (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  kind integer NOT NULL DEFAULT 0,
  test_week_count integer NOT NULL DEFAULT 1,
  CONSTRAINT test_week_count_only_for_tests CHECK (((kind = 1) OR (test_week_count = 1)))
);

-- case: MaMpf-HD/mampf/assignments.passing_points_only_for_tests
-- source: https://github.com/MaMpf-HD/mampf/blob/a46ddf1e26dbbce4d2e2e4e9a49e1696b64f1064/db/schema.rb
-- license: MIT
CREATE TABLE assignments (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  kind integer NOT NULL DEFAULT 0,
  passing_points numeric(10,2),
  CONSTRAINT passing_points_only_for_tests CHECK (((kind = 1) OR (passing_points IS NULL)))
);

-- case: MaMpf-HD/mampf/assessment_participations.valid_german_grades
-- source: https://github.com/MaMpf-HD/mampf/blob/a46ddf1e26dbbce4d2e2e4e9a49e1696b64f1064/db/schema.rb
-- license: MIT
CREATE TABLE assessment_participations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  grade_numeric numeric(2,1),
  CONSTRAINT valid_german_grades CHECK (((grade_numeric IS NULL) OR (grade_numeric = ANY (ARRAY[1.0, 1.3, 1.7, 2.0, 2.3, 2.7, 3.0, 3.3, 3.7, 4.0, 5.0]))))
);

-- case: MaMpf-HD/mampf/assignments.passing_points_positive
-- source: https://github.com/MaMpf-HD/mampf/blob/a46ddf1e26dbbce4d2e2e4e9a49e1696b64f1064/db/schema.rb
-- license: MIT
CREATE TABLE assignments (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  passing_points numeric(10,2),
  CONSTRAINT passing_points_positive CHECK ((passing_points > (0)::numeric))
);

-- case: MaMpf-HD/mampf/assessment_tasks.max_points_non_negative
-- source: https://github.com/MaMpf-HD/mampf/blob/a46ddf1e26dbbce4d2e2e4e9a49e1696b64f1064/db/schema.rb
-- license: MIT
CREATE TABLE assessment_tasks (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  max_points numeric(10,2) NOT NULL,
  CONSTRAINT max_points_non_negative CHECK ((max_points >= (0)::numeric))
);

-- case: meedan/check-api/relationships.source_target_must_be_different
-- source: https://github.com/meedan/check-api/blob/62b96db6eba3632bd3565819f53a29adc6f47c2e/db/schema.rb
-- license: MIT
CREATE TABLE relationships (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  source_id integer NOT NULL,
  target_id integer NOT NULL,
  CONSTRAINT source_target_must_be_different CHECK ((source_id <> target_id))
);

-- case: meme-search/meme-search/description_provider_settings.description_provider_settings_singleton_key_check
-- source: https://github.com/meme-search/meme-search/blob/5d6ecb3021e7dc3041b84383c25a3429ccea1e2b/meme_search/meme_search_app/db/schema.rb
-- license: Apache-2.0
CREATE TABLE description_provider_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  singleton_key integer NOT NULL DEFAULT 0,
  CONSTRAINT description_provider_settings_singleton_key_check CHECK ((singleton_key = 0))
);

-- case: ministryofjustice/prison-visits-2/visit_state_changes.visitor_or_processed_by_set
-- source: https://github.com/ministryofjustice/prison-visits-2/blob/6527a1985ed229657f569ee5105b68c95bbf2546/db/schema.rb
-- license: MIT
CREATE TABLE visit_state_changes (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  processed_by_id uuid,
  visitor_id uuid,
  CONSTRAINT visitor_or_processed_by_set CHECK (((visitor_id IS NULL) OR (processed_by_id IS NULL)))
);

-- case: MITLibraries/thing/related_hold_exceptions.related_hold_exceptions_not_self
-- source: https://github.com/MITLibraries/thing/blob/58a99d0482dfc6d918088778ac4eee4f26bd20c3/db/schema.rb
-- license: Apache-2.0
CREATE TABLE related_hold_exceptions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  related_thesis_id integer NOT NULL,
  thesis_id integer NOT NULL,
  CONSTRAINT related_hold_exceptions_not_self CHECK ((thesis_id <> related_thesis_id))
);

-- case: motohiro-mm/GohanSummit/meals.timing_check
-- source: https://github.com/motohiro-mm/GohanSummit/blob/3d9ffe2892fcf679a5f8053c4c2b1f108e891067/db/schema.rb
-- license: MIT
CREATE TABLE meals (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  timing integer NOT NULL,
  CONSTRAINT timing_check CHECK ((timing = ANY (ARRAY[0, 1, 2])))
);

-- case: motohiro-mm/GohanSummit/remarks.remark_type_check
-- source: https://github.com/motohiro-mm/GohanSummit/blob/3d9ffe2892fcf679a5f8053c4c2b1f108e891067/db/schema.rb
-- license: MIT
CREATE TABLE remarks (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  remark_type integer NOT NULL,
  CONSTRAINT remark_type_check CHECK ((remark_type = ANY (ARRAY[0, 1])))
);

-- case: openHPI/codeocean/testruns.exit_code_constraint
-- source: https://github.com/openHPI/codeocean/blob/ff24c533bc0bd94704483bf8e5df0177ea4145a2/db/schema.rb
-- license: BSD-3-Clause
CREATE TABLE testruns (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  exit_code smallint,
  CONSTRAINT exit_code_constraint CHECK (((exit_code >= 0) AND (exit_code <= 255)))
);

-- case: openHPI/codeocean/testrun_messages.either_data_or_log
-- source: https://github.com/openHPI/codeocean/blob/ff24c533bc0bd94704483bf8e5df0177ea4145a2/db/schema.rb
-- license: BSD-3-Clause
CREATE TABLE testrun_messages (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  log text,
  data jsonb,
  CONSTRAINT either_data_or_log CHECK (((log IS NULL) OR (data IS NULL)))
);

-- case: OpenReceive/openreceive/shop_order_items.shop_order_items_quantity_check
-- source: https://github.com/OpenReceive/openreceive/blob/277fd29bd70d1e5cb7fffc1f93174bebe49c97b7/examples/buttons/server/rails/db/schema.rb
-- license: MIT
CREATE TABLE shop_order_items (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  quantity integer NOT NULL,
  CONSTRAINT shop_order_items_quantity_check CHECK ((quantity > 0))
);

-- case: OpenReceive/openreceive/openreceive_payments.openreceive_payments_payment_hash_check
-- source: https://github.com/OpenReceive/openreceive/blob/277fd29bd70d1e5cb7fffc1f93174bebe49c97b7/examples/buttons/server/rails/db/schema.rb
-- license: MIT
CREATE TABLE openreceive_payments (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  payment_hash character varying(64) NOT NULL,
  CONSTRAINT openreceive_payments_payment_hash_check CHECK (((payment_hash)::text ~ '^[0-9a-f]{64}$'::text))
);

-- case: ossf/best-practices-badge/projects.check_access_continuity_status_range
-- source: https://github.com/ossf/best-practices-badge/blob/32447ea1c63d44c6b8bb749779979d5781d5a773/db/schema.rb
-- license: MIT
CREATE TABLE projects (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  access_continuity_status smallint NOT NULL DEFAULT 0,
  CONSTRAINT check_access_continuity_status_range CHECK (((access_continuity_status >= 0) AND (access_continuity_status <= 3)))
);

-- case: philipqnguyen/vanai/bounties.bounties_status
-- source: https://github.com/philipqnguyen/vanai/blob/db09ef2442f7d301dcf665323163c7e7d1fd593f/server/db/schema.rb
-- license: MIT
CREATE TABLE bounties (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying NOT NULL DEFAULT 'open'::character varying,
  CONSTRAINT bounties_status CHECK (((status)::text = ANY (ARRAY['open'::text, 'filled'::text, 'expired'::text])))
);

-- case: philipqnguyen/vanai/bounties.bounties_kind
-- source: https://github.com/philipqnguyen/vanai/blob/db09ef2442f7d301dcf665323163c7e7d1fd593f/server/db/schema.rb
-- license: MIT
CREATE TABLE bounties (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  kind character varying NOT NULL,
  CONSTRAINT bounties_kind CHECK (((kind)::text = ANY (ARRAY['kill'::text, 'collect'::text])))
);

-- case: philipqnguyen/vanai/court_letters.court_letters_status
-- source: https://github.com/philipqnguyen/vanai/blob/db09ef2442f7d301dcf665323163c7e7d1fd593f/server/db/schema.rb
-- license: MIT
CREATE TABLE court_letters (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying NOT NULL DEFAULT 'unread'::character varying,
  CONSTRAINT court_letters_status CHECK (((status)::text = ANY (ARRAY['unread'::text, 'read'::text, 'accepted'::text, 'declined'::text, 'lapsed'::text])))
);

-- case: philipqnguyen/vanai/courts.courts_tax_bp_range
-- source: https://github.com/philipqnguyen/vanai/blob/db09ef2442f7d301dcf665323163c7e7d1fd593f/server/db/schema.rb
-- license: MIT
CREATE TABLE courts (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tax_bp integer NOT NULL DEFAULT 300,
  CONSTRAINT courts_tax_bp_range CHECK (((tax_bp >= 100) AND (tax_bp <= 1500)))
);

-- case: philipqnguyen/vanai/bounties.bounties_places_paid_range
-- source: https://github.com/philipqnguyen/vanai/blob/db09ef2442f7d301dcf665323163c7e7d1fd593f/server/db/schema.rb
-- license: MIT
CREATE TABLE bounties (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  places integer NOT NULL,
  places_paid integer NOT NULL DEFAULT 0,
  CONSTRAINT bounties_places_paid_range CHECK (((places_paid >= 0) AND (places_paid <= places)))
);

-- case: philipqnguyen/vanai/zone_claims.zone_claims_points_nonnegative
-- source: https://github.com/philipqnguyen/vanai/blob/db09ef2442f7d301dcf665323163c7e7d1fd593f/server/db/schema.rb
-- license: MIT
CREATE TABLE zone_claims (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  points double precision NOT NULL DEFAULT 0.0,
  CONSTRAINT zone_claims_points_nonnegative CHECK ((points >= (0)::double precision))
);

-- case: pstrzalk/hifumi-dev/ruby_llm_usages.ruby_llm_usages_operation_check
-- source: https://github.com/pstrzalk/hifumi-dev/blob/d11339d2f75b9ae57820dbf918f120f521e268bb/db/schema.rb
-- license: MIT
CREATE TABLE ruby_llm_usages (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  operation character varying NOT NULL,
  CONSTRAINT ruby_llm_usages_operation_check CHECK (((operation)::text = ANY ((ARRAY['chat'::character varying, 'embedding'::character varying, 'moderation'::character varying, 'image'::character varying, 'speech'::character varying, 'transcription'::character varying, 'ocr'::character varying, 'rerank'::character varying])::text[])))
);

-- case: pubannotation/pubannotation/segments.segments_times_in_order
-- source: https://github.com/pubannotation/pubannotation/blob/090a36f2b5e975bb59f3bf63752ba7e439e74719/db/schema.rb
-- license: Apache-2.0
CREATE TABLE segments (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  end_ms integer NOT NULL,
  start_ms integer NOT NULL,
  CONSTRAINT segments_times_in_order CHECK (((start_ms >= 0) AND (end_ms >= start_ms)))
);

-- case: QueueClassic/queue_classic_admin/queue_classic_jobs.queue_classic_jobs_method_check
-- source: https://github.com/QueueClassic/queue_classic_admin/blob/e2deebeb5661dcd81d74b8d1c251ed55e8cdf95c/spec/dummy/db/schema.rb
-- license: MIT
CREATE TABLE queue_classic_jobs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  method text NOT NULL,
  CONSTRAINT queue_classic_jobs_method_check CHECK ((length(method) > 0))
);

-- case: rails/ai-evals/tickets.tickets_closed_requires_merge
-- source: https://github.com/rails/ai-evals/blob/6028cb729362f69cc2cd9b09210177c0213e682d/apps/helpdesk/db/schema.rb
-- license: MIT
CREATE TABLE tickets (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying NOT NULL DEFAULT 'open'::character varying,
  merged_into_id uuid,
  CONSTRAINT tickets_closed_requires_merge CHECK (((((status)::text = 'closed'::text) AND (merged_into_id IS NOT NULL)) OR (((status)::text <> 'closed'::text) AND (merged_into_id IS NULL))))
);

-- case: rails/ai-evals/tickets.tickets_cannot_merge_into_self
-- source: https://github.com/rails/ai-evals/blob/6028cb729362f69cc2cd9b09210177c0213e682d/apps/helpdesk/db/schema.rb
-- license: MIT
CREATE TABLE tickets (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  merged_into_id uuid,
  CONSTRAINT tickets_cannot_merge_into_self CHECK ((merged_into_id <> id))
);

-- case: rubyforgood/casa/case_contacts.want_driving_reimbursement_only_when_miles_driven
-- source: https://github.com/rubyforgood/casa/blob/de5b9b933ec626e9cf1a343b8955d229055f81ba/db/schema.rb
-- license: MIT
CREATE TABLE case_contacts (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  miles_driven integer NOT NULL DEFAULT 0,
  want_driving_reimbursement boolean DEFAULT false,
  CONSTRAINT want_driving_reimbursement_only_when_miles_driven CHECK (((miles_driven IS NOT NULL) OR (NOT want_driving_reimbursement)))
);

-- case: rubyforgood/homeward-tails/people.phone_number_valid_e164
-- source: https://github.com/rubyforgood/homeward-tails/blob/51b1c424807bf2c67970db09612bb54dc913b02f/db/schema.rb
-- license: MIT
CREATE TABLE people (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  phone_number character varying(15),
  CONSTRAINT phone_number_valid_e164 CHECK (((phone_number)::text ~ '^[+]?[0-9]*$'::text))
);

-- case: rubyforgood/human-essentials/partner_groups.deadline_day_of_month_check
-- source: https://github.com/rubyforgood/human-essentials/blob/b41d699c2aad602157e304d8b39e0d9e2a01eaae/db/schema.rb
-- license: MIT
CREATE TABLE partner_groups (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  deadline_day integer,
  CONSTRAINT deadline_day_of_month_check CHECK ((deadline_day <= 28))
);

-- case: rubyforgood/human-essentials/items.distribution_quantity_nonnegative
-- source: https://github.com/rubyforgood/human-essentials/blob/b41d699c2aad602157e304d8b39e0d9e2a01eaae/db/schema.rb
-- license: MIT
CREATE TABLE items (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  distribution_quantity integer,
  CONSTRAINT distribution_quantity_nonnegative CHECK ((distribution_quantity >= 0))
);

-- case: rubygems/rubygems.org/api_keys.api_keys_owner_id_null
-- source: https://github.com/rubygems/rubygems.org/blob/8979e443ac635031fefbfcc9cc3cdf66d18fe81e/db/schema.rb
-- license: MIT
CREATE TABLE api_keys (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  owner_id bigint,
  CONSTRAINT api_keys_owner_id_null CHECK ((owner_id IS NOT NULL))
);

-- case: rubygems/rubygems.org/api_keys.api_keys_owner_type_null
-- source: https://github.com/rubygems/rubygems.org/blob/8979e443ac635031fefbfcc9cc3cdf66d18fe81e/db/schema.rb
-- license: MIT
CREATE TABLE api_keys (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  owner_type character varying,
  CONSTRAINT api_keys_owner_type_null CHECK ((owner_type IS NOT NULL))
);

-- case: rubygems/rubygems.org/api_keys.api_keys_scopes_null
-- source: https://github.com/rubygems/rubygems.org/blob/8979e443ac635031fefbfcc9cc3cdf66d18fe81e/db/schema.rb
-- license: MIT
CREATE TABLE api_keys (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  scopes character varying[],
  CONSTRAINT api_keys_scopes_null CHECK ((scopes IS NOT NULL))
);

-- case: rubynor/stemplin/plan_assignments.plan_assignments_one_assignee
-- source: https://github.com/rubynor/stemplin/blob/3887a6326c5b78352947ebe7db42b8a476ac055f/db/schema.rb
-- license: Apache-2.0
-- expect: CLEAR_ERROR
CREATE TABLE plan_assignments (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id bigint,
  placeholder_id bigint,
  CONSTRAINT plan_assignments_one_assignee CHECK (((user_id IS NULL) <> (placeholder_id IS NULL)))
);

-- case: rubynor/stemplin/plan_assignments.plan_assignments_date_order
-- source: https://github.com/rubynor/stemplin/blob/3887a6326c5b78352947ebe7db42b8a476ac055f/db/schema.rb
-- license: Apache-2.0
CREATE TABLE plan_assignments (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  start_date date NOT NULL,
  end_date date NOT NULL,
  CONSTRAINT plan_assignments_date_order CHECK ((end_date >= start_date))
);

-- case: saeloun/miru-web/payroll_expense_reservations.payroll_expense_reservation_amount
-- source: https://github.com/saeloun/miru-web/blob/c23e8fcd185951061d8256834e85886a20fbe1fb/db/schema.rb
-- license: MIT
CREATE TABLE payroll_expense_reservations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  amount_minor bigint NOT NULL,
  CONSTRAINT payroll_expense_reservation_amount CHECK ((amount_minor > 0))
);

-- case: saeloun/miru-web/hiring_interview_bookings.hiring_booking_number
-- source: https://github.com/saeloun/miru-web/blob/c23e8fcd185951061d8256834e85886a20fbe1fb/db/schema.rb
-- license: MIT
CREATE TABLE hiring_interview_bookings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  number integer NOT NULL,
  CONSTRAINT hiring_booking_number CHECK ((number > 0))
);

-- case: saeloun/miru-web/agreements.agreements_status
-- source: https://github.com/saeloun/miru-web/blob/c23e8fcd185951061d8256834e85886a20fbe1fb/db/schema.rb
-- license: MIT
CREATE TABLE agreements (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying NOT NULL DEFAULT 'draft'::character varying,
  CONSTRAINT agreements_status CHECK (((status)::text = 'draft'::text))
);

-- case: saeloun/miru-web/employee_payments.employee_payments_positive_amount
-- source: https://github.com/saeloun/miru-web/blob/c23e8fcd185951061d8256834e85886a20fbe1fb/db/schema.rb
-- license: MIT
CREATE TABLE employee_payments (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  amount numeric(12,2) NOT NULL,
  CONSTRAINT employee_payments_positive_amount CHECK ((amount > (0)::numeric))
);

-- case: saeloun/miru-web/metrics.valid_period
-- source: https://github.com/saeloun/miru-web/blob/c23e8fcd185951061d8256834e85886a20fbe1fb/db/schema.rb
-- license: MIT
CREATE TABLE metrics (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  period character varying NOT NULL,
  CONSTRAINT valid_period CHECK (((period)::text = ANY (ARRAY[('hour'::character varying)::text, ('day'::character varying)::text, ('week'::character varying)::text, ('month'::character varying)::text, ('quarter'::character varying)::text, ('year'::character varying)::text, ('all_time'::character varying)::text])))
);

-- case: saeloun/miru-web/hiring_candidates.hiring_candidate_normalized_email
-- source: https://github.com/saeloun/miru-web/blob/c23e8fcd185951061d8256834e85886a20fbe1fb/db/schema.rb
-- license: MIT
CREATE TABLE hiring_candidates (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  email character varying(254) NOT NULL,
  CONSTRAINT hiring_candidate_normalized_email CHECK (((email)::text = lower(btrim((email)::text))))
);

-- case: scottohara/loot/accounts.accounts_status_check
-- source: https://github.com/scottohara/loot/blob/8cb09daa24b6cfb8783f204a884b785b00682170/db/schema.rb
-- license: MIT
CREATE TABLE accounts (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(255) NOT NULL,
  CONSTRAINT accounts_status_check CHECK (((status)::text = ANY (ARRAY[('open'::character varying)::text, ('closed'::character varying)::text])))
);

-- case: scottohara/loot/transactions.transactions_transaction_type_check
-- source: https://github.com/scottohara/loot/blob/8cb09daa24b6cfb8783f204a884b785b00682170/db/schema.rb
-- license: MIT
CREATE TABLE transactions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  transaction_type character varying(255) NOT NULL,
  CONSTRAINT transactions_transaction_type_check CHECK (((transaction_type)::text = ANY (ARRAY[('Basic'::character varying)::text, ('Split'::character varying)::text, ('Transfer'::character varying)::text, ('Payslip'::character varying)::text, ('LoanRepayment'::character varying)::text, ('Sub'::character varying)::text, ('Subtransfer'::character varying)::text, ('SecurityTransfer'::character varying)::text, ('SecurityHolding'::character varying)::text, ('SecurityInvestment'::character varying)::text, ('Dividend'::character varying)::text])))
);

-- case: scottohara/loot/accounts.accounts_account_type_check
-- source: https://github.com/scottohara/loot/blob/8cb09daa24b6cfb8783f204a884b785b00682170/db/schema.rb
-- license: MIT
CREATE TABLE accounts (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  account_type character varying(255) NOT NULL,
  CONSTRAINT accounts_account_type_check CHECK (((account_type)::text = ANY (ARRAY[('bank'::character varying)::text, ('credit'::character varying)::text, ('cash'::character varying)::text, ('asset'::character varying)::text, ('liability'::character varying)::text, ('investment'::character varying)::text, ('loan'::character varying)::text])))
);

-- case: scottohara/loot/schedules.schedules_frequency_check
-- source: https://github.com/scottohara/loot/blob/8cb09daa24b6cfb8783f204a884b785b00682170/db/schema.rb
-- license: MIT
CREATE TABLE schedules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  frequency character varying(255) NOT NULL,
  CONSTRAINT schedules_frequency_check CHECK (((frequency)::text = ANY (ARRAY[('Weekly'::character varying)::text, ('Fortnightly'::character varying)::text, ('Monthly'::character varying)::text, ('Bimonthly'::character varying)::text, ('Quarterly'::character varying)::text, ('Yearly'::character varying)::text])))
);

-- case: scottohara/loot/transaction_accounts.transaction_accounts_status_check
-- source: https://github.com/scottohara/loot/blob/8cb09daa24b6cfb8783f204a884b785b00682170/db/schema.rb
-- license: MIT
CREATE TABLE transaction_accounts (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(255),
  CONSTRAINT transaction_accounts_status_check CHECK (((status IS NULL) OR ((status)::text = ANY (ARRAY[('Cleared'::character varying)::text, ('Reconciled'::character varying)::text]))))
);

-- case: SpeciesFileGroup/taxonworks/composition_items.composition_item_opacity_check
-- source: https://github.com/SpeciesFileGroup/taxonworks/blob/984fccac5d43631b6b2839355358dcd0e6e0d66a/db/schema.rb
-- license: MIT
CREATE TABLE composition_items (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  opacity double precision,
  CONSTRAINT composition_item_opacity_check CHECK (((opacity >= (0)::double precision) AND (opacity <= (100)::double precision)))
);

-- case: SpeciesFileGroup/taxonworks/compositions.composition_community_project_check
-- source: https://github.com/SpeciesFileGroup/taxonworks/blob/984fccac5d43631b6b2839355358dcd0e6e0d66a/db/schema.rb
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE compositions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  is_community boolean,
  project_id bigint,
  CONSTRAINT composition_community_project_check CHECK (((is_community IS TRUE) = (project_id IS NULL)))
);

-- case: spree/spree-starter/spree_commission_lines.chk_spree_commission_lines_one_subject
-- source: https://github.com/spree/spree-starter/blob/e831599eae9386b297aa9b0c404223b8c0a68fad/db/schema.rb
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE spree_commission_lines (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  line_item_id bigint,
  fulfillment_id bigint,
  CONSTRAINT chk_spree_commission_lines_one_subject CHECK (((line_item_id IS NULL) <> (fulfillment_id IS NULL)))
);

-- case: spree/spree-starter/spree_discounts.chk_spree_discounts_amount_nonpositive
-- source: https://github.com/spree/spree-starter/blob/e831599eae9386b297aa9b0c404223b8c0a68fad/db/schema.rb
-- license: MIT
CREATE TABLE spree_discounts (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  amount numeric(19,4) NOT NULL,
  CONSTRAINT chk_spree_discounts_amount_nonpositive CHECK ((amount <= (0)::numeric))
);

-- case: spree/spree-starter/spree_fees.chk_spree_fees_amount_nonnegative
-- source: https://github.com/spree/spree-starter/blob/e831599eae9386b297aa9b0c404223b8c0a68fad/db/schema.rb
-- license: MIT
CREATE TABLE spree_fees (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  amount numeric(19,4) NOT NULL,
  CONSTRAINT chk_spree_fees_amount_nonnegative CHECK ((amount >= (0)::numeric))
);

-- case: testdouble/course-cypress-intro-demo-app/task_dependencies.task_dependencies_check
-- source: https://github.com/testdouble/course-cypress-intro-demo-app/blob/92c0b1affea78b1111e55d66d84d439d5e1dc9ca/yams_api/db/schema.rb
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE task_dependencies (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  task_id uuid NOT NULL,
  dependency_id uuid NOT NULL,
  CONSTRAINT task_dependencies_check CHECK ((task_id <> dependency_id))
);

-- case: tramlinehq/tramline/notification_settings.notification_settings_check
-- source: https://github.com/tramlinehq/tramline/blob/f12b90a88bd5be6e26d9b9465415193f6e9c4d8d/db/schema.rb
-- license: Apache-2.0
-- expect: CLEAR_ERROR
CREATE TABLE notification_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  active boolean NOT NULL DEFAULT true,
  release_specific_enabled boolean DEFAULT false,
  core_enabled boolean NOT NULL DEFAULT false,
  CONSTRAINT notification_settings_check CHECK ((((active IS TRUE) AND (true = ANY (ARRAY[core_enabled, release_specific_enabled]))) OR ((active IS FALSE) AND (false = ALL (ARRAY[core_enabled, release_specific_enabled])))))
);

-- case: tramlinehq/tramline/integrations.chk_rails_status_enum
-- source: https://github.com/tramlinehq/tramline/blob/f12b90a88bd5be6e26d9b9465415193f6e9c4d8d/db/schema.rb
-- license: Apache-2.0
CREATE TABLE integrations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying,
  CONSTRAINT chk_rails_status_enum CHECK (((status)::text = ANY (ARRAY[('connected'::character varying)::text, ('disconnected'::character varying)::text, ('needs_reauth'::character varying)::text])))
);
