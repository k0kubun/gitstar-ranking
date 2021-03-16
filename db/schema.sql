CREATE TABLE access_tokens (
    "id" bigserial NOT NULL,
    "user_id" bigint NOT NULL,
    "token" text NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "enabled" boolean,
    PRIMARY KEY ("id")
);
CREATE INDEX index_access_tokens_on_enabled ON access_tokens (enabled);
CREATE UNIQUE INDEX index_access_tokens_on_user_id ON access_tokens (user_id);

CREATE TABLE last_updates (
    "id" integer NOT NULL,
    "cursor" bigint NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    PRIMARY KEY ("id")
);

CREATE TABLE organization_ranks (
    "id" bigserial NOT NULL,
    "stargazers_count" bigint NOT NULL,
    "rank" bigint NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX index_organization_ranks_on_rank ON organization_ranks (rank);
CREATE UNIQUE INDEX index_organization_ranks_on_stargazers_count ON organization_ranks (stargazers_count);

CREATE TABLE repositories (
    "id" bigint NOT NULL,
    "name" text NOT NULL,
    "full_name" citext NOT NULL,
    "owner_id" bigint NOT NULL,
    "description" text,
    "fork" boolean,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "homepage" text,
    "stargazers_count" bigint NOT NULL DEFAULT 0,
    "language" text,
    "fetched_at" timestamp with time zone,
    PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX index_repositories_on_full_name ON repositories (full_name);
CREATE INDEX index_repositories_on_owner_id_and_stargazers_count ON repositories (owner_id, stargazers_count);
CREATE INDEX index_repositories_on_stargazers_count ON repositories (stargazers_count);

CREATE TABLE repository_ranks (
    "id" bigserial NOT NULL,
    "stargazers_count" bigint NOT NULL,
    "rank" bigint NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX index_repository_ranks_on_rank ON repository_ranks (rank);
CREATE UNIQUE INDEX index_repository_ranks_on_stargazers_count ON repository_ranks (stargazers_count);

CREATE TABLE user_update_jobs (
    "id" bigserial NOT NULL,
    "payload" text NOT NULL,
    "timeout_at" timestamp with time zone,
    "owner" bigint,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    PRIMARY KEY ("id")
);
CREATE INDEX index_user_update_jobs_on_timeout_at ON user_update_jobs (timeout_at);

CREATE TABLE user_ranks (
    "id" bigserial NOT NULL,
    "stargazers_count" bigint NOT NULL,
    "rank" bigint NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX index_user_ranks_on_rank ON user_ranks (rank);
CREATE UNIQUE INDEX index_user_ranks_on_stargazers_count ON user_ranks (stargazers_count);

CREATE TABLE users (
    "id" bigint NOT NULL,
    "login" citext NOT NULL,
    "avatar_url" text,
    "type" text NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "queued_at" timestamp with time zone,
    "stargazers_count" bigint NOT NULL DEFAULT 0,
    "location" text,
    PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX index_users_on_login ON users (login);
CREATE INDEX index_users_on_stargazers_count ON users (stargazers_count);
