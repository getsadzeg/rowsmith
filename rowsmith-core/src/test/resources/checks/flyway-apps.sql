-- case: ABHIJEET-MUNESHWAR/PaceEngine/budgets.budgets_daily_budget_check
-- source: https://github.com/ABHIJEET-MUNESHWAR/PaceEngine/tree/eef89ebf4f9cfb62e1cdd718800b7439559da844/src/main/resources/db/migration
-- license: MIT
CREATE TABLE budgets (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  daily_budget numeric(20,6) NOT NULL,
  CONSTRAINT budgets_daily_budget_check CHECK ((daily_budget >= (0)::numeric))
);

-- case: ABHIJEET-MUNESHWAR/PaceEngine/budgets.chk_spent_le_budget
-- source: https://github.com/ABHIJEET-MUNESHWAR/PaceEngine/tree/eef89ebf4f9cfb62e1cdd718800b7439559da844/src/main/resources/db/migration
-- license: MIT
CREATE TABLE budgets (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  daily_budget numeric(20,6) NOT NULL,
  spent_today numeric(20,6) NOT NULL DEFAULT 0,
  CONSTRAINT chk_spent_le_budget CHECK ((spent_today <= daily_budget))
);

-- case: adept-industries/adept-api/external_identities.external_identities_provider_check
-- source: https://github.com/adept-industries/adept-api/tree/937606e2325c79ad414a7006e27ef9b01bfbd9ee/src/main/resources/db/migration
-- license: MIT
CREATE TABLE external_identities (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  provider character varying(16) NOT NULL,
  CONSTRAINT external_identities_provider_check CHECK (((provider)::text = ANY ((ARRAY['GITHUB'::character varying, 'JIRA'::character varying])::text[])))
);

-- case: adept-industries/adept-api/alert_rules.alert_rules_cooldown_minutes_check
-- source: https://github.com/adept-industries/adept-api/tree/937606e2325c79ad414a7006e27ef9b01bfbd9ee/src/main/resources/db/migration
-- license: MIT
CREATE TABLE alert_rules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  cooldown_minutes integer NOT NULL DEFAULT 1440,
  CONSTRAINT alert_rules_cooldown_minutes_check CHECK ((cooldown_minutes >= 0))
);

-- case: adept-industries/adept-api/alert_rules.alert_rules_comparator_check
-- source: https://github.com/adept-industries/adept-api/tree/937606e2325c79ad414a7006e27ef9b01bfbd9ee/src/main/resources/db/migration
-- license: MIT
CREATE TABLE alert_rules (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  comparator character varying(8) NOT NULL,
  CONSTRAINT alert_rules_comparator_check CHECK (((comparator)::text = ANY ((ARRAY['GT'::character varying, 'GTE'::character varying, 'LT'::character varying, 'LTE'::character varying, 'EQ'::character varying])::text[])))
);

-- case: adept-industries/adept-api/deployment_pull_requests.deployment_pull_requests_link_method_check
-- source: https://github.com/adept-industries/adept-api/tree/937606e2325c79ad414a7006e27ef9b01bfbd9ee/src/main/resources/db/migration
-- license: MIT
CREATE TABLE deployment_pull_requests (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  link_method character varying(32) NOT NULL,
  CONSTRAINT deployment_pull_requests_link_method_check CHECK (((link_method)::text = ANY ((ARRAY['COMMIT_GRAPH'::character varying, 'MERGE_SHA'::character varying, 'MERGE_WINDOW'::character varying, 'MANUAL'::character varying])::text[])))
);

-- case: AlvaroMinarro/PlaceNote-Server/ratings.ratings_nota_check
-- source: https://github.com/AlvaroMinarro/PlaceNote-Server/tree/752329a8dab63b414f4101214b1f9972581c70a8/src/main/resources/db/migration
-- license: MIT
CREATE TABLE ratings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nota double precision NOT NULL,
  CONSTRAINT ratings_nota_check CHECK (((nota >= (0)::double precision) AND (nota <= (10)::double precision)))
);

-- case: AlvaroMinarro/PlaceNote-Server/friendships.friendships_check
-- source: https://github.com/AlvaroMinarro/PlaceNote-Server/tree/752329a8dab63b414f4101214b1f9972581c70a8/src/main/resources/db/migration
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE friendships (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_a_id uuid NOT NULL,
  user_b_id uuid NOT NULL,
  CONSTRAINT friendships_check CHECK ((user_a_id < user_b_id))
);

-- case: arulam11/observability/rides.rides_price_cents_check
-- source: https://github.com/arulam11/observability/tree/47af3fe1fd0b0b8840aba4964a4a994cac6d68b7/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE rides (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  price_cents integer NOT NULL,
  CONSTRAINT rides_price_cents_check CHECK ((price_cents >= 0))
);

-- case: Backfisch145/stammesverwaltung/event_participants.event_participants_status_check
-- source: https://github.com/Backfisch145/stammesverwaltung/tree/3c45e5bb0a109d8d9c6ee4dcb5d07aded89ce099/src/main/resources/db/migration
-- license: Unlicense
CREATE TABLE event_participants (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(255),
  CONSTRAINT event_participants_status_check CHECK (((status)::text = ANY (ARRAY[('INVITED'::character varying)::text, ('DECLINED'::character varying)::text, ('IN_PAYMENT'::character varying)::text, ('IN_REPAYMENT'::character varying)::text, ('REGISTERED'::character varying)::text])))
);

-- case: Backfisch145/stammesverwaltung/tribe.tribe_intervall_unit_check
-- source: https://github.com/Backfisch145/stammesverwaltung/tree/3c45e5bb0a109d8d9c6ee4dcb5d07aded89ce099/src/main/resources/db/migration
-- license: Unlicense
CREATE TABLE tribe (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  intervall_unit character varying(255),
  CONSTRAINT tribe_intervall_unit_check CHECK (((intervall_unit)::text = ANY (ARRAY[('NANOS'::character varying)::text, ('MICROS'::character varying)::text, ('MILLIS'::character varying)::text, ('SECONDS'::character varying)::text, ('MINUTES'::character varying)::text, ('HOURS'::character varying)::text, ('HALF_DAYS'::character varying)::text, ('DAYS'::character varying)::text, ('WEEKS'::character varying)::text, ('MONTHS'::character varying)::text, ('YEARS'::character varying)::text, ('DECADES'::character varying)::text, ('CENTURIES'::character varying)::text, ('MILLENNIA'::character varying)::text, ('ERAS'::character varying)::text, ('FOREVER'::character varying)::text])))
);

-- case: Backfisch145/stammesverwaltung/roles_permissions.roles_permissions_permission_check
-- source: https://github.com/Backfisch145/stammesverwaltung/tree/3c45e5bb0a109d8d9c6ee4dcb5d07aded89ce099/src/main/resources/db/migration
-- license: Unlicense
CREATE TABLE roles_permissions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  permission character varying(255) NOT NULL,
  CONSTRAINT roles_permissions_permission_check CHECK (((permission)::text = ANY (ARRAY[('MEMBER_READ'::character varying)::text, ('MEMBER_INSERT'::character varying)::text, ('MEMBER_DELETE'::character varying)::text, ('MEMBER_UPDATE'::character varying)::text, ('INVENTORY_READ'::character varying)::text, ('INVENTORY_INSERT'::character varying)::text, ('INVENTORY_DELETE'::character varying)::text, ('INVENTORY_UPDATE'::character varying)::text, ('EVENT_READ'::character varying)::text, ('EVENT_INSERT'::character varying)::text, ('EVENT_DELETE'::character varying)::text, ('EVENT_UPDATE'::character varying)::text])))
);

-- case: Backfisch145/stammesverwaltung/users.users_gender_check
-- source: https://github.com/Backfisch145/stammesverwaltung/tree/3c45e5bb0a109d8d9c6ee4dcb5d07aded89ce099/src/main/resources/db/migration
-- license: Unlicense
CREATE TABLE users (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  gender character varying(255),
  CONSTRAINT users_gender_check CHECK (((gender)::text = ANY (ARRAY[('MALE'::character varying)::text, ('FEMALE'::character varying)::text, ('OTHER'::character varying)::text])))
);

-- case: bereket-7/online-art-gallery-springboot/artwork.artwork_status_check
-- source: https://github.com/bereket-7/online-art-gallery-springboot/tree/b4007cbc9b0f5795c6cef92223a7d32735ceb334/src/main/resources/db/migration
-- license: MIT
CREATE TABLE artwork (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(255),
  CONSTRAINT artwork_status_check CHECK (((status)::text = ANY ((ARRAY['ACCEPTED'::character varying, 'REJECTED'::character varying, 'PENDING'::character varying, 'SOLD'::character varying])::text[])))
);

-- case: bereket-7/online-art-gallery-springboot/orders.orders_status_check
-- source: https://github.com/bereket-7/online-art-gallery-springboot/tree/b4007cbc9b0f5795c6cef92223a7d32735ceb334/src/main/resources/db/migration
-- license: MIT
CREATE TABLE orders (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(255) NOT NULL,
  CONSTRAINT orders_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'PROCESSING'::character varying, 'SHIPPED'::character varying, 'DELIVERED'::character varying, 'CANCELLED'::character varying])::text[])))
);

-- case: bereket-7/online-art-gallery-springboot/event.event_status_check
-- source: https://github.com/bereket-7/online-art-gallery-springboot/tree/b4007cbc9b0f5795c6cef92223a7d32735ceb334/src/main/resources/db/migration
-- license: MIT
CREATE TABLE event (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(255),
  CONSTRAINT event_status_check CHECK (((status)::text = ANY ((ARRAY['ACCEPTED'::character varying, 'PENDING'::character varying, 'REJECTED'::character varying])::text[])))
);

-- case: bereket-7/online-art-gallery-springboot/one_time_password.one_time_password_notification_type_check
-- source: https://github.com/bereket-7/online-art-gallery-springboot/tree/b4007cbc9b0f5795c6cef92223a7d32735ceb334/src/main/resources/db/migration
-- license: MIT
CREATE TABLE one_time_password (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  notification_type smallint NOT NULL,
  CONSTRAINT one_time_password_notification_type_check CHECK (((notification_type >= 0) AND (notification_type <= 6)))
);

-- case: BozhidarBoyadzhiev/TiP/device.device_secret_hash_length
-- source: https://github.com/BozhidarBoyadzhiev/TiP/tree/6408f2920eecd323770a982a2ed72303ed8579ee/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE device (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  secret_hash bytea NOT NULL,
  CONSTRAINT device_secret_hash_length CHECK ((octet_length(secret_hash) = 32))
);

-- case: BozhidarBoyadzhiev/TiP/owner.owner_real_has_login
-- source: https://github.com/BozhidarBoyadzhiev/TiP/tree/6408f2920eecd323770a982a2ed72303ed8579ee/src/main/resources/db/migration
-- license: Apache-2.0
-- expect: CLEAR_ERROR
CREATE TABLE owner (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  email character varying(254),
  password_hash character varying(255),
  kind character varying(16) NOT NULL DEFAULT 'REAL'::character varying,
  CONSTRAINT owner_real_has_login CHECK ((((kind)::text <> 'REAL'::text) OR ((email IS NOT NULL) AND (password_hash IS NOT NULL))))
);

-- case: BozhidarBoyadzhiev/TiP/owner.owner_sandbox_expires
-- source: https://github.com/BozhidarBoyadzhiev/TiP/tree/6408f2920eecd323770a982a2ed72303ed8579ee/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE owner (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  kind character varying(16) NOT NULL DEFAULT 'REAL'::character varying,
  expires_at timestamp with time zone,
  CONSTRAINT owner_sandbox_expires CHECK ((((kind)::text <> 'SANDBOX'::text) OR (expires_at IS NOT NULL)))
);

-- case: BozhidarBoyadzhiev/TiP/tip.tip_status_valid
-- source: https://github.com/BozhidarBoyadzhiev/TiP/tree/6408f2920eecd323770a982a2ed72303ed8579ee/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE tip (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(16) NOT NULL,
  CONSTRAINT tip_status_valid CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'PENDING'::character varying, 'CAPTURED'::character varying, 'FAILED'::character varying, 'REFUNDED'::character varying, 'REVERSED'::character varying, 'ABANDONED'::character varying])::text[])))
);

-- case: BrandonDHaskell/ForgeGuard/runs.runs_attempt_check
-- source: https://github.com/BrandonDHaskell/ForgeGuard/tree/2659889effebe802160d23705809b8d4fc7987b4/src/main/resources/db/migration
-- license: CC0-1.0
CREATE TABLE runs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  attempt integer NOT NULL DEFAULT 0,
  CONSTRAINT runs_attempt_check CHECK ((attempt >= 0))
);

-- case: cskwork/olive-clone/coupons.coupons_discount_value_non_negative
-- source: https://github.com/cskwork/olive-clone/tree/cd3bff9607f17842bd4c85bbbc0ad35b6ffd5eaa/src/main/resources/db/migration
-- license: MIT
CREATE TABLE coupons (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  discount_value numeric(12,2) NOT NULL,
  CONSTRAINT coupons_discount_value_non_negative CHECK ((discount_value >= (0)::numeric))
);

-- case: cskwork/olive-clone/inventory_reservations.inventory_reservations_status_check
-- source: https://github.com/cskwork/olive-clone/tree/cd3bff9607f17842bd4c85bbbc0ad35b6ffd5eaa/src/main/resources/db/migration
-- license: MIT
CREATE TABLE inventory_reservations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(20) NOT NULL DEFAULT 'HELD'::character varying,
  CONSTRAINT inventory_reservations_status_check CHECK (((status)::text = ANY ((ARRAY['HELD'::character varying, 'COMMITTED'::character varying, 'RELEASED'::character varying])::text[])))
);

-- case: cskwork/olive-clone/coupons.coupons_issued_count_non_negative
-- source: https://github.com/cskwork/olive-clone/tree/cd3bff9607f17842bd4c85bbbc0ad35b6ffd5eaa/src/main/resources/db/migration
-- license: MIT
CREATE TABLE coupons (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  issued_count integer NOT NULL DEFAULT 0,
  CONSTRAINT coupons_issued_count_non_negative CHECK ((issued_count >= 0))
);

-- case: cskwork/olive-clone/coupons.coupons_min_order_amount_non_negative
-- source: https://github.com/cskwork/olive-clone/tree/cd3bff9607f17842bd4c85bbbc0ad35b6ffd5eaa/src/main/resources/db/migration
-- license: MIT
CREATE TABLE coupons (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  min_order_amount numeric(12,2),
  CONSTRAINT coupons_min_order_amount_non_negative CHECK ((min_order_amount >= (0)::numeric))
);

-- case: danielwalter0/bookt/booking.valid_interval
-- source: https://github.com/danielwalter0/bookt/tree/19d881c0ad5b6fd5a98c750eac673f1c8f218432/src/main/resources/db/migration
-- license: MIT
CREATE TABLE booking (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  starts_at timestamp with time zone NOT NULL,
  ends_at timestamp with time zone NOT NULL,
  CONSTRAINT valid_interval CHECK ((ends_at > starts_at))
);

-- case: djonique/cashback-advisor-bot/bank_category_mcc.chk_bcm_mcc_format
-- source: https://github.com/djonique/cashback-advisor-bot/tree/9b7c96f4213803232afad059777fe7dc30954175/src/main/resources/db/migration
-- license: MIT
CREATE TABLE bank_category_mcc (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  mcc_code character(4) NOT NULL,
  CONSTRAINT chk_bcm_mcc_format CHECK ((mcc_code ~ '^[0-9]{4}$'::text))
);

-- case: djonique/cashback-advisor-bot/payment.chk_payment_amount
-- source: https://github.com/djonique/cashback-advisor-bot/tree/9b7c96f4213803232afad059777fe7dc30954175/src/main/resources/db/migration
-- license: MIT
CREATE TABLE payment (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  amount bigint NOT NULL,
  CONSTRAINT chk_payment_amount CHECK ((amount > 0))
);

-- case: djonique/cashback-advisor-bot/user_bank_selection.chk_ubs_percent
-- source: https://github.com/djonique/cashback-advisor-bot/tree/9b7c96f4213803232afad059777fe7dc30954175/src/main/resources/db/migration
-- license: MIT
CREATE TABLE user_bank_selection (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  cashback_percent numeric(5,2) NOT NULL,
  CONSTRAINT chk_ubs_percent CHECK (((cashback_percent > (0)::numeric) AND (cashback_percent <= (100)::numeric)))
);

-- case: felipevenas/api-dev-rats/challenge_participants.challenge_participants_best_streak_check
-- source: https://github.com/felipevenas/api-dev-rats/tree/e06de191560c3d82df7ae3e91a56289a195d7e29/src/main/resources/db/migration
-- license: MIT
CREATE TABLE challenge_participants (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  best_streak integer NOT NULL DEFAULT 0,
  CONSTRAINT challenge_participants_best_streak_check CHECK ((best_streak >= 0))
);

-- case: felipevenas/api-dev-rats/challenge_repositories.challenge_repositories_slot_check
-- source: https://github.com/felipevenas/api-dev-rats/tree/e06de191560c3d82df7ae3e91a56289a195d7e29/src/main/resources/db/migration
-- license: MIT
CREATE TABLE challenge_repositories (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  slot integer NOT NULL,
  CONSTRAINT challenge_repositories_slot_check CHECK (((slot >= 1) AND (slot <= 3)))
);

-- case: felipevenas/api-dev-rats/chat_message_attachments.chat_message_attachments_size_bytes_check
-- source: https://github.com/felipevenas/api-dev-rats/tree/e06de191560c3d82df7ae3e91a56289a195d7e29/src/main/resources/db/migration
-- license: MIT
CREATE TABLE chat_message_attachments (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  size_bytes bigint NOT NULL,
  CONSTRAINT chat_message_attachments_size_bytes_check CHECK ((size_bytes > 0))
);

-- case: felipevenas/api-dev-rats/conversations.ck_conversations_shape
-- source: https://github.com/felipevenas/api-dev-rats/tree/e06de191560c3d82df7ae3e91a56289a195d7e29/src/main/resources/db/migration
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE conversations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  type character varying(16) NOT NULL,
  group_id uuid,
  user_a_id uuid,
  user_b_id uuid,
  direct_key character varying(73),
  CONSTRAINT ck_conversations_shape CHECK (((((type)::text = 'DIRECT'::text) AND (group_id IS NULL) AND (user_a_id IS NOT NULL) AND (user_b_id IS NOT NULL) AND (direct_key IS NOT NULL) AND (user_a_id <> user_b_id)) OR (((type)::text = 'GROUP'::text) AND (group_id IS NOT NULL) AND (user_a_id IS NULL) AND (user_b_id IS NULL) AND (direct_key IS NULL))))
);

-- case: geethapriya1625/approval-workflow-service/approval_request.current_step_matches_status
-- source: https://github.com/geethapriya1625/approval-workflow-service/tree/db07c8522afa34b0ed8bb7dc31fceb109c9532cb/src/main/resources/db/migration
-- license: MIT
CREATE TABLE approval_request (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(16) NOT NULL,
  current_step integer,
  CONSTRAINT current_step_matches_status CHECK ((((status)::text = 'PENDING'::text) = (current_step IS NOT NULL)))
);

-- case: geethapriya1625/approval-workflow-service/approval_request.approval_request_amount_check
-- source: https://github.com/geethapriya1625/approval-workflow-service/tree/db07c8522afa34b0ed8bb7dc31fceb109c9532cb/src/main/resources/db/migration
-- license: MIT
CREATE TABLE approval_request (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  amount numeric(14,2) NOT NULL,
  CONSTRAINT approval_request_amount_check CHECK ((amount > (0)::numeric))
);

-- case: gelecekbilimde/backend/gb_media.c__gb_media__status
-- source: https://github.com/gelecekbilimde/backend/tree/624af080e6d381af1fadd96bb9ff61e3a39c5341/src/main/resources/db/migration
-- license: MIT
CREATE TABLE gb_media (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(25),
  CONSTRAINT c__gb_media__status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DELETED'::character varying])::text[])))
);

-- case: gelecekbilimde/backend/gb_user_follow.c__gb_user_follow__follower_user_id__followed_user_id
-- source: https://github.com/gelecekbilimde/backend/tree/624af080e6d381af1fadd96bb9ff61e3a39c5341/src/main/resources/db/migration
-- license: MIT
CREATE TABLE gb_user_follow (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  follower_user_id character varying(36) NOT NULL,
  followed_user_id character varying(36) NOT NULL,
  CONSTRAINT c__gb_user_follow__follower_user_id__followed_user_id CHECK (((follower_user_id)::text <> (followed_user_id)::text))
);

-- case: grigoriev/im/invoice.invoice_payment_days_check
-- source: https://github.com/grigoriev/im/tree/fed03ff053f7aaae1341a94534c4ed0b58b3739e/src/main/resources/db/migration
-- license: MIT
CREATE TABLE invoice (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  payment_days integer NOT NULL,
  CONSTRAINT invoice_payment_days_check CHECK (((payment_days >= 0) AND (payment_days <= 365)))
);

-- case: grigoriev/im/invoice_line.invoice_line_period
-- source: https://github.com/grigoriev/im/tree/fed03ff053f7aaae1341a94534c4ed0b58b3739e/src/main/resources/db/migration
-- license: MIT
CREATE TABLE invoice_line (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  service_from date,
  service_to date,
  CONSTRAINT invoice_line_period CHECK ((((service_from IS NULL) = (service_to IS NULL)) AND (service_to >= service_from)))
);

-- case: grigoriev/im/number_sequence.number_sequence_manual_open
-- source: https://github.com/grigoriev/im/tree/fed03ff053f7aaae1341a94534c4ed0b58b3739e/src/main/resources/db/migration
-- license: MIT
CREATE TABLE number_sequence (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  manual boolean NOT NULL DEFAULT false,
  closed boolean NOT NULL DEFAULT false,
  CONSTRAINT number_sequence_manual_open CHECK ((NOT (manual AND closed)))
);

-- case: grigoriev/im/number_gap.number_gap_counter_check
-- source: https://github.com/grigoriev/im/tree/fed03ff053f7aaae1341a94534c4ed0b58b3739e/src/main/resources/db/migration
-- license: MIT
CREATE TABLE number_gap (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  counter integer NOT NULL,
  CONSTRAINT number_gap_counter_check CHECK ((counter >= 1))
);

-- case: hmcts/civil-sdt/bulk_customer_applications.bca_cai_nn
-- source: https://github.com/hmcts/civil-sdt/tree/fe584c31bed53d3fe1033390b2ca37b5a32ad890/src/main/resources/db/migration
-- license: MIT
CREATE TABLE bulk_customer_applications (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  customer_application_id character varying(32),
  CONSTRAINT bca_cai_nn CHECK ((customer_application_id IS NOT NULL))
);

-- case: hmcts/civil-sdt/bulk_customer_applications.bca_vn_nn
-- source: https://github.com/hmcts/civil-sdt/tree/fe584c31bed53d3fe1033390b2ca37b5a32ad890/src/main/resources/db/migration
-- license: MIT
CREATE TABLE bulk_customer_applications (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version_number numeric DEFAULT 0,
  CONSTRAINT bca_vn_nn CHECK ((version_number IS NOT NULL))
);

-- case: hmcts/civil-sdt/bulk_submissions.bs_cd_nn
-- source: https://github.com/hmcts/civil-sdt/tree/fe584c31bed53d3fe1033390b2ca37b5a32ad890/src/main/resources/db/migration
-- license: MIT
CREATE TABLE bulk_submissions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  created_date timestamp(6) without time zone,
  CONSTRAINT bs_cd_nn CHECK ((created_date IS NOT NULL))
);

-- case: hmcts/civil-sdt/individual_requests.ir_dl_nn
-- source: https://github.com/hmcts/civil-sdt/tree/fe584c31bed53d3fe1033390b2ca37b5a32ad890/src/main/resources/db/migration
-- license: MIT
CREATE TABLE individual_requests (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  dead_letter boolean,
  CONSTRAINT ir_dl_nn CHECK ((dead_letter IS NOT NULL))
);

-- case: IronX10/Mini_Payment_Gateway/ledger_entries.ledger_entries_amount_check
-- source: https://github.com/IronX10/Mini_Payment_Gateway/tree/a05eb5f59d13ee8688bf238128cf37beaa71e6fa/src/main/resources/db/migration
-- license: MIT
CREATE TABLE ledger_entries (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  amount numeric(18,2) NOT NULL,
  CONSTRAINT ledger_entries_amount_check CHECK ((amount > (0)::numeric))
);

-- case: IronX10/Mini_Payment_Gateway/accounts.balance_non_negative
-- source: https://github.com/IronX10/Mini_Payment_Gateway/tree/a05eb5f59d13ee8688bf238128cf37beaa71e6fa/src/main/resources/db/migration
-- license: MIT
CREATE TABLE accounts (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  balance numeric(18,2) NOT NULL DEFAULT 0,
  CONSTRAINT balance_non_negative CHECK ((balance >= (0)::numeric))
);

-- case: java-park-mail-ru/SpaceInvasion-09-2017/users.ch__users_email
-- source: https://github.com/java-park-mail-ru/SpaceInvasion-09-2017/tree/60f79a22e55a025298133468f1c68aef7678e1be/src/main/resources/db/migration
-- license: MIT
CREATE TABLE users (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  email character varying NOT NULL,
  CONSTRAINT ch__users_email CHECK (((email)::text ~ ''::text))
);

-- case: java-park-mail-ru/SpaceInvasion-09-2017/users.ch__users_score
-- source: https://github.com/java-park-mail-ru/SpaceInvasion-09-2017/tree/60f79a22e55a025298133468f1c68aef7678e1be/src/main/resources/db/migration
-- license: MIT
CREATE TABLE users (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  score bigint NOT NULL DEFAULT 0,
  CONSTRAINT ch__users_score CHECK ((score >= 0))
);

-- case: jinwovo/order-payment/product.product_price_cents_check
-- source: https://github.com/jinwovo/order-payment/tree/774e6fa8526d27f279e7db5203ad9416851b0844/src/main/resources/db/migration
-- license: MIT
CREATE TABLE product (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  price_cents bigint NOT NULL,
  CONSTRAINT product_price_cents_check CHECK ((price_cents >= 0))
);

-- case: jinwovo/order-payment/product.product_stock_check
-- source: https://github.com/jinwovo/order-payment/tree/774e6fa8526d27f279e7db5203ad9416851b0844/src/main/resources/db/migration
-- license: MIT
CREATE TABLE product (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  stock integer NOT NULL,
  CONSTRAINT product_stock_check CHECK ((stock >= 0))
);

-- case: jinwovo/order-payment/order_item.order_item_quantity_check
-- source: https://github.com/jinwovo/order-payment/tree/774e6fa8526d27f279e7db5203ad9416851b0844/src/main/resources/db/migration
-- license: MIT
CREATE TABLE order_item (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  quantity integer NOT NULL,
  CONSTRAINT order_item_quantity_check CHECK ((quantity > 0))
);

-- case: JoarEliasson/image-questionnaire/question.question_index_in_set_check
-- source: https://github.com/JoarEliasson/image-questionnaire/tree/eca775a089f67797349905be0a36aa5c48362d57/src/main/resources/db/migration
-- license: MIT
CREATE TABLE question (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  index_in_set integer NOT NULL,
  CONSTRAINT question_index_in_set_check CHECK (((index_in_set >= 1) AND (index_in_set <= 15)))
);

-- case: JoarEliasson/image-questionnaire/invitation.invitation_status_check
-- source: https://github.com/JoarEliasson/image-questionnaire/tree/eca775a089f67797349905be0a36aa5c48362d57/src/main/resources/db/migration
-- license: MIT
CREATE TABLE invitation (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status text NOT NULL,
  CONSTRAINT invitation_status_check CHECK ((status = ANY (ARRAY['created'::text, 'sent'::text, 'started'::text, 'completed'::text, 'expired'::text, 'bounced'::text])))
);

-- case: Kaushal2910/finledger/postings.chk_posting_amount_positive
-- source: https://github.com/Kaushal2910/finledger/tree/424e8e7673869c2b2560236e2573043939f449d3/src/main/resources/db/migration
-- license: MIT
CREATE TABLE postings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  amount numeric(20,2) NOT NULL,
  CONSTRAINT chk_posting_amount_positive CHECK ((amount > (0)::numeric))
);

-- case: Kaushal2910/finledger/transfers.chk_transfer_distinct_parties
-- source: https://github.com/Kaushal2910/finledger/tree/424e8e7673869c2b2560236e2573043939f449d3/src/main/resources/db/migration
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE transfers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  payer_id uuid NOT NULL,
  payee_id uuid NOT NULL,
  CONSTRAINT chk_transfer_distinct_parties CHECK ((payer_id <> payee_id))
);

-- case: Kaushal2910/finledger/accounts.chk_accounts_balance_scale
-- source: https://github.com/Kaushal2910/finledger/tree/424e8e7673869c2b2560236e2573043939f449d3/src/main/resources/db/migration
-- license: MIT
CREATE TABLE accounts (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  balance numeric(20,2) NOT NULL DEFAULT 0,
  CONSTRAINT chk_accounts_balance_scale CHECK ((balance = round(balance, 2)))
);

-- case: kenanatasoy/online-charging-service/account.account_check
-- source: https://github.com/kenanatasoy/online-charging-service/tree/91705d2692aea9cd96e9245ab6aceb4152c71d9c/src/main/resources/db/migration
-- license: MIT
CREATE TABLE account (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  balance bigint NOT NULL,
  reserved bigint NOT NULL DEFAULT 0,
  CONSTRAINT account_check CHECK (((reserved >= 0) AND (reserved <= balance)))
);

-- case: kenanatasoy/online-charging-service/account.account_balance_check
-- source: https://github.com/kenanatasoy/online-charging-service/tree/91705d2692aea9cd96e9245ab6aceb4152c71d9c/src/main/resources/db/migration
-- license: MIT
CREATE TABLE account (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  balance bigint NOT NULL,
  CONSTRAINT account_balance_check CHECK ((balance >= 0))
);

-- case: LijuanTang94/flashdeal/seckill_voucher.chk_stock_nonnegative
-- source: https://github.com/LijuanTang94/flashdeal/tree/900a7cc17a702674ef33108e0b33f74efce528c0/src/main/resources/db/migration
-- license: MIT
CREATE TABLE seckill_voucher (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  stock integer NOT NULL,
  CONSTRAINT chk_stock_nonnegative CHECK ((stock >= 0))
);

-- case: Lucas-blip-png/mesa-pronta/session.session_max_players_check
-- source: https://github.com/Lucas-blip-png/mesa-pronta/tree/31d2d486906a6060be86111cb4221f00dd5fe4ce/src/main/resources/db/migration
-- license: MIT
CREATE TABLE session (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  max_players integer NOT NULL,
  CONSTRAINT session_max_players_check CHECK (((max_players >= 1) AND (max_players <= 50)))
);

-- case: MaheshBhushan/mediavault/upload_part.upload_part_part_number_check
-- source: https://github.com/MaheshBhushan/mediavault/tree/a6efb7958e8fde4b9a02690bc068feef94bd3834/src/main/resources/db/migration
-- license: MIT
CREATE TABLE upload_part (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  part_number integer NOT NULL,
  CONSTRAINT upload_part_part_number_check CHECK (((part_number >= 1) AND (part_number <= 10000)))
);

-- case: MaheshBhushan/mediavault/media.media_size_bytes_check
-- source: https://github.com/MaheshBhushan/mediavault/tree/a6efb7958e8fde4b9a02690bc068feef94bd3834/src/main/resources/db/migration
-- license: MIT
CREATE TABLE media (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  size_bytes bigint NOT NULL,
  CONSTRAINT media_size_bytes_check CHECK ((size_bytes > 0))
);

-- case: mahira-manico/Correspondances/booking.ck_total_price
-- source: https://github.com/mahira-manico/Correspondances/tree/bb83195217c4516c09d217455b47198e8a55bf7f/src/main/resources/db/migration
-- license: MIT
CREATE TABLE booking (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  total_price numeric(10,2) NOT NULL,
  CONSTRAINT ck_total_price CHECK ((total_price >= (0)::numeric))
);

-- case: mahira-manico/Correspondances/carriage.ck_carriage_number
-- source: https://github.com/mahira-manico/Correspondances/tree/bb83195217c4516c09d217455b47198e8a55bf7f/src/main/resources/db/migration
-- license: MIT
CREATE TABLE carriage (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  carriage_number smallint NOT NULL,
  CONSTRAINT ck_carriage_number CHECK ((carriage_number > 0))
);

-- case: mahira-manico/Correspondances/calendar.ck_calendar_validity
-- source: https://github.com/mahira-manico/Correspondances/tree/bb83195217c4516c09d217455b47198e8a55bf7f/src/main/resources/db/migration
-- license: MIT
CREATE TABLE calendar (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  valid_from date NOT NULL,
  valid_to date NOT NULL,
  CONSTRAINT ck_calendar_validity CHECK ((valid_from <= valid_to))
);

-- case: mahira-manico/Correspondances/stop_time.ck_arrival_departure_order
-- source: https://github.com/mahira-manico/Correspondances/tree/bb83195217c4516c09d217455b47198e8a55bf7f/src/main/resources/db/migration
-- license: MIT
CREATE TABLE stop_time (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  arrival_time time without time zone NOT NULL,
  departure_time time without time zone NOT NULL,
  CONSTRAINT ck_arrival_departure_order CHECK ((arrival_time <= departure_time))
);

-- case: MansurTiyes/newport-whale-api/daily_report.daily_report_tours_check
-- source: https://github.com/MansurTiyes/newport-whale-api/tree/689bf711a46b337a7d5dc1a1e901c6359fd618aa/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE daily_report (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tours integer NOT NULL,
  CONSTRAINT daily_report_tours_check CHECK ((tours >= 0))
);

-- case: MatheusCavalari/wallet-service/ledger_entry.ledger_entry_amount_check
-- source: https://github.com/MatheusCavalari/wallet-service/tree/6172081219b116e03535bcf4ca79c31b3826d59c/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE ledger_entry (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  amount numeric(19,2) NOT NULL,
  CONSTRAINT ledger_entry_amount_check CHECK ((amount > (0)::numeric))
);

-- case: ministryofjustice/hmpps-non-associations-api/non_association.non_association_first_prisoner_number_check
-- source: https://github.com/ministryofjustice/hmpps-non-associations-api/tree/2fb440797a3cb995dfb0c2ddf270c25631ac0500/src/main/resources/db/migration
-- license: MIT
CREATE TABLE non_association (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  first_prisoner_number character varying(10) NOT NULL,
  CONSTRAINT non_association_first_prisoner_number_check CHECK ((length((first_prisoner_number)::text) > 0))
);

-- case: ministryofjustice/hmpps-non-associations-api/non_association.non_association_check
-- source: https://github.com/ministryofjustice/hmpps-non-associations-api/tree/2fb440797a3cb995dfb0c2ddf270c25631ac0500/src/main/resources/db/migration
-- license: MIT
CREATE TABLE non_association (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  is_closed boolean NOT NULL DEFAULT false,
  closed_by character varying(60),
  CONSTRAINT non_association_check CHECK ((((is_closed IS TRUE) AND (closed_by IS NOT NULL)) OR ((is_closed IS FALSE) AND (closed_by IS NULL))))
);

-- case: ministryofjustice/hmpps-non-associations-api/non_association.non_association_check1
-- source: https://github.com/ministryofjustice/hmpps-non-associations-api/tree/2fb440797a3cb995dfb0c2ddf270c25631ac0500/src/main/resources/db/migration
-- license: MIT
CREATE TABLE non_association (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  is_closed boolean NOT NULL DEFAULT false,
  closed_reason text,
  CONSTRAINT non_association_check1 CHECK ((((is_closed IS TRUE) AND (closed_reason IS NOT NULL)) OR ((is_closed IS FALSE) AND (closed_reason IS NULL))))
);

-- case: ministryofjustice/hmpps-non-associations-api/non_association.non_association_check2
-- source: https://github.com/ministryofjustice/hmpps-non-associations-api/tree/2fb440797a3cb995dfb0c2ddf270c25631ac0500/src/main/resources/db/migration
-- license: MIT
CREATE TABLE non_association (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  is_closed boolean NOT NULL DEFAULT false,
  closed_at timestamp with time zone,
  CONSTRAINT non_association_check2 CHECK ((((is_closed IS TRUE) AND (closed_at IS NOT NULL)) OR ((is_closed IS FALSE) AND (closed_at IS NULL))))
);

-- case: MogProgJ/MiniERP-Springboot/inventory.inventory_quantity_on_hand_check
-- source: https://github.com/MogProgJ/MiniERP-Springboot/tree/f64428a3c9dff3064289a7c8b16be7c390ec5101/src/main/resources/db/migration
-- license: MIT
CREATE TABLE inventory (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  quantity_on_hand integer NOT NULL DEFAULT 0,
  CONSTRAINT inventory_quantity_on_hand_check CHECK ((quantity_on_hand >= 0))
);

-- case: MogProgJ/MiniERP-Springboot/orders.orders_status_check
-- source: https://github.com/MogProgJ/MiniERP-Springboot/tree/f64428a3c9dff3064289a7c8b16be7c390ec5101/src/main/resources/db/migration
-- license: MIT
CREATE TABLE orders (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status text NOT NULL,
  CONSTRAINT orders_status_check CHECK ((status = ANY (ARRAY['DRAFT'::text, 'CONFIRMED'::text, 'CANCELLED'::text])))
);

-- case: MogProgJ/MiniERP-Springboot/order_items.order_items_qty_check
-- source: https://github.com/MogProgJ/MiniERP-Springboot/tree/f64428a3c9dff3064289a7c8b16be7c390ec5101/src/main/resources/db/migration
-- license: MIT
CREATE TABLE order_items (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  qty integer NOT NULL,
  CONSTRAINT order_items_qty_check CHECK ((qty > 0))
);

-- case: Mosugar/recur-api/subscriptions.ck_subscriptions_category
-- source: https://github.com/Mosugar/recur-api/tree/dbdbbcd759d59f3bf0a9c2383051159b9f000920/src/main/resources/db/migration
-- license: MIT
CREATE TABLE subscriptions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  category character varying(30) NOT NULL DEFAULT 'OTHER'::character varying,
  CONSTRAINT ck_subscriptions_category CHECK (((category)::text = ANY ((ARRAY['STREAMING'::character varying, 'MUSIC'::character varying, 'SOFTWARE'::character varying, 'GAMING'::character varying, 'FITNESS'::character varying, 'CLOUD'::character varying, 'NEWS'::character varying, 'OTHER'::character varying])::text[])))
);

-- case: Mosugar/recur-api/subscriptions.ck_subscriptions_price
-- source: https://github.com/Mosugar/recur-api/tree/dbdbbcd759d59f3bf0a9c2383051159b9f000920/src/main/resources/db/migration
-- license: MIT
CREATE TABLE subscriptions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  price_cents bigint NOT NULL,
  CONSTRAINT ck_subscriptions_price CHECK ((price_cents >= 0))
);

-- case: nathanTuala/ZeraPay/payments.chk_payments_notify_status
-- source: https://github.com/nathanTuala/ZeraPay/tree/b42bcda2d6dd0483812d55c1b405683b593be9a4/src/main/resources/db/migration
-- license: MIT
CREATE TABLE payments (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  client_notify_status character varying(30),
  CONSTRAINT chk_payments_notify_status CHECK (((client_notify_status IS NULL) OR ((client_notify_status)::text = ANY ((ARRAY['CLIENT_NOTIFY_PENDING'::character varying, 'CLIENT_NOTIFY_SUCCEEDED'::character varying, 'CLIENT_NOTIFY_FAILED'::character varying])::text[]))))
);

-- case: navikt/veilarbdialog/eskaleringsvarsel.gjeldende
-- source: https://github.com/navikt/veilarbdialog/tree/8748bb6b68c51abdc1572b412d859f2f713eaeee/src/main/resources/db/migration
-- license: MIT
CREATE TABLE eskaleringsvarsel (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  aktor_id character varying(255) NOT NULL,
  avsluttet_dato timestamp(6) without time zone,
  gjeldende character varying(255),
  CONSTRAINT gjeldende CHECK ((((avsluttet_dato IS NULL) AND ((gjeldende)::text = (aktor_id)::text)) OR ((avsluttet_dato IS NOT NULL) AND (gjeldende IS NULL))))
);

-- case: normikoto/elysium/custom_emoji.custom_emoji_check
-- source: https://github.com/normikoto/elysium/tree/96e1de25af890f01edc611aba1721ced9f9a7742/src/main/resources/db/migration
-- license: MIT
CREATE TABLE custom_emoji (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  image_name text,
  static_name text,
  CONSTRAINT custom_emoji_check CHECK (((static_name IS NULL) = (image_name IS NULL)))
);

-- case: normikoto/elysium/media_attachment.media_attachment_type_check
-- source: https://github.com/normikoto/elysium/tree/96e1de25af890f01edc611aba1721ced9f9a7742/src/main/resources/db/migration
-- license: MIT
CREATE TABLE media_attachment (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  type text NOT NULL,
  CONSTRAINT media_attachment_type_check CHECK ((type = ANY (ARRAY['image'::text, 'video'::text, 'audio'::text, 'unknown'::text])))
);

-- case: normikoto/elysium/invite.invite_max_uses_check
-- source: https://github.com/normikoto/elysium/tree/96e1de25af890f01edc611aba1721ced9f9a7742/src/main/resources/db/migration
-- license: MIT
CREATE TABLE invite (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  max_uses integer,
  CONSTRAINT invite_max_uses_check CHECK ((max_uses > 0))
);

-- case: normikoto/elysium/instance_actor.instance_actor_singleton_check
-- source: https://github.com/normikoto/elysium/tree/96e1de25af890f01edc611aba1721ced9f9a7742/src/main/resources/db/migration
-- license: MIT
CREATE TABLE instance_actor (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  singleton boolean NOT NULL DEFAULT true,
  CONSTRAINT instance_actor_singleton_check CHECK (singleton)
);

-- case: olliveirapedro/track-my-learn/study_session.study_session_duration_minutes_check
-- source: https://github.com/olliveirapedro/track-my-learn/tree/870c31a7dfa8b073ee3274b8de269588e3752435/src/main/resources/db/migration
-- license: MIT
CREATE TABLE study_session (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  duration_minutes integer NOT NULL,
  CONSTRAINT study_session_duration_minutes_check CHECK ((duration_minutes > 0))
);

-- case: OtterMind/Nubase/ai_gateway_billing_accounts.ck_billing_accounts_currency
-- source: https://github.com/OtterMind/Nubase/tree/4b1fb453c3a5a25f31f6e951a41bb6102b2d6d77/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE ai_gateway_billing_accounts (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  currency character varying(8) NOT NULL DEFAULT 'USD'::character varying,
  CONSTRAINT ck_billing_accounts_currency CHECK (((currency)::text ~ '^[A-Z]{3,8}$'::text))
);

-- case: OtterMind/Nubase/ai_gateway_billing_accounts.ck_billing_accounts_credit_nonnegative
-- source: https://github.com/OtterMind/Nubase/tree/4b1fb453c3a5a25f31f6e951a41bb6102b2d6d77/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE ai_gateway_billing_accounts (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  credit_limit numeric(24,8) NOT NULL DEFAULT 0,
  CONSTRAINT ck_billing_accounts_credit_nonnegative CHECK ((credit_limit >= (0)::numeric))
);

-- case: OtterMind/Nubase/database_configs.database_configs_init_lease_consistency
-- source: https://github.com/OtterMind/Nubase/tree/4b1fb453c3a5a25f31f6e951a41bb6102b2d6d77/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE database_configs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  init_status character varying(32),
  init_lease_token uuid,
  init_lease_expires_at timestamp with time zone,
  CONSTRAINT database_configs_init_lease_consistency CHECK ((((init_lease_token IS NULL) AND (init_lease_expires_at IS NULL)) OR ((init_lease_token IS NOT NULL) AND (init_lease_expires_at IS NOT NULL) AND ((init_status)::text = 'INITIALIZING'::text))))
);

-- case: OtterMind/Nubase/ai_gateway_billing_requests.ck_billing_requests_amounts_nonnegative
-- source: https://github.com/OtterMind/Nubase/tree/4b1fb453c3a5a25f31f6e951a41bb6102b2d6d77/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE ai_gateway_billing_requests (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  reserved_amount numeric(24,8) NOT NULL,
  actual_amount numeric(24,8),
  CONSTRAINT ck_billing_requests_amounts_nonnegative CHECK (((reserved_amount >= (0)::numeric) AND ((actual_amount IS NULL) OR (actual_amount >= (0)::numeric))))
);

-- case: P31P/Project_Tracker_CS5013/idea.idea_title_check
-- source: https://github.com/P31P/Project_Tracker_CS5013/tree/838c533f91a4c93616e33d09d0e4e654faf74127/src/main/resources/db/migration
-- license: MIT
CREATE TABLE idea (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  title text NOT NULL,
  CONSTRAINT idea_title_check CHECK ((btrim(title) <> ''::text))
);

-- case: P31P/Project_Tracker_CS5013/idea_link.idea_link_url_check
-- source: https://github.com/P31P/Project_Tracker_CS5013/tree/838c533f91a4c93616e33d09d0e4e654faf74127/src/main/resources/db/migration
-- license: MIT
CREATE TABLE idea_link (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  url text NOT NULL,
  CONSTRAINT idea_link_url_check CHECK ((url ~* '^https?://'::text))
);

-- case: P31P/Project_Tracker_CS5013/tag.tag_name_check
-- source: https://github.com/P31P/Project_Tracker_CS5013/tree/838c533f91a4c93616e33d09d0e4e654faf74127/src/main/resources/db/migration
-- license: MIT
CREATE TABLE tag (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name text NOT NULL,
  CONSTRAINT tag_name_check CHECK (((name = lower(btrim(name))) AND (name <> ''::text)))
);

-- case: PavelPutin/restobook_backend/reservation.persons_number_is_positive
-- source: https://github.com/PavelPutin/restobook_backend/tree/3d11237beea4f9286091ba0bf1ec234d486d7eff/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE reservation (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  persons_number integer NOT NULL,
  CONSTRAINT persons_number_is_positive CHECK ((persons_number > 0))
);

-- case: piashcse/ktor-E-Commerce/b2b_account.b2b_terms_positive
-- source: https://github.com/piashcse/ktor-E-Commerce/tree/761fb97895f39fd8c94bbbf435405ed16d114d2d/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE b2b_account (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  payment_terms_days integer NOT NULL DEFAULT 30,
  CONSTRAINT b2b_terms_positive CHECK ((payment_terms_days > 0))
);

-- case: piashcse/ktor-E-Commerce/b2b_account.b2b_credit_non_negative
-- source: https://github.com/piashcse/ktor-E-Commerce/tree/761fb97895f39fd8c94bbbf435405ed16d114d2d/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE b2b_account (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  credit_limit numeric(12,2) NOT NULL DEFAULT 0,
  CONSTRAINT b2b_credit_non_negative CHECK ((credit_limit >= (0)::numeric))
);

-- case: piashcse/ktor-E-Commerce/commission_rule.commission_rule_rate_check
-- source: https://github.com/piashcse/ktor-E-Commerce/tree/761fb97895f39fd8c94bbbf435405ed16d114d2d/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE commission_rule (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  rate numeric(5,2) NOT NULL,
  CONSTRAINT commission_rule_rate_check CHECK (((rate >= (0)::numeric) AND (rate <= (100)::numeric)))
);

-- case: piashcse/ktor-E-Commerce/review_rating.review_rating_rating_check
-- source: https://github.com/piashcse/ktor-E-Commerce/tree/761fb97895f39fd8c94bbbf435405ed16d114d2d/src/main/resources/db/migration
-- license: Apache-2.0
CREATE TABLE review_rating (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  rating integer NOT NULL,
  CONSTRAINT review_rating_rating_check CHECK (((rating >= 1) AND (rating <= 5)))
);

-- case: RIPE-NCC/rpki-publication-server/versions.delta_field_in_sync
-- source: https://github.com/RIPE-NCC/rpki-publication-server/tree/5805abf90ea10eacec8e3e2b03ba2dbaaaf7fa14/src/main/resources/db/migration
-- license: BSD-3-Clause
CREATE TABLE versions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  delta_file_name text,
  delta_hash bytea,
  delta_size bigint,
  CONSTRAINT delta_field_in_sync CHECK ((((delta_size IS NULL) AND (delta_hash IS NULL) AND (delta_file_name IS NULL)) OR ((delta_size IS NOT NULL) AND (delta_hash IS NOT NULL) AND (delta_file_name IS NOT NULL))))
);

-- case: RIPE-NCC/rpki-publication-server/versions.versions_delta_hash_check
-- source: https://github.com/RIPE-NCC/rpki-publication-server/tree/5805abf90ea10eacec8e3e2b03ba2dbaaaf7fa14/src/main/resources/db/migration
-- license: BSD-3-Clause
CREATE TABLE versions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  delta_hash bytea,
  CONSTRAINT versions_delta_hash_check CHECK ((length(delta_hash) = 32))
);

-- case: RIPE-NCC/rpki-publication-server/objects.objects_hash_check
-- source: https://github.com/RIPE-NCC/rpki-publication-server/tree/5805abf90ea10eacec8e3e2b03ba2dbaaaf7fa14/src/main/resources/db/migration
-- license: BSD-3-Clause
CREATE TABLE objects (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  hash bytea NOT NULL,
  CONSTRAINT objects_hash_check CHECK ((length(hash) = 32))
);

-- case: RIPE-NCC/rpki-publication-server/object_log.object_log_check
-- source: https://github.com/RIPE-NCC/rpki-publication-server/tree/5805abf90ea10eacec8e3e2b03ba2dbaaaf7fa14/src/main/resources/db/migration
-- license: BSD-3-Clause
-- expect: CLEAR_ERROR
CREATE TABLE object_log (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  operation text NOT NULL,
  new_object_id bigint,
  old_object_id bigint,
  CONSTRAINT object_log_check CHECK (
CASE operation
    WHEN 'INS'::text THEN ((old_object_id IS NULL) AND (new_object_id IS NOT NULL))
    WHEN 'UPD'::text THEN ((new_object_id IS NOT NULL) AND (new_object_id IS NOT NULL))
    WHEN 'DEL'::text THEN ((old_object_id IS NOT NULL) AND (new_object_id IS NULL))
    ELSE false
END)
);

-- case: rubenrzprz/reshub/reservation.reservation_cancelled_at_check
-- source: https://github.com/rubenrzprz/reshub/tree/e48d00eec628c761a816838c96af9b778d72448a/src/main/resources/db/migration
-- license: MIT
CREATE TABLE reservation (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(16) NOT NULL,
  cancelled_at timestamp with time zone,
  CONSTRAINT reservation_cancelled_at_check CHECK (((((status)::text = 'CANCELLED'::text) AND (cancelled_at IS NOT NULL)) OR (((status)::text <> 'CANCELLED'::text) AND (cancelled_at IS NULL))))
);

-- case: rubenrzprz/reshub/reservation.reservation_party_check
-- source: https://github.com/rubenrzprz/reshub/tree/e48d00eec628c761a816838c96af9b778d72448a/src/main/resources/db/migration
-- license: MIT
CREATE TABLE reservation (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  adults smallint NOT NULL DEFAULT 1,
  children smallint NOT NULL DEFAULT 0,
  CONSTRAINT reservation_party_check CHECK (((adults >= 1) AND (children >= 0)))
);

-- case: rubenrzprz/reshub/app_user.app_user_affiliation_xor
-- source: https://github.com/rubenrzprz/reshub/tree/e48d00eec628c761a816838c96af9b778d72448a/src/main/resources/db/migration
-- license: MIT
-- expect: CLEAR_ERROR
CREATE TABLE app_user (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  hotel_id uuid,
  agency_id uuid,
  CONSTRAINT app_user_affiliation_xor CHECK (((hotel_id IS NULL) <> (agency_id IS NULL)))
);

-- case: rubenrzprz/reshub/agency_hotel_auth.agency_hotel_auth_validity_check
-- source: https://github.com/rubenrzprz/reshub/tree/e48d00eec628c761a816838c96af9b778d72448a/src/main/resources/db/migration
-- license: MIT
CREATE TABLE agency_hotel_auth (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  valid_from date,
  valid_to date,
  CONSTRAINT agency_hotel_auth_validity_check CHECK (((valid_from IS NULL) OR (valid_to IS NULL) OR (valid_from <= valid_to)))
);

-- case: Salad109/library-management/copies.chk_copies_due_at
-- source: https://github.com/Salad109/library-management/tree/8573fde43e94431d48074f5daf211b170cb48452/src/main/resources/db/migration
-- license: MIT
CREATE TABLE copies (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(9) NOT NULL,
  due_at timestamp with time zone,
  CONSTRAINT chk_copies_due_at CHECK ((((status)::text = ANY ((ARRAY['RESERVED'::character varying, 'BORROWED'::character varying])::text[])) = (due_at IS NOT NULL)))
);

-- case: Salad109/library-management/books.chk_books_year
-- source: https://github.com/Salad109/library-management/tree/8573fde43e94431d48074f5daf211b170cb48452/src/main/resources/db/migration
-- license: MIT
CREATE TABLE books (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  publication_year integer,
  CONSTRAINT chk_books_year CHECK ((publication_year >= 1))
);

-- case: Salad109/SIMPLETON/conjunction.objects_ordered
-- source: https://github.com/Salad109/SIMPLETON/tree/bae7a0522174611195cf52e734387069b6ee7042/src/main/resources/db/migration
-- license: MIT
CREATE TABLE conjunction (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  object1_norad_id integer NOT NULL,
  object2_norad_id integer NOT NULL,
  CONSTRAINT objects_ordered CHECK ((object1_norad_id < object2_norad_id))
);

-- case: Shiva04-Dev/E_Commerce_Platform/order.order_total_amount_check
-- source: https://github.com/Shiva04-Dev/E_Commerce_Platform/tree/6809fb18a7f395effe92dfd23b3e2b03f576102b/src/main/resources/db/migration
-- license: MIT
CREATE TABLE "order" (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  total_amount numeric(10,2) NOT NULL,
  CONSTRAINT order_total_amount_check CHECK ((total_amount >= (0)::numeric))
);

-- case: Shiva04-Dev/E_Commerce_Platform/cart_item.cart_item_quantity_check
-- source: https://github.com/Shiva04-Dev/E_Commerce_Platform/tree/6809fb18a7f395effe92dfd23b3e2b03f576102b/src/main/resources/db/migration
-- license: MIT
CREATE TABLE cart_item (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  quantity integer NOT NULL,
  CONSTRAINT cart_item_quantity_check CHECK ((quantity > 0))
);

-- case: Shiva04-Dev/E_Commerce_Platform/product.product_stock_quantity_check
-- source: https://github.com/Shiva04-Dev/E_Commerce_Platform/tree/6809fb18a7f395effe92dfd23b3e2b03f576102b/src/main/resources/db/migration
-- license: MIT
CREATE TABLE product (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  stock_quantity integer NOT NULL DEFAULT 0,
  CONSTRAINT product_stock_quantity_check CHECK ((stock_quantity >= 0))
);

-- case: thesunnycode/ResolveAI/agent_profile.ck_agent_max
-- source: https://github.com/thesunnycode/ResolveAI/tree/2db2aecc950c63b9cc5bd3961076eb4e0fce0ee5/src/main/resources/db/migration
-- license: MIT
CREATE TABLE agent_profile (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  max_concurrent integer NOT NULL DEFAULT 15,
  CONSTRAINT ck_agent_max CHECK ((max_concurrent > 0))
);

-- case: thesunnycode/ResolveAI/tenant_ai_policy.ck_policy_budget
-- source: https://github.com/thesunnycode/ResolveAI/tree/2db2aecc950c63b9cc5bd3961076eb4e0fce0ee5/src/main/resources/db/migration
-- license: MIT
CREATE TABLE tenant_ai_policy (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  monthly_budget_micros bigint NOT NULL DEFAULT 5000000,
  CONSTRAINT ck_policy_budget CHECK ((monthly_budget_micros >= 0))
);

-- case: thesunnycode/ResolveAI/agent_profile.ck_agent_open
-- source: https://github.com/thesunnycode/ResolveAI/tree/2db2aecc950c63b9cc5bd3961076eb4e0fce0ee5/src/main/resources/db/migration
-- license: MIT
CREATE TABLE agent_profile (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  open_count integer NOT NULL DEFAULT 0,
  CONSTRAINT ck_agent_open CHECK ((open_count >= 0))
);

-- case: thesunnycode/ResolveAI/business_calendar.ck_calendar_hours
-- source: https://github.com/thesunnycode/ResolveAI/tree/2db2aecc950c63b9cc5bd3961076eb4e0fce0ee5/src/main/resources/db/migration
-- license: MIT
CREATE TABLE business_calendar (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  day_start time without time zone NOT NULL DEFAULT '09:00:00'::time without time zone,
  day_end time without time zone NOT NULL DEFAULT '18:00:00'::time without time zone,
  CONSTRAINT ck_calendar_hours CHECK ((day_end > day_start))
);

-- case: Vinay2080/dragon-of-north/event_publication.event_publication_status_check
-- source: https://github.com/Vinay2080/dragon-of-north/tree/30a8b32d5701da8f233079be3778370d1c8be3f9/src/main/resources/db/migration
-- license: MIT
CREATE TABLE event_publication (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  status character varying(255),
  CONSTRAINT event_publication_status_check CHECK (((status)::text = ANY ((ARRAY['PUBLISHED'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'RESUBMITTED'::character varying])::text[])))
);

-- case: Vineet829/florin/business_settings.chk_bs_upi
-- source: https://github.com/Vineet829/florin/tree/efd460fc8232b6aed9a4e43f4ceeba96767f63f2/src/main/resources/db/migration
-- license: MIT
CREATE TABLE business_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pay_upi_id character varying(100),
  CONSTRAINT chk_bs_upi CHECK (((pay_upi_id)::text ~ '^[a-z0-9][a-z0-9._-]{1,63}@[a-z][a-z0-9.]{1,31}$'::text))
);

-- case: Vineet829/florin/documents.chk_documents_exchange
-- source: https://github.com/Vineet829/florin/tree/efd460fc8232b6aed9a4e43f4ceeba96767f63f2/src/main/resources/db/migration
-- license: MIT
CREATE TABLE documents (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  currency character varying(10) NOT NULL,
  exchange_rate numeric(20,10),
  exchange_base character varying(3),
  CONSTRAINT chk_documents_exchange CHECK ((((exchange_rate IS NULL) AND (exchange_base IS NULL)) OR ((exchange_rate > (0)::numeric) AND (exchange_base IS NOT NULL) AND ((exchange_base)::text <> (currency)::text))))
);

-- case: Vineet829/florin/payment_claims.chk_claims_amount
-- source: https://github.com/Vineet829/florin/tree/efd460fc8232b6aed9a4e43f4ceeba96767f63f2/src/main/resources/db/migration
-- license: MIT
CREATE TABLE payment_claims (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  amount numeric(19,2) NOT NULL,
  CONSTRAINT chk_claims_amount CHECK ((amount > (0)::numeric))
);

-- case: Vineet829/florin/catalog_items.chk_catalog_items_price
-- source: https://github.com/Vineet829/florin/tree/efd460fc8232b6aed9a4e43f4ceeba96767f63f2/src/main/resources/db/migration
-- license: MIT
CREATE TABLE catalog_items (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  unit_price numeric(19,4) NOT NULL DEFAULT 0,
  CONSTRAINT chk_catalog_items_price CHECK ((unit_price >= (0)::numeric))
);

-- case: yu-teacher/doro-interview/my_note.my_note_body_len_chk
-- source: https://github.com/yu-teacher/doro-interview/tree/fc23163af662eea03172f8d34f2fb09f9220b95c/src/main/resources/db/migration
-- license: MIT
CREATE TABLE my_note (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  body text NOT NULL,
  CONSTRAINT my_note_body_len_chk CHECK (((char_length(body) >= 1) AND (char_length(body) <= 2000)))
);

-- case: yu-teacher/doro-interview/quiz_session.quiz_session_count_chk
-- source: https://github.com/yu-teacher/doro-interview/tree/fc23163af662eea03172f8d34f2fb09f9220b95c/src/main/resources/db/migration
-- license: MIT
CREATE TABLE quiz_session (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  planned_count integer NOT NULL,
  CONSTRAINT quiz_session_count_chk CHECK (((planned_count >= 1) AND (planned_count <= 30)))
);

-- case: yu-teacher/doro-interview/question.question_difficulty_chk
-- source: https://github.com/yu-teacher/doro-interview/tree/fc23163af662eea03172f8d34f2fb09f9220b95c/src/main/resources/db/migration
-- license: MIT
CREATE TABLE question (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  difficulty smallint NOT NULL,
  CONSTRAINT question_difficulty_chk CHECK (((difficulty >= 1) AND (difficulty <= 3)))
);

-- case: yu-teacher/doro-interview/quiz_question_stats.quiz_stats_counts_chk
-- source: https://github.com/yu-teacher/doro-interview/tree/fc23163af662eea03172f8d34f2fb09f9220b95c/src/main/resources/db/migration
-- license: MIT
CREATE TABLE quiz_question_stats (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ask_count integer NOT NULL DEFAULT 0,
  miss_count integer NOT NULL DEFAULT 0,
  CONSTRAINT quiz_stats_counts_chk CHECK (((ask_count >= 0) AND (miss_count >= 0) AND (miss_count <= ask_count)))
);

-- case: zach-garno/CurrencyTransWEXlator/exchange_rates.chk_exchange_rates_rate_positive
-- source: https://github.com/zach-garno/CurrencyTransWEXlator/tree/963b03fc5dc47b933109e6e83e1f285d6203dbc7/src/main/resources/db/migration
-- license: BSD-3-Clause
CREATE TABLE exchange_rates (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  rate numeric(19,6) NOT NULL,
  CONSTRAINT chk_exchange_rates_rate_positive CHECK ((rate > (0)::numeric))
);

-- case: zach-garno/CurrencyTransWEXlator/transactions.chk_transactions_description_not_blank
-- source: https://github.com/zach-garno/CurrencyTransWEXlator/tree/963b03fc5dc47b933109e6e83e1f285d6203dbc7/src/main/resources/db/migration
-- license: BSD-3-Clause
CREATE TABLE transactions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  description character varying(50) NOT NULL,
  CONSTRAINT chk_transactions_description_not_blank CHECK ((TRIM(BOTH FROM description) <> ''::text))
);
