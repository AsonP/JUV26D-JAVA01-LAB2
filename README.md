# 💰 Budgethanteraren

Konsolapplikation i Java för att registrera inkomster och utgifter, kategorisera
dem, spara och läsa dem från fil samt visa en sammanställning av ekonomin.

---

## Köra programmet

Kräver Java 17 eller senare. Utvecklat med JDK 27, kompilerat mot språknivå 25.

Kör `budgethanteraren.Main` från IntelliJ med VM-flaggan:

```
-Djava.util.logging.config.file=src/main/resources/logging.properties
```

Utan flaggan fungerar programmet, men loggningen faller tillbaka på JUL:s
standard och skrivs till konsolen istället för till fil.

Datafilen är `data/transaktioner.csv` och skapas automatiskt om den saknas.
Loggfilen hamnar i `logs/budgethanteraren.log`; mappen `logs` måste finnas
eftersom `FileHandler` inte skapar kataloger.

Tester körs med `mvn test` eller direkt i IntelliJ.

---

## Arkitektur

| Klass | Ansvar |
|---|---|
| `Main` | Startpunkt. Kopplar ihop lagren, inget annat. |
| `Menu` | Användargränssnitt: meny, inmatning, utskrifter. |
| `BudgetService` | Affärslogik: saldo, summering, filtrering. |
| `Repository<T>` | Generiskt lager över `ArrayList<T>`. |
| `Transaction` | Record med validering i kompakt konstruktor. |
| `TransactionType` | Enum som bär sitt eget tecken (+1 / −1). |
| `CsvTransactionStore` | Läser och skriver CSV. |
| `InvalidTransactionException` | Unchecked — ogiltig indata. |
| `FileFormatException` | Checked — trasig rad i filen. |

Beroendena går bara nedåt: `Menu` → `BudgetService` → `Repository` →
`Transaction`. Affärslogiken känner inte till vare sig `Scanner` eller filer,
vilket är förutsättningen för att kunna enhetstesta den utan att simulera
inmatning.

### Centralt designbeslut

Beloppet lagras **alltid positivt**. Riktningen bärs av `TransactionType`, som
själv känner till sitt tecken. Det gör kravet "inget negativt belopp för en
utgift utan att markeras som sådan" till en invariant i modellen istället för en
regel i gränssnittet — garantin gäller även för data som läses från fil.

`Transaction.signedAmount()` innehåller därför ingen `if`-sats om typen, utan
frågar enumen: `type.applySign(amount)`. Skulle en tredje typ tillkomma behöver
ingen annan klass ändras.

### Två sorters undantag

`InvalidTransactionException` är unchecked. Det är delvis ett medvetet val —
ogiltigt belopp är ett programmeringsfel snarare än en förväntad händelse — men
också ett tekniskt tvång: undantaget kastas från den kompakta konstruktorn i ett
record, och en sådan får inte ha någon `throws`-deklaration.

`FileFormatException` är checked. En trasig rad i en fil är precis vad checked
undantag finns till för: något som kan gå fel trots att koden är korrekt, och
där anroparen måste ta ställning till vad som ska hända.

---

## Testning

| Testklass | Tester | Täcker |
|---|---|---|
| `RepositoryTest` | 3 | Tillägg, null-hantering, defensiv kopiering |
| `BudgetServiceTest` | 4 | Saldo, summering per kategori, filtrering |
| `CsvTransactionStoreTest` | 4 | Round-trip, saknad fil, trasig rad, tomt fält |

Alla följer Arrange-Act-Assert. Både normalfall och gränsfall testas: tom lista,
noll transaktioner, gränsvärden på belopp, och transaktioner som ligger exakt på
gränsdagarna i ett datumintervall.

`assertThrows` används mot båda egna undantagen samt mot
`IllegalArgumentException` vid försök att lagra null.

`CsvTransactionStoreTest` använder JUnits `@TempDir`, som ger varje testmetod en
egen tom katalog. Det är möjligt eftersom `CsvTransactionStore` tar emot sin
`Path` via konstruktorn — hade sökvägen varit hårdkodad i klassen hade testerna
skrivit i det riktiga datakatalogen och stört varandra.

### Mutationstestning

Ett grönt test bevisar ingenting om man aldrig sett det bli rött. Varje test har
därför verifierats genom att medvetet bryta koden det testar, köra om, och
återställa.

| Mutation | Utfall |
|---|---|
| `items.add(item)` borttagen i `Repository.add` | Rött — dödad |
| `findAll()` returnerar `items` istället för en kopia | Rött — dödad |
| `.sum()` → `.max().orElse(0)` i `balance()` | Rött — dödad |
| `signedAmount` → `amount` i `sumPerCategory()` | Rött — dödad |
| `==` → `!=` i `findByType()` | Rött — dödad |
| `Locale.ROOT` → `Locale.of("sv","SE")` i `toLine()` | Rött — dödad |
| `split(DELIMITER, -1)` → `split(DELIMITER)` | **Grönt — överlevde** |

Två av dessa förtjänar en kommentar.

**`.sum()` → `.max()`** avslöjade att det första saldotestet var värdelöst i
isolering. Med en tom lista är summan noll och maximum noll — testet kunde inte
skilja en korrekt saldoberäkning från något helt annat. Ett andra test med
faktiska transaktioner (30 000 in, 500 ut) löste det: den muterade versionen ger
då 30 000 istället för 29 500.

**Den överlevande mutationen** är ett ärligt fynd. Båda varianterna kastar
`FileFormatException` på en rad med tomt sista fält, men av olika skäl: med `-1`
passerar längdkontrollen och `TransactionType.valueOf("")` kastar; utan `-1`
slänger `split` bort det tomma fältet och längdkontrollen slår till direkt.
Testet kontrollerar bara *att* undantaget kastas, inte varför.

`-1` behålls ändå. Det är rätt beteende när flera fält i slutet är tomma, och
felmeddelandet blir mer korrekt — "fel antal fält" är missvisande när fälten
finns men är tomma.

**Lärdom:** `assertThrows` på enbart undantagstypen är ett trubbigt instrument.
Två olika kodvägar kan ge samma undantag, och testet ser ingen skillnad.

---

## Buggar och felsökning

### 1. Loggfilen dränktes i brus från JVM:ens interna klasser

**Symptom.** Efter att loggningen kopplats in blev `budgethanteraren.log` flera
tusen rader lång på någon minut. Programmets egna rader fanns där, men begravda
bland hundratals rader per sekund av den här typen:

```
FINE [sun.rmi.transport.tcp] RMI TCP Connection(1)-192.168.68.103: op = 82
FINE [sun.rmi.loader] name = "javax.management.ObjectName", codebase = ""
FINE [javax.management.remote.rmi] connectionId=rmi://..., attribute=HeapMemoryUsage
```

**Upptäckt.** Vid genomläsning av loggen efter en testkörning, i syfte att
kontrollera att alla fyra loggnivåer faktiskt förekom. Programmet fungerade
felfritt — buggen fanns bara i loggen, och hade inte upptäckts om filen aldrig
lästs.

**Felsökning.** Första hypotesen var att något i applikationen öppnade en
nätverksanslutning, men ingenting i koden gör det. Nyckeln låg i klassnamnen
inom hakparentes: `sun.rmi.*` och `javax.management.*` tillhör JDK:n, inte
projektet.

Kontroll av kommandoraden som IntelliJ skriver överst i konsolen visade
`-javaagent:idea_rt.jar`. IntelliJ kopplar in en övervakningsagent som pollar
CPU-last och heap via JMX ungefär en gång per sekund — precis den frekvens
raderna hade.

Därifrån gick orsaken att härleda till en enda rad i `logging.properties`:

```properties
.level = FINE
```

**Orsak.** Den raden sätter nivån på *root-loggern*. I `java.util.logging` bildar
loggers ett träd efter paketnamn — `budgethanteraren.Menu` ärver från
`budgethanteraren`, som ärver från root. `.level = FINE` sänkte alltså tröskeln
för hela JVM:en, inklusive JDK:ns interna klasser och IntelliJs agent.

**Åtgärd.** Root sattes till INFO, och FINE begränsades till det egna paketet:

```properties
.level = INFO
budgethanteraren.level = FINE
```

`FileHandler.level` behölls på FINE. Handlern har ett eget filter oberoende av
loggerns, så båda portarna måste vara öppna för att en FINE-rad ska nå filen.
Sätts handlern till INFO försvinner DEBUG-raderna trots att loggern släppt igenom
dem.

Resultatet blev en logg på ett tjugotal rader per session, där hela förloppet går
att följa:

```
11:23:00 FINE    [budgethanteraren.Main] Startar med datafil: data\transaktioner.csv
11:23:00 INFO    [budgethanteraren.Menu] Applikationen startad.
11:23:24 WARNING [budgethanteraren.Menu] Ogiltigt belopp avvisat: -30000
11:23:57 FINE    [budgethanteraren.BudgetService] Transaktion tillagd: Transaction[...]
11:25:12 INFO    [budgethanteraren.CsvTransactionStore] Sparade 3 transaktioner
```

**Lärdom.** En loggkonfiguration som är för generös är inte bara ineffektiv —
den förstör loggens syfte helt. Information som inte går att hitta är lika
oanvändbar som information som aldrig skrevs.

### 2. Onåbar loggning i undantagshanteringen

**Symptom.** Trots upprepade felaktiga inmatningar dök inga WARNING-rader upp i
loggen. INFO och FINE fungerade.

**Felsökning.** `logger.warning(...)` fanns i catch-blocket i
`Menu.addTransaction`, som fångar `InvalidTransactionException`. Genomgång av
anropskedjan visade varför den aldrig kördes: inläsningsmetoderna
(`readAmount`, `readDate`, `readType`) fångar själva felaktig inmatning och
frågar om i en loop. De släpper alltså aldrig igenom ogiltiga värden till
`Transaction`-konstruktorn, och undantaget kastas aldrig.

**Orsak.** Loggningen satt på rätt begreppsnivå men fel plats i koden —
catch-blocket är ett skyddsnät som i praktiken aldrig nås.

**Åtgärd.** WARNING-loggning flyttades till inläsningsmetoderna, där felen
faktiskt uppstår, och med det värde användaren matade in:

```java
catch (NumberFormatException e) {
    logger.warning("Ogiltigt tal avvisat: " + input);
    System.out.println("Ogiltigt tal. Exempel: 249,50");
}
```

Catch-blocket i `addTransaction` behölls som skyddsnät, men är inte längre den
enda platsen där ogiltig inmatning syns.

**Lärdom.** Att loggningen "finns" är inte samma sak som att den körs. Frånvaro
av förväntade loggrader är i sig ett felsökningsspår värt att följa.

### 3. Locale gjorde filformatet maskinberoende

**Symptom.** Inget — buggen upptäcktes genom att tänka igenom konsekvenserna av
en observation, inte genom att något gick sönder.

**Observation.** Utvecklingsmaskinen kör engelsk Windows, och `printf` skrev
saldot som `0.00` med punkt trots att gränssnittet i övrigt är svenskt. `printf`
använder systemets locale, så på en svensk maskin hade samma kod gett `0,00`.

**Analys.** För utskrifter är skillnaden kosmetisk. För **filen** är den
allvarlig: en CSV skriven på en svensk maskin innehåller `249,50`, vilket
`Double.parseDouble` inte kan läsa på en engelsk maskin. Filen hade blivit
oläsbar vid flytt mellan datorer — och eftersom läraren rimligen kör svensk
Windows hade det kunnat slå till vid rättningen.

**Åtgärd.** `CsvTransactionStore.toLine()` formaterar med `Locale.ROOT`, vilket
garanterar punkt oavsett maskin. Vid inmatning accepteras både komma och punkt
genom `input.replace(',', '.')`.

Principen: **tillåtande inåt, strikt utåt.**

**Verifiering.** Att detta inte bara är en försiktighetsåtgärd visades genom
mutation — byte till `Locale.of("sv","SE")` gör round-trip-testet rött, eftersom
filen då skrivs med komma och inte går att läsa tillbaka. Just round-trip-testet
fångar det; ett test av enbart skrivning eller enbart läsning hade missat det.

**Lärdom.** Detta är en bugg som inte hade kunnat upptäckas genom att testa på
den egna maskinen, hur noggrant man än testar. Den uppstår först när filen byter
miljö.

### 4. Gammal kod kördes trots ändrad källkod

**Symptom.** Menyval 1 skrev `[TODO] Lägg till transaktion` trots att switchen
anropade `addTransaction()`. Källkoden var korrekt, beteendet var fel.

**Felsökning.** Switchen kontrollerades — korrekt. Metoden fanns — korrekt.
`Rebuild Project` löste det tillfälligt, men efter omstart av IntelliJ visade
samtliga klasser kaffekoppsikon i projektträdet, vilket betyder att de ses som
fristående Java-filer utan modultillhörighet.

**Orsak.** Kopplingen till Maven-projektet hade tappats, så de kompilerade
`.class`-filerna matchade inte längre källkoden.

**Åtgärd.** *Sync All Maven Projects* i Maven-panelen.

**Lärdom.** När beteendet inte matchar källkoden är byggsteget värt att
misstänka innan man letar vidare i logiken. Ikonerna i projektträdet säger något
om projektets tillstånd.

---

## Loggning

`java.util.logging` används, utan externa beroenden. Nivåerna mappar mot
uppgiftens benämningar så här:

| Uppgiftens nivå | JUL | Används när | Exempel i koden |
|---|---|---|---|
| ERROR | `SEVERE` | Fel som stoppar en operation | Trasig rad i datafilen |
| WARNING | `WARNING` | Oväntat men hanterat | Ogiltig inmatning avvisad |
| INFO | `INFO` | Normala händelser | Start, inläsning, sparning |
| DEBUG | `FINE` | Detaljer för felsökning | Varje tolkad rad, varje tillagd transaktion |

Fem klasser har egna loggers: `Main`, `Menu`, `BudgetService`, `Repository` och
`CsvTransactionStore`. Modellklasserna och undantagen har inga — de är
värdeobjekt utan beteende värt att logga, och ett undantag loggas av den som
fångar det, inte av sig självt.

Att transaktioner loggas på FINE och inte INFO är medvetet: i normal drift vore
en rad per transaktion rent brus, men vid felsökning är det precis vad som
behövs.

Loggen skrivs till fil, inte till konsolen. I en konsolapplikation konkurrerar
annars loggen med användargränssnittet om samma yta, och en logg som stör
användaren blir avstängd — vilket gör den värdelös just när den behövs.

**Noterat:** en trasig rad ger två SEVERE — en från `parseLine`, som vet *vilken*
rad som är trasig, och en från `loadFromFile`, som vet *vad som händer härnäst*.
Detta kallas "log and throw" och är omdiskuterat eftersom samma fel kan hamna i
loggen flera gånger. I ett program av den här storleken är det ett medvetet val
som gör loggen mer komplett; i större system brukar man välja ett av de två.

**Noterat:** väljer man 5 och strax därefter `e` syns två identiska "Sparade N
transaktioner". Båda är avsiktliga — uppgiften säger "vid avslut eller efter
varje ändring", och här görs båda.

---

## Reflektion: generics och Stream API

### `Repository<T>`

Alternativen hade varit en `TransactionRepository` som lagrar `Transaction`
direkt, eller — betydligt värre — en som lagrar `Object` och castar vid varje
utplock.

Den generiska varianten ger tre saker:

**Typsäkerhet vid kompilering.** `repository.add("en sträng")` på ett
`Repository<Transaction>` går helt enkelt inte att kompilera. Med `Object` hade
samma misstag upptäckts först vid körning, som en `ClassCastException` på en helt
annan plats i koden än där felet begicks — alltså precis den sortens bugg som är
dyrast att hitta.

**Återanvändbarhet.** Klassen innehåller ingen rad som nämner `Transaction`. Den
skulle utan en enda ändring kunna lagra böcker, användare eller vad som helst.
Det är inte ett teoretiskt argument: samma mönster användes i en tidigare uppgift
för en helt annan domän.

**Ett ställe att ändra på.** Defensiv kopiering, null-kontroll och loggning finns
samlat. Hade varje domäntyp haft sitt eget repository hade logiken dubblerats,
och varje rättning behövt göras flera gånger — med risk att en glöms.

Priset är att typinformationen försvinner vid körning, så kallad type erasure.
Därför heter loggern `Repository.class` och inte `Repository<Transaction>.class`,
och därför går det inte att skriva `instanceof Repository<Transaction>`. Det
finns bara en `Repository`-klass i JVM:en oavsett hur många typer den används
med. I det här programmet spelar det ingen roll, men det är värt att känna till.

### Stream API

För saldoberäkningen är skillnaden mot en loop liten:

```java
// Stream
return repository.findAll().stream()
        .mapToDouble(Transaction::signedAmount)
        .sum();

// Loop
double total = 0;
for (Transaction t : repository.findAll()) {
    total += t.signedAmount();
}
return total;
```

Den blir tydlig i `sumPerCategory()`:

```java
return repository.findAll().stream()
        .collect(Collectors.groupingBy(
                Transaction::category,
                TreeMap::new,
                Collectors.summingDouble(Transaction::signedAmount)));
```

Loopversionen kräver en `Map`, en kontroll av om nyckeln redan finns, en
initialisering av värdet och en uppdatering — plus en separat sortering om
kategorierna ska visas i ordning. Fem till sex rader där varje rad är ett
tillfälle att göra fel, särskilt den där ett nytt värde ska initieras till noll.

Streamversionen säger *vad* som ska hända — gruppera på kategori, summera
signerade belopp, håll nycklarna sorterade — istället för *hur* det ska gå till
steg för steg.

`Predicate<T>` i `findWhere` är den andra vinsten, och den hänger ihop med
generics. Repositoryt behöver inte känna till ett enda filtreringsvillkor;
anroparen skickar in sitt eget som en lambda:

```java
repository.findWhere(t -> t.type() == type);
repository.findWhere(t -> !t.date().isBefore(from) && !t.date().isAfter(to));
```

Utan det hade `Repository` behövt en metod per filtreringsbehov, och därmed känt
till `Transaction` — vilket hade förstört hela poängen med att den är generisk.
De två mekanismerna förutsätter alltså varandra.

**Avvägning värd att nämna:** streams är inte alltid tydligare. Den trerads
`groupingBy` ovan tog längre tid att förstå första gången än motsvarande loop
hade gjort. Vinsten ligger inte i att koden är lättare att läsa för en nybörjare,
utan i att den är svårare att skriva *fel* när man väl kan läsa den — och i att
avsikten framgår direkt istället för att behöva härledas ur en loop.
