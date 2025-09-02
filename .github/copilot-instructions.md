Ten projekt jest to praca magisterka której celem jest stworzenie bezpiecznej aplikacji webowej do zarządzania danymi osobowymi. Aplikacja będzie wykorzystywać najnowsze technologie i najlepsze praktyki w zakresie bezpieczeństwa, aby zapewnić ochronę danych użytkowników.

Używam postgresa w wersji 16

Projekt to: **Projekt i implementacja aplikacji do zarządzania elektroniczną dokumentacją medyczną (EMR) – analiza zagrożeń i podatności**

Chce żebyś pomógł mi z tym projektem, odpowiadając na pytania dotyczące bezpieczeństwa aplikacji webowych, najlepszych praktyk w zakresie bezpieczeństwa, analizy zagrożeń i podatności, a także pomagając w implementacji funkcji związanych z bezpieczeństwem.
Ma być to wystarczająco dobre i bezpieczne żeby wystarczyło na pracę dyplomową i żeby można było to wdrożyć w rzeczywistości.

# Copilot – Guidelines for Generating Unit Tests (Java 21, Spring Boot, JUnit 5, Mockito)

Dokument ten definiuje precyzyjne wytyczne, według których Copilot ma generować testy jednostkowe w naszym projekcie Spring Boot. Celem jest uzyskanie: spójności stylu, wysokiej czytelności, pokrycia wszystkich istotnych ścieżek wykonania oraz zgodności z narzędziami jakości (np. SonarQube).

## 1. Zakres (Scope)
Te wytyczne dotyczą wyłącznie testów _jednostkowych_ (warstwa logiki: serwisy, komponenty, helpery).
Nie dotyczą:
- Testów integracyjnych (@SpringBootTest, Testcontainers, pełny kontekst)
- Testów kontraktowych, e2e
- Testów kontrolerów z MockMvc (chyba że wyraźnie o nie poproszono – wtedy osobny plik)

## 2. Cel
Zapewnić:
- Deterministyczność i izolację
- Jasne DSL testowy (Given / When / Then)
- Łatwe rozszerzanie przy zmianach logiki
- Weryfikację zarówno stanu jak i kluczowych interakcji

## 3. Technologie i wersje
- Java: 21+
- JUnit: JUnit 5 (Jupiter)
- Mockito: z użyciem `@ExtendWith(MockitoExtension.class)`
- AssertJ: wyłącznie (dla spójności asercji)
- Brak użycia przestarzałych runnerów (`@RunWith`)
- Nie dodawać nowych zależności bez uzasadnienia

## 4. Struktura i lokalizacja
- Testy trafiają do: `src/test/java/...`
- Struktura pakietów odzwierciedla `src/main/java`
- Nazwa klasy testowej: `<NazwaTestowanejKlasy>Test` (np. `ProjectServiceTest`)
- Każda klasa testowa powinna być w osobnym pliku
- Generowana klasa testowa powinna zawierać pełną strukturę pakietu i wszystkie wymagane importy, ale nie dodawać nieużywanych importów. (chyba że propmt dotyczył tylko fragmentu klasy np. dodania testów tylko dla jednej nowej metody)

## 5. Styl i Format AAA / Given-When-Then
Każda metoda testowa MUSI zawierać sekcje:
```
// Given
... przygotowanie danych i mocków

// When
... wywołanie metody testowanej

// Then
... asercje i verify()
```
Oddziel sekcje pustą linią. Nie mieszaj przygotowania i asercji.

## 6. Nazewnictwo metod testowych
Konwencja: `methodName_stateUnderTest_expectedOutcome`
Przykłady:
- `getAllProjects_adminUser_returnsProjects`
- `deleteProject_nonExistingId_throwsResourceNotFoundException`
- `calculatePrice_discountApplied_returnsReducedValue`

Unikaj:
- Skrótów niezrozumiałych
- Nazw typu `testGetAll()` lub `shouldReturnProjects()`

## 7. Zawartość testów (Scenariusze)
Dla **każdej PUBLICZNEJ** metody w testowanej klasie, **muszą** zostać wygenerowane testy pokrywające co najmniej:
1.  **Happy Path**: Poprawne wykonanie dla typowych, prawidłowych danych.
2.  **Obsługa błędów**: Każdy jawnie rzucany wyjątek (np. `ResourceNotFoundException`, `IllegalArgumentException`) musi mieć dedykowany test.
3.  **Edge cases**: Wartości graniczne, puste kolekcje, `null` (jeśli logika na to pozwala).
4.  **Wszystkie kluczowe ścieżki warunkowe**: Każda gałąź `if/else` lub `switch`, która prowadzi do znacząco różnego zachowania (np. wywołania innego serwisu, innej modyfikacji danych), musi być przetestowana osobno.

**Checklista dla Copilota**: Przed zakończeniem generowania, sprawdź, czy wszystkie publiczne metody z pliku mają co najmniej jeden test. Jeśli nie, dodaj brakujące.

## 8. Konwencje językowe
- Kod testów po angielsku (nazwy klas, metody, zmienne)
- Komentarze i opisy w testach po angielsku (krótkie, rzeczowe)

## 9. Zaległości / Błędy w istniejących testach
Jeżeli generujesz nowe testy i istnieje podobny scenariusz – NIE duplikuj go; dobuduj brakujące przypadki.

## 10. Konfiguracja Mockito
- Używaj:
```java
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;
}
```
- Nie używaj `MockitoAnnotations.openMocks(this)` jeśli korzystasz z rozszerzenia
- Nie używaj statycznych `MockitoJUnitRunner`

## 11. Mockowanie i Stubbing
- Używaj `when(...).thenReturn(...)` lub `when(...).thenThrow(...)`
- Dla void + efekt uboczny: `doNothing().when(mock).method(...)` tylko jeśli konieczne
- Nie stubuj wywołań, które nie są używane
- Unikaj nadmiernego mockowania (mockuj tylko zewnętrzne zależności testowanej klasy)

## 11a. Unikaj `any(MyGeneric.class)` - używaj inferencję z `Mockito.<MyGeneric<...>>any()`

```java
when(repo.findAll(Mockito.<Specification<Entity>>any(), any(Pageable.class)))
    .thenReturn(Page.empty());

when(service.process(Mockito.<List<MyDto>>any()))
    .thenReturn(result);

when(cache.putAll(Mockito.<Map<String, List<Long>>>any()))
        .thenReturn(true);
```

## 12. Asercje (AssertJ)
- `assertThat(actual).isEqualTo(expected);`
- Wyjątki:
    - Preferuj: `assertThatThrownBy(() -> service.call()).isInstanceOf(MyException.class).hasMessageContaining("...");`
    - Jeśli wymagane przez styl: `assertThrows(MyException.class, () -> service.call());`
- Kolekcje: `assertThat(list).isNotEmpty().hasSize(2).containsExactlyInAnyOrder(a, b);`, `assertThat(actual).containsEntry(key, value)`
- Wartości logiczne / null: `assertThat(obj).isNotNull();`

Nie używaj mieszaniny `Assertions.assertEquals` / `org.junit.Assert.*`

## 13. Weryfikacja interakcji (Mockito verify)
W sekcji `// Then` po asercjach stanu, **zawsze weryfikuj kluczowe interakcje**.

- **Potwierdzenie wywołania**: Użyj `verify(mock).method(...)`, aby upewnić się, że kluczowa operacja (np. zapis, usunięcie, wysłanie notyfikacji) miała miejsce.
- **Weryfikacja liczby wywołań**: Jeśli metoda może być wywołana wielokrotnie w pętli lub wcale, użyj `verify(mock, times(n)).method(...)`. Jest to krytyczne w testach metod usuwających zasoby powiązane.
- **Weryfikacja braku wywołania**: Użyj `verify(mock, never()).method(...)`, aby upewnić się, że określona ścieżka kodu **nie została** wykonana (np. wysłanie maila przy błędzie walidacji).
- **Ograniczenie interakcji**: Używaj `verifyNoMoreInteractions(...)` na koniec sekcji `Then`, aby zagwarantować, że nie wystąpiły żadne nieoczekiwane efekty uboczne. Jest to szczególnie ważne w testach, gdzie logika warunkowa jest złożona.

**Przykład (metoda `delete`):**
```java
@Test
void deleteEnvironment_withDependencies_deletesAllRelatedResources() {
    // Given
    // ... setup z powiązanymi serwisami, produktami etc.
    when(serviceService.getAllServices(...)).thenReturn(Page.forResult(List.of(service1, service2)));

    // When
    environmentService.deleteEnvironment(1, 1);

    // Then
    // ... inne asercje
    verify(locationService).deleteByEnvironment(any());
    verify(productService).deleteByEnvironment(any());
    verify(resourceCredentialService, times(2)).deleteResourceCredential(anyInt(), anyInt()); // Dwa, bo dla produktu i lokalizacji
    verify(serviceService, times(2)).deleteService(anyInt(), anyInt()); // Dwa, bo znaleziono dwa serwisy
    verify(privilegeService).deletePrivilegeByResource(anyInt(), any(), anyInt());
    verify(environmentRepository).save(any()); // Zapis encji z flagą usunięcia
}
```
## 14. ArgumentCaptor
Używaj do sprawdzenia transformacji danych:
```java
ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
verify(projectRepository).save(captor.capture());
assertThat(captor.getValue().getName()).isEqualTo("Demo");
```
Nie używaj jeśli zwykłe `verify(mock).method(expectedArg)` wystarcza.

## 14a. Weryfikacja dynamicznych argumentów (ArgumentCaptor)
Używaj `ArgumentCaptor` nie tylko do sprawdzania transformacji encji, ale również do weryfikacji **dynamicznie budowanych obiektów przekazywanych do mocków**.

**Kluczowe zastosowania:**
- **Spring Data `Specification`**: Gdy logika serwisu warunkowo buduje zapytanie (`spec.and(...)`), **zawsze** użyj `ArgumentCaptor<Specification>`, aby zweryfikować, czy do repozytorium trafiła poprawna specyfikacja dla danego scenariusza (np. inna dla admina, inna dla użytkownika).
- **Obiekty DTO/Request**: Gdy obiekt jest tworzony i wypełniany wewnątrz testowanej metody przed przekazaniem go do innej zależności.

## 14b. Używaj `@Captor` na polu klasy testowej zamiast ręcznego `ArgumentCaptor.forClass(...)` gdy typ jest generyczny

```java
@Captor
private ArgumentCaptor<Comparator<MyEntity>> comparatorCaptor;

@Captor
private ArgumentCaptor<Specification<EnvironmentData>> specCaptor;

// Przykład użycia w teście
verify(repo).save(comparatorCaptor.capture());
Comparator<MyEntity> captured = comparatorCaptor.getValue();
```

**Przykład (Specification):**
```java
@Test
void getAllEnvironments_nonAdminUser_filtersByPrivileges() {
    // Given
    // ... setup
    ArgumentCaptor<Specification<EnvironmentData>> specCaptor = ArgumentCaptor.forClass(Specification.class);

    // When
    environmentService.getAllEnvironments(initialSpec, userId, UserRole.USER, pageable);

    // Then
    verify(environmentRepository).findAll(specCaptor.capture(), eq(pageable));
    Specification<EnvironmentData> capturedSpec = specCaptor.getValue();
    assertNotNull(capturedSpec);
    assertNotSame(initialSpec, capturedSpec); // Sprawdzenie, że specyfikacja została zmodyfikowana dla użytkownika
    assertThat(capturedSpec).isNotNull().isNotEqualTo(initialSpec);
}
```

## 15. Parametryzowane testy
Dla wielokrotnych podobnych przypadków:
```java
@ParameterizedTest
@CsvSource({
    "100, 0.10, 90",
    "200, 0.25, 150"
})
void calculatePrice_withDiscounts_returnsReducedValue(
        BigDecimal base, BigDecimal discount, BigDecimal expected) {

    // Given
    PriceCalculator calculator = new PriceCalculator();

    // When
    BigDecimal result = calculator.calculate(base, discount);

    // Then
    assertThat(result).isEqualByComparingTo(expected);
}
```
Nie parametryzuj testów wyjątków jeśli czytelność spada.

## 16. Atomiczność
Jeden test = jeden aspekt funkcjonalności.
Zasady:
- Nie łącz dwóch różnych wyjątków w jednym teście
- Nie sprawdzaj kilku niezależnych kolekcji w jednym teście – rozbij

## 17. Czytelność i Dokumentacja
- Krótkie komentarze w sekcjach:
```java
// Given: existing project in repository
// When: deleting by id
// Then: repository.deleteById called
```
- Unikaj komentarzy oczywistych (`// call method`)

## 18. Tworzenie obiektów domenowych
- (Zgodnie z wytycznymi) Używaj setterów zamiast konstruktorów – chyba że klasa jest immutable (wtedy konstruktor/builder)
- Przykład:
```java
Project project = new Project();
project.setId(1L);
project.setName("Test");
```
Jeśli istnieją fabryki / buildery testowe – użyj ich zamiast ręcznych setterów (lepsza spójność). Jeżeli w przyszłości wprowadzimy buildery – zaktualizujemy dokument.

## 19. Dane testowe
- Używaj czytelnych wartości: `"Project A"`, `"user@example.org"`
- Daty / czas:
    - Preferuj wstrzykiwany `Clock`
    - Jeśli klasa używa `LocalDateTime.now()`, rozważ refactor (ale test jednostkowy NIE powinien zmuszać do sleep)
- Liczby graniczne: 0, 1, max/ min (jeśli sensowne)
- Jeśli tworzene danych się powtarza – rozważ użycie metody pomocniczej (ale nie ukrywaj logiki w metodach util, które są trudne do zrozumienia)

## 20. Pokrycie ścieżek (Control Flow)
Testy powinny odwzorować wszystkie gałęzie:
- Bloki `if/else`
- Warunki w pętlach
- Segmenty w `switch`
- Bloki `try/catch` (osobne testy potwierdzające złapanie i propagację / konwersję wyjątku)

Nie celem jest 100% coverage kosztem czytelności – celem pokrycie semantycznych wariantów.

## 21. Czego unikać
- `System.out.println` w testach
- Milczące puste `catch` (łap i asertywnie weryfikuj)
- Magic numbers – nadawaj nazwy (konst lub lokalna zmienna)
- Mockowanie własnych prostych POJO
- Nadmiarowe `verify` (każde wywołanie – tylko kluczowe)

## 22. SonarQube / Jakość
- Brak duplikacji kodu setup – ale unikaj nadmiernego ukrywania logiki w metodach util testowych
- Brak nieużywanych importów / mocków
- Brak ignorowanych testów bez TODO (Jeśli `@Disabled`, to z uzasadnieniem)

## 23. Spójność stylu
Jeśli istnieją już testy – dopasuj stylem:
- Formatowanie (odstępy, organizacja importów)
- Sposób weryfikacji (np. czy stosujemy `assertThatThrownBy`)

## 24. Specification (Spring Data JPA)
Nie używaj przestarzałych metod (np. jeśli w dokumentacji użyto niepoprawnie `Specification.when()` – KOREKTA: stosuj `Specification.where(...)`, `Specification.not(...)` zgodnie z aktualnym API).
Jeśli potrzebna negacja: `Specification.not(<spec>)`.
Upewnij się, że generowany kod nie używa metod oznaczonych jako deprecated.

(W razie niejasności doprecyzuj przy generowaniu promptem: "Use modern Spring Data JPA Specification API without deprecated methods.")

## 25. Checklist (Copilot Quick Rules)
Copilot MUST:
1. Use JUnit 5 + Mockito + AssertJ
2. Apply Given / When / Then comments
3. Name tests `method_condition_result`
4. Mock external dependencies, inject with `@InjectMocks`
5. Use AssertJ everywhere
6. Cover: happy path, edge, errors, branches
7. Verify key interactions
8. Use ArgumentCaptor only when needed
9. Use setters to build domain objects (unless immutable)
10. Ensure exception tests assert type + message (jeśli logicznie znaczące)
11. Avoid unused stubs / mocks
12. Keep each test atomic
13. Add short English intent comments
14. Avoid deprecated API (Specifications, old JUnit)
15. Keep code clean (no unused imports, no println)

## 26. Przykład – Serwis (Happy Path)
```java
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void getAllProjects_adminUser_returnsProjects() {
        // Given: repository returns two projects
        Project p1 = new Project();
        p1.setId(1L);
        p1.setName("Alpha");

        Project p2 = new Project();
        p2.setId(2L);
        p2.setName("Beta");

        when(projectRepository.findAll()).thenReturn(List.of(p1, p2));

        // When: fetching all projects
        List<Project> result = projectService.getAllProjects(true);

        // Then: list contains both projects and no notifications sent
        assertThat(result)
            .isNotNull()
            .hasSize(2)
            .extracting(Project::getName)
            .containsExactlyInAnyOrder("Alpha", "Beta");

        verify(projectRepository).findAll();
        verify(notificationService, never()).notify(any());
        verifyNoMoreInteractions(projectRepository, notificationService);
    }
}
```

## 27. Przykład – Wyjątek
```java
@Test
void deleteProject_nonExistingId_throwsResourceNotFoundException() {
    // Given: repository does not find project
    Long id = 999L;
    when(projectRepository.findById(id)).thenReturn(Optional.empty());

    // When / Then: exception thrown
    assertThatThrownBy(() -> projectService.deleteProject(id))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Project", String.valueOf(id));

    verify(projectRepository).findById(id);
    verify(projectRepository, never()).delete(any());
    verifyNoMoreInteractions(projectRepository);
}
```

## 28. Przykład – ArgumentCaptor
```java
@Test
void createProject_validInput_savesTransformedEntity() {
    // Given
    ProjectRequest request = new ProjectRequest();
    request.setName(" Demo ");
    request.setDescription(" Desc ");

    Project saved = new Project();
    saved.setId(10L);
    saved.setName("Demo");
    saved.setDescription("Desc");

    when(projectRepository.save(any(Project.class))).thenReturn(saved);

    // When
    Project result = projectService.createProject(request);

    // Then
    assertThat(result.getId()).isEqualTo(10L);

    ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
    verify(projectRepository).save(captor.capture());
    Project captured = captor.getValue();
    assertThat(captured.getName()).isEqualTo("Demo");
    assertThat(captured.getDescription()).isEqualTo("Desc");
    verifyNoMoreInteractions(projectRepository);
}
```

## 29. Przykład – Parametryzowany Edge Case
```java
@ParameterizedTest
@CsvSource({
    "0, 0",
    "1, 1",
    "5, 120"
})
void factorial_validInput_returnsExpected(int input, int expected) {
    // Given
    MathService mathService = new MathService();

    // When
    int result = mathService.factorial(input);

    // Then
    assertThat(result).isEqualTo(expected);
}
```

## 30. Przykład – Pokrycie ścieżek warunkowych
Załóżmy metodę:
```java
// In service:
// if (enabled) repository.enable(id); else repository.disable(id);
```
Testy:
- `toggleFeature_enabledTrue_callsEnable`
- `toggleFeature_enabledFalse_callsDisable`

### 31. Audit Fields (Mandatory Assertions)
Dla metod modyfikujących stan encji (create/update/delete) testy MUSZĄ weryfikować pola audytowe:
- Create:
    - assertThat(entity.getAuditCd()).isNotNull();
    - assertThat(entity.getAuditCu()).isEqualTo(userId);
- Update:
    - poprzednie auditCd pozostaje bez zmian
    - auditMd != null, auditMu == userId
    - (opcjonalnie) auditMd jest po auditCd jeśli używamy wstrzykniętego Clock
- Delete (soft delete):
    - auditRd != null, auditRu == userId
- Brak nieoczekiwanego wyzerowania istniejących pól

## 32. Generuj cały kod klasy testu jednostkowego
- poprawny pakiet
- wszystkie wymagane importy
- nie dodawaj nieużywanych importów