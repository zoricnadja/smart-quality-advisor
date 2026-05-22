3.2 CEP – Agregacija i praćenje trendova
CEP se koristi u dve faze: Faza 3 (trend pada pH) i Faza 4 (agregacija temperature
dima). Oba CEP mehanizma su ulančana – niže rezolucije hrane više, analogno
ulančavanju agregata u Smart Home primeru.
ID FAZA USLOV AKCIJA TIP
C-1 Faza 4 Prikupljeni svi TempDimEvent
u prozoru od 15 min
Kreira
Aggregated15min_Dim sa
prosekom, min, max
temperature; timestamp
zaokružen na 15 min
Agregacija
C-2 Faza 4 3 uzastopna
Aggregated15min_Dim (= 45
min)
Kreira
Aggregated45min_Dim;
prosek < 60°C okida
NedovoljnaTermickaObrada
(F4-4)
Ulančavanj
e
C-3 Faza 3 Dva uzastopna dnevna pH
unosa
Računa delta_pH =
pH_dan_N – pH_dan_N-1;
ako delta < 0.1 →
TrendPhPresporoOpada
(F3-5)
Trend
detekcija
C-4 Faza 3 Dva uzastopna
TrendPhPresporoOpada
događaja
Generiše
FermentacijaUgrozena (F3
6) – ulančavanje trend
   događaja
   Ulančavanj
   e trenda
   C-5 Faza 5 TempSusareEvent svakih 60
   sek u prozoru od 4h
   Kreira
   Aggregated4h_Susara;
   prosek > 16°C okida
   TrajniTemperaturniRizikSus
   enja (F5-5)
   Agregacija