Plan Gruppe xx
Mitglieder: Dana Frey, Jonas Graubner, Mika Kuge

Genetischer Algorithmus; Simulated Aneeling
1. 100 Lösungen Initial generieren
2. Agenten müssen 80% akzeptieren (Daraus schnittmenge evtl.)
3. Neues Set mit cross-over Mutation -> 100 Lösungen wieder
4. Agenten müssen 70% akzeptieren
5. Neues set mit cross Over und Mutation (Mutation reduziert)
6. ... repeat 4 & 5
7. Auf den letzten 100 rausstreichverfahren


Agenten Methode
voteLoop: Bekommt vom Mediator ein Array an Lösungen. Return Boolean Array mit seinen akzeptierten Lösungen

voteEnd: Bekommt vom Mediator Lösungen als Array. Gibt Index von der zu entfernen Lösung zurück.