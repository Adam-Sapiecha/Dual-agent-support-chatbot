# Poprawki publicznego interfejsu — 22.10.2026

Rozmowa i szczegóły odpowiedzi tworzą dwie kolumny, na telefonie jedną.
Usunięto reklamowy nagłówek, numerowane karty, schemat routera i powtarzaną
architekturę. Przykłady są małymi przyciskami; routing, narzędzia i źródła
pozostają widoczne. Rozmowę można przewijać klawiaturą; poprawiono kontrast.
Publiczny tryb pozostaje regułowy, bez wywołań AI, emaili i prawdziwych zwrotów.

19 testów Maven verify przeszło; sprawdzono składnię app.js. Wydanie
`audit-20261022`, `/home/pi/apps/dual-agent-support/current`, `compose.yaml`.
Zaktualizowano tylko usługę web. Ograniczenia żądań, sesji, pamięci JVM,
CPU/PID, kontener bez roota i prywatny loopback pozostały aktywne.
Poprzedni obraz zachowano. Wdrożenie resetuje nietrwałe sesje rozmów.

Zewnętrzne scenariusze i dowody przeglądarkowe:
https://github.com/Adam-Sapiecha/Portfolio-AdamSapiecha/actions/workflows/public-demos.yml.
Każdy przebieg używa nowych sesji, rzeczywistego backendu, walidacji TLS i szerokości
1280/390/360 px. Nie jest to pełna certyfikacja WCAG ani test każdej sieci firmowej.
