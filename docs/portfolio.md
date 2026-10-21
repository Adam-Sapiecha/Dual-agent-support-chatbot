# Opis do portfolio

## PL

Dual-Agent Support System — aplikacja Java dzieląca obsługę rozmowy pomiędzy
specjalistę technicznego i rozliczeniowego. Publiczne demo pokazuje trasę
wiadomości, cytowane fragmenty dokumentacji i wieloturowe zbieranie danych
do symulowanego zgłoszenia zwrotu. Ma REST API, izolowane sesje, reset,
walidację i limity zasobów. Działa w Dockerze na Raspberry Pi.

Demo używa reguł i danych przykładowych, bez generacji LLM i prawdziwych
operacji finansowych. Osobny CLI zawiera integrację OpenAI i wyszukiwanie
wektorowe. Naprawy tej integracji mają testy lokalne; live API nie było
sprawdzane podczas audytu. Projekt pierwotnie był zadaniem rekrutacyjnym;
warstwa WWW, zabezpieczenia demo i testy dodano podczas późniejszej rozbudowy.
Szczegóły osobistego wkładu historycznego wymagają potwierdzenia autora.

## EN

Dual-Agent Support System is a Java application that separates technical and
billing conversations. The public demo exposes routing decisions, cited
documentation excerpts and a multi-turn simulated refund workflow. It has a
REST API, isolated sessions, reset, validation and resource limits, deployed
with Docker on a Raspberry Pi.

The demo uses rules and sample data, without LLM generation or real financial
actions. A separate CLI retains OpenAI and vector retrieval integration.
Integration fixes have local regression tests; live API calls were not verified
during this audit. Originally a recruitment task, the project later gained the
web demo, public-use safeguards and regression suite. Historical personal
contributions should be confirmed by the author.
