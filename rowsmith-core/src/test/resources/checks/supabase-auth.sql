-- case: supabase-auth/oauth_authorizations.oauth_authorizations_authorization_code_length
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE oauth_authorizations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  authorization_code text,
  CONSTRAINT oauth_authorizations_authorization_code_length CHECK ((char_length(authorization_code) <= 255))
);

-- case: supabase-auth/custom_oauth_providers.custom_oauth_providers_authorization_url_length
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE custom_oauth_providers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  authorization_url text,
  CONSTRAINT custom_oauth_providers_authorization_url_length CHECK (((authorization_url IS NULL) OR (char_length(authorization_url) <= 2048)))
);

-- case: supabase-auth/one_time_tokens.one_time_tokens_token_hash_check
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE one_time_tokens (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  token_hash text NOT NULL,
  CONSTRAINT one_time_tokens_token_hash_check CHECK ((char_length(token_hash) > 0))
);

-- case: supabase-auth/custom_oauth_providers.custom_oauth_providers_authorization_url_https
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE custom_oauth_providers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  authorization_url text,
  CONSTRAINT custom_oauth_providers_authorization_url_https CHECK (((authorization_url IS NULL) OR (authorization_url ~~ 'https://%'::text)))
);

-- case: supabase-auth/oauth_authorizations.oauth_authorizations_redirect_uri_length
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE oauth_authorizations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  redirect_uri text NOT NULL,
  CONSTRAINT oauth_authorizations_redirect_uri_length CHECK ((char_length(redirect_uri) <= 2048))
);

-- case: supabase-auth/custom_oauth_providers.custom_oauth_providers_oidc_discovery_url_https
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE custom_oauth_providers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  provider_type text NOT NULL,
  discovery_url text,
  CONSTRAINT custom_oauth_providers_oidc_discovery_url_https CHECK (((provider_type <> 'oidc'::text) OR (discovery_url IS NULL) OR (discovery_url ~~ 'https://%'::text)))
);

-- case: supabase-auth/saml_providers.metadata_url_not_empty
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE saml_providers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  metadata_url text,
  CONSTRAINT "metadata_url not empty" CHECK (((metadata_url = NULL::text) OR (char_length(metadata_url) > 0)))
);

-- case: supabase-auth/oauth_consents.oauth_consents_revoked_after_granted
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE oauth_consents (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  granted_at timestamp with time zone NOT NULL DEFAULT now(),
  revoked_at timestamp with time zone,
  CONSTRAINT oauth_consents_revoked_after_granted CHECK (((revoked_at IS NULL) OR (revoked_at >= granted_at)))
);

-- case: supabase-auth/custom_oauth_providers.custom_oauth_providers_client_id_length
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE custom_oauth_providers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  client_id text NOT NULL,
  CONSTRAINT custom_oauth_providers_client_id_length CHECK (((char_length(client_id) >= 1) AND (char_length(client_id) <= 512)))
);

-- case: supabase-auth/oauth_clients.oauth_clients_token_endpoint_auth_method_check
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE oauth_clients (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  token_endpoint_auth_method text NOT NULL,
  CONSTRAINT oauth_clients_token_endpoint_auth_method_check CHECK ((token_endpoint_auth_method = ANY (ARRAY['client_secret_basic'::text, 'client_secret_post'::text, 'none'::text])))
);

-- case: supabase-auth/custom_oauth_providers.custom_oauth_providers_identifier_format
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE custom_oauth_providers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  identifier text NOT NULL,
  CONSTRAINT custom_oauth_providers_identifier_format CHECK ((identifier ~ '^[a-z0-9][a-z0-9:-]{0,48}[a-z0-9]$'::text))
);

-- case: supabase-auth/custom_oauth_providers.custom_oauth_providers_oauth2_requires_endpoints
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE custom_oauth_providers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  provider_type text NOT NULL,
  authorization_url text,
  token_url text,
  userinfo_url text,
  CONSTRAINT custom_oauth_providers_oauth2_requires_endpoints CHECK (((provider_type <> 'oauth2'::text) OR ((authorization_url IS NOT NULL) AND (token_url IS NOT NULL) AND (userinfo_url IS NOT NULL))))
);

-- case: supabase-auth/custom_oauth_providers.custom_oauth_providers_oidc_requires_issuer
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE custom_oauth_providers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  provider_type text NOT NULL,
  issuer text,
  CONSTRAINT custom_oauth_providers_oidc_requires_issuer CHECK (((provider_type <> 'oidc'::text) OR (issuer IS NOT NULL)))
);

-- case: supabase-auth/users.users_email_change_confirm_status_check
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE users (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  email_change_confirm_status smallint DEFAULT 0,
  CONSTRAINT users_email_change_confirm_status_check CHECK (((email_change_confirm_status >= 0) AND (email_change_confirm_status <= 2)))
);

-- case: supabase-auth/custom_oauth_providers.custom_oauth_providers_issuer_length
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE custom_oauth_providers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  issuer text,
  CONSTRAINT custom_oauth_providers_issuer_length CHECK (((issuer IS NULL) OR ((char_length(issuer) >= 1) AND (char_length(issuer) <= 2048))))
);

-- case: supabase-auth/scim_tokens.scim_tokens_expires_at_future
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE scim_tokens (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  expires_at timestamp with time zone,
  CONSTRAINT scim_tokens_expires_at_future CHECK (((expires_at IS NULL) OR (expires_at > created_at)))
);

-- case: supabase-auth/custom_oauth_providers.custom_oauth_providers_provider_type_check
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE custom_oauth_providers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  provider_type text NOT NULL,
  CONSTRAINT custom_oauth_providers_provider_type_check CHECK ((provider_type = ANY (ARRAY['oauth2'::text, 'oidc'::text])))
);

-- case: supabase-auth/mfa_recovery_code_sets.mfa_recovery_code_sets_failed_verification_count_check
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE mfa_recovery_code_sets (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  failed_verification_count integer NOT NULL DEFAULT 0,
  CONSTRAINT mfa_recovery_code_sets_failed_verification_count_check CHECK ((failed_verification_count >= 0))
);

-- case: supabase-auth/oauth_authorizations.oauth_authorizations_expires_at_future
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE oauth_authorizations (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  expires_at timestamp with time zone NOT NULL DEFAULT (now() + '00:03:00'::interval),
  CONSTRAINT oauth_authorizations_expires_at_future CHECK ((expires_at > created_at))
);

-- case: supabase-auth/oauth_consents.oauth_consents_scopes_not_empty
-- source: https://github.com/supabase/auth/tree/83865c14a595aa616af30549ddf4477de0a0d732/migrations
-- license: MIT
CREATE TABLE oauth_consents (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  scopes text NOT NULL,
  CONSTRAINT oauth_consents_scopes_not_empty CHECK ((char_length(TRIM(BOTH FROM scopes)) > 0))
);
