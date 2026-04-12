# Projet TAS 2026 — Domaines abstraits numériques

## Auteurs
- **Liu YANG**
- **Mickael VILAYVANH**

## Objectif du projet
L’objectif du projet est d’implémenter :

1. **un domaine abstrait numérique non relationnel**
2. **un domaine abstrait numérique relationnel**
3. **leur produit cartésien**

En suivant les spécifications du dépôt LiSA tutorial (`tas2026`), notre groupe a choisi les deux domaines suivants :

- **Non relationnel : intervalles avec prise en compte des overflows**
  - Difficulté : **4**
- **Relationnel : inégalités linéaires entre trois variables**
  - Difficulté : **3**

La difficulté totale est donc de **7**, ce qui respecte les contraintes du projet.

---

## 1. Domaine non relationnel : intervalles avec overflow

### Classe d’implémentation
Ce domaine abstrait non relationnel est implémenté dans :

- `src/main/java/it/unive/lisa/tutorial/IntervalsWithOverflow.java`

Classe de test associée :

- `src/test/java/it/unive/lisa/tutorial/IntervalsWithOverflowTest.java`

Programme IMP de validation :

- `inputs/intervalsoverflow.imp`

Ce domaine implémente :

```java
BaseNonRelationalValueDomain<IntervalsWithOverflow>
```

---

### Objectif du domaine

Ce domaine permet de représenter l’analyse d’intervalles sur les entiers signés 32 bits avec sémantique d’overflow, exactement comme les `int` Java.

Contrairement aux intervalles classiques, il peut représenter les valeurs qui franchissent la borne modulaire des entiers machine.

Par exemple :

```
2147483647 + 1 = -2147483648
```

Ce comportement est modélisé à l’aide d’intervalles circulaires.

---

#### Représentation abstraite
Chaque valeur abstraite possède deux formes :

intervalle standard : `low <= high`
intervalle circulaire : `low > high`

#### Intervalle standard
```
[low, high]
```
Représente tous les entiers entre `low` et `high`.

#### Intervalle circulaire
```
[low, high]
```

Représente :

```
[low, MAX_INT] ∪ [MIN_INT, high]
```

Cela permet de représenter précisément les résultats après overflow.

#### Éléments spéciaux

Le treillis définit également :

- `TOP` : tous les entiers 32 bits
- `BOTTOM` : ensemble vide

---

### Opérations de treillis implémentées

Les opérations suivantes sont implémentées :
- `lessOrEqualAux`
- `lubAux`
- `glbAux`
- `wideningAux`

### Stratégie de widening

Lors de l’analyse des boucles, si une borne continue de croître sans se stabiliser, elle est directement élargie jusqu’aux bornes machine :

- borne inférieure → `Integer.MIN_VALUE`
- borne supérieure → `Integer.MAX_VALUE`

Cela garantit la convergence du calcul de point fixe.

Sémantique abstraite implémentée

---

#### Constantes

Les constantes entières produisent des intervalles singletons.

Exemple :
```
5 → [5,5]
```

---

#### Négation unaire

La négation unaire est précise dans le cas général.

Si l’intervalle contient `Integer.MIN_VALUE`, le cas :

```
-(-2147483648)
```

produit un overflow en Java, donc le domaine retourne conservativement `TOP`.

---

#### Addition

L’addition modulaire modulo 2^32 est implémentée.

Exemple :
```
[MAX_INT, MAX_INT] + [1,1]
```

revient correctement à `MIN_INT`.

Si le résultat couvre tout le cercle des entiers, le résultat devient `TOP`.

---

#### Soustraction

La soustraction suit la même sémantique modulaire que les entiers Java.

Les overflows sont représentés par les intervalles circulaires.

---

#### Multiplication

Pour des intervalles non circulaires sans overflow, la multiplication reste précise.

Si un overflow est possible, le domaine retourne `TOP`.

Par exemple :
```
2147483647 * 2
```

---

#### Division

La division reste précise si :

- le diviseur ne peut pas être nul
- le cas `MIN_INT / -1` n’est pas possible
- les deux opérandes ne sont pas circulaires

Sinon le résultat est `TOP`.

---

#### Raffinement par conditions

Le domaine implémente le raffinement par conditions :

- `==`
- `!=`
- `<`
- `<=`
- `>`
- `>=`

via :
```
assumeBinaryExpression(...)
```

Cela améliore la précision dans les branches conditionnelles et les gardes de boucle.

---

### Résultats de l’analyse sur le programme IMP

Le programme `inputs/intervalsoverflow.imp` contient plusieurs cas de validation.

---

#### Arithmétique de base

Par exemple :
```
def x = 2;
def y = 3;
def z = x * y;
```

produit un intervalle singleton exact.

---

#### Overflow sur addition

Le programme :
```
def x = 2147483647;
def y = x + 1;
```

simule correctement l’overflow Java et revient à `Integer.MIN_VALUE`.

---

#### Overflow sur multiplication

Le programme :
```
def x = 2147483647;
def y = 2;
def z = x * y;
```

retourne `TOP`.

C’est le comportement attendu, car le résultat ne peut pas être représenté précisément par un seul intervalle.

---

#### Analyse de boucle et widening

Le programme de validation contient la boucle suivante :
```
def c = 1;
def b = 0;
while (b < 10)
    b = b + c;
return b;
```

#### À l’intérieur de la boucle

Dans le corps de boucle :
```
b ∈ [1,10]
```

ce résultat est précis.

#### À la sortie

Au point de retour :
```
b ∈ [10,2147483647]
```

La perte de précision ici est normale.

Comme la borne supérieure continue d’augmenter dans la boucle, le widening la promeut directement à `Integer.MAX_VALUE` afin de garantir la convergence.

Ce comportement est cohérent avec le widening classique sur les intervalles.