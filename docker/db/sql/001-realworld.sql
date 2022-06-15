CREATE TABLE public.article_comments (
    id serial NOT NULL,
    author_id bigint NOT NULL,
    article_id bigint NOT NULL,
    body text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);
CREATE TABLE public.article_tags (
    id serial NOT NULL,
    article_id bigint NOT NULL,
    tag_id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL
);
CREATE TABLE public.articles (
    id serial NOT NULL,
    author_id bigint NOT NULL,
    title character varying NOT NULL,
    slug character varying NOT NULL,
    body text NOT NULL,
    description character varying NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);
CREATE TABLE public.favorites (
    id serial NOT NULL,
    user_id bigint NOT NULL,
    article_id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL
);
CREATE TABLE public.followings (
    id serial NOT NULL,
    following_id bigint NOT NULL,
    follower_id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL
);
CREATE TABLE public.profiles (
    id serial NOT NULL,
    user_id bigint NOT NULL,
    bio text NOT NULL,
    image text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);
CREATE TABLE public.tags (
    id serial NOT NULL,
    name character varying NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);
CREATE TABLE public.users (
    id serial NOT NULL,
    email character varying NOT NULL,
    username text NOT NULL,
    password text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);

-- PK
ALTER TABLE ONLY public.article_comments
    ADD CONSTRAINT article_comments_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.article_tags
    ADD CONSTRAINT article_tags_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.articles
    ADD CONSTRAINT articles_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT favorites_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.followings
    ADD CONSTRAINT followings_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.profiles
    ADD CONSTRAINT profiles_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.tags
    ADD CONSTRAINT tags_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

-- Index
CREATE UNIQUE INDEX article_tags_index ON public.article_tags USING btree (article_id, tag_id);
CREATE UNIQUE INDEX favorites_index ON public.favorites USING btree (user_id, article_id);
CREATE UNIQUE INDEX followings_index ON public.followings USING btree (following_id, follower_id);
CREATE UNIQUE INDEX index_articles_on_slug ON public.articles USING btree (slug);
CREATE UNIQUE INDEX index_users_on_email ON public.users USING btree (email);
CREATE UNIQUE INDEX index_users_on_username ON public.users USING btree (username);

-- Relation
ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT fk_00f2e522fe FOREIGN KEY (article_id) REFERENCES public.articles(id);
ALTER TABLE ONLY public.followings
    ADD CONSTRAINT fk_1668ccdb36 FOREIGN KEY (follower_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.article_comments
    ADD CONSTRAINT fk_33ea79615a FOREIGN KEY (author_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.followings
    ADD CONSTRAINT fk_5371baeb2d FOREIGN KEY (following_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.article_tags
    ADD CONSTRAINT fk_646e8d3122 FOREIGN KEY (article_id) REFERENCES public.articles(id);
ALTER TABLE ONLY public.article_comments
    ADD CONSTRAINT fk_67982717fa FOREIGN KEY (article_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.article_tags
    ADD CONSTRAINT fk_b651172c61 FOREIGN KEY (tag_id) REFERENCES public.tags(id);
ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT fk_d15744e438 FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.profiles
    ADD CONSTRAINT fk_e424190865 FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.articles
    ADD CONSTRAINT fk_e74ce85cbc FOREIGN KEY (author_id) REFERENCES public.users(id);
