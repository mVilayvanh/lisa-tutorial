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

Les fichiers écrits sont les suivant:

Classes:

[IntervalsWithOverflow.java](src/main/java/it/unive/lisa/tutorial/IntervalsWithOverflow.java)

[LinearInequalitiesAmongThreeVariables.java](src/main/java/it/unive/lisa/tutorial/LinearInequalitiesAmongThreeVariables.java)
    
[IntervalsWithOverflowTest.java](src/test/java/it/unive/lisa/tutorial/IntervalsWithOverflowTest.java)
    
[LinearInequalitiesAmongThreeVariablesTest.java](src/test/java/it/unive/lisa/tutorial/LinearInequalitiesAmongThreeVariablesTest.java)
    
[CartesianProductIntervalLinearInequalitiesTest.java](src/test/java/it/unive/lisa/tutorial/CartesianProductIntervalLinearInequalitiesTest.java)

Tests:

[intervalsoverflow.imp](inputs/intervalsoverflow.imp)

[linearinequalities.imp](inputs/linearinequalities.imp)

[cartesianproductintervalslinearinequalities.imp](inputs/cartesianproductintervalslinearinequalities.imp)

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

## 2. Domaine relationnel : inéquation linéaire parmi 3 variables

On veut décrire les relations tel que x > y + z avec x, y et z des variables du logiciel analysé.

On ne se restreint qu'à ce type d'inégalité, on ne regardera pas les opérateurs
tel que ge, le, lt, eq, neq...
Cela aurait pu être aussi implémenté mais est redondant avec le cas gt.

Afin de définir ce domaine, il fallait déjà définir quel type de treillis on avait affaire.
Ici, comme on veut définir un treillis qui décrit des identifiants et leur relations avec d'autres,
il est évident de dire que c'est un treillis à ordre inversé, en particulier, bottom veut dire qu'on
est plus précis sur les relations et top veut dire qu'on ne connaît rien.

Maintenant que cela est clarifié, on utilise le même principe que pour le not equal vu en cours.
On dit qu'une variable est lié à un ensemble de pair de variable. En particulier, les éléments
dans cet ensemble de pair sont les pairs de variables (y, z) auquels la variable lié à l'ensemble
sont lié par la relation x est plus grand l'assocation de y et z.

On ne décrira pas dans le détail comme le paragraphe 1 pour maintenir ce readme uniquement informatif
de l'implémentation et structure utilisée.

Il faut surtout retenir qu'il n'y a pas de maintenance de widening, uniquement la maintenance
de l'environnement dans lequel les liaisons évoluent. Si une réaffectation d'une des paires
de variables est faite, il faut oublier la paire car l'information n'est plus d'actualité.

Pour l'opération assume, on fait évoluer l'environnement en pour ajouter la liaison
entre une variable et la paire de variable associée.

Pour trouver le plus petit majorant, sans oublié qu'on est en treillis inversé, on dit que plus on se
rapproche de top, moins on en sait donc top est vide, et plus on est vers bottom, plus on détaille
les laisons entre les variables et leur paires. Donc pour agrandir l'environnement,
il faut merge les deux environnement ensemble.

## 3. Le produit cartésien

Pour le produit cartésien, il suffit de regarder dans le package de test,
[CartesianProductIntervalLinearInequalitiesTest.java](src/test/java/it/unive/lisa/tutorial/CartesianProductIntervalLinearInequalitiesTest.java)

On utilise ce qu'on a vu en cours pour associer les deux domaines ensembles. Il fallait
faire attention à ne pas utiliser ValueEnvironment pour le LinearInequalities car c'est un domaine
relationnel est ValueEnvironment ne prend que des domaines relationnels.