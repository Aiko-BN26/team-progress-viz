create table if not exists "user" (
    id bigserial primary key,
    github_id bigint not null unique,
    login varchar(255) not null,
    name varchar(255),
    avatar_url varchar(512),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone
);

create table if not exists organization (
    id bigserial primary key,
    github_id bigint not null unique,
    login varchar(255) not null,
    name varchar(255),
    description text,
    avatar_url varchar(512),
    html_url varchar(512),
    default_link_url varchar(512),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone
);

create table if not exists repository (
    id bigserial primary key,
    github_id bigint not null unique,
    organization_id bigint not null,
    owner_login varchar(255),
    name varchar(255) not null,
    full_name varchar(255),
    description text,
    html_url varchar(512),
    language varchar(128),
    stargazers_count integer,
    forks_count integer,
    default_branch varchar(255),
    is_private boolean not null default false,
    archived boolean not null default false,
    size integer,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    constraint fk_repository_organization foreign key (organization_id) references organization (id)
);

create table if not exists git_commit (
    id bigserial primary key,
    repository_id bigint not null,
    sha varchar(255) not null,
    message text,
    html_url varchar(512),
    author_name varchar(255),
    author_email varchar(255),
    committer_name varchar(255),
    committer_email varchar(255),
    committed_at timestamp with time zone,
    pushed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    constraint fk_git_commit_repository foreign key (repository_id) references repository (id)
);

create table if not exists commit_file (
    id bigserial primary key,
    commit_id bigint not null,
    path text not null,
    filename varchar(255),
    extension varchar(64),
    status varchar(64),
    additions integer,
    deletions integer,
    changes integer,
    raw_blob_url varchar(512),
    created_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    constraint fk_commit_file_commit foreign key (commit_id) references git_commit (id)
);

create table if not exists pull_request (
    id bigserial primary key,
    number integer not null,
    repository_id bigint not null,
    github_id bigint,
    title varchar(255),
    body text,
    state varchar(64),
    merged boolean,
    author_user_id bigint,
    merged_by_user_id bigint,
    html_url varchar(512),
    additions integer,
    deletions integer,
    changed_files integer,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    closed_at timestamp with time zone,
    merged_at timestamp with time zone,
    deleted_at timestamp with time zone,
    constraint fk_pull_request_repository foreign key (repository_id) references repository (id),
    constraint fk_pull_request_author foreign key (author_user_id) references "user" (id),
    constraint fk_pull_request_merged_by foreign key (merged_by_user_id) references "user" (id)
);

create table if not exists pull_request_file (
    id bigserial primary key,
    pull_request_id bigint not null,
    path text not null,
    extension varchar(64),
    additions integer,
    deletions integer,
    changes integer,
    raw_blob_url varchar(512),
    created_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    constraint fk_pull_request_file_pull_request foreign key (pull_request_id) references pull_request (id)
);

create table if not exists activity_daily (
    id bigserial primary key,
    organization_id bigint not null,
    user_id bigint not null,
    date date not null,
    commit_count integer,
    files_changed integer,
    additions integer,
    deletions integer,
    available_minutes integer,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    constraint fk_activity_daily_organization foreign key (organization_id) references organization (id),
    constraint fk_activity_daily_user foreign key (user_id) references "user" (id)
);

create table if not exists daily_status (
    id bigserial primary key,
    user_id bigint not null,
    organization_id bigint not null,
    date date not null,
    available_minutes integer,
    status_type varchar(64),
    status_message text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    constraint fk_daily_status_user foreign key (user_id) references "user" (id),
    constraint fk_daily_status_organization foreign key (organization_id) references organization (id)
);

create table if not exists comment (
    id bigserial primary key,
    user_id bigint not null,
    organization_id bigint not null,
    target_type varchar(128) not null,
    target_id bigint,
    parent_comment_id bigint,
    content text not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    constraint fk_comment_user foreign key (user_id) references "user" (id),
    constraint fk_comment_organization foreign key (organization_id) references organization (id),
    constraint fk_comment_parent foreign key (parent_comment_id) references comment (id)
);

create table if not exists user_organization (
    id bigserial primary key,
    user_id bigint not null,
    organization_id bigint not null,
    role varchar(64),
    joined_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    constraint fk_user_organization_user foreign key (user_id) references "user" (id),
    constraint fk_user_organization_organization foreign key (organization_id) references organization (id)
);

create table if not exists repository_sync_status (
    id bigserial primary key,
    repository_id bigint not null,
    last_synced_at timestamp with time zone,
    last_synced_commit_sha varchar(255),
    error_message text,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    constraint fk_repository_sync_status_repository foreign key (repository_id) references repository (id)
);

create table if not exists webhook_event (
    id bigserial primary key,
    event_type varchar(128),
    delivery_id varchar(255),
    signature varchar(255),
    payload text,
    received_at timestamp with time zone not null,
    processed_at timestamp with time zone,
    status varchar(64) default 'pending',
    error_message text,
    deleted_at timestamp with time zone
);

create index if not exists idx_repository_organization on repository (organization_id);
create index if not exists idx_git_commit_repository on git_commit (repository_id);
create index if not exists idx_commit_file_commit on commit_file (commit_id);
create index if not exists idx_pull_request_repository on pull_request (repository_id);
create index if not exists idx_pull_request_author on pull_request (author_user_id);
create index if not exists idx_pull_request_file_pull_request on pull_request_file (pull_request_id);
create index if not exists idx_activity_daily_organization on activity_daily (organization_id);
create index if not exists idx_activity_daily_user on activity_daily (user_id);
create index if not exists idx_daily_status_user on daily_status (user_id);
create index if not exists idx_daily_status_organization on daily_status (organization_id);
create index if not exists idx_comment_organization on comment (organization_id);
create index if not exists idx_comment_user on comment (user_id);
create index if not exists idx_comment_parent on comment (parent_comment_id);
create index if not exists idx_user_organization_user on user_organization (user_id);
create index if not exists idx_user_organization_org on user_organization (organization_id);
create index if not exists idx_repository_sync_status_repository on repository_sync_status (repository_id);
