# Audyt projektu — 21.10.2026

## Werdykt i zakres

Gotowy jako publiczne demo portfolio w trybie **sample**. Wdrożenie i publiczny
przepływ HTTP zostały sprawdzone; szczegóły w `deployment.md`.
To demonstracja routingu, źródeł i walidowanych narzędzi w Javie, nie publiczny
chat z modelem językowym. Integracja OpenAI pozostaje w osobnym trybie CLI.
Nie wykonano płatnych wywołań OpenAI ani integracji z rzeczywistym billingiem.

## Stan początkowy

Atuty: rozdzielone klasy routera, rozmowy, specjalistów i narzędzi; lokalna
dokumentacja; Maven Wrapper i szkielet Dockera. Problem biznesowy jest czytelny:
jedna rozmowa rozdziela wsparcie integracji API i rozliczeń.

| Priorytet | Potwierdzone ustalenie | Zmiana i dowód |
|---|---|---|
| P1 | BillingAgent dodawał odpowiedź `tool` bez poprzedzającego `assistant.tool_calls`; API odrzuca taki przebieg | Dodano wiadomość wywołania i serializację; test `toolResultHasPrecedingAssistantCall` |
| P1 | Projekt miał tylko CLI; Spring uruchamiał pusty kontekst | Dodano REST, interfejs, tryb sample bez sekretów, kontener WWW; testy HTTP |
| P1 | BillingTools przyjmował pusty email/powód, ignorował datę i sugerował wysłanie formularza | Walidacja argumentów, polityki 14 dni i dat; statusy SIMULATED i SIMULATED_NOT_SENT; regresje narzędzi i przebiegu |
| P2 | Cache embeddingów po ID wykorzystywał stary wektor po zmianie tekstu | Porównanie treści, odbudowa uszkodzonego cache, zamknięcie listowania plików; test cache |
| P2 | Main dodawał bieżące pytanie do historii, a agent dodawał je ponownie | Zapis pary wiadomości po odpowiedzi |
| P2 | Cyfry po billing kierowały np. HTTP 401 do rozliczeń; krótka wiadomość po TECH uniemożliwiała zmianę tematu | Usunięto ogólną regułę cyfr, zawężono krótkie kontynuacje i wykluczono ceny planów |
| P2 | Router akceptował nieznane wartości lub null z JSON | Walidacja dozwolonych tras, bezpieczny fallback; regresje |
| P2 | Wywołania HTTP bez ograniczeń czasu, surowe odpowiedzi usług w błędach | Timeouty i ograniczenie ujawnianych błędów |
| P2 | Dwie wersje Jackson Databind w POM, stare zależności bazowe | Jedna zależność zarządzana BOM; Spring Boot 4.0.8, Tomcat 11.0.24 |
| P2 | Brak granic pamięci i izolacji publicznych rozmów | Losowe sesje, TTL 30 min + czyszczenie co minutę, 300 sesji, 30 tur, 120 wiadomości/min globalnie; regresje |
| P3 | Cytowania wymagały ID sekcji, lecz prompt dostawał tylko nazwę pliku | Prompt otrzymuje pełne ID sekcji; sample zwraca rzeczywiste fragmenty |
| P3 | README wskazywał niekompletne uruchomienie przez classpath i miał uszkodzone bloki kodu | Nowe instrukcje JAR, CLI, Compose i deploymentu |

## Weryfikacja

- Początkowo przechodził wyłącznie `contextLoads`; nie testował głównej logiki.
- Obecnie 19 testów: 11 scenariuszy demo, 5 regresji rdzenia, 2 testy HTTP
  i 1 test kontekstu. `mvnw.cmd clean verify` na Javie 21; obraz buduje i
  uruchamia ten sam zestaw na Javie 17 ARM64.
- HTTP: strona, health, chat, nagłówki, zły JSON, puste wejście, limit body,
  nieznana sesja. Regresje obejmują przełączenie specjalisty po zwrocie,
  brak odpowiedzi w dokumentach, osobne sesje, reset i brak duplikowania spraw.
- JavaScript sprawdzony przez `node --check`; wynik kontroli wdrożenia oraz
  interfejsu zapisany w `docs/deployment.md`.

## Decyzje z tej rozbudowy

1. Publiczny adapter nie ma dostępu do OpenAI ani zewnętrznego billingu:
   nie tworzy kosztów i pozwala powtórzyć scenariusz bez klucza. Nie imituje
   działania LLM — wprost opisuje reguły i cytaty.
2. Session state przechowuje tylko pola procesu, bez trwałej historii.
   To celowy kompromis demo; restart usuwa dane. Reset unieważnia identyfikator.
3. Dokumentacja pochodzi z czterech jawnie dozwolonych plików. Nowe raporty
   audytu i wdrożenia nie trafiają do źródeł odpowiedzi.

## Świadome ograniczenia

- Publiczny router używa słów kluczowych, głównie angielskich; nie rozumie
  dowolnego języka i prosi o rozdzielenie mieszanego pytania.
- Wyszukiwanie demo to scoring słów i nagłówków, nie semantyczny RAG.
  CLI nadal używa embeddingów. Nie zmierzono jakości odpowiedzi modelu.
- Naprawy OpenAI sprawdzono lokalnymi testami, bez live API. Brak dowodu
  działania konkretnego modelu na koncie użytkownika. Prompt nie jest
  formalną gwarancją ugruntowania ani odporności na prompt injection.
- Ceny i zgłoszenia są fikcyjne. Nie ma bazy spraw, wysyłki email ani zwrotów.
  Podana data musi spełnić politykę; bez daty zgłoszenie nie potwierdza uprawnienia.
- Limit globalny chroni mały serwer, ale inni odwiedzający mogą go wyczerpać.
  Brak klastra, trwałości, SLA i testów obciążeniowych.
- Aktualizacja zależności nie jest pełnym skanem CVE ani formalnym pentestem.

## Ocena przed / po (1–10)

| Obszar | Przed | Po | Uzasadnienie |
|---|---:|---:|---|
| Sens i zakres | 6 | 7 | Dwa konkretne typy spraw, pełny przykładowy zwrot |
| Poprawność | 3 | 7 | Naprawione narzędzia/cache/walidacja; LLM bez testu live |
| Kod i architektura | 5 | 7 | Zachowany podział; publiczny adapter oddzielony od płatnych usług |
| Testy | 2 | 7 | Scenariusze, regresje i rzeczywiste HTTP zamiast samego kontekstu |
| Demo | 1 | 8 | Przeglądarka, przykłady, źródła, trasa, reset i komunikaty |
| Odtwarzalność | 3 | 8 | JAR, build/test w Dockerze, Compose i instrukcje |
| Dowód umiejętności backend | 4 | 7 | Kontrakty API, stany, limity, izolacja i testy; celowo mały zakres |

## Prezentacja 2–3 minuty

1. Otwórz HTTP 401, pokaż trasę TECH i źródło X-API-Key.
2. Zapytaj o Pro; pokaż przełączenie na BILLING i dane z narzędzia.
3. Nowa rozmowa → refund → przykładowy email → powód. Pokaż zbieranie
   danych i DEMO-ID. Wyjaśnij, że nic nie zostało faktycznie wysłane.
4. Pokaż pytanie poza zakresem, reset i kod testów.

Tematy techniczne: protokół assistant/tool i regresja; izolacja i czas życia
sesji z limitami zasobów; różnica między wyszukiwaniem słów, embeddingami
i generacją oraz unieważnianie cache po zmianie dokumentacji.

Źródło protokołu: [oficjalna dokumentacja OpenAI](https://developers.openai.com/api/docs/guides/function-calling).
