CREATE TABLE public.access_tokens (
    "id" bigserial NOT NULL,
    "user_id" bigint,
    "token" text,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "enabled" boolean,
    PRIMARY KEY ("id")
);
CREATE INDEX index_access_tokens_on_enabled ON public.access_tokens USING btree (enabled);
CREATE INDEX index_access_tokens_on_user_id ON public.access_tokens USING btree (user_id);

CREATE TABLE public.last_updates (
    "id" integer NOT NULL,
    "cursor" bigint NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    PRIMARY KEY ("id")
);

CREATE TABLE public.organization_ranks (
    "id" bigserial NOT NULL,
    "stargazers_count" bigint,
    "rank" bigint,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX index_organization_ranks_on_rank ON public.organization_ranks USING btree (rank);
CREATE UNIQUE INDEX index_organization_ranks_on_stargazers_count ON public.organization_ranks USING btree (stargazers_count);

CREATE TABLE public.repositories (
    "id" bigserial NOT NULL,
    "name" text,
    "full_name" citext,
    "owner_id" bigint,
    "author_id" bigint,
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
CREATE INDEX index_repositories_on_author_id_and_stargazers_count ON public.repositories USING btree (author_id, stargazers_count);
CREATE INDEX index_repositories_on_full_name ON public.repositories USING btree (full_name);
CREATE INDEX index_repositories_on_owner_id_and_stargazers_count ON public.repositories USING btree (owner_id, stargazers_count);
CREATE INDEX index_repositories_on_stargazers_count ON public.repositories USING btree (stargazers_count);

CREATE TABLE public.repository_ranks (
    "id" bigserial NOT NULL,
    "stargazers_count" bigint,
    "rank" bigint,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX index_repository_ranks_on_rank ON public.repository_ranks USING btree (rank);
CREATE UNIQUE INDEX index_repository_ranks_on_stargazers_count ON public.repository_ranks USING btree (stargazers_count);

CREATE TABLE public.update_user_jobs (
    "id" bigserial NOT NULL,
    "payload" text,
    "timeout_at" timestamp with time zone,
    "owner" bigint,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    PRIMARY KEY ("id")
);
CREATE INDEX index_update_user_jobs_on_timeout_at ON public.update_user_jobs USING btree (timeout_at);

CREATE TABLE public.user_ranks (
    "id" bigserial NOT NULL,
    "stargazers_count" bigint,
    "rank" bigint,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX index_user_ranks_on_rank ON public.user_ranks USING btree (rank);
CREATE UNIQUE INDEX index_user_ranks_on_stargazers_count ON public.user_ranks USING btree (stargazers_count);

CREATE TABLE public.users (
    "id" bigserial NOT NULL,
    "login" citext,
    "avatar_url" text,
    "type" text,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "queued_at" timestamp with time zone,
    "stargazers_count" bigint NOT NULL DEFAULT 0,
    "location" text,
    PRIMARY KEY ("id")
);
CREATE INDEX index_users_on_login ON public.users USING btree (login);
CREATE INDEX index_users_on_stargazers_count ON public.users USING btree (stargazers_count);
