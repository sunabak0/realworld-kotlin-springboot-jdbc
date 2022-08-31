--
-- PostgreSQL database dump
--

-- Dumped from database version 14.4 (Debian 14.4-1.pgdg110+1)
-- Dumped by pg_dump version 14.4 (Debian 14.4-1.pgdg110+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: article_comments; Type: TABLE DATA; Schema: public; Owner: realworld-user
--

INSERT INTO public.article_comments VALUES (1, 3, 1, 'dummy-comment-body-01', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.article_comments VALUES (2, 1, 3, 'dummy-comment-body-02', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.article_comments VALUES (3, 2, 1, 'dummy-comment-body-03', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.article_comments VALUES (4, 3, 3, 'dummy-comment-body-04', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.article_comments VALUES (5, 3, 1, 'dummy-comment-body-02', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');


--
-- Data for Name: article_tags; Type: TABLE DATA; Schema: public; Owner: realworld-user
--

INSERT INTO public.article_tags VALUES (1, 1, 1, '2022-08-31 09:18:14+00');
INSERT INTO public.article_tags VALUES (2, 1, 2, '2022-08-31 09:18:14+00');
INSERT INTO public.article_tags VALUES (3, 1, 3, '2022-08-31 09:18:14+00');
INSERT INTO public.article_tags VALUES (4, 2, 3, '2022-08-31 09:18:14+00');


--
-- Data for Name: articles; Type: TABLE DATA; Schema: public; Owner: realworld-user
--

INSERT INTO public.articles VALUES (1, 1, 'Rust vs Scala vs Kotlin', 'rust-vs-scala-vs-kotlin', 'dummy-body', 'dummy-description', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.articles VALUES (2, 1, 'Functional programming kotlin', 'functional-programming-kotlin', 'dummy-body', 'dummy-description', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.articles VALUES (3, 2, 'TDD(Type Driven Development)', 'tdd-type-driven-development', 'dummy-body', 'dummy-description', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');


--
-- Data for Name: favorites; Type: TABLE DATA; Schema: public; Owner: realworld-user
--

INSERT INTO public.favorites VALUES (1, 2, 1, '2022-08-31 09:18:14+00');
INSERT INTO public.favorites VALUES (2, 3, 2, '2022-08-31 09:18:14+00');
INSERT INTO public.favorites VALUES (3, 1, 3, '2022-08-31 09:18:14+00');
INSERT INTO public.favorites VALUES (4, 2, 3, '2022-08-31 09:18:14+00');


--
-- Data for Name: followings; Type: TABLE DATA; Schema: public; Owner: realworld-user
--

INSERT INTO public.followings VALUES (1, 1, 2, '2022-08-31 09:18:14+00');
INSERT INTO public.followings VALUES (2, 1, 3, '2022-08-31 09:18:14+00');
INSERT INTO public.followings VALUES (3, 2, 3, '2022-08-31 09:18:14+00');


--
-- Data for Name: profiles; Type: TABLE DATA; Schema: public; Owner: realworld-user
--

INSERT INTO public.profiles VALUES (1, 1, 'Lisper', '', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.profiles VALUES (2, 2, 'Rubyを作った', '', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.profiles VALUES (3, 3, 'Rustを作った', '', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');


--
-- Data for Name: tags; Type: TABLE DATA; Schema: public; Owner: realworld-user
--

INSERT INTO public.tags VALUES (1, 'rust', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.tags VALUES (2, 'scala', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.tags VALUES (3, 'kotlin', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.tags VALUES (4, 'ocaml', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.tags VALUES (5, 'elixir', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: realworld-user
--

INSERT INTO public.users VALUES (1, 'paul-graham@example.com', 'paul-graham', 'Passw0rd', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.users VALUES (2, 'matz@example.com', '松本行弘', 'Passw0rd', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');
INSERT INTO public.users VALUES (3, 'graydon-hoare@example.com', 'graydon-hoare', 'Passw0rd', '2022-08-31 09:18:14+00', '2022-08-31 09:18:14+00');


--
-- Name: article_comments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: realworld-user
--

SELECT pg_catalog.setval('public.article_comments_id_seq', 10000, true);


--
-- Name: article_tags_id_seq; Type: SEQUENCE SET; Schema: public; Owner: realworld-user
--

SELECT pg_catalog.setval('public.article_tags_id_seq', 10000, true);


--
-- Name: articles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: realworld-user
--

SELECT pg_catalog.setval('public.articles_id_seq', 1, false);


--
-- Name: favorites_id_seq; Type: SEQUENCE SET; Schema: public; Owner: realworld-user
--

SELECT pg_catalog.setval('public.favorites_id_seq', 10000, true);


--
-- Name: followings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: realworld-user
--

SELECT pg_catalog.setval('public.followings_id_seq', 10000, true);


--
-- Name: profiles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: realworld-user
--

SELECT pg_catalog.setval('public.profiles_id_seq', 10000, true);


--
-- Name: tags_id_seq; Type: SEQUENCE SET; Schema: public; Owner: realworld-user
--

SELECT pg_catalog.setval('public.tags_id_seq', 10000, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: realworld-user
--

SELECT pg_catalog.setval('public.users_id_seq', 10000, true);


--
-- PostgreSQL database dump complete
--

