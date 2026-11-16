-- HomeKeep: home asset register
-- Run this in the Supabase SQL editor (or `supabase db push`) once per project.
-- Every table is owned per user and protected by row level security.

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------------------
-- Assets
-- ---------------------------------------------------------------------------
create table if not exists public.hk_assets (
    id                  uuid primary key default gen_random_uuid(),
    user_id             uuid not null default auth.uid() references auth.users (id) on delete cascade,
    name                text not null,
    category            text not null default 'Other',
    brand               text,
    model               text,
    serial_number       text,
    location            text,
    purchase_date       date,
    purchase_price      numeric(12, 2),
    currency            text default 'CAD',
    retailer            text,
    warranty_expires_on date,
    notes               text,
    photo_path          text,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

create index if not exists hk_assets_user_idx on public.hk_assets (user_id, created_at desc);
create index if not exists hk_assets_warranty_idx on public.hk_assets (user_id, warranty_expires_on);
create index if not exists hk_assets_serial_idx on public.hk_assets (user_id, serial_number);

-- ---------------------------------------------------------------------------
-- Receipts, warranties, manuals and photos (files live in Supabase Storage)
-- ---------------------------------------------------------------------------
create table if not exists public.hk_attachments (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null default auth.uid() references auth.users (id) on delete cascade,
    asset_id    uuid not null references public.hk_assets (id) on delete cascade,
    kind        text not null default 'receipt',
    file_path   text not null,
    file_name   text,
    mime_type   text,
    byte_size   bigint,
    created_at  timestamptz not null default now()
);

create index if not exists hk_attachments_asset_idx on public.hk_attachments (asset_id, created_at desc);
create index if not exists hk_attachments_user_idx on public.hk_attachments (user_id);

-- ---------------------------------------------------------------------------
-- Repair / service history
-- ---------------------------------------------------------------------------
create table if not exists public.hk_repairs (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid not null default auth.uid() references auth.users (id) on delete cascade,
    asset_id     uuid not null references public.hk_assets (id) on delete cascade,
    description  text not null,
    vendor       text,
    cost         numeric(12, 2),
    performed_on date not null default current_date,
    notes        text,
    created_at   timestamptz not null default now()
);

create index if not exists hk_repairs_asset_idx on public.hk_repairs (asset_id, performed_on desc);
create index if not exists hk_repairs_user_idx on public.hk_repairs (user_id);

-- ---------------------------------------------------------------------------
-- Maintenance reminders
-- ---------------------------------------------------------------------------
create table if not exists public.hk_reminders (
    id             uuid primary key default gen_random_uuid(),
    user_id        uuid not null default auth.uid() references auth.users (id) on delete cascade,
    asset_id       uuid not null references public.hk_assets (id) on delete cascade,
    title          text not null,
    due_on         date not null,
    repeat_months  integer check (repeat_months is null or repeat_months between 1 and 120),
    notes          text,
    completed_on   date,
    created_at     timestamptz not null default now()
);

create index if not exists hk_reminders_due_idx on public.hk_reminders (user_id, due_on);
create index if not exists hk_reminders_asset_idx on public.hk_reminders (asset_id);

-- ---------------------------------------------------------------------------
-- updated_at maintenance
-- ---------------------------------------------------------------------------
create or replace function public.hk_touch_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists hk_assets_touch_updated_at on public.hk_assets;
create trigger hk_assets_touch_updated_at
    before update on public.hk_assets
    for each row execute function public.hk_touch_updated_at();

-- ---------------------------------------------------------------------------
-- Row level security: every row belongs to auth.uid()
-- ---------------------------------------------------------------------------
alter table public.hk_assets      enable row level security;
alter table public.hk_attachments enable row level security;
alter table public.hk_repairs     enable row level security;
alter table public.hk_reminders   enable row level security;

drop policy if exists "hk_assets_select_own" on public.hk_assets;
create policy "hk_assets_select_own" on public.hk_assets
    for select to authenticated using (user_id = (select auth.uid()));

drop policy if exists "hk_assets_insert_own" on public.hk_assets;
create policy "hk_assets_insert_own" on public.hk_assets
    for insert to authenticated with check (user_id = (select auth.uid()));

drop policy if exists "hk_assets_update_own" on public.hk_assets;
create policy "hk_assets_update_own" on public.hk_assets
    for update to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

drop policy if exists "hk_assets_delete_own" on public.hk_assets;
create policy "hk_assets_delete_own" on public.hk_assets
    for delete to authenticated using (user_id = (select auth.uid()));

drop policy if exists "hk_attachments_select_own" on public.hk_attachments;
create policy "hk_attachments_select_own" on public.hk_attachments
    for select to authenticated using (user_id = (select auth.uid()));

drop policy if exists "hk_attachments_insert_own" on public.hk_attachments;
create policy "hk_attachments_insert_own" on public.hk_attachments
    for insert to authenticated with check (user_id = (select auth.uid()));

drop policy if exists "hk_attachments_update_own" on public.hk_attachments;
create policy "hk_attachments_update_own" on public.hk_attachments
    for update to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

drop policy if exists "hk_attachments_delete_own" on public.hk_attachments;
create policy "hk_attachments_delete_own" on public.hk_attachments
    for delete to authenticated using (user_id = (select auth.uid()));

drop policy if exists "hk_repairs_select_own" on public.hk_repairs;
create policy "hk_repairs_select_own" on public.hk_repairs
    for select to authenticated using (user_id = (select auth.uid()));

drop policy if exists "hk_repairs_insert_own" on public.hk_repairs;
create policy "hk_repairs_insert_own" on public.hk_repairs
    for insert to authenticated with check (user_id = (select auth.uid()));

drop policy if exists "hk_repairs_update_own" on public.hk_repairs;
create policy "hk_repairs_update_own" on public.hk_repairs
    for update to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

drop policy if exists "hk_repairs_delete_own" on public.hk_repairs;
create policy "hk_repairs_delete_own" on public.hk_repairs
    for delete to authenticated using (user_id = (select auth.uid()));

drop policy if exists "hk_reminders_select_own" on public.hk_reminders;
create policy "hk_reminders_select_own" on public.hk_reminders
    for select to authenticated using (user_id = (select auth.uid()));

drop policy if exists "hk_reminders_insert_own" on public.hk_reminders;
create policy "hk_reminders_insert_own" on public.hk_reminders
    for insert to authenticated with check (user_id = (select auth.uid()));

drop policy if exists "hk_reminders_update_own" on public.hk_reminders;
create policy "hk_reminders_update_own" on public.hk_reminders
    for update to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

drop policy if exists "hk_reminders_delete_own" on public.hk_reminders;
create policy "hk_reminders_delete_own" on public.hk_reminders
    for delete to authenticated using (user_id = (select auth.uid()));

-- ---------------------------------------------------------------------------
-- Storage: private "homekeep" bucket, one folder per user
-- ---------------------------------------------------------------------------
insert into storage.buckets (id, name, public, file_size_limit)
values ('homekeep', 'homekeep', false, 10485760)
on conflict (id) do update
    set public = false,
        file_size_limit = 10485760;

drop policy if exists "homekeep_objects_select_own" on storage.objects;
create policy "homekeep_objects_select_own" on storage.objects
    for select to authenticated
    using (
        bucket_id = 'homekeep'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

drop policy if exists "homekeep_objects_insert_own" on storage.objects;
create policy "homekeep_objects_insert_own" on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'homekeep'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

drop policy if exists "homekeep_objects_update_own" on storage.objects;
create policy "homekeep_objects_update_own" on storage.objects
    for update to authenticated
    using (
        bucket_id = 'homekeep'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    )
    with check (
        bucket_id = 'homekeep'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

drop policy if exists "homekeep_objects_delete_own" on storage.objects;
create policy "homekeep_objects_delete_own" on storage.objects
    for delete to authenticated
    using (
        bucket_id = 'homekeep'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

-- ---------------------------------------------------------------------------
-- Account deletion (required by Google Play for apps with accounts).
-- Deletes the caller's files, rows and auth user. SECURITY DEFINER because
-- only the auth admin can remove an auth.users row.
-- ---------------------------------------------------------------------------
create or replace function public.hk_delete_account()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    uid uuid := auth.uid();
begin
    if uid is null then
        raise exception 'not authenticated';
    end if;

    delete from storage.objects
     where bucket_id = 'homekeep'
       and (storage.foldername(name))[1] = uid::text;

    delete from public.hk_assets where user_id = uid;

    delete from auth.users where id = uid;
end;
$$;

revoke all on function public.hk_delete_account() from public;
revoke all on function public.hk_delete_account() from anon;
grant execute on function public.hk_delete_account() to authenticated;
